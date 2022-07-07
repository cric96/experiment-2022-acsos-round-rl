sbt "startBlockLocal"
sbt "generatePlots"
for runtime in $(ls res/*/*/runtime.json); do sbt "renderResult $runtime"; done