TAG=${GITHUB_REF##*/}

for name in "acsos-2022-generate-plots plots.Dockerfile" "acsos-2022-simulation simulation.Dockerfile" "acsos-2022-local-plots localPlots.Dockerfile"
do
    set -- $name
    docker build -f docker/$2 . -t gianlucaaguzzi/$1:$TAG --build-arg TAG=$TAG
    docker push gianlucaaguzzi/$1:$TAG
    docker tag gianlucaaguzzi/$1:$TAG gianlucaaguzzi/$1:latest
    docker push gianlucaaguzzi/$1:latest
done
