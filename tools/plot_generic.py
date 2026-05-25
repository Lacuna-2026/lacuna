import sys
import csv
import matplotlib.pyplot as plt

if len(sys.argv) < 4:
    print("Usage: plot_generic.py input.csv x_column output.png")
    sys.exit(1)

infile = sys.argv[1]
xcol = sys.argv[2]
outfile = sys.argv[3]

xs = []
ys = []
with open(infile, newline='') as f:
    reader = csv.DictReader(f)
    for row in reader:
        xs.append(float(row[xcol]))
        ys.append(float(row['win_rate']))

plt.figure()
plt.plot(xs, ys, marker='o')
plt.xlabel(xcol)
plt.ylabel('Win Rate')
plt.title(f'{xcol} vs Win Rate')
plt.grid(True)
plt.savefig(outfile)
print('Saved', outfile)
