nohup sbt "startGradient" > gradient.out &
sleep 2;
nohup sbt "startGradientMulti" > multi-gradient.out &
sleep 2;
nohup sbt "startBlockC" > block-c.out &