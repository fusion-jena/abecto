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
<#ftl output_format="plainText">
aspect,dataset1,resource1,resource1Labels,resource2Labels,resource2,dataset2,processor
<#list data as row>
"${row.aspect!}","${row.dataset1!}","${row.resource1!}","${row.resource1Labels!}","${row.resource2Labels!}","${row.resource2!}","${row.dataset2!}","${row.processor!}"
</#list>
