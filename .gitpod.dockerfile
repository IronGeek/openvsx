FROM gitpod/workspace-postgres:latest

USER gitpod

RUN curl https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.8.12.tar.gz --output elasticsearch-6.8.12.tar.gz \
    && tar -xzf elasticsearch-6.8.12.tar.gz \
    && rm elasticsearch-6.8.12.tar.gz
ENV ES_HOME="$HOME/elasticsearch-6.8.12"
