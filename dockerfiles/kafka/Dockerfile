FROM dockerfile/java:oracle-java8
MAINTAINER martin.coxall@shneiderinvestmentassociates.com
ADD http://apache.mirror.anlx.net/kafka/0.8.2.0/kafka_2.10-0.8.2.0.tgz /opt/
WORKDIR /opt
RUN tar xvfz kafka_2.10-0.8.2.0.tgz
RUN ln -s kafka_2.10-0.8.2.0 kafka
ADD server.properties /opt/kafka/config/
EXPOSE 9092
WORKDIR /opt/kafka
CMD ["bin/kafka-server-start.sh","config/server.properties"]
