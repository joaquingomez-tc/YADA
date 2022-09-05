FROM maven:3.8.5-eclipse-temurin-11-alpine

ARG YADA_SRC 
ARG YADA_VERSION 
ARG YADA_HOME /yada
ARG YADA_USER yada
ARG YADA_GROUP yada
ARG YADA_PROPS="YADA_PROPS=\"-DYADA.properties.path=${YADA_HOME}/YADA.properties\""
ARG YADA_LOG /var/log/yada.log

RUN apk add git
RUN mkdir -p yada/lib yada/bin yada/files/in yada/files/out
 
ADD ${YADA_SRC}/yada-assembly/target/YADA-${YADA_VERSION}/YADA-${YADA_VERSION}/lib yada/lib
ADD ${YADA_SRC}/yada-assembly/target/classes yada
RUN mv yada/lib/yada-api-${YADA_VERSION}.jar yada

ENV YADA_server_keystore_path /etc/ssl/yada.jks
ENV YADA_server_keystore_secret changeit
ENV YADA_server_CORS_allow_origin "https?://[a-zA-Z0-9\-\.]+(:[0-9]+)?$"

RUN sed -i 's#YADA_SERVER_KEYSTORE_PATH#'"$YADA_server_keystore_path"'#' /yada/YADA.properties 
RUN sed -i 's#YADA_SERVER_KEYSTORE_SECRET#'"$YADA_server_keystore_secret"'#' /yada/YADA.properties
RUN sed -i 's#YADA_SERVER_CORS_ALLOW_ORIGIN#'"$YADA_server_CORS_allow_origin"'#' /yada/YADA.properties

CMD ["bash"]