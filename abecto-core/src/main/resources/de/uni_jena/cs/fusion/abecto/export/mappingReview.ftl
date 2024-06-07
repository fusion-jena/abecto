<#ftl output_format="plainText">
aspect,dataset1,resource1,resource1Labels,resource2Labels,resource2,dataset2,processor
<#list data as row>
"${row.aspect!}","${row.dataset1!}","${row.resource1!}","${row.resource1Labels!}","${row.resource2Labels!}","${row.resource2!}","${row.dataset2!}","${row.processor!}"
</#list>
