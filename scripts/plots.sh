sbt "generatePlots 50 1000"
ls img/grad*/analysis.csv |  xargs -I{} cp "{}" temp.csv
python3 analysis.py temp.csv
rm temp.csv