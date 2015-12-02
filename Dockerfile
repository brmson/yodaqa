# Inherit official Java image, see https://hub.docker.com/_/java/
FROM java:8 

# Update and install dependencies [cmp. https://docs.docker.com/engine/articles/dockerfile_best-practices/]
RUN apt-get update && apt-get install -y \
    gradle \
    libgfortran3

# JAVA_HOME is not set by default
ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk-amd64/

# Same as "export TERM=dumb"; prevents error "Could not open terminal for stdout: $TERM not set"
ENV TERM dumb

# Copy source code into image
ADD . /yodaqa
# Alternative: Only take this Dockerfile and obtain Yoda source by cloning the official Git repository
# RUN git clone https://github.com/brmson/yodaqa.git

# Define working directory
WORKDIR /yodaqa

# Run YodaQA preparation steps
RUN ./gradlew check
RUN echo | ./gradlew run

# Expose port for web interface
# NOTE: "./gradlew web -q" only works after https://github.com/brmson/yodaqa/issues/15 is solved in the container
#       If the container is not commited to the image afterwards, launching a new instance will fail again
#       Commit syntax: "docker commit -m "<message>" -a "<author>" <container_id> <image_name>:<tag_name>"
#                      (find container_id and image_name with "docker ps -a", tag_name is optional)
EXPOSE 4567

# Can be built with: "docker build -t <image_name> ."
# Can be run with: "docker run -it --entrypoint="./gradlew" <image_name> run -q"
# NOTE: For web interface you need to specify port in Docker command with parameter "-p 4567:4567"

# BEWARE: Due to https://github.com/brmson/yodaqa/issues/15 it is necessary to use "docker run -it --entrypoint="/bin/bash" -p 4567:4567 <image_name>", in bash "./gradlew run -q" and then to follow the advice in the bug report to ask a question repeatedly until the exception disappears for now (known bug in YodaQA 1.4)

# You can define a default command with CMD here, cmp. http://docs.docker.com/engine/reference/builder/#cmd
# After building you can inherit the image with "FROM <image_name>"

# To enter container:
# 1) "docker run -v /usr/local/bin:/target jpetazzo/nsenter" and "PID=$(docker inspect --format {{.State.Pid}} <container_id>)" and "nsenter --target $PID --mount --uts --ipc --net --pid"
# or
# 2) "docker run -it --entrypoint="/bin/bash" -p 4567:4567 <image_name>" (can also be used in exec)

# Use "docker rm $(docker ps -a -q)" to remove all containers
