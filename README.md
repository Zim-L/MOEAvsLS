# MOEAs vs Local Search

Code and data archive for the paper:

> **Miqing Li, Xiaofeng Han, Xiaochen Chu, and Zimin Liang. "Empirical Comparison between MOEAs and Local Search on Multi-Objective Combinatorial Optimisation Problems." *GECCO 2024*.** [[DOI: 10.1145/3638529.3654077]](https://doi.org/10.1145/3638529.3654077)

---

## ⚠️ Correction Notice

**An error was identified in the experimental setup for binary-encoded problems (Knapsack and NK-landscape) after publication.**

In the original code, the mutation rate was specified as `1/D` (where `D` is the number of variables). Due to **integer division in Java**, this expression evaluated to `0` rather than the intended floating-point value `1.0/D`. As a result, mutation was effectively **disabled** in the MOEA implementations (NSGA-II, SMS-EMOA, MOEA/D) for these two problem types, significantly impairing their search capability — especially under small evaluation budgets where mutation plays a critical role.

**What changed:** The corrected code uses `1.0 / problem.getNumberOfVariables()` for the `BitFlipMutation` probability in binary problems, ensuring mutation is properly enabled.

**What this means for the results:**
- The original study systematically **underestimated** the performance of MOEAs on Knapsack and NK-landscape problems.
- After correction, the performance gap between SEMO and MOEAs is **reduced**, particularly under small budgets where MOEAs are now more competitive, and in some cases may outperform SEMO.
- However, the **qualitative conclusions of the paper remain largely valid**: SEMO still outperforms NSGA-II on several problem instances, especially under larger evaluation budgets and on problems with rugged fitness landscapes (e.g., NK-landscape with large D).

**Corrected version of the paper**: [`MOEAs_vs_LS__Data_Fixed.pdf`](https://github.com/Zim-L/MOEAvsLS/blob/main/MOEAs_vs_LS__Data_Fixed.pdf)

---

## Contents

### Source Code (Java)

All algorithms are implemented in Java under the `gecco24` package, built on top of the [jMetal](https://github.com/jMetal/jMetal) framework.

| File | Description |
|---|---|
| `ExperimentRunner.java` | Main entry point. Sets up problem instances, instantiates algorithms, runs experiments in parallel, and saves results. Accepts command-line arguments for output directory, problem data directory, core count, algorithm ID, and wall-clock time limit. |
| `ZAlgorithm.java` | Abstract base class / interface shared by all algorithm implementations. |
| `SEMO.java` | Simple Evolutionary Multiobjective Optimiser — a single-solution evolutionary algorithm using neighbourhood-based mutation. |
| `PLS.java` | Pareto Local Search — iteratively improves an archive by exploring the neighbourhood of each non-dominated solution. |
| `ATPLS.java` | Anytime PLS — a variant of PLS that prioritises dominating solutions to improve anytime behaviour and convergence speed. |
| `RandomSearch.java` | Random search baseline — samples solutions uniformly at random up to the evaluation budget. |
| `NSGAII.java` | NSGA-II — Non-dominated Sorting Genetic Algorithm II, a classic population-based MOEA. |
| `SMSEMOA.java` | SMS-EMOA — a steady-state MOEA using hypervolume-based selection. |
| `MOEAD.java` | MOEA/D — a decomposition-based MOEA using Tchebycheff scalarisation. |
| `Knapsack01.java` | Multi-objective 0/1 Knapsack problem (binary encoding). Tested at 100 and 1000 items. |
| `MONKLand.java` | Multi-objective NK-landscape problem (binary encoding). Tested at D=50 and D=200 with K=10. |
| `MOTSP.java` | Multi-objective Travelling Salesman Problem (permutation encoding). Tested at 50 and 500 cities. |
| `MOQAP.java` | Multi-objective Quadratic Assignment Problem (permutation encoding). Tested at sizes 50 and 200. |
| `OrderCrossover.java` | Order crossover (OX) operator for permutation solutions (used for TSP). |
| `CycleCrossover.java` | Cycle crossover (CX) operator for permutation solutions (used for QAP). |
| `InversionMutation.java` | Inversion mutation operator for permutation solutions (used for TSP). |

### Data

| File | Description |
|---|---|
| `GECCO24Data.zip` | Problem instance files for all four problem types (Knapsack, NK-landscape, TSP, QAP) at all tested scales. Required as input by `ExperimentRunner`. |

---

## Experimental Setup

The study compares **7 algorithms** across **4 multi-objective combinatorial optimisation problems** at two scales each, under two termination criteria:

- **100,000 fitness evaluations** — a standard budget favouring population-based MOEAs.
- **1 hour wall-clock time** — a budget favouring local search, which can evaluate solutions much faster per unit time.

**Algorithms:** Random Search, PLS, Anytime PLS, SEMO, NSGA-II, SMS-EMOA, MOEA/D.

**Problems:** Knapsack (100D, 1000D), NK-landscape (50D, 200D, K=10), TSP (50, 500 cities), QAP (50, 200).

Each configuration is run 30 times independently. Statistical comparisons use the Friedman rank-sum test followed by Wilcoxon's rank-sum test with Holm correction (α = 0.05), reported as better/equal/worse counts per algorithm.

---

## Running the Code

**Prerequisites:** Java, [jMetal](https://github.com/jMetal/jMetal) on the classpath.

Compile and run `ExperimentRunner` with the following command-line arguments:

```
java gecco24.ExperimentRunner <outputDir> <dataDir> <numCores> <algID> <runtimeSeconds>
```

| Argument | Description |
|---|---|
| `outputDir` | Directory where results will be saved |
| `dataDir` | Directory containing the extracted `GECCO24Data` problem instances |
| `numCores` | Number of parallel cores to use |
| `algID` | `A` = all algorithms; `M` = MOEAs only; `0`–`6` = individual algorithm index |
| `runtimeSeconds` | Wall-clock timeout per run in seconds (e.g., `3600` for 1 hour) |

Results are saved per problem and algorithm as CSV files (`FUN*.csv` for objective values, `VAR*.csv` for decision variables) along with runtime info (`INFO*.txt`).

---

## Citation

If you use this code or data, please cite the original paper:

```bibtex
@inproceedings{li2024empirical,
  title     = {Empirical Comparison between {MOEAs} and Local Search on Multi-Objective Combinatorial Optimisation Problems},
  author    = {Li, Miqing and Han, Xiaofeng and Chu, Xiaochen and Liang, Zimin},
  booktitle = {Proceedings of the Genetic and Evolutionary Computation Conference (GECCO)},
  year      = {2024},
  doi       = {10.1145/3638529.3654077}
}
```

---

## License

This project is licensed under the [GPL-3.0 License](LICENSE).
