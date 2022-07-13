#!/bin/bash
sbt "generatePlots 1 1"
ls img/plain-grad*/analysis.csv |  xargs -I{} cp "{}" temp.csv
python3 plot-change-scale.py res/plain-gradient.csv
python3 analysis.py temp.csv
ls res/plain-grad*/0.99-0.4-0.4-0.1-7-0.975-1/runtime.json | xargs -I{} sbt "renderResult {} gradient plain"
rm temp.csv