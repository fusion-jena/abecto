"""This is the ABECTO module.

This module provides handy functions to use the ABECTO REST service, hidding the raw HTTP requests.
"""

__version__ = '0.1'
__author__ = 'Jan Martin Keil'

from IPython.core.display import HTML
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
    
    def project(self, label = "", id = None):
        if id is None:
            return Project(self, label = label)
        else:
            return Project(self, id = id)        
    
    def projects(self):
        r = requests.get(self.base + "project")
        r.raise_for_status()
        return list(map(lambda x: self.project(id=x["id"]), r.json()))
    
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
    def __init__(self, server, label = "", id = None):
        self.server = server
        if id is None:
            r = requests.post(self.server.base + "project", data = {"label": label})
            r.raise_for_status()
            self.id = r.json()["id"]
        else:
            self.id = id
        
    def __repr__(self):
        return str(self.info())
    
    def delete(self):
        r = requests.delete(self.server.base + "project/" + self.id)
        r.raise_for_status()
    
    def info(self):
        r = requests.get(self.server.base + "project/" + self.id)
        r.raise_for_status()
        return r.json()

    def knowledgeBase(self, label = "", id = None):
        if id is None:
            return KnowledgeBase(self.server, self, label = label)
        else:
            return KnowledgeBase(self.server, self, id = id)
    
    def knowledgeBases(self):
        r = requests.get(self.server.base + "knowledgebase", params = {"project": self.id})
        r.raise_for_status()
        return list(map(lambda x: self.knowledgeBase(id=x["id"]), r.json()))
    
    def step(self, id):
        r = requests.get(self.server.base + "step/" + id)
        r.raise_for_status()
        step = r.json()
        return Step(self.server, self, self.knowledgeBase(id=step["knowledgeBase"]), step["processorClass"], [], step["parameter"], step["id"])
    
    def steps(self):
        r = requests.get(self.server.base + "step", params = {"project": self.id})
        r.raise_for_status()
        return list(map(lambda x: self.step(id=x["id"]), r.json()))

    def run(self):
        r = requests.get(self.server.base + "project/" + self.id + "/run", params = {"await": False})
        r.raise_for_status()
        
    def runAndAwait(self):
        r = requests.get(self.server.base + "project/" + self.id + "/run", params = {"await": True})
        r.raise_for_status()

class KnowledgeBase:
    def __init__(self, server, project, label = None, id = None):
        self.server = server
        self.project = project
        if id is None:
            r = requests.post(self.server.base + "knowledgebase", data = {"project": project.id, "label": label})
            r.raise_for_status()
            self.id = r.json()["id"]
        else:
            self.id = id
        
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
        return Step(self.server, self.project, self, processorName, [], parameters)
    
class Step:
    def __init__(self, server, project, knowledgeBase, processorName, inputSteps = [], parameters = {}, id = None):
        self.server = server
        self.project = project
        self.knowledgeBase = knowledgeBase
        self.inputSteps = inputSteps
        if knowledgeBase is not None:
            knowledgeBaseId = knowledgeBase.id
        else:
            knowledgeBaseId = None
        inputStepIds = list(map(lambda inputStep: inputStep.id, inputSteps))
        if id is None:
            r = requests.post(self.server.base + "step", data = {"class": processorName, "input" : inputStepIds, "knowledgebase": knowledgeBaseId, "parameters": json.dumps(parameters)})
            r.raise_for_status()
            self.id = r.json()["id"]
        else:
            self.id = id
        
    def __repr__(self):
        return str(self.info())
    
    def info(self):
        r = requests.get(self.server.base + "step/" + self.id)
        r.raise_for_status()
        return r.json()
    
    def into(self, processorName, parameters = {}):
        return Step(self.server, self.project, None, processorName, [self], parameters)
    
    def last(self):
        r = requests.get(self.server.base + "step/" + self.id + "/processing/last")
        r.raise_for_status()
        return Processing(self.server, self.project, self, r.json()["id"])

    def processing(self, id):
        return Processing(self.server, self.project, self, id)
    
    def processings(self):
        r = requests.get(self.server.base + "step/" + self.id + "/processing")
        r.raise_for_status()
        return list(map(lambda x: self.processing(id=x["id"]), r.json()))
    
    def load(self, file):
        r = requests.post(self.server.base + "step/" + self.id + "/load", files = {"file": file})
        r.raise_for_status()
        return self
    
    def plus(self, step):
        return Steps(self, step)

class Steps:
    def __init__(self, steps, step):
        if steps.server != step.server:
            raise ValueError("steps must belonge to the same server")
        if steps.project != step.project:
            raise ValueError("steps must belonge to the same project")
        if steps.knowledgeBase == step.knowledgeBase:
            self.knowledgeBase = step.knowledgeBase
        else:
            self.knowledgeBase = None
        if isinstance(steps, Steps):
            self.stepList = steps.stepList + [step]
        elif isinstance(steps, Step):
            self.stepList = [steps, step]
        self.server = step.server
        self.project = step.project
        self.knowledgeBase = step.knowledgeBase

    def into(self, processorName, parameters = {}):
        return Step(self.server, self.project, None, processorName, self.stepList, parameters)
    
    def plus(self, step):
        return Steps(self, step)

class Processing:
    def __init__(self, server, project, step, id):
        self.server = server
        self.project = project
        self.step = step
        self.id = id
        
    def __repr__(self):
        return str(self.info())

    def graph(self):
        r = requests.get(self.server.base + "processing/" + self.id + "/result")
        r.raise_for_status()
        return r.json()["@graph"]
    
    def graphAsDataFrame(self):
        graph = self.graph()
        return pd.DataFrame.from_records(graph)
    
    def report(self):
        Report.of(self)
    
    def info(self):
        r = requests.get(self.server.base + "processing/" + self.id)
        r.raise_for_status()
        return r.json()
        
class Report:

    def countReport(cls, processing):
        display(HTML("<h1>Category Count Report</h1>"))
        totalData = processing.graphAsDataFrame()
        knowledgeBaseIds = list(totalData.filter(["knowledgeBase"]).drop_duplicates()["knowledgeBase"])
        categoryNames = list(totalData.filter(["categoryName"]).drop_duplicates()["categoryName"])
        # table header
        table = "<table>"
        table += "<tr>"
        table += "<th>Category</th>"
        table += "<th >Variable</th>"
        for knowledgeBaseId in knowledgeBaseIds:
            knowledgeBaseLabel = processing.project.knowledgeBase(id = knowledgeBaseId).info()["label"]
            table += "<th>" + knowledgeBaseLabel + "</th>"
        table += "</tr>"
        # iterate categories
        for categoryName in categoryNames:
            categoryData = totalData[totalData.categoryName.eq(categoryName)]
            varialbeNames = list(categoryData[categoryData.variableName.notna()].filter(["variableName"]).drop_duplicates().sort_values("variableName")["variableName"])
            # category total
            table += "<tr>"
            table += "<th>" + categoryName + "</th>"
            table += "<th></th>"
            for knowledgeBaseId in knowledgeBaseIds:
                value = categoryData[categoryData.variableName.isna() & categoryData.knowledgeBase.eq(knowledgeBaseId)]["value"].iat[-1]
                table += "<td>" + value + "</td>"
            table += "</tr>"
            # category variable
            for varialbeName in varialbeNames:
                table += "<tr>"
                table += "<th>" + categoryName + "</th>"
                table += "<th>" + varialbeName + "</th>"
                for knowledgeBaseId in knowledgeBaseIds:
                    valueSeries =categoryData[categoryData.variableName.eq(varialbeName) & categoryData.knowledgeBase.eq(knowledgeBaseId)]["value"]
                    if valueSeries.size == 0:
                        table += "<td></td>"
                    else:
                        table += "<td>" + str(valueSeries.iat[-1]) + "</td>"
                table += "</tr>"
        table += "</table>"
        display(HTML(table))

    def default(cls, processing):
        return processing.graphAsDataFrame()
    
    def deviationReport(cls, processing):
        display(HTML("<h1>Value Deviation Report</h1>"))
        totalData = processing.graphAsDataFrame()
        categoryNames = list(totalData.filter(["categoryName"]).drop_duplicates()["categoryName"])
        # iterate categories
        for categoryName in categoryNames:
            display(HTML("<h2>Category: " + categoryName + "</h2>"))
            categoryData = totalData[totalData.categoryName.eq(categoryName)]
            kbPairs = categoryData.filter(["knowledgeBaseId1","knowledgeBaseId2"]).drop_duplicates().sort_values(["knowledgeBaseId1","knowledgeBaseId2"])
             # iterate knowledge bases
            for index, kbPair in kbPairs.iterrows():
                kb1Label = processing.project.knowledgeBase(id = kbPair["knowledgeBaseId1"]).info()["label"]
                kb2Label = processing.project.knowledgeBase(id = kbPair["knowledgeBaseId2"]).info()["label"]
                kbsData = categoryData[categoryData.knowledgeBaseId1.eq(kbPair["knowledgeBaseId1"]) & categoryData.knowledgeBaseId2.eq(kbPair["knowledgeBaseId2"])]
                kbsData = kbsData.sort_values(["knowledgeBaseId1","knowledgeBaseId2"])
                table = "<table>"
                table += "<tr>"
                table += "<th style=\"text-align:center;\" colspan=\"3\">" + kb1Label + "</th>"
                table += "<th style=\"text-align:center;\" colspan=\"3\">" + kb2Label + "</th>"
                table += "</tr>"
                resourcePairs = kbsData.filter(["resource1","resource2"]).drop_duplicates().sort_values(["resource1","resource2"])
                # iterate resources
                for index, resourcePair in resourcePairs.iterrows():
                    resourceData = kbsData[categoryData.resource1.eq(resourcePair["resource1"]) & categoryData.resource2.eq(resourcePair["resource2"])].sort_values(["resource1","resource2"])
                    variablesCount = len(resourceData)
                    firstRow = True
                    for index, row in resourceData.iterrows():
                        table += "<tr rowspan=\"variablesCount\">"
                        if firstRow:
                            table += "<td>" + resourcePair["resource1"] + "</td>"
                        table += "<td>" + row.variableName + "</td>"
                        table += "<td>" + (row.value1 if not row.isna().value1 else "") + "</td>"
                        table += "<td>" + (row.value2 if not row.isna().value2 else "") + "</td>"
                        table += "<td>" + row.variableName + "</td>"
                        if firstRow:
                            table += "<td>" + resourcePair["resource2"] + "</td>"
                            firstRow = False
                        table += "</tr>"
                table += "</table>"
                display(HTML(table))
    
    def mappingReport(cls, processing):
        display(HTML("<h1>Mapping Report</h1>"))
        totalData = processing.graphAsDataFrame()
        categoryPairs = totalData.filter(["category1","category2"]).drop_duplicates().sort_values(["category1","category2"])
        # iterate categories
        for index, categoryPair in categoryPairs.iterrows():
            display(HTML("<h2>Category: " + categoryPair["category1"] + "</h2>"))
            categoryData = totalData[totalData.category1.eq(categoryPair["category1"]) & totalData.category2.eq(categoryPair["category2"])]
            kbPairs = categoryData.filter(["knowledgeBase1","knowledgeBase2"]).drop_duplicates().sort_values(["knowledgeBase1","knowledgeBase2"])
            # iterate knowledge bases
            for index, kbPair in kbPairs.iterrows():
                kb1Label = processing.project.knowledgeBase(id = kbPair["knowledgeBase1"]).info()["label"]
                kb2Label = processing.project.knowledgeBase(id = kbPair["knowledgeBase2"]).info()["label"]
                data = categoryData[categoryData.knowledgeBase1.eq(kbPair["knowledgeBase1"]) & categoryData.knowledgeBase2.eq(kbPair["knowledgeBase2"])]
                data = data.sort_values(["id1","id2"])
                table = "<table>"
                table += "<tr>"
                table += "<th style=\"text-align:center;\" colspan=\"2\">" + kb1Label + "</th>"
                table += "<th style=\"text-align:center;\" colspan=\"2\">" + kb2Label + "</th>"
                table += "</tr>"
                for index, row in data.iterrows():
                    table += "<tr>"
                    table += "<td>" + (row.id1 if not row.isna().id1 else "") + "</td>"
                    table += "<td>" + (cls.mappingReportEntityDataToHtml(row.data1) if not row.isna().data1 else "") + "</td>"
                    table += "<td>" + (cls.mappingReportEntityDataToHtml(row.data2) if not row.isna().data2 else "") + "</td>"
                    table += "<td>" + (row.id2 if not row.isna().id2 else "") + "</td>"
                    table += "</tr>"
                table += "</table>"
                display(HTML(table))
            
    def mappingReportEntityDataToHtml(entityDataJson):
        entityData = json.loads(entityDataJson)
        html = "<table>"
        for key, value in entityData.items():
            html += "<tr>"
            html += "<td>" + key + "</td>"
            html += "<td>" + str(value) + "</td>"
            html += "</tr>"
        html += "</table>"
        return html
    
    @classmethod
    def of(cls, processing):
        method = cls.switcher.get(processing.step.info()["processorClass"], cls.default)
        method(cls, processing)
    
    switcher = {
        "de.uni_jena.cs.fusion.abecto.processor.implementation.MappingReportProcessor": mappingReport,
        "de.uni_jena.cs.fusion.abecto.processor.implementation.CategoryCountProcessor": countReport,
        "de.uni_jena.cs.fusion.abecto.processor.implementation.LiteralDeviationProcessor": deviationReport
    }