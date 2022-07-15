<#ftl output_format="plainText">
<#--

    Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->

# Measurements

<#list data?sort_by("affectedAspect") as aspectRow>
	<#assign aspect = aspectRow.affectedAspect>
	<#if !prevAspect?? || prevAspect!=aspect>
		<#assign prevAspect = aspect>
		<#assign aspectData = data?filter(r -> r.affectedAspect == aspect)>

		<#compress>
		## ${aspect}
		</#compress>

		<#list aspectData as measurementRow>
			<#assign measurement = measurementRow.measurementTypeName>
			<#if !prevMeasurement?? || prevMeasurement!=measurement>
				<#assign prevMeasurement = measurement>
				<#assign description = measurementRow.measurementTypeDescription>
				<#assign measurementData = aspectData?filter(r -> r.measurementTypeName == measurement)>

				<#compress>
				### ${measurement?capitalize}

				${description}
				</#compress>

				<#-- preserve this two empty lines (above and below) -->

				<#if (measurementData?map(r->r.affectedVariableName???then(1,0))?max == 1)>
					<#-- TODO affected variable -->
					Measurements with affected variables not supported by this report yet.
				<#else>
					<#-- no affected variable -->
					<#compress>
						<#if (measurementData?map(r->r.comparedToDatasetsCount?number)?max == 1)>
							<#-- compared to one dataset -->
	
							<#assign computedOnDatasets = []>
							<#list measurementData?map(r -> r.computedOnDataset) as dataset>
								<#if !computedOnDatasets?seq_contains(dataset)>
									<#assign computedOnDatasets = computedOnDatasets + [dataset]>
								</#if>
							</#list>
	
							<#assign comparedToDatasets = []>
							<#list measurementData?map(r -> r.comparedToDatasets) as datasets>
								<#if !comparedToDatasets?seq_contains(datasets)>
									<#assign comparedToDatasets = comparedToDatasets + [datasets]>
								</#if>
							</#list>
	
							| Dataset |<#list comparedToDatasets?sort as comparedToDatasets> Compared to ${comparedToDatasets} |</#list>
							| ------- |<#list comparedToDatasets?sort as comparedToDatasets> --- |</#list>
							<#list computedOnDatasets?sort as computedOnDataset>
								| ${computedOnDataset} |<#list comparedToDatasets?sort as comparedToDatasets> ${measurementData?filter(r->r.comparedToDatasets == comparedToDatasets && r.computedOnDataset == computedOnDataset)?map(r->[r.value, r.unitSymbol!]?join(" ")?trim)?first!"-"} |</#list>
							</#list>
	
						<#elseif (measurementData?map(r->r.comparedToDatasetsCount?number)?max > 1)>
							<#-- compared to multiple datasets -->
	
							| Dataset | Value | Compared to |
							| ------- | ----- | ----------- |
							<#list measurementData?sort_by("computedOnDataset") as row>
								| ${row.computedOnDataset} | ${row.value!"-"}<#if row.unitSymbol??> ${row.unitSymbol}</#if> | ${row.comparedToDatasets} |
							</#list>
	
						<#else>
							<#-- compared to zero datasets -->
	
							| Dataset | Value |
							| ------- | ----- |
							<#list measurementData?sort_by("computedOnDataset") as row>
								| ${row.computedOnDataset} | ${row.value!"-"}<#if row.unitSymbol??> ${row.unitSymbol}</#if> |
							</#list>
	
						</#if>
					</#compress>
				</#if>

			</#if>
		</#list>

	</#if>
</#list>
