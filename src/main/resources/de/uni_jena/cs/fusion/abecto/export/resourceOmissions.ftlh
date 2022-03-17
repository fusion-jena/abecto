aspect,missedInDataset,missingResource,foundInDataset
<#list data as row>
"${row.aspect!}","${row.missedInDataset}","${row.missingResource}","${row.foundInDataset}"
</#list>
