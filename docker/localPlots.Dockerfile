FROM sbtscala/scala-sbt:18.0.1.1_1.6.2_2.13.8

WORKDIR .
COPY lib ./lib
COPY src ./src
COPY analysis.py ./analysis.py
COPY scripts ./scripts
COPY build.sbt ./build.sbt
COPY  requirements.txt ./requirements.txt
COPY plot-change-scale.py ./plot-change-scale.py
RUN mkdir /root/img
RUN apt-get update && apt-get install -y python3-pip
RUN python3 -m pip install -r requirements.txt
CMD ["scripts/plots.sh"]