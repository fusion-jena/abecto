<#ftl output_format="plainText">
item_id,statement_guid,property_id,wikidata_value,meta_wikidata_value,external_value,external_url
<#list data as row>
"${row.item_id}","${row.statement_guid!}","${row.property_id}","${row.wikidata_value!}","${row.meta_wikidata_value!}","${row.external_value}","${row.external_url!}"
</#list>
