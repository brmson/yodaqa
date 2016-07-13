######################################
## DBpedia / Freebase Service Image ##
######################################

# Proposed image name: dbpedia
# Readme https://github.com/brmson/yodaqa/blob/master/data/dbpedia/

# Inherit official Java image, see https://hub.docker.com/_/java/
FROM java:8

# Could copy fuseki files...
# ADD ./jena-fuseki-1.1.1 /jena-fuseki
# ...or get them online (and the newer 1.4.0 should work)
RUN wget http://www-eu.apache.org/dist/jena/binaries/jena-fuseki1-1.4.0-distribution.tar.gz
RUN mkdir jena-fuseki; tar -xvzf jena-fuseki1-1.4.0-distribution.tar.gz -C jena-fuseki --strip-components=1

# Same as "export TERM=dumb"; prevents error "Could not open terminal for stdout: $TERM not set"
ENV TERM dumb

# Define working directory
WORKDIR /jena-fuseki

# Expose ports (3030 for Freebase, 3037 for DBpedia)
EXPOSE 3037
EXPOSE 3030

##########
# BEWARE #####################################################################################
# With SELinux you need to run chcon -Rt svirt_sandbox_file_t /run/media/<user>/<longid>/home/<user>/Downloads/Backends/DBpedia/jena-fuseki-1.1.1/db/
##############################################################################################

# Can be built with: "docker build -t fuseki ."

# .:: DBpedia
# docker run -it -v /home/fp/docker/data/db/:/jena-fuseki-1.1.1/db/ --entrypoint="./fuseki-server" -p 3037:3037 fuseki --port 3037 --loc db /dbpedia
# RUN ./fuseki-server --port 3037 --loc db /dbpedia is done in run command; need to map  as volume (read-only via :ro)

# .:: Freebase
# docker run -it -v /home/fp/docker/data/d-freebase/:/jena-fuseki-1.1.1/d-freebase/ --entrypoint="./fuseki-server" -p 3030:3030 fuseki --loc d-freebase /freebase
# RUN ./fuseki-server --loc d-freebase /freebase
