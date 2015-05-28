FROM dockerfile/java:oracle-java8
MAINTAINER martin.coxall@schneiderinvestmentassociates.com
ADD http://mirrors.ukfast.co.uk/sites/ftp.apache.org/zookeeper/zookeeper-3.4.6/zookeeper-3.4.6.tar.gz /opt/
WORKDIR /opt
RUN tar xvfz zookeeper-3.4.6.tar.gz
RUN ln -s zookeeper-3.4.6 zookeeper
ADD zoo.cfg /opt/zookeeper/conf/
EXPOSE 2181
CMD ["/opt/zookeeper/bin/zkServer.sh","start-foreground"]
