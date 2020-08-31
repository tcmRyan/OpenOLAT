FROM tomcat:8-jdk8
ADD target/openolat-lms-15.3-SNAPSHOT.war .
RUN mkdir openolat-15.3
RUN unzip -d openolat-15.3 openolat-lms-15.3-SNAPSHOT.war
RUN ln -s openolat-15.3 webapp
#COPY setenv.sh bin/   #Currently causing and error and the Tomcat defaults are working so far
COPY server.xml conf/
COPY olat.local.properties lib/
RUN mkdir -p conf/Catalina/localhost/
COPY ROOT.xml conf/Catalina/localhost/
COPY log4j2.xml lib/
#RUN rm webapp/WEB-INF/classes/log4j.xml
CMD ["catalina.sh", "run"]
