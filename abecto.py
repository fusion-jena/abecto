from __future__ import print_function
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

from ipywidgets import interact, interactive, fixed, interact_manual
import ipywidgets as widgets

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
    
    def stop(self):
        if self.running():
            r = requests.post(self.base + "actuator/shutdown")
            r.raise_for_status()
        while self.running():
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
    
    def lastProcessing(self):
        r = requests.get(self.server.base + "step/" + self.id + "/processing/last")
        r.raise_for_status()
        return Processing(self.server, r.json())

    def parameters(self):
        r = requests.get(self.server.base + "step/" + self.id + "/parameters")
        r.raise_for_status()
        return r.json()

    def setParameter(self, key, value):
        r = requests.post(self.server.base + "step/" + self.id + "/parameters", data = {"key": key, "value": json.dumps(value)})
        r.raise_for_status()

    def processings(self):
        r = requests.get(self.server.base + "step/" + self.id + "/processing")
        r.raise_for_status()
        return list(map(lambda processingData: Processing(self.server, processingData), r.json()))
    
    def load(self, file):
        r = requests.post(self.server.base + "step/" + self.id + "/load", files = {"file": file})
        r.raise_for_status()
        return self
    
    def __add__(self, other):
        if self.server != other.server:
            raise ValueError("steps must belonge to the same server")
        return Steps(self.server, [self.id, other.id])

class Steps:
    def __init__(self, server, stepList):
        self.server = server
        self.stepList = stepList

    def into(self, processorName, parameters = {}):
        r = requests.post(self.server.base + "step", data = {"class": processorName, "input" : self.stepList, "parameters": json.dumps(parameters)})
        r.raise_for_status()
        return Step(self.server, r.json())
    
    def __add__(self, other):
        if self.server != other.server:
            raise ValueError("steps must belonge to the same server")
        return Steps(self.server, self.stepList + [other.id])

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
        return r.json()
    
    def dataFrame(self):
        graph = self.graph()["@graph"]
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
    
    def dataDataFrame(self, categoryName, knowledgeBaseId):
        return pd.DataFrame.from_records(self.data(categoryName, knowledgeBaseId))
    
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
                
    def mappings(self, manualMappingStep):
        # symboles
        accepted = "✓"
        retained = "?"
        rejected = "✗"

        output = widgets.Output()

        # widgets templates
        def unmappedWidget(resource, resourceData, resourceSink):
            with output:
                table = "<table>"
                if any(list(resourceData)):
                    for key in sorted(resourceData):
                        table += "<tr>"
                        table += "<td style=\"text-align:center;\">" + key + "</td>"
                        table += "<td style=\"text-align:right;\">" + ", ".join(resourceData[key]) + "</td>"
                        table += "</tr>"
                table += "</table>"
                button = widgets.Button(description=resource, tooltip='Use', layout={'width': 'max-content'})
                def use(b):
                    resourceSink.value = resource
                button.on_click(use)
                return widgets.VBox([button, widgets.HTML(value=table)], layout={'border': 'solid 1px lightgrey'})

        def mappingPairWidget(resource1, resource2, resource1Data, resource1Data2, value):
            with output:
                keys = set(list(resource1Data)).union(set(list(resource2Data)))
                table = "<table><tr><th>" + resource1 + "</th><th></th><th>"+ resource2 + "</th></tr>"
                if any(keys):
                    for key in sorted(keys):
                        table += "<tr>"
                        table += "<td style=\"text-align:right;\">" + (", ".join(resource1Data[key]) if key in resource1Data else "") + "</td>"
                        table += "<td style=\"text-align:center;\">" + key + "</td>"
                        table += "<td style=\"text-align:left;\">" + (", ".join(resource2Data[key]) if key in resource2Data else "") + "</td>"
                        table += "</tr>"
                table += "</table>"
                button = widgets.ToggleButtons(options=[accepted,retained,rejected],value=value,tooltips=["Accept", "Retain", "Reject"], style={'button_width': 'auto'})
                return widgets.HBox([button, widgets.HTML(value=table)], layout={'border': 'solid 1px lightgrey'})

        def newMappingWidget(resource1, resource2, newMappingSink):
            with output:
                button = widgets.Button(description=rejected, tooltip='Remove')
                widget = widgets.HBox([widgets.Label(value=resource1), button, widgets.Label(value=resource2)])
                def remove(b):
                    newMappingSink.children = tuple(x for x in newMappingSink.children if x != widget)
                button.on_click(remove)
                return widget

        def unmappedPairingWidget(resource1Widget, resource2Widget, newMappingSink):
            with output:
                button = widgets.Button(description='Add Mapping')
                def add(b):
                    if resource1Widget.value != "" and resource2Widget.value != "":
                        newMappingSink.children += (newMappingWidget(resource1Widget.value, resource2Widget.value, newMappingSink),)
                        resource1Widget.value = ""
                        resource2Widget.value = ""
                button.on_click(add)
                return widgets.VBox([widgets.HBox([resource1Widget, button, resource2Widget]), newMappingSink])

        # collect execution data
        categoryData = self.resultDataFrame("Category")
        categories =  set(categoryData["name"])
        knowledgeBases = self.sortedKnowledgeBases(set(categoryData["knowledgeBase"]))
        mappings = {}
        for mapping in self.results("Mapping"):
            if mapping["resourcesMap"]:
                if mapping["resource1"] in mappings:
                    mappings[mapping["resource1"]].add(mapping["resource2"])
                else:
                    mappings[mapping["resource1"]] = {mapping["resource2"]}
                if mapping["resource2"] in mappings:
                    mappings[mapping["resource2"]].add(mapping["resource1"])
                else:
                    mappings[mapping["resource2"]] = {mapping["resource1"]}
        # get manual mapping parameters
        manualMappingParameters = manualMappingStep.parameters()["parameters"]
        # collect positive manual mappings from parameters
        manualPositiveMappings = {}
        for mappingList in (manualMappingParameters["mappings"] if manualMappingParameters["mappings"] else []):
            for resource1 in mappingList:
                for resource2 in mappingList:
                    if resource1 != resource2:
                        if resource1 in manualPositiveMappings:
                            manualPositiveMappings[resource1].add(resource2)
                        else:
                            manualPositiveMappings[resource1] = {resource2}
                        if resource2 in manualPositiveMappings:
                            manualPositiveMappings[resource2].add(resource1)
                        else:
                            manualPositiveMappings[resource2] = {resource1}
        # collect negativ manual mappings from parameters
        manualNegativeMappings = {}
        for mappingList in (manualMappingParameters["suppressed_mappings"] if manualMappingParameters["suppressed_mappings"] else []):
            for resource1 in mappingList:
                for resource2 in mappingList:
                    if resource1 != resource2:
                        if resource1 in manualNegativeMappings:
                            manualNegativeMappings[resource1].add(resource2)
                        else:
                            manualNegativeMappings[resource1] = {resource2}
                        if resource2 in manualNegativeMappings:
                            manualNegativeMappings[resource2].add(resource1)
                        else:
                            manualNegativeMappings[resource2] = {resource1}
        # prepare unmapped resources widgets
        resourceSinks = {}
        unmappedResourceWidgets = {}
        kbData = {}
        for categoryName in categories:
            resourceSinks[categoryName] = {}
            kbData[categoryName]= {}
            unmappedResourceWidgets[categoryName]= {}
            for (kbId, kbLabel) in knowledgeBases:
                resourceSinks[categoryName][kbId] = widgets.Text(value='', placeholder='Resource to map')
                kbData[categoryName][kbId] = self.data(categoryName, kbId)
                unmapped = []
                for unmappedResource in set(kbData[categoryName][kbId])-set(mappings)-set(manualPositiveMappings):
                    unmapped.append(unmappedWidget(unmappedResource, kbData[categoryName][kbId][unmappedResource], resourceSinks[categoryName][kbId]))
                unmappedResourceWidgets[categoryName][kbId] = widgets.VBox(unmapped,layout={'width': '50%','max_height':'20em'})

        mappingPairWidgets = {}
        newMappingSinks = []
        categoryTabChildren = []
        categoryTabTitles = []
        for categoryName in categories:
            kbTabChildrens = []
            kbTabTitles = []
            for (kb1Id, kb1Label) in knowledgeBases:
                for (kb2Id, kb2Label) in knowledgeBases:
                    if (kb1Label < kb2Label):
                        pairs = []
                        for resource1 in kbData[categoryName][kb1Id]:
                            resource1Data = kbData[categoryName][kb1Id][resource1]
                            # add positive manual mappings
                            if resource1 in manualPositiveMappings:
                                for resource2 in manualPositiveMappings[resource1]:
                                    if resource2 in kbData[categoryName][kb2Id]:
                                        resource2Data = kbData[categoryName][kb2Id][resource2]
                                        pair = mappingPairWidget(resource1, resource2, resource1Data, resource2Data, accepted)
                                        mappingPairWidgets[pair] = [resource1, resource2]
                                        pairs.append(pair)
                            # add negative manual mappings
                            if resource1 in manualNegativeMappings:
                                for resource2 in manualNegativeMappings[resource1]:
                                    if resource2 in kbData[categoryName][kb2Id]:
                                        resource2Data = kbData[categoryName][kb2Id][resource2]
                                        pair = mappingPairWidget(resource1, resource2, resource1Data, resource2Data, rejected)
                                        mappingPairWidgets[pair] = [resource1, resource2]
                                        pairs.append(pair)
                            # add none manual mappings
                            if resource1 in mappings:
                                for resource2 in mappings[resource1]:
                                    if resource2 in kbData[categoryName][kb2Id] and not (
                                        resource1 in manualPositiveMappings and resource2 in manualPositiveMappings[resource1] or
                                        resource1 in manualNegativeMappings and resource2 in manualNegativeMappings[resource1] ):
                                        resource2Data = kbData[categoryName][kb2Id][resource2]
                                        pair = mappingPairWidget(resource1, resource2, resource1Data, resource2Data, retained)
                                        mappingPairWidgets[pair] = [resource1, resource2]
                                        pairs.append(pair)
                        # widgets management
                        newMappingSink = widgets.VBox([],layout={'max_height':'30em'})
                        pairTab = widgets.VBox([
                            widgets.VBox(pairs,layout={'max_height':'30em'}),
                            widgets.HBox([
                                unmappedResourceWidgets[categoryName][kb1Id],
                                unmappedResourceWidgets[categoryName][kb2Id]
                            ]),
                            unmappedPairingWidget(resourceSinks[categoryName][kb1Id], resourceSinks[categoryName][kb2Id], newMappingSink)
                        ])
                        kbTabChildrens.append(pairTab)
                        newMappingSinks.append(newMappingSink)
                        kbTabTitles.append(kb1Label + " <-> " + kb2Label)
            # widgets management
            kbTabs = widgets.Tab(children=kbTabChildrens)
            for i, title in enumerate(kbTabTitles):
                kbTabs.set_title(i, title)    
            categoryTabChildren.append(kbTabs)
            categoryTabTitles.append(categoryName)
        # widgets management
        categoryTabs = widgets.Tab(children=categoryTabChildren)
        for i, title in enumerate(categoryTabTitles):
            categoryTabs.set_title(i, title)

        display(HTML("<h2>Mapping Review</h2>"))
        display(categoryTabs)

        updateButton = widgets.Button(description='Update Mappings')
        hideButton = widgets.Button(description='Show Only Undecided', layout={'width': 'max-content'})
        showButton = widgets.Button(description='Show All')
        showButton.layout.display = "none"
        display(widgets.HBox([updateButton, hideButton, showButton]), output)
        def updateMappings(b):
            with output:
                # get remote manual mapping data
                manualMappingParameters = manualMappingStep.parameters()["parameters"]
                manualPositiveMappings = manualMappingParameters["mappings"] if manualMappingParameters["mappings"] else []
                manualNegativeMappings = manualMappingParameters["suppressed_mappings"] if manualMappingParameters["suppressed_mappings"] else []
                # update local manual mapping data
                for mappingPairWidget in mappingPairWidgets:
                    if mappingPairWidget.children[0].value == accepted:
                        while mappingPairWidgets[mappingPairWidget] not in manualPositiveMappings: manualPositiveMappings.append(mappingPairWidgets[mappingPairWidget])
                        while mappingPairWidgets[mappingPairWidget] in manualNegativeMappings: manualNegativeMappings.remove(mappingPairWidgets[mappingPairWidget])
                    if mappingPairWidget.children[0].value == retained:
                        while mappingPairWidgets[mappingPairWidget] in manualPositiveMappings: manualPositiveMappings.remove(mappingPairWidgets[mappingPairWidget])
                        while mappingPairWidgets[mappingPairWidget] in manualNegativeMappings: manualNegativeMappings.remove(mappingPairWidgets[mappingPairWidget])
                    elif mappingPairWidget.children[0].value == rejected:
                        while mappingPairWidgets[mappingPairWidget] in manualPositiveMappings: manualPositiveMappings.remove(mappingPairWidgets[mappingPairWidget])
                        while mappingPairWidgets[mappingPairWidget] not in manualNegativeMappings: manualNegativeMappings.append(mappingPairWidgets[mappingPairWidget])
                for newMappingSink in newMappingSinks:
                    for newMapping in newMappingSink.children:
                        manualPositiveMappings.append([newMapping.children[0].value, newMapping.children[2].value])
                # update remote manual mapping data
                manualMappingStep.setParameter("mappings", manualPositiveMappings)
                manualMappingStep.setParameter("suppressed_mappings", manualNegativeMappings)
                display(HTML("Manual Mappings updated."))
        updateButton.on_click(updateMappings)
        def hide(b):
            with output:
                for mappingPairWidget in mappingPairWidgets:
                    if mappingPairWidget.children[0].value != retained:
                        mappingPairWidget.layout.display = "none"
                hideButton.layout.display = "none"
                showButton.layout.display = "inline-flex"
        hideButton.on_click(hide)
        def show(b):
            with output:
                for mappingPairWidget in mappingPairWidgets:
                    if mappingPairWidget.children[0].value != retained:
                        mappingPairWidget.layout.display = "inline-flex"
                hideButton.layout.display = "inline-flex"
                showButton.layout.display = "none"
        showButton.on_click(show)