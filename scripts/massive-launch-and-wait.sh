#!/bin/bash
source ./scripts/massive-launch.sh
wait
ls res/plain-grad*/0.99-0.4-0.4-0.1-7-0.975-1/runtime.json |  xargs -I{} cp "{}" runtime.json
sbt "startCheckScale runtime.json" & > check-scale.out
wait
