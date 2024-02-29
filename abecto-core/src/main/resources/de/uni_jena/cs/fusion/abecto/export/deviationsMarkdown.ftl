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
<#list data?sort_by("affectedGraph") as graphRow>
	<#assign graph = graphRow.affectedGraph>
	<#if !prevGraph?? || prevGraph!=graph>
		<#assign prevGraph = graph>
		<#assign graphData = data?filter(r -> r.affectedGraph == graph)>

		<#compress>
		## Dataset: ${graph}
		</#compress>

        <#list graphData as aspectRow>
            <#assign aspect = aspectRow.aspect>

            <#if !prevAspect?? || prevAspect!=aspect>
                <#assign prevAspect = aspect>
                <#assign aspectData = graphData?filter(r -> r.aspect == aspect)>

                <#compress>
                ### Aspect: ${aspect}

                | Resource | Variable Name | Value | Compared Value | Compared Resource | Compared Dataset | Mapped By | Wrong Compared Value Annotate Snippet |
                |---|---|---|---|---|---|---|---|
                <#list aspectData as row>
                | ${row.affectedResource!} | ${row.affectedVariableName!} | ${row.affectedValue!} | ${row.comparedToValue!} | ${row.comparedToResource!} | ${row.comparedToDataset!} | ${row.mappedBy!} | `${row.snippetToAnnotateValueComparedToAsWrong!}` |
                </#list>
                </#compress>

            </#if>
        </#list>
        <#assign prevAspect = "">

	</#if>
</#list>
