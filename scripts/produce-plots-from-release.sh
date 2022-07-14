#!/bin/bash
rm res.tag.gz &>/dev/null
wget https://github.com/cric96/experiment-2022-acsos-round-rl/releases/download/$1/res.tar.gz
tar xvfz res.tar.gz
./scripts/plots.sh