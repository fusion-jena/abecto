#
# Copyright © 2019-2022 Heinz Nixdorf Chair for Distributed Information Systems,
#                       Friedrich Schiller University Jena (http://www.fusion.uni-jena.de/)
# Copyright © 2023-2024 Jan Martin Keil (jan-martin.keil@uni-jena.de)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM maven:3.9.7-eclipse-temurin-17 AS builder
COPY . /var/abecto/
RUN cd /var/abecto/ && mvn -B package

FROM eclipse-temurin:17-jre
RUN echo '#!/bin/sh\nexec java -jar /opt/abecto.jar "$@"' >> /bin/abecto && chmod +x /bin/abecto
COPY --from=builder /var/abecto/abecto-core/target/abecto.jar /opt/abecto.jar
