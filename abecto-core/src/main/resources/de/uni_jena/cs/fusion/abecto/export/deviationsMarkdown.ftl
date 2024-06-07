<#ftl output_format="plainText">
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
