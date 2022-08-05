# Experiments for ACSOS 2022
## Addressing Collective Computations Efficiency: Towards a Platform-level Reinforcement Learning
This repository contains the experiment performed for the paper submitted at ACSOS 2022 entitled: *Addressing Collective Computations Efficiency: Towards a Platform-level Reinforcement Learning*.
Particularly, in these experiments, we combined Aggregate Computing -- a macro programming approach to devise self-organising collective behaviours -- and Q-Learning in order to reduce the overall power consumption of collective computations.

The Aggregate Computing program is supported through ScaFi -- a Scala DSL for aggregate programming.
If you want more information about ScaFi, please refer to the official site:
[https://scafi.github.io/](https://scafi.github.io/)

## Repository Structure:
- `./src`: it contains the main scala code developed (following a standard SBT project structure)
- `./configurations`: it contains the main configurations used to run the simulation presented in the paper
- `./scripts`: it contains the main scripts used to launch simulation and to generate plots
- `./lib`: it contains the ScaFi library with the new simulator --- see the end of the README
- `./docker`: it contains the main configuration to create the images presented in the following

If you follow the instructions described in [Launch Experiment (In the Host Machine)](#host-machine) other two folders will be generated, that are:
- `./res`: it contains the data generated from simulations. Here three folders are:
  - plain-cblock: contains the results produced using the C building block in the swap scenario;
  - plain-gradient: contains the results produced using the gradient building block in the swap scenario;
  - multiswap: contains the results produced using the gradient building block in the multi swap scenario.

  Please refer to the paper to get more details about the experiments and the different scenarios (*swap* and *multiswap*). In each folder, there are other 72 folders (one for each configuration) that contain the main metrics extracted for each episode and `runtime.json`, which contains the Q-Table generated in that configuration. With the latter, we verify how the system handles several nodes, producing the `plain-gradient.csv` file.
- `./img`: it contains the charts produced with the plotting scripts. It has the same three folders (plain-cblock, multiswap and plain-gradient). For each experiment, there are the charts shown in the paper. Particularly:
  - error-and-ticks.svg: show the evolution of the error and the ticks for each episode.
  - image-rl-*episode*: show the performance of the algorithm in a certain episode. We produce one every 50 episodes. The images included in the paper are produced with the configuration expressed in `./solution.txt`

These folders are the ones that you can download in the [GitHub release](https://github.com/cric96/experiment-2022-acsos-round-rl/releases/download/0.1.1/img.tar.gz) (if you do not want to re-launch the simulations).
## Launch experiments
### Docker
If you don't want to download the repository, you can launch the simulations and the plot generation directly using Docker.
The following commands are issued using `bash`.
If you are using Windows, we suggest launching these commands leveraging WSL 2
(please follow the instruction pointed out [here](https://docs.microsoft.com/en-us/windows/wsl/install)).
Otherwise, you can use the [Git BASH](https://gitforwindows.org/).
- launch the simulations (i.e., generate the data):
  - this will produce a local folder `result` with the data necessary to produce the plots

```docker run -v "$(pwd)"/result:/root/res gianlucaaguzzi/acsos-2022-simulation:0.1.1```

- plots the results (data gathered from release):
  - this will produce a local folder `images` containing the plots and a video in the swap scenario
  - the data generated could be found [here](https://github.com/cric96/experiment-2022-acsos-round-rl/releases/download/0.1.1/img.tar.gz)

```docker run -v "$(pwd)"/images:/root/img gianlucaaguzzi/acsos-2022-generate-plots:0.1.1```

- plots charts from local data (i.e., produce using the docker image gianlucaaguzzi/acsos-2022-simulation):
  - use this only if you launch a local simulation and, you would see the differences w.r.t. the published version
  - use the right folder in which you have generated the data. Here I suppose that is `./result`, the one generated from the first command

```docker run -v "$(pwd)"/result:/root/res -v "$(pwd)"/images:/root/img gianlucaaguzzi/acsos-2022-local-plots:0.1.1```
### Host Machine
- Prerequisites:
  - JDK > 11
  - Python 3.9 (plots)

This project is a standard Scala project managed by Scala Build Tool (SBT).
Particularly, we use [sbt-extra](https://github.com/dwijnand/sbt-extras) which adds more features to the standard sbt runner (e.g., wrapper).
Hereafter, I typed the command using **bash** as a shell.

First of all, you should clone this repository using git:
```bash
git clone https://github.com/cric96/experiment-2022-acsos-round-rl.git ## HTTPS
### OR
git clone git@github.com:cric96/experiment-2022-acsos-round-rl.git ## SSH
```
As a smoke test, type in your console:
```bash
./sbtx compile
```
Then, you have to install python dependencies:
```bash
pip install -r requirements.txt
```
To execute the simulations, use:
```bash
./scripts/massive-launch-and-wait.sh
```
The data generated from this command are stored in `res/`.
You would re-run simulations if:
- you perform changes in the code and you want to see how things change
- you change some configurations (please check `configurations` folder)
- you want to check the reproducibility of the simulations

In modern machines, the simulations could last entire days.
For this reason, we upload the data in the GitHub release (download [here](https://github.com/cric96/experiment-2022-acsos-round-rl/releases/download/0.1.1/res.tar.gz)).
In this way, if you want to check to regenerate the plots only, you can download the archive in the release.

**NB** To use the archive, you should unzip it in the root folder of the project.

After you have gathered the data (either with simulation or downloading it),
you can  launch the following command to generate the plots presented in the paper:
```bash
./scripts/plots.sh
```
All the plots associated with each simulation will be placed in `./img/`
Furthermore, in `./img/video` there is the result of the learnt scheduling policy in the swap scenario (please refer to the paper for more details).
.


### Results
Inside `img/video/0`, there is the result of the policy learnt via Q-Learning:

![Gif](https://user-images.githubusercontent.com/23448811/179006064-f0f56dbb-6775-4e50-ba9e-4e759078df5f.gif)

The colour of the small rectangles describes the node frequency (the redder the colour is, the higher the frequency).
The colour of the large rectangle, instead, underlines the output (the greener the colour is, the nearest the nodes are to the sources

The other charts presented in the paper could be found in the [release](https://github.com/cric96/experiment-2022-acsos-round-rl/releases/download/0.1.1/img.tar.gz) (or inside the img folder).
Each scenario (gradient, multiswap, block-c) contains several subfolders, one for each configuration (see `configurations`).

## Miscellaneous
This repository used an ad-hoc ScaFi version with the support of DES-like simulations.
This will probably add to the main repository, currently, this work is [here](https://github.com/cric96/scafi/tree/des-simulator).

