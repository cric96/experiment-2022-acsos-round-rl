import numpy as np
import matplotlib.pyplot as plt
from importlib import reload
import matplotlib.patches as mpatches
reload(plt)
import pandas as pd
import sys

analysis = pd.read_csv(sys.argv[1], delimiter=";")

path = sys.argv[1].split("/")
path.reverse()
file_name = path[0]
file_name, _ = file_name.split(".csv")

x = analysis["size"]

plt.plot(x, analysis.rlerror, marker="x", label = "% Error RL")
plt.plot(x, analysis.adhocerror, marker="x", label = "% Error AdHhoc")
plt.plot(x, analysis.rlticks, marker="x", label = "% Ticks RL")
plt.plot(x, analysis.adhocticks, marker="x", label = "% Ticks AdHoc")
plt.xlabel("Nodes")

ax = plt.gca()
ax.legend(loc="best")
ax.set_ylim([-0.02, 1.0])
plt.tight_layout()

plt.savefig("img/" + file_name + ".pdf")

