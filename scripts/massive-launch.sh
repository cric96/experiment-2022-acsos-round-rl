#!/bin/bash
echo "Computations starts...."
sbt -J-Xmx4G "startGradient" > gradient.out &
sleep 2;
sbt -J-Xmx4G "startGradientMulti" > multi-gradient.out &
sleep 2;
sbt -J-Xmx4G "startBlockC" > block-c.out &
echo "Launched..."