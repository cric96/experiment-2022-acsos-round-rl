#!/bin/bash
rm res.tag.gz &>/dev/null
wget https://github.com/cric96/experiment-2022-acsos-round-rl/releases/latest/download/res.tar.gz
tar xvfz res.tar.gz
./scripts/plots.sh