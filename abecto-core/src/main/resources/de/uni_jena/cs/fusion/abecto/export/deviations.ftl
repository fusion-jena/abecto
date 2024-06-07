<#ftl output_format="plainText">
aspect,affectedGraph,affectedResource,affectedVariableName,affectedValue,comparedToValue,comparedToResource,comparedToDataset,mappedBy,snippetToAnnotateValueComparedToAsWrong
<#list data as row>
"${row.aspect!}","${row.affectedGraph!}","${row.affectedResource!}","${row.affectedVariableName!}","${row.affectedValue!}","${row.comparedToValue!}","${row.comparedToResource!}","${row.comparedToDataset!}","${row.mappedBy!}","${row.snippetToAnnotateValueComparedToAsWrong!}"
</#list>
