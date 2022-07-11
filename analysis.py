import numpy as np
import matplotlib.pyplot as plt
from importlib import reload
import matplotlib.patches as mpatches
reload(plt)
import pandas as pd
import sys

# Fixing random state for reproducibility
np.random.seed(19680801)

analysis = pd.read_csv(sys.argv[1])
N = 50
x = analysis.ticks
y = analysis.error
colors = analysis.theta
analysis.loc[analysis.gamma > 0.98, "gamma"] = 50
analysis.loc[analysis.gamma < 0.98, "gamma"] = 20
a = plt.gca()
plt.scatter(x, y, c=colors, alpha=0.5, marker="o", s=analysis.w * 40)
bar = plt.colorbar()
bar.set_label(r'$\theta$')
plt.xlabel('Average ticks')
plt.ylabel('Total error')
plt.title("Solutions")
plt.tight_layout()
el = mpatches.Ellipse((450, 450), 500, 700,
                      angle=30, alpha=0.5, edgecolor='b',
                      facecolor='none')


a.add_patch(el)
plt.savefig('solution-g.pdf')