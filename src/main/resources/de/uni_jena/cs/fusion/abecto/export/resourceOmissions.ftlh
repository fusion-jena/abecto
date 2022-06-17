aspect,missedInDataset,missingResource,missingResourceLabel,foundInDataset
<#list data as row>
"${row.aspect!}","${row.missedInDataset}","${row.missingResource}","${row.missingResourceLabelConcat!}","${row.foundInDataset}"
</#list>
