#!/bin/bash
docker run -it --entrypoint="./gradlew" -p 4568:4568 hub run -PexecArgs="4568 http://cloud.ailao.eu:4567/"

