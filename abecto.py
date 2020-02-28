"""This is the ABECTO module.

This module provides handy functions to use the ABECTO REST service, hidding the raw HTTP requests.
"""

__version__ = '0.1'
__author__ = 'Jan Martin Keil'

from IPython.core.display import HTML
import html
import json
import pandas as pd
import requests
import subprocess
import tempfile
import time

class Abecto:
    def __init__(self, base, jar):
        self.base = base
        self.jar = jar

    def getExecution(self, id):
        r = requests.get(self.base + "execution/" + id)
        r.raise_for_status()
        return Execution(self, r.json())

    def getProcessing(self, id):
        r = requests.get(self.base + "processing/" + id)
        r.raise_for_status()
        return Processing(self, r.json())

    def getKnowledgeBase(self, id):
        r = requests.get(self.base + "knowledgebase/" + id)
        r.raise_for_status()
        return KnowledgeBase(self, r.json())

    def getProject(self, id):
        r = requests.get(self.base + "project/" + id)
        r.raise_for_status()
        return Project(self, r.json())
    
    def getStep(self, id):
        r = requests.get(self.base + "step/" + id)
        r.raise_for_status()
        return Step(self, r.json())

    def project(self, label = ""):
        r = requests.post(self.base + "project", data = {"label": label})
        r.raise_for_status()
        return Project(self, r.json())        
    
    def projects(self):
        r = requests.get(self.base + "project")
        r.raise_for_status()
        return list(map(lambda projectData: Project(self, projectData), r.json()))
    
    def running(self):
        try:
            return requests.get(self.base).status_code == 200
        except:
            return False

    def start(self):
        if not self.running():
            subprocess.Popen(["java","-jar",self.jar])
            while not self.running():
                time.sleep(0.1)

class Project:
    def __init__(self, server, data):
        self.server = server
        self.id = data["id"]
        
    def __repr__(self):
        return str(self.info())
    
    def delete(self):
        r = requests.delete(self.server.base + "project/" + self.id)
        r.raise_for_status()
    
    def info(self):
        r = requests.get(self.server.base + "project/" + self.id)
        r.raise_for_status()
        return r.json()

    def knowledgeBase(self, label = ""):
        r = requests.post(self.server.base + "knowledgebase", data = {"project": self.id, "label": label})
        r.raise_for_status()
        return KnowledgeBase(self.server, r.json())
    
    def knowledgeBases(self):
        r = requests.get(self.server.base + "knowledgebase", params = {"project": self.id})
        r.raise_for_status()
        return list(map(lambda knowledgeBaseData: KnowledgeBase(self.server, knowledgeBaseData), r.json()))

    def steps(self):
        r = requests.get(self.server.base + "step", params = {"project": self.id})
        r.raise_for_status()
        return list(map(lambda stepData: Step(self.server, stepData), r.json()))

    def run(self):
        r = requests.get(self.server.base + "project/" + self.id + "/run", params = {"await": False})
        r.raise_for_status()
        
    def runAndAwait(self):
        r = requests.get(self.server.base + "project/" + self.id + "/run", params = {"await": True})
        r.raise_for_status()
        execution = Execution(self.server, r.json())
        for processing in execution.processings:
            if processing.info()["status"] != "SUCCEEDED":
                html = "<h3>" + processing.info()["processorClass"] + ": " + processing.info()["status"] + "</h3>"
                html += "<pre>" + processing.info()["stackTrace"] + "</pre>"
                display(HTML(html))
        return execution

class KnowledgeBase:
    def __init__(self, server, data):
        self.server = server
        self.id = data["id"]
        
    def __repr__(self):
        return str(self.info())
    
    def delete(self):
        r = requests.delete(self.server.base + "knowledgebase/" + self.id)
        r.raise_for_status()
    
    def info(self):
        r = requests.get(self.server.base + "knowledgebase/" + self.id)
        r.raise_for_status()
        return r.json()

    def source(self, processorName, parameters={}):
        r = requests.post(self.server.base + "step", data = {"class": processorName, "knowledgebase" : self.id, "parameters": json.dumps(parameters)})
        r.raise_for_status()
        return Step(self.server, r.json())
    
class Step:
    def __init__(self, server, data):
        self.server = server
        self.id = data["id"]
        
    def __repr__(self):
        return str(self.info())
    
    def info(self):
        r = requests.get(self.server.base + "step/" + self.id)
        r.raise_for_status()
        return r.json()
    
    def into(self, processorName, parameters = {}):
        r = requests.post(self.server.base + "step", data = {"class": processorName, "input" : [self.id], "parameters": json.dumps(parameters)})
        r.raise_for_status()
        return Step(self.server, r.json())
    
    def last(self):
        r = requests.get(self.server.base + "step/" + self.id + "/processing/last")
        r.raise_for_status()
        return Processing(self.server, r.json())
    
    def processings(self):
        r = requests.get(self.server.base + "step/" + self.id + "/processing")
        r.raise_for_status()
        return list(map(lambda processingData: Processing(self.server, processingData), r.json()))
    
    def load(self, file):
        r = requests.post(self.server.base + "step/" + self.id + "/load", files = {"file": file})
        r.raise_for_status()
        return self
    
    def plus(self, step):
        return Steps(self, step)

    def report(self):
        return self.last().report()

class Steps:
    def __init__(self, steps, step):
        if steps.server != step.server:
            raise ValueError("steps must belonge to the same server")
        self.server = step.server
        if isinstance(steps, Steps):
            self.stepList = steps.stepList + [step.id]
        elif isinstance(steps, Step):
            self.stepList = [steps.id, step.id]

    def into(self, processorName, parameters = {}):
        r = requests.post(self.server.base + "step", data = {"class": processorName, "input" : self.stepList, "parameters": json.dumps(parameters)})
        r.raise_for_status()
        return Step(self.server, r.json())
    
    def plus(self, step):
        return Steps(self, step)

class Processing:
    def __init__(self, server, data):
        self.server = server
        self.id = data["id"]
        
    def __repr__(self):
        return str(self.info())

    def raw(self):
        r = requests.get(self.server.base + "processing/" + self.id + "/result")
        r.raise_for_status()
        return r.text

    def graph(self):
        r = requests.get(self.server.base + "processing/" + self.id + "/result")
        r.raise_for_status()
        json = r.json()
        if "@graph"  in json:
            return r.json()["@graph"]
        else:
            return []
    
    def graphAsDataFrame(self):
        graph = self.graph()
        return pd.DataFrame.from_records(graph)
    
    def info(self):
        r = requests.get(self.server.base + "processing/" + self.id)
        r.raise_for_status()
        return r.json()

class Execution:
    def __init__(self, server, data):
        self.server = server
        self.id = data["id"]
        self.processings = []
        for processingId in data["processings"]:
            self.processings.append(server.getProcessing(processingId))

    def results(self, resultType):
        r = requests.get(self.server.base + "execution/" + self.id + "/results", params = {"type": resultType})
        r.raise_for_status()
        return r.json()
    
    def resultDataFrame(self, resultType):
        return pd.DataFrame.from_records(self.results(resultType))

    def data(self, categoryName, knowledgeBaseId):
        r = requests.get(self.server.base + "execution/" + self.id + "/data", params = {"category": categoryName, "knowledgebase": knowledgeBaseId})
        r.raise_for_status()
        return r.json()
    
    def sortedKnowledgeBases(self, knowledgeBaseIds):
        return sorted({ knowledgeBaseId : self.server.getKnowledgeBase(knowledgeBaseId).info()["label"] for knowledgeBaseId in knowledgeBaseIds }.items(), key = lambda x:x[1])
    
    def measures(self):
        data = self.resultDataFrame("Measurement")
        if not data.empty:
            knowledgeBases = self.sortedKnowledgeBases(set(data["knowledgeBase"]))
            for measure in set(data["measure"]):
                html = "<h1>" + measure + " Report</h1>"
                measureData = data[data.measure.eq(measure)]
                dimension1Used = any(set(data["dimension1Value"]))
                dimension2Used = any(set(data["dimension2Value"]))
                html += "<table>"
                html += "<tr>"
                if dimension1Used:
                    html += "<th>" + "/".join(set(data["dimension1Key"])) + "</th>"
                if dimension2Used:
                    html += "<th>" + "/".join(set(data["dimension2Key"])) + "</th>"
                for (kbId, kbLabel) in knowledgeBases:
                    html += "<th>" + kbLabel + "</th>"
                html += "</tr>"
                # total row
                totalData = measureData[measureData.dimension1Value.isna() & measureData.dimension2Value.isna()]
                if not totalData.empty:
                    html += "<tr>"
                    if dimension1Used:
                        html += "<td></td>"
                    if dimension2Used:
                        html += "<td></td>"
                    for (kbId, kbLabel) in knowledgeBases:
                        row = totalData[totalData.knowledgeBase.eq(kbId)]
                        html += "<td>" + (str(row["value"].iat[-1]) if row.size > 0 else "") + "</td>"
                    html += "</tr>"
                # dimension 1 rows
                for dimension1Value in set(measureData["dimension1Value"]):
                    d1Data = measureData[measureData.dimension1Value.notna() & measureData.dimension1Value.eq(dimension1Value)]
                    # dimension 1 total row
                    d1TotalData = d1Data[d1Data.dimension2Value.isna()]
                    if not d1TotalData.empty:
                        html += "<tr>"
                        html += "<td>" + dimension1Value + "</td>"
                        if dimension2Used:
                            html += "<td></td>"
                        for (kbId, kbLabel) in knowledgeBases:
                            row = d1TotalData[d1TotalData.knowledgeBase.eq(kbId)]
                            html += "<td>" + (str(row["value"].iat[-1]) if row.size > 0 else "") + "</td>"
                        html += "</tr>"
                    # dimension 2 rows
                    for dimension2Value in set(d1Data["dimension2Value"]):
                        d2Data = d1Data[d1Data.dimension2Value.notna() & d1Data.dimension2Value.eq(dimension2Value)]
                        if not d2Data.empty:
                            html += "<tr>"
                            html += "<td>" + dimension1Value + "</td>"
                            html += "<td>" + dimension2Value + "</td>"
                            for (kbId, kbLabel) in knowledgeBases:
                                row = d2Data[d2Data.knowledgeBase.eq(kbId)]
                                html += "<td>" + (str(row["value"].iat[-1]) if row.size > 0 else "") + "</td>"
                            html += "</tr>"
                html += "</table>"
                display(HTML(html))
    
    def deviations(self):
        display(HTML("<h2>Deviation Report</h2>"))
        totalData =  self.resultDataFrame("Deviation")
        if not totalData.empty:
            categoryNames = set(totalData["categoryName"])
            knowledgeBases = self.sortedKnowledgeBases(set(totalData["knowledgeBaseId1"]).union(set(totalData["knowledgeBaseId2"])))
            # iterate categories
            for categoryName in categoryNames:
                display(HTML("<h3>Category: " + categoryName + "</h3>"))
                categoryData = totalData[totalData.categoryName.eq(categoryName)]
                kbPairs = categoryData.filter(["knowledgeBaseId1","knowledgeBaseId2"]).drop_duplicates().sort_values(["knowledgeBaseId1","knowledgeBaseId2"])
                 # iterate knowledge bases
                for (kb1Id, kb1Label) in knowledgeBases:
                    for (kb2Id, kb2Label) in knowledgeBases:
                        if kb1Id != kb2Id and kb1Label < kb2Label:
                            kbsData = categoryData[categoryData.knowledgeBaseId1.eq(kb1Id) & categoryData.knowledgeBaseId2.eq(kb2Id)]
                            if not kbsData.empty:
                                kbsData = kbsData.sort_values(["knowledgeBaseId1","knowledgeBaseId2"])
                                table = "<table>"
                                table += "<tr>"
                                table += "<th style=\"text-align:center;\" colspan=\"3\">" + kb1Label + "</th>"
                                table += "<th style=\"text-align:center;\" colspan=\"3\">" + kb2Label + "</th>"
                                table += "</tr>"
                                resourcePairs = kbsData.filter(["resource1","resource2"]).drop_duplicates().sort_values(["resource1","resource2"])
                                # iterate resources
                                for index, resourcePair in resourcePairs.iterrows():
                                    resourceData = kbsData[kbsData.resource1.eq(resourcePair["resource1"]) & kbsData.resource2.eq(resourcePair["resource2"])]
                                    resourceData = resourceData.sort_values(["resource1","resource2"])
                                    variablesCount = len(resourceData)
                                    firstRow = True
                                    for index, row in resourceData.iterrows():
                                        table += "<tr rowspan=\"variablesCount\">"
                                        if firstRow:
                                            table += "<td>" + resourcePair["resource1"] + "</td>"
                                        table += "<td>" + row.variableName + "</td>"
                                        table += "<td>" + html.escape(row.value1 if not row.isna().value1 else "") + "</td>"
                                        table += "<td>" + html.escape(row.value2 if not row.isna().value2 else "") + "</td>"
                                        table += "<td>" + row.variableName + "</td>"
                                        if firstRow:
                                            table += "<td>" + resourcePair["resource2"] + "</td>"
                                            firstRow = False
                                        table += "</tr>"
                                table += "</table>"
                                display(HTML(table))

    def issues(self):
        display(HTML("<h2>Issue Report</h2>"))
        totalData =  self.resultDataFrame("Issue")
        if not totalData.empty:
            totalData = totalData.sort_values(["knowledgeBase","entity"])
            knowledgeBases = self.sortedKnowledgeBases(set(totalData["knowledgeBase"]))
            for (kbId, kbLabel) in knowledgeBases:
                display(HTML("<h3>Knowledge Base: " + kbLabel + "</h3>"))
                kbData = totalData[totalData.knowledgeBase.eq(kbId)]
                table = "<table>"
                table += "<tr>"
                table += "<th>" + "Issue Type" + "</th>"
                table += "<th>" + "Affected Entity" + "</th>"
                table += "<th>" + "Message" + "</th>"
                table += "</tr>"
                for index, issue in kbData.iterrows():
                    table += "<tr>"
                    table += "<td>" + issue["type"] + "</td>"
                    table += "<td>" + issue["entity"] + "</td>"
                    table += "<td>" + issue["message"] + "</td>"
                    table += "</tr>"
                table += "</table>"
                display(HTML(table))
                
    def mappings(self):
        display(HTML("<h1>Mapping Report</h1>"))
        totalData =  self.resultDataFrame("MappingReportEntity")
        display(totalData)
        if not totalData.empty:
            knowledgeBases = self.sortedKnowledgeBases(set(totalData["firstKnowledgeBase"]).union(set(totalData["secondKnowledgeBase"])))
            categories = set(totalData["firstCategory"]).union(set(totalData["secondCategory"]))
            # iterate categories
            for categoryName in categories:
                display(HTML("<h2>Category: " + categoryName + "</h2>"))
                categoryData = totalData[totalData.firstCategory.eq(categoryName) & totalData.secondCategory.eq(categoryName)]
                # iterate knowledge bases
                for (kb1Id, kb1Label) in knowledgeBases:
                    for (kb2Id, kb2Label) in knowledgeBases:
                        data = categoryData[categoryData.firstKnowledgeBase.eq(kb1Id) & categoryData.secondKnowledgeBase.eq(kb2Id)]
                        if not data.empty:
                            data = data.sort_values(["first","second"])
                            table = "<table>"
                            table += "<tr>"
                            table += "<th style=\"text-align:center;font-size:larger;\">" + kb1Label + "</th>"
                            table += "<th></th>"
                            table += "<th style=\"text-align:center;font-size:larger;\">" + kb2Label + "</th>"
                            table += "</tr>"
                            for index, row in data.iterrows():
                                entityData1 = json.loads(row.firstData) if not row.isna().firstData else {}
                                entityData2 = json.loads(row.secondData) if not row.isna().secondData else {}
                                keys = set(list(entityData1)).union(set(list(entityData2)))
                                keys.discard(categoryName)
                                table += "<tr style=\"border-top:solid 1px #000\">"
                                table += "<th style=\"text-align:right;\">" + (row.first if not row.isna().first else "") + "</th>"
                                table += "<td></td>"
                                table += "<th style=\"text-align:left;\">" + (row.second if not row.isna().second else "") + "</th>"
                                table += "</tr>"
                                if any(keys):
                                    for key in sorted(keys):
                                        table += "<tr>"
                                        table += "<td style=\"text-align:right;\">" + entityData1.get(key,"") + "</td>"
                                        table += "<td style=\"text-align:center;\">" + key + "</td>"
                                        table += "<td style=\"text-align:left;\">" + entityData2.get(key,"") + "</td>"
                                        table += "</tr>"
                            table += "</table>"
                            display(HTML(table))
