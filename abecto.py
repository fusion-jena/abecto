from __future__ import print_function
"""This is the ABECTO module.

This module provides handy functions to use the ABECTO REST service, hidding the raw HTTP requests.
"""

__version__ = '0.1'
__author__ = 'Jan Martin Keil'

from IPython.core.display import HTML
import html
import json
import os
import pandas as pd
import random
import requests
import subprocess
import tempfile
import time

from ipywidgets import interact, interactive, fixed, interact_manual
import ipywidgets as widgets

class Abecto:
    def __init__(self, jar, host="http://localhost", port=random.randrange(49152, 65535), storage="./.abecto"):
        self.base = host + ":" + str(port) + "/"
        self.port = port
        self.storage = os.path.abspath(storage)
        self.jar = jar

    def getExecution(self, id):
        r = requests.get(self.base + "execution/" + id)
        r.raise_for_status()
        return Execution(self, r.json())

    def getProcessing(self, id):
        r = requests.get(self.base + "processing/" + id)
        r.raise_for_status()
        return Processing(self, r.json())

    def getOntology(self, id):
        r = requests.get(self.base + "ontology/" + id)
        r.raise_for_status()
        return Ontology(self, r.json())

    def getProject(self, id):
        r = requests.get(self.base + "project/" + id)
        r.raise_for_status()
        return Project(self, r.json())
    
    def getNode(self, id):
        r = requests.get(self.base + "node/" + id)
        r.raise_for_status()
        return Node(self, r.json())

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
            subprocess.Popen(["java","-jar", self.jar, "--server.port=" + str(self.port), "--abecto.storage=" + self.storage])
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

    def ontology(self, label = ""):
        r = requests.post(self.server.base + "ontology", data = {"project": self.id, "label": label})
        r.raise_for_status()
        return Ontology(self.server, r.json())
    
    def ontologies(self):
        r = requests.get(self.server.base + "ontology", params = {"project": self.id})
        r.raise_for_status()
        return list(map(lambda ontologyData: Ontology(self.server, ontologyData), r.json()))

    def nodes(self):
        r = requests.get(self.server.base + "node", params = {"project": self.id})
        r.raise_for_status()
        return list(map(lambda nodeData: Node(self.server, nodeData), r.json()))

    def run(self):
        r = requests.get(self.server.base + "project/" + self.id + "/run", params = {"await": False})
        r.raise_for_status()
        
    def runAndAwait(self):
        r = requests.get(self.server.base + "project/" + self.id + "/run", params = {"await": True})
        r.raise_for_status()
        execution = Execution(self.server, r.json())
        for processing in execution.processings:
            processing.raiseForStatus()
        return execution

class Ontology:
    def __init__(self, server, data):
        self.server = server
        self.id = data["id"]
        
    def __repr__(self):
        return str(self.info())
    
    def delete(self):
        r = requests.delete(self.server.base + "ontology/" + self.id)
        r.raise_for_status()
    
    def info(self):
        r = requests.get(self.server.base + "ontology/" + self.id)
        r.raise_for_status()
        return r.json()

    def source(self, processorName, parameters={}):
        r = requests.post(self.server.base + "node", data = {"class": processorName, "ontology" : self.id, "parameters": json.dumps(parameters)})
        r.raise_for_status()
        return Node(self.server, r.json())
    
class Node:
    def __init__(self, server, data):
        self.server = server
        self.id = data["id"]
        
    def __repr__(self):
        return str(self.info())
    
    def info(self):
        r = requests.get(self.server.base + "node/" + self.id)
        r.raise_for_status()
        return r.json()
    
    def into(self, processorName, parameters = {}):
        r = requests.post(self.server.base + "node", data = {"class": processorName, "input" : [self.id], "parameters": json.dumps(parameters)})
        r.raise_for_status()
        return Node(self.server, r.json())
    
    def lastProcessing(self):
        r = requests.get(self.server.base + "node/" + self.id + "/processing/last")
        r.raise_for_status()
        return Processing(self.server, r.json())

    def parameters(self):
        r = requests.get(self.server.base + "node/" + self.id + "/parameters")
        r.raise_for_status()
        return r.json()

    def setParameter(self, key, value):
        r = requests.post(self.server.base + "node/" + self.id + "/parameters", data = {"key": key, "value": json.dumps(value)})
        r.raise_for_status()

    def processings(self):
        r = requests.get(self.server.base + "node/" + self.id + "/processing")
        r.raise_for_status()
        return list(map(lambda processingData: Processing(self.server, processingData), r.json()))
    
    def load(self, file):
        r = requests.post(self.server.base + "node/" + self.id + "/load", files = {"file": file})
        r.raise_for_status()
        Processing(self.server, r.json()).raiseForStatus()
        return self

    def __add__(self, other):
        if self.server != other.server:
            raise ValueError("nodes must belonge to the same server")
        return Nodes(self.server, [self.id, other.id])

class Nodes:
    def __init__(self, server, nodeList):
        self.server = server
        self.nodeList = nodeList

    def into(self, processorName, parameters = {}):
        r = requests.post(self.server.base + "node", data = {"class": processorName, "input" : self.nodeList, "parameters": json.dumps(parameters)})
        r.raise_for_status()
        return Node(self.server, r.json())
    
    def __add__(self, other):
        if self.server != other.server:
            raise ValueError("nodes must belonge to the same server")
        return Nodes(self.server, self.nodeList + [other.id])

class Processing:
    def __init__(self, server, data):
        self.server = server
        self.id = data["id"]
        self.nodeId = data["node"]
        self.status = data["status"]
        self.stackTrace = data["stackTrace"]
        
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

    def raiseForStatus(self):
        if self.status != "SUCCEEDED" and not self.stackTrace.startswith("java.util.concurrent.ExecutionException"):
            raise Exception("Node " + str(self.server.getNode(self.nodeId)) + " has status " + self.status + ":\n" + self.stackTrace.replace("\\n","\n"))

class Execution:
    def __init__(self, server, data):
        self.server = server
        self.id = data["id"]
        self.processings = []
        self.resourcesData = {}
        for processingId in data["processings"]:
            self.processings.append(server.getProcessing(processingId))

    def results(self, resultType):
        r = requests.get(self.server.base + "execution/" + self.id + "/results", params = {"type": resultType})
        r.raise_for_status()
        return r.json()
    
    def resultDataFrame(self, resultType):
        return pd.DataFrame.from_records(self.results(resultType))

    def data(self, categoryName, ontologyId):
        if categoryName not in self.resourcesData:
            self.resourcesData[categoryName] = {}
        if ontologyId not in self.resourcesData[categoryName]:
            r = requests.get(self.server.base + "execution/" + self.id + "/data", params = {"category": categoryName, "ontology": ontologyId})
            r.raise_for_status()
            self.resourcesData[categoryName][ontologyId] = r.json()
        return self.resourcesData[categoryName][ontologyId]
    
    def dataDataFrame(self, categoryName, ontologyId):
        return pd.DataFrame.from_records(self.data(categoryName, ontologyId))
    
    def sortedOntologies(self, ontologyIds):
        return sorted({ ontologyId : self.server.getOntology(ontologyId).info()["label"] for ontologyId in ontologyIds }.items(), key = lambda x:x[1])
    
    def measurements(self):
        data = self.resultDataFrame("Measurement")
        if not data.empty:
            ontologies = self.sortedOntologies(set(data["ontology"]))
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
                for (ontoId, ontoLabel) in ontologies:
                    html += "<th>" + ontoLabel + "</th>"
                html += "</tr>"
                # total row
                totalData = measureData[measureData.dimension1Value.isna() & measureData.dimension2Value.isna()]
                if not totalData.empty:
                    html += "<tr>"
                    if dimension1Used:
                        html += "<td></td>"
                    if dimension2Used:
                        html += "<td></td>"
                    for (ontoId, ontoLabel) in ontologies:
                        row = totalData[totalData.ontology.eq(ontoId)]
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
                        for (ontoId, ontoLabel) in ontologies:
                            row = d1TotalData[d1TotalData.ontology.eq(ontoId)]
                            html += "<td>" + (str(row["value"].iat[-1]) if row.size > 0 else "") + "</td>"
                        html += "</tr>"
                    # dimension 2 rows
                    for dimension2Value in set(d1Data["dimension2Value"]):
                        d2Data = d1Data[d1Data.dimension2Value.notna() & d1Data.dimension2Value.eq(dimension2Value)]
                        if not d2Data.empty:
                            html += "<tr>"
                            html += "<td>" + dimension1Value + "</td>"
                            html += "<td>" + dimension2Value + "</td>"
                            for (ontoId, ontoLabel) in ontologies:
                                row = d2Data[d2Data.ontology.eq(ontoId)]
                                html += "<td>" + (str(row["value"].iat[-1]) if row.size > 0 else "") + "</td>"
                            html += "</tr>"
                html += "</table>"
                display(HTML(html))
    
    def deviations(self):
        display(HTML("<h2>Deviation Report</h2>"))
        totalData =  self.resultDataFrame("Deviation")
        if not totalData.empty:
            categoryNames = set(totalData["categoryName"])
            ontologies = self.sortedOntologies(set(totalData["ontologyId1"]).union(set(totalData["ontologyId2"])))
            # iterate categories
            for categoryName in categoryNames:
                display(HTML("<h3>Category: " + categoryName + "</h3>"))
                categoryData = totalData[totalData.categoryName.eq(categoryName)]
                ontoPairs = categoryData.filter(["ontologyId1","ontologyId2"]).drop_duplicates().sort_values(["ontologyId1","ontologyId2"])
                 # iterate ontologies
                for (onto1Id, onto1Label) in ontologies:
                    for (onto2Id, onto2Label) in ontologies:
                        if onto1Id != onto2Id and onto1Label < onto2Label:
                            ontosData = categoryData[categoryData.ontologyId1.eq(onto1Id) & categoryData.ontologyId2.eq(onto2Id)]
                            if not ontosData.empty:
                                ontosData = ontosData.sort_values(["ontologyId1","ontologyId2"])
                                table = "<table>"
                                table += "<tr>"
                                table += "<th style=\"text-align:center;\" colspan=\"3\">" + onto1Label + "</th>"
                                table += "<th style=\"text-align:center;\" colspan=\"3\">" + onto2Label + "</th>"
                                table += "</tr>"
                                resourcePairs = ontosData.filter(["resource1","resource2"]).drop_duplicates().sort_values(["resource1","resource2"])
                                # iterate resources
                                for index, resourcePair in resourcePairs.iterrows():
                                    resourceData = ontosData[ontosData.resource1.eq(resourcePair["resource1"]) & ontosData.resource2.eq(resourcePair["resource2"])]
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
            totalData = totalData.sort_values(["ontology","entity"])
            ontologies = self.sortedOntologies(set(totalData["ontology"]))
            for (ontoId, ontoLabel) in ontologies:
                display(HTML("<h3>Ontology: " + ontoLabel + "</h3>"))
                ontoData = totalData[totalData.ontology.eq(ontoId)]
                table = "<table>"
                table += "<tr>"
                table += "<th>" + "Issue Type" + "</th>"
                table += "<th>" + "Affected Entity" + "</th>"
                table += "<th>" + "Message" + "</th>"
                table += "</tr>"
                for index, issue in ontoData.iterrows():
                    table += "<tr>"
                    table += "<td>" + issue["type"] + "</td>"
                    table += "<td>" + issue["entity"] + "</td>"
                    table += "<td>" + issue["message"] + "</td>"
                    table += "</tr>"
                table += "</table>"
                display(HTML(table))
                
    def mappings(self, manualMappingNode):
        # symboles
        accepted = "✓"
        retained = "?"
        rejected = "✗"

        output = widgets.Output()

        mappings = {}
        def getMappings():
            with output:
                if not mappings:
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
                return mappings

        resourceDataWidgets = {}
        def getResourceDataWidget(categoryName, ontoId, resource, resourceData):
            with output:
                if categoryName not in resourceDataWidgets:
                    resourceDataWidgets[categoryName] = {}
                if ontoId not in resourceDataWidgets[categoryName]:
                    resourceDataWidgets[categoryName][ontoId] = {}
                if resource not in resourceDataWidgets[categoryName][ontoId]:
                    html = "<dl>"
                    if any(list(resourceData)):
                        for key in sorted(resourceData):
                            html += "<dt>" + key + "</dt>"
                            html += "<dd>" + ", ".join(resourceData[key]) + "</dd>"
                    html += "</dl>"
                    resourceDataWidgets[categoryName][ontoId][resource] = widgets.HTML(value=html)
                return resourceDataWidgets[categoryName][ontoId][resource]

        newMappingResourceFormWidgets = {}
        def getNewMappingResourceFormWidget(categoryName, ontoId):
            with output:
                if categoryName not in newMappingResourceFormWidgets:
                    newMappingResourceFormWidgets[categoryName] = {}
                if ontoId not in newMappingResourceFormWidgets[categoryName]:
                    newMappingResourceFormWidgets[categoryName][ontoId] = widgets.Text(value='', placeholder='Resource to map', layout={'width':'available'})
                return newMappingResourceFormWidgets[categoryName][ontoId]

        resourceButtonWidgets = {}
        def getResourceButtonWidget(categoryName, ontoId, resource):
            with output:
                if categoryName not in resourceButtonWidgets:
                    resourceButtonWidgets[categoryName] = {}
                if ontoId not in resourceButtonWidgets[categoryName]:
                    resourceButtonWidgets[categoryName][ontoId] = {}
                if resource not in resourceButtonWidgets[categoryName][ontoId]:
                    newMappingResourceFormWidget = getNewMappingResourceFormWidget(categoryName, ontoId)
                    button = widgets.Button(description=resource, tooltip='Use for new Mapping', layout={'min_width': 'max-content'})
                    def use(b):
                        newMappingResourceFormWidget.value = resource
                    button.on_click(use)
                    resourceButtonWidgets[categoryName][ontoId][resource] = button
                return resourceButtonWidgets[categoryName][ontoId][resource]
        
        resourceWidgets = {}
        def getResourceWidget(categoryName, ontoId, resource, resourceData):
            with output:
                if categoryName not in resourceWidgets:
                    resourceWidgets[categoryName] = {}
                if ontoId not in resourceWidgets[categoryName]:
                    resourceWidgets[categoryName][ontoId] = {}
                if resource not in resourceWidgets[categoryName][ontoId]:
                    resourceButtonWidget = getResourceButtonWidget(categoryName, ontoId, resource)
                    resourceDataWidget = getResourceDataWidget(categoryName, ontoId, resource, resourceData)
                    resourceWidgets[categoryName][ontoId][resource] = widgets.VBox([resourceButtonWidget, resourceDataWidget], layout={'border':'solid 1px lightgrey', 'min_height':'max-content', 'width':'available', 'min_width':'min-content'})
                return resourceWidgets[categoryName][ontoId][resource]
        
        unmappedResourcesWidgets = {}
        def getUnmappedResourcesWidget(categoryName, ontoId):
            with output:
                if categoryName not in unmappedResourcesWidgets:
                    unmappedResourcesWidgets[categoryName] = {}
                if ontoId not in unmappedResourcesWidgets[categoryName]:
                    resourcesData = self.data(categoryName, ontoId)
                    
                    unmapped = []
                    for unmappedResource in set(resourcesData)-set(getMappings())-set(manualPositiveMappings):
                        unmapped.append(getResourceWidget(categoryName, ontoId, unmappedResource, resourcesData[unmappedResource]))
                    unmappedResourcesWidgets[categoryName][ontoId] = widgets.VBox(unmapped,layout={'width':'50%', 'max_height':'25em', 'overflow_y':'scroll', 'display':'block'})
                return unmappedResourcesWidgets[categoryName][ontoId]

        resourcePairWidgets = {}
        def resourcePairWidget(categoryName, onto1Id, onto2Id, resource1, resource2, resource1Data, resource1Data2, value):
            with output:
                resourceWidget1 = getResourceWidget(categoryName, onto1Id, resource1, resource1Data)
                resourceWidget2 = getResourceWidget(categoryName, onto2Id, resource2, resource2Data)                    
                buttons = widgets.ToggleButtons(options=[accepted,retained,rejected],value=value,tooltips=["Accepted", "Undecided", "Rejecte"], style={'button_width':'auto'}, layout={'min_width':'max-content', 'height':'available'})
                resourcePairWidget = widgets.HBox([buttons, resourceWidget1, resourceWidget2], layout={'border':'solid 1px lightgrey', 'min_height':'max-content', 'min_width':'available'})
                resourcePairWidgets[resourcePairWidget] = [resource1,resource2]
                return resourcePairWidget

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
        ontologies = self.sortedOntologies(set(categoryData["ontology"]))
        # get manual mapping parameters
        manualMappingParameters = manualMappingNode.parameters()["parameters"]
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

        newMappingSinks = []
        categoryTabChildren = []
        categoryTabTitles = []
        for categoryName in categories:
            ontoTabChildrens = []
            ontoTabTitles = []
            for (onto1Id, onto1Label) in ontologies:
                for (onto2Id, onto2Label) in ontologies:
                    if (onto1Label < onto2Label):
                        pairs = []
                        resources1Data = self.data(categoryName, onto1Id)
                        resources2Data = self.data(categoryName, onto2Id)
                        for resource1 in resources1Data:
                            resource1Data = resources1Data[resource1]
                            # add positive manual mappings
                            if resource1 in manualPositiveMappings:
                                for resource2 in manualPositiveMappings[resource1]:
                                    if resource2 in resources2Data:
                                        resource2Data = resources2Data[resource2]
                                        pair = resourcePairWidget(categoryName, onto1Id, onto2Id, resource1, resource2, resource1Data, resource2Data, accepted)
                                        pairs.append(pair)
                            # add negative manual mappings
                            if resource1 in manualNegativeMappings:
                                for resource2 in manualNegativeMappings[resource1]:
                                    if resource2 in resources2Data:
                                        resource2Data = resources2Data[resource2]
                                        pair = resourcePairWidget(categoryName, onto1Id, onto2Id, resource1, resource2, resource1Data, resource2Data, rejected)
                                        pairs.append(pair)
                            # add none manual mappings
                            if resource1 in getMappings():
                                for resource2 in mappings[resource1]:
                                    if resource2 in resources2Data and not (
                                        resource1 in manualPositiveMappings and resource2 in manualPositiveMappings[resource1] or
                                        resource1 in manualNegativeMappings and resource2 in manualNegativeMappings[resource1] ):
                                        resource2Data = resources2Data[resource2]
                                        pair = resourcePairWidget(categoryName, onto1Id, onto2Id, resource1, resource2, resource1Data, resource2Data, retained)
                                        pairs.append(pair)
                        # widgets management
                        newMappingSink = widgets.VBox([],layout={'max_height':'25em', 'overflow_y':'scroll', 'display':'block'})
                        pairTab = widgets.VBox([
                            widgets.HTML(value="<h6>Present mappings</h6><p>Click the button to accept or reject the mapping. Mappings can be filtered to display only undesided pairs using the button below.</p>"),
                            widgets.VBox(pairs, layout={'max_height':'25em', 'overflow_y':'scroll', 'display':'block'}),
                            widgets.HTML(value="<h6>Unmapped resources</h6>"),
                            widgets.HBox([
                                getUnmappedResourcesWidget(categoryName, onto1Id),
                                getUnmappedResourcesWidget(categoryName, onto2Id)
                            ]),
                            widgets.HTML(value="<h6>Add further mappings</h6><p>To select a resurce, click on the IRIs.</p>"),
                            unmappedPairingWidget(getNewMappingResourceFormWidget(categoryName, onto1Id), getNewMappingResourceFormWidget(categoryName, onto2Id), newMappingSink)
                        ])
                        ontoTabChildrens.append(pairTab)
                        newMappingSinks.append(newMappingSink)
                        ontoTabTitles.append(onto1Label + " <-> " + onto2Label)
            # widgets management
            ontoTabs = widgets.Tab(children=ontoTabChildrens)
            for i, title in enumerate(ontoTabTitles):
                ontoTabs.set_title(i, title)    
            categoryTabChildren.append(ontoTabs)
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
                manualMappingParameters = manualMappingNode.parameters()["parameters"]
                manualPositiveMappings = manualMappingParameters["mappings"] if manualMappingParameters["mappings"] else []
                manualNegativeMappings = manualMappingParameters["suppressed_mappings"] if manualMappingParameters["suppressed_mappings"] else []
                # update local manual mapping data
                for resourcePairWidget in resourcePairWidgets:
                    if resourcePairWidget.children[0].value == accepted:
                        while resourcePairWidgets[resourcePairWidget] not in manualPositiveMappings: manualPositiveMappings.append(resourcePairWidgets[resourcePairWidget])
                        while resourcePairWidgets[resourcePairWidget] in manualNegativeMappings: manualNegativeMappings.remove(resourcePairWidgets[resourcePairWidget])
                    if resourcePairWidget.children[0].value == retained:
                        while resourcePairWidgets[resourcePairWidget] in manualPositiveMappings: manualPositiveMappings.remove(resourcePairWidgets[resourcePairWidget])
                        while resourcePairWidgets[resourcePairWidget] in manualNegativeMappings: manualNegativeMappings.remove(resourcePairWidgets[resourcePairWidget])
                    elif resourcePairWidget.children[0].value == rejected:
                        while resourcePairWidgets[resourcePairWidget] in manualPositiveMappings: manualPositiveMappings.remove(resourcePairWidgets[resourcePairWidget])
                        while resourcePairWidgets[resourcePairWidget] not in manualNegativeMappings: manualNegativeMappings.append(resourcePairWidgets[resourcePairWidget])
                for newMappingSink in newMappingSinks:
                    for newMapping in newMappingSink.children:
                        manualPositiveMappings.append([newMapping.children[0].value, newMapping.children[2].value])
                # update remote manual mapping data
                manualMappingNode.setParameter("mappings", manualPositiveMappings)
                manualMappingNode.setParameter("suppressed_mappings", manualNegativeMappings)
                display(HTML("Manual Mappings updated."))
        updateButton.on_click(updateMappings)
        def hide(b):
            with output:
                for resourcePairWidget in resourcePairWidgets:
                    if resourcePairWidget.children[0].value != retained:
                        resourcePairWidget.layout.display = "none"
                hideButton.layout.display = "none"
                showButton.layout.display = "inline-flex"
        hideButton.on_click(hide)
        def show(b):
            with output:
                for resourcePairWidget in resourcePairWidgets:
                    if resourcePairWidget.children[0].value != retained:
                        resourcePairWidget.layout.display = "inline-flex"
                hideButton.layout.display = "inline-flex"
                showButton.layout.display = "none"
        showButton.on_click(show)
