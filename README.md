# Experiments for ACSOS 2022
## Addressing Collective Computations Efficiency: Towards a Platform-level Reinforcement Learning
This repository contains the experiment performed for the paper submitted at ACSOS 2022 entitled: *Addressing Collective Computations Efficiency: Towards a Platform-level Reinforcement Learning*.
Particularly, in these experiments, we combined Aggregate Computing -- a macro programming approach to devise self-organising collective behaviours -- and Q-Learning in order to reduce the overall power consumption of collective computations.
The Aggregate Computing program is supported through ScaFi -- a Scala DSL for aggregate programming.

## Launch experiements (Docker)
- launch the simulations (i.e., generate the data): 
  - this will produce a local folder `result` with the data necessary to produce the plots
  
```docker run -v "$(pwd)"/result:/root/res gianlucaaguzzi/acsos-2022-simulation:0.1.0```
  
- plots the results (data gathered from release):
  - this will produce a local folder `images` containing the plots and a video in the swap scenario
  - the data generated could be found [here](https://github.com/cric96/experiment-2022-acsos-round-rl/releases/download/0.1.0/img.tar.gz)
  
```docker run -v "$(pwd)"/images:/root/img gianlucaaguzzi/acsos-2022-generate-plots:0.1.0```

- plots charts from local data generation:
  - use this only if you launch a local simulation and you would see the differences w.r.t. the published version
  - use the right folder in which you have generated the data
  
```docker run -v "$(pwd)"/result:/root/res -v "$(pwd)"/images:/root/img gianlucaaguzzi/acsos-2022-local-plots```
## Launch Experiment (Local)
- Prerequisites:
  - JDK > 11
  - Python 3.9 (plots)
  - SBT (simulations)

As a smoke test, type in your console:
```
sbt compile
```
Then, you have to install python dependencies:
```
pip install -r requirements.txt
```
To execute the simulations, use:
```
./scripts/massive-launch-and-wait.sh
```
If you are using Windows, we suggest to launch these experiment leveraging WSL 2.

In a moderm machines, the simulations could last an entire days. For this reason, we upload the data in the github release. (download [here](https://github.com/cric96/experiment-2022-acsos-round-rl/releases/download/0.1.0/res.tar.gz))

Then, launch the command:
```
./scripts/plots.sh
```
Will generate all the plots associated with each simulation in `img/`
Futheremore, in `img/video` there is the result of the learnt scheduling policy in the swap scenario (please refer to the paper for more details).

The data generated from this command are store in `res/` (the same data upload to GH release).
You would re-run simulations if:
- you perform changes in the code and you want to see how the things changes
- you change some configurations (please check `configurations` folder)
- you want to check the reproducibily of the simulations

### Results
Inside images/video, there is the result of the policy learnt via Q-Learning:

![Gif](https://user-images.githubusercontent.com/23448811/179006064-f0f56dbb-6775-4e50-ba9e-4e759078df5f.gif)

The colour of the small rectangles describes the node frequency (the redder the colour is, the higher the frequency). 
The colour of the large rectangle, instead, underlines the output (the greener the colour is, the nearest the nodes are to the sources

The other charts presented in the paper could be found in the [release](https://github.com/cric96/experiment-2022-acsos-round-rl/releases/download/0.1.0/img.tar.gz) (or inside the img folder).
Each scenario (gradient, multiswap, block-c) contains several subfolder, one for each configuration (see `configurations`).

## Miscellaneous
This repository used an ad-hoc ScaFi version with the support of DES-like simulations.
This will probably add to the main repository, currently, this work is [here](https://github.com/cric96/scafi/tree/des-simulator).

