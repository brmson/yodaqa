##########################
## enwiki Service Image ##
##########################

# Proposed image name: enwiki
# Readme https://github.com/brmson/yodaqa/tree/master/data/enwiki

# Inherit official Java image, see https://hub.docker.com/_/java/
FROM java:8

# Copy fuseki files
# ADD ./solr-4.6.0 /solr
RUN wget https://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz
RUN mkdir solr; tar -xvzf solr-4.6.0.tgz -C solr --strip-components=1


# JAVA_HOME is not set by default
ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk-amd64/

# Same as "export TERM=dumb"; prevents error "Could not open terminal for stdout: $TERM not set"
ENV TERM dumb

# Define working directory
WORKDIR /solr/example

# Expose port
EXPOSE 8983

##########
# BEWARE #####################################################################################
# With SELinux you need to run chcon -Rt svirt_sandbox_file_t /run/media/<user>/<longid>/home/<user>/Downloads/Backends/enwiki/solr-4.6.0/example/enwiki/collection1/
##############################################################################################

# Can be built with: "docker build -t enwiki ."

# docker run -it -v /run/media/<user>/<longid>/home/<user>/Downloads/Backends/enwiki/solr-4.6.0/example/enwiki/collection1/:/solr-4.6.0/example/enwiki/collection1/ --entrypoint="java" -p 8983:8983 enwiki -Dsolr.solr.home=enwiki -jar start.jar
#docker run -it -v /home/fp/docker/data/enwiki/collection1/:/solr-4.6.0/example/enwiki/collection1/ --entrypoint="java" -p 8983:8983 enwiki -Dsolr.solr.home=enwiki -jar start.jar
# RUN java -Dsolr.solr.home=enwiki -jar start.jar is done in run command; need to map  as volume (read-only via :ro) 
