FROM sbtscala/scala-sbt:18.0.1.1_1.6.2_2.13.8

WORKDIR .
ARG TAG
COPY lib ./lib
COPY src ./src
COPY scripts ./scripts
COPY configurations ./configurations
COPY build.sbt ./build.sbt
RUN mkdir /root/res
CMD ["scripts/massive-launch-and-wait.sh"]