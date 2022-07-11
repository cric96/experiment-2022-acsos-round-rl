nohup sbt -J-Xmx4G "startGradient" > gradient.out &
sleep 2;
nohup sbt -J-Xmx4G "startGradientMulti" > multi-gradient.out &
sleep 2;
nohup sbt -J-Xmx4G "startBlockC" > block-c.out &