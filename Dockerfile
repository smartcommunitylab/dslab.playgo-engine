# syntax=docker/dockerfile:experimental
FROM maven:3-openjdk-11 as mvn
COPY src /tmp/src
COPY pom.xml /tmp/pom.xml
WORKDIR /tmp
#RUN --mount=type=bind,target=/root/.m2,source=/root/.m2,from=smartcommunitylab/aac:cache-alpine mvn package -DskipTests
RUN mvn package -DskipTests

FROM adoptopenjdk/openjdk11:alpine
ARG VER=1.0
ARG USER=playngo
ARG USER_ID=1005
ARG USER_GROUP=playngo
ARG USER_GROUP_ID=1005
ARG USER_HOME=/home/${USER}
ENV FOLDER=/tmp/target
#dslab.playandgo.engine-1.0.jar
ENV APP=dslab.playandgo.engine
ENV VER=${VER}
# create a user group and a user
RUN  addgroup -g ${USER_GROUP_ID} ${USER_GROUP}; \
     adduser -u ${USER_ID} -D -g '' -h ${USER_HOME} -G ${USER_GROUP} ${USER} ;

WORKDIR ${USER_HOME}
COPY --chown=playngo:playngo --from=mvn /tmp/target/${APP}-${VER}.jar ${USER_HOME}
USER playngo
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar ${APP}-${VER}.jar"]
