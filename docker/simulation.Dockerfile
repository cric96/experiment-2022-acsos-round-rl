FROM sbtscala/scala-sbt:18.0.1.1_1.6.2_2.13.8

WORKDIR .
COPY ../lib ./lib
COPY ../src ./src
COPY ../analysis.py ./analysis.py
COPY ../scripts ./scripts
COPY ../build.sbt ./build.sbt

CMD ["scripts/produce-plots-from-release.sh"]