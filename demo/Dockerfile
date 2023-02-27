FROM centos:latest

RUN yum -y install java-17.0.6-openjdk


WORKDIR /app


COPY target/demo-0.0.1-SNAPSHOT.jar.jar /app


EXPOSE 8080


CMD java -jar demo-0.0.1-SNAPSHOT.jar
