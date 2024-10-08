<#ftl output_format="plainText">

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
				<#assign hasAffectedVariable = (measurementData?map(r->r.affectedVariableName???then(1,0))?max == 1)>

				<#compress>
				### ${measurement?capitalize}

				${description}
				</#compress>

				<#-- preserve this two empty lines (above and below) -->

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

						<#assign variables = []>
						<#if hasAffectedVariable>
							<#list measurementData?filter(r -> r.affectedVariableName??)?map(r -> r.affectedVariableName) as variable>
								<#if !variables?seq_contains(variable)>
									<#assign variables = variables + [variable]>
								</#if>
							</#list>
						</#if>

						| Dataset |<#if hasAffectedVariable> Variable |</#if><#list comparedToDatasets?sort as comparedToDatasets> Compared to ${comparedToDatasets} |</#list>
						| ------- |<#if hasAffectedVariable> -------- |</#if><#list comparedToDatasets?sort as comparedToDatasets> --- |</#list>
						<#list computedOnDatasets?sort as computedOnDataset>
							| ${computedOnDataset} |<#if hasAffectedVariable> |</#if><#list comparedToDatasets?sort as comparedToDatasets> ${measurementData?filter(r->r.comparedToDatasets == comparedToDatasets && r.computedOnDataset == computedOnDataset && !r.affectedVariableName??)?map(r->[r.value, r.unitSymbol!]?join(" ")?trim)?first!"-"} |</#list>
							<#if hasAffectedVariable>
								<#list variables?sort as variable>
									| ${computedOnDataset} | ${variable} |<#list comparedToDatasets?sort as comparedToDatasets> ${measurementData?filter(r->r.comparedToDatasets == comparedToDatasets && r.computedOnDataset == computedOnDataset && r.affectedVariableName?? && r.affectedVariableName == variable)?map(r->[r.value, r.unitSymbol!]?join(" ")?trim)?first!"-"} |</#list>
								</#list>
							</#if>
						</#list>

					<#elseif (measurementData?map(r->r.comparedToDatasetsCount?number)?max > 1)>
						<#-- compared to multiple datasets -->

						| Dataset |<#if hasAffectedVariable> Variable |</#if> Value | Compared to |
						| ------- |<#if hasAffectedVariable> -------- |</#if> ----- | ----------- |
						<#list measurementData?sort_by("computedOnDataset") as row>
							| ${row.computedOnDataset} |<#if hasAffectedVariable> ${row.affectedVariableName!} |</#if> ${row.value!"-"}<#if row.unitSymbol??> ${row.unitSymbol}</#if> | ${row.comparedToDatasets} |
						</#list>

					<#else>
						<#-- compared to zero datasets -->

						| Dataset |<#if hasAffectedVariable> Variable |</#if>  Value |
						| ------- |<#if hasAffectedVariable> -------- |</#if>  ----- |
						<#list measurementData?sort_by("computedOnDataset") as row>
							| ${row.computedOnDataset} |<#if hasAffectedVariable> ${row.affectedVariableName!} |</#if> ${row.value!"-"}<#if row.unitSymbol??> ${row.unitSymbol}</#if> |
						</#list>

					</#if>
				</#compress>

			</#if>
		</#list>

	</#if>
</#list>
