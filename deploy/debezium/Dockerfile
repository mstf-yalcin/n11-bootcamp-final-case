FROM confluentinc/cp-kafka-connect-base:7.7.0 AS plugin

USER root

RUN mkdir -p /plugins && \
    confluent-hub install --no-prompt \
    confluentinc/kafka-connect-elasticsearch:14.1.7 \
    --component-dir /plugins

FROM debezium/connect:2.7.3.Final

USER root

COPY --from=plugin /plugins /kafka/connect

ENV CONNECT_PLUGIN_PATH="/kafka/connect"

USER kafka
