#
# Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

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
import re
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

    def project(self, name):
        r = requests.post(self.base + "project", data = {"name": name, "useIfExists": "true"})
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

    def ontology(self, name):
        r = requests.post(self.server.base + "ontology", data = {"project": self.id, "name": name, "useIfExists": "true"})
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
    
    def load(self, file=None):
        if file:
            r = requests.post(self.server.base + "node/" + self.id + "/load", files = {"file": file})
        else:
            r = requests.post(self.server.base + "node/" + self.id + "/load")
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
        if self.status != "SUCCEEDED" and (not self.stackTrace or not self.stackTrace.startswith("java.util.concurrent.ExecutionException")):
            raise Exception("Node " + str(self.server.getNode(self.nodeId)) + " has status " + self.status + ":\n" + (self.stackTrace.replace("\\n","\n") if self.stackTrace else ""))

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

    def metadata(self):
        r = requests.get(self.server.base + "execution/" + self.id + "/metadata")
        r.raise_for_status()
        ontologies = r.json()
        usedMetadata = set()
        for ontology in ontologies:
            sources = ontologies[ontology]
            for source in sources:
                usedMetadata.update(sources[source].keys())
        table = "<table>\n"
        table += "<tr>"
        table += "<th>Ontology</th>"
        #table += "<th>Source</th>"
        table += "<th>Processor</th>"
        table += "<th>Parameter</th>"
        table += "<th>Loading DateTime</th>"
        if "iri" in usedMetadata:
            table += "<th>Ontology IRI</th>"
        if "version" in usedMetadata:
            table += "<th>Ontology Version</th>"
        if "versionDateTime" in usedMetadata:
            table += "<th>Ontology Date</th>"
        if "versionIri" in usedMetadata:
            table += "<th>Ontology Version IRI</th>"
        table += "</tr>\n"
        for ontology in self.sortedOntologies(ontologies.keys()):
            ontologyNameDisplayed = False
            sources = ontologies[ontology[0]]
            for source in sources:
                table += "<tr>"
                if not ontologyNameDisplayed:
                    table += "<td rowspan=\"" + str(len(sources)) + "\">" + ontology[1] + "</td>"
                    ontologyNameDisplayed = True
                #table += "<td>" + source + "</td>"
                table += "<td>" + sources[source]["processor"].rsplit(".",1)[1] + "</td>"
                if sources[source]["parameter"]:
                    table += "<td>" + sources[source]["parameter"][1:-1] + "</td>"
                else:
                    table += "<td></td>"
                table += "<td>" + sources[source]["loading datetime"] + "</td>"
                if "iri" in usedMetadata:
                    table += "<td>" + sources[source].get("iri","") + "</td>"
                if "version" in usedMetadata:
                    table += "<td>" + sources[source].get("version","") + "</td>"
                if "versionDateTime" in usedMetadata:
                    table += "<td>" + sources[source].get("versionDateTime","") + "</td>"
                if "versionIri" in usedMetadata:
                    table += "<td>" + sources[source].get("versionIri","") + "</td>"
                table += "</tr>\n"
        table += "</table>"
        display(HTML(table))

    def dataDataFrame(self, categoryName, ontologyId):
        return pd.DataFrame.from_records(self.data(categoryName, ontologyId))

    def sortedOntologies(self, ontologyIds):
        return sorted({ ontologyId : self.server.getOntology(ontologyId).info()["name"] for ontologyId in ontologyIds }.items(), key = lambda x:x[1])

    def __formatValue(self,value):
        return re.compile('(.*)(\^\^[^"]*)').sub(r'\1<span style="opacity:0.6;">\2</span>', value)

    def measurements(self):
        data = self.resultDataFrame("Measurement")
        if not data.empty:
            ontologies = self.sortedOntologies(set(data["ontology"]))
            # replace ontology ids in dimension values by ontology name
            data["dimension1Value"] = data["dimension1Value"].replace(dict(ontologies))
            data["dimension2Value"] = data["dimension2Value"].replace(dict(ontologies))
            # iterate measures
            for measure in sorted(set(data["measure"])):
                html = "<h1>" + measure + "</h1>"
                measureData = data[data.measure.eq(measure)]
                dimension1Used = any(set(measureData["dimension1Value"]))
                dimension2Used = any(set(measureData["dimension2Value"]))
                html += "<table>\n"
                html += "<tr>"
                if dimension1Used:
                    html += "<th>" + "/".join(set(measureData["dimension1Key"])) + "</th>"
                if dimension2Used:
                    html += "<th>" + "/".join(set(measureData["dimension2Key"])) + "</th>"
                for (ontoId, ontoName) in ontologies:
                    html += "<th>" + ontoName + "</th>"
                html += "</tr>\n"
                # total row
                totalData = measureData[measureData.dimension1Value.isna() & measureData.dimension2Value.isna()]
                if not totalData.empty:
                    html += "<tr>"
                    if dimension1Used:
                        html += "<td></td>"
                    if dimension2Used:
                        html += "<td></td>"
                    for (ontoId, ontoName) in ontologies:
                        row = totalData[totalData.ontology.eq(ontoId)]
                        html += "<td>" + (str(row["value"].iat[-1]) if row.size > 0 else "") + "</td>"
                    html += "</tr>\n"
                # dimension 1 rows
                nototalData = measureData[measureData.dimension1Value.notna()]
                for dimension1Value in sorted(set(nototalData["dimension1Value"])):
                    d1Data = nototalData[nototalData.dimension1Value.eq(dimension1Value)]
                    # dimension 1 total row
                    d1TotalData = d1Data[d1Data.dimension2Value.isna()]
                    if not d1TotalData.empty:
                        html += "<tr>"
                        html += "<td>" + dimension1Value + "</td>"
                        if dimension2Used:
                            html += "<td></td>"
                        for (ontoId, ontoName) in ontologies:
                            row = d1TotalData[d1TotalData.ontology.eq(ontoId)]
                            html += "<td>" + (str(row["value"].iat[-1]) if row.size > 0 else "") + "</td>"
                        html += "</tr>\n"
                    # dimension 2 rows
                    d1NontotalData = d1Data[d1Data.dimension2Value.notna()]
                    for dimension2Value in sorted(set(d1NontotalData["dimension2Value"])):
                        d2Data = d1NontotalData[d1NontotalData.dimension2Value.eq(dimension2Value)]
                        if not d2Data.empty:
                            html += "<tr>"
                            html += "<td>" + dimension1Value + "</td>"
                            html += "<td>" + dimension2Value + "</td>"
                            for (ontoId, ontoName) in ontologies:
                                row = d2Data[d2Data.ontology.eq(ontoId)]
                                html += "<td>" + (str(row["value"].iat[-1]) if row.size > 0 else "") + "</td>"
                            html += "</tr>\n"
                html += "</table>"
                display(HTML(html))
    
    def deviations(self):
        display(HTML("<h2>Deviation Report</h2>"))
        totalData =  self.resultDataFrame("Deviation")
        if not totalData.empty:
            categoryNames = set(totalData["categoryName"])
            ontologies = self.sortedOntologies(set(totalData["ontologyId1"]).union(set(totalData["ontologyId2"])))
            # iterate categories
            for categoryName in sorted(set(categoryNames)):
                display(HTML("<h3>Category: " + categoryName + "</h3>"))
                categoryData = totalData[totalData.categoryName.eq(categoryName)]
                 # iterate ontologies
                for (ontology1Id, ontology1Name) in ontologies:
                    for (ontology2Id, ontology2Name) in ontologies:
                        if ontology1Id != ontology2Id and ontology1Name < ontology2Name:
                            ontologiesData = categoryData[categoryData.ontologyId1.eq(ontology1Id) & categoryData.ontologyId2.eq(ontology2Id)]
                            ontologiesData = ontologiesData.append(
                                        categoryData[categoryData.ontologyId1.eq(ontology2Id) & categoryData.ontologyId2.eq(ontology1Id)]\
                                        .rename(columns={"resource1": "resource2", "resource2": "resource1", "value1": "value2", "value2": "value1"}),
                                        sort=True)
                            if not ontologiesData.empty:
                                ontologiesData = ontologiesData.sort_values(["ontologyId1","ontologyId2"])
                                table = "<div style=\"max-height:30em\"><table>\n"
                                table += "<tr>"
                                table += "<th style=\"text-align:center;\" colspan=\"3\">" + ontology1Name + "</th>"
                                table += "<th style=\"text-align:center;\" colspan=\"3\">" + ontology2Name + "</th>"
                                table += "</tr>\n"
                                resourcePairs = ontologiesData.filter(["resource1","resource2"]).drop_duplicates().sort_values(["resource1","resource2"])
                                # iterate resources
                                for index, resourcePair in resourcePairs.iterrows():
                                    resourceData = ontologiesData[ontologiesData.resource1.eq(resourcePair["resource1"]) & ontologiesData.resource2.eq(resourcePair["resource2"])]
                                    resourceData = resourceData.sort_values(["resource1","resource2"])
                                    variablesCount = len(resourceData)
                                    firstRow = True
                                    for index, row in resourceData.iterrows():
                                        table += "<tr>"
                                        if firstRow:
                                            table += "<td rowspan=\"" + str(variablesCount) + "\"><a href=\"" + resourcePair["resource1"] + "\">" + resourcePair["resource1"] + "</a></td>"
                                        table += "<td>" + row.variableName + "</td>"
                                        table += "<td>" + self.__formatValue(html.escape(row.value1 if not row.isna().value1 else "")) + "</td>"
                                        table += "<td>" + self.__formatValue(html.escape(row.value2 if not row.isna().value2 else "")) + "</td>"
                                        table += "<td>" + row.variableName + "</td>"
                                        if firstRow:
                                            table += "<td rowspan=\"" + str(variablesCount) + "\"><a href=\"" + resourcePair["resource2"] + "\">" + resourcePair["resource2"] + "</a></td>"
                                            firstRow = False
                                        table += "</tr>\n"
                                table += "</table></div>"
                                display(HTML(table))

    def issues(self):
        display(HTML("<h2>Issue Report</h2>"))
        totalData =  self.resultDataFrame("Issue")
        if not totalData.empty:
            totalData = totalData.sort_values(["ontology","entity"])
            ontologies = self.sortedOntologies(set(totalData["ontology"]))
            for (ontoId, ontoName) in ontologies:
                display(HTML("<h3>Ontology: " + ontoName + "</h3>"))
                ontoData = totalData[totalData.ontology.eq(ontoId)]
                table = "<table>\n"
                table += "<tr>"
                table += "<th>" + "Issue Type" + "</th>"
                table += "<th>" + "Affected Entity" + "</th>"
                table += "<th>" + "Message" + "</th>"
                table += "</tr>\n"
                for index, issue in ontoData.iterrows():
                    table += "<tr>"
                    table += "<td>" + issue["type"] + "</td>"
                    table += "<td>" + issue["entity"] + "</td>"
                    table += "<td>" + issue["message"] + "</td>"
                    table += "</tr>\n"
                table += "</table>"
                display(HTML(table))
                
    def omissions(self):
        display(HTML("<h2>Omission Report</h2>"))
        data =  self.resultDataFrame("Omission")
        if not data.empty:
            # replace ontology ids by ontology name
            ontologyList = set(data["ontology"])
            ontologyList.update(data["source"])
            ontologies = self.sortedOntologies(ontologyList)
            data["ontology"] = data["ontology"].replace(dict(ontologies))
            data["source"] = data["source"].replace(dict(ontologies))
            # iterate categories
            for categoryName in sorted(set(data["categoryName"])):
                display(HTML("<h3>Category: " + categoryName + "</h3>"))
                categoryData = data[data.categoryName.eq(categoryName)]
                # iterate ontology pairs
                for ontology1 in sorted(set(categoryData["ontology"])):
                    for ontology2 in sorted(set(categoryData["source"])):
                        if (ontology1 < ontology2):
                            html = "<div>"
                            ontology1Data = categoryData[categoryData.ontology.eq(ontology1) & categoryData.source.eq(ontology2)]
                            ontology2Data = categoryData[categoryData.ontology.eq(ontology2) & categoryData.source.eq(ontology1)]
                            # iterate ontologies of ontology pair
                            for (ontology, ontologyData) in [(ontology1,ontology1Data),(ontology2,ontology2Data)]:
                                html += "<div style=\"float:left;width:50%\">"
                                html += "<h4>Resources from " + (ontology1 if ontology2 == ontology else ontology2) + " missing in " + ontology + "</h4>"
                                html += "<ul style=\"max-height:30em;overflow-x:scroll\">\n"
                                # iterate omitted resources
                                for omittedResource in sorted(set(ontologyData["omittedResource"])):
                                    html += "<li><a href=\"" + omittedResource + "\">" + omittedResource + "</a>\n"
                                html += "</ul>"
                                html += "</div>"
                            html += "</div>"
                            display(HTML(html))

    def mappings(self):
        display(HTML("<h2>Mapping Report</h2>"))
        mappings = self.resultDataFrame("Mapping")
        mappings = mappings[mappings.resourcesMap][["resource1","resource2"]]
        if not mappings.empty:
            categories = self.resultDataFrame("Category")
            for categoryName in sorted(set(categories["name"])):
                display(HTML("<h3>Category: " + categoryName + "</h3>"))
                ontologies = set(categories[categories.name.eq(categoryName)]["ontology"])
                ontologies = self.sortedOntologies(ontologies)
                resources = {}
                # get resources of category by ontologies
                for (ontologyId, ontologyName) in ontologies:
                    resources[ontologyId] = set(self.data(categoryName, ontologyId).keys())
                for (ontologyId1, ontologyName1) in ontologies:
                    for (ontologyId2, ontologyName2) in ontologies:
                        if (ontologyName1 < ontologyName2):
                            # prepare data
                            mappingsOfPair = mappings[
                                mappings.resource1.isin(resources[ontologyId1]) &
                                mappings.resource2.isin(resources[ontologyId2])
                            ][["resource1","resource2"]]\
                            .rename(columns={"resource1": ontologyName1, "resource2": ontologyName2})
                            mappingsOfPair = mappingsOfPair.append(
                                mappings[
                                    mappings.resource2.isin(resources[ontologyId1]) &
                                    mappings.resource1.isin(resources[ontologyId2])
                                ][["resource2","resource1"]]\
                                .rename(columns={"resource2": ontologyName1, "resource1": ontologyName2}))
                            mappingsOfPair.sort_values(by=[ontologyName1,ontologyName2], inplace=True)
                            # display
                            html = "<div style=\"max-height:30em;overflow-x:scroll\">"
                            html += "<table>\n"
                            html += "<tr><th>" + ontologyName1 + "</th><th>" + ontologyName2 + "</th></tr>"
                            for index, row in mappingsOfPair.iterrows():
                                html += "<tr>"
                                html += "<td><a href=\"" + str(row[0]) + "\">" + str(row[0]) + "</a></td>"
                                html += "<td><a href=\"" + str(row[1]) + "\">" + str(row[1]) + "</a></td>"
                                html += "</tr>\n"
                            html += "</table>"
                            html += "</div>"
                            display(HTML(html))
