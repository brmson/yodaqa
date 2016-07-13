################################
## YodaQA Label Service Image ##
################################

# Proposed image name: label-service

# Inherit Debian image
FROM debian:jessie

# Update and install dependencies [cmp. https://docs.docker.com/engine/articles/dockerfile_best-practices/]
RUN apt-get update && apt-get install -y \
    curl \
    git  \
    pypy

RUN git clone https://github.com/brmson/label-lookup.git
# If we were to copy label-service files into image
#ADD ./label-service /label-service

RUN cd label-lookup
RUN curl -O https://bootstrap.pypa.io/get-pip.py
# If you run this on an actual system instead of a container: The following 3 commands need root privileges
RUN pypy get-pip.py
RUN mv /usr/local/bin/pip ./pypy_pip
RUN ./pypy_pip install flask SPARQLWrapper

# Same as "export TERM=dumb"; prevents error "Could not open terminal for stdout: $TERM not set"
ENV TERM dumb

# Define working directory
WORKDIR /label-lookup

# Expose port
EXPOSE 5000
EXPOSE 5001

##########
# BEWARE #####################################################################################
# With SELinux you need to run chcon -Rt svirt_sandbox_file_t /home/<user>/docker/docker_shared/
##############################################################################################

# Can be built with: "docker build -t labels ."

# docker run -it -v /home/fp/docker/data/labels/:/shared --entrypoint="pypy" -p 5000:5000 labels /label-lookup/lookup-service.py /shared/sorted_list.dat
# docker run -it -v /home/fp/docker/data/labels/:/shared --entrypoint="pypy" -p 5001:5001 labels /label-lookup/lookup-service-sqlite.py /shared/labels.db
# RUN pypy lookup-service.py /shared/sorted_list.dat is done in run command; need to map sorted_list.dat as volume (read-only)

# Can be used as usual: curl 127.0.0.1:5000/search/AlbaniaPeople 
