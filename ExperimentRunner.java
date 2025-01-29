package gecco24;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.uma.jmetal.algorithm.multiobjective.moead.AbstractMOEAD;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.UniformCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.BitFlipMutation;
import org.uma.jmetal.operator.mutation.impl.PermutationSwapMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.binarySet.BinarySet;
import org.uma.jmetal.util.comparator.DominanceComparator;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.legacy.qualityindicator.impl.hypervolume.Hypervolume;
import org.uma.jmetal.util.legacy.qualityindicator.impl.hypervolume.impl.WFGHypervolume;

public class ExperimentRunner {

	private static boolean debug = true;

	private static int INDEPENDENT_RUNS;
	private static String experimentBaseDirectory;
	private static String problemInfoDirectory;
	private static String coreNum;
	private static String algID;
	private static String runtimeStr;
	private static int runTime;
	private static int maxEval;

	public static void main(String[] args) throws IOException {
		maxEval = 100000;
		if (args.length==0) {
			debug = true;
			INDEPENDENT_RUNS = 30;
			experimentBaseDirectory = "D:/experimentsGECCO24/";
			problemInfoDirectory = "D:/GECCO24Data/";
			coreNum = "2";
			algID = "A";
			runtimeStr = "3600";
			runTime = Integer.valueOf(runtimeStr);
		} else {
			debug = false;
			if (args.length == 0) {
				throw new JMetalException(
						"Missing argument: experimentBaseDirectory, problemInfoDirectory, numberOfCores, algID, runtime(sec)");
			} else if (args.length == 1) {
				throw new JMetalException("Missing argument: problemInfoDirectory, numberOfCores, algID, runtime(sec)");
			} else if (args.length == 2) {
				throw new JMetalException("Missing argument: numberOfCores, algID, runtime(sec)");
			} else if (args.length == 3) {
				throw new JMetalException("Missing argument: algID, runtime(sec)");
			} else if (args.length == 4) {
				throw new JMetalException("Missing argument: runtime(sec)");
			} else {
				experimentBaseDirectory = args[0];
				problemInfoDirectory = args[1];
				coreNum = args[2];
				algID = args[3];
				runtimeStr = args[4];
				runTime = Integer.valueOf(runtimeStr);
			}
		}

		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", coreNum);

		List<Problem> problems = setupProblems();
		List<ZAlgorithm> tasks = setupTasks(problems);
		runExperiment(tasks);
		System.exit(0);
	}

	public static List<Problem> setupProblems() {
		if (debug)
			System.out.println("Loading problems instances ");
		long t0 = System.currentTimeMillis();
		List<Problem> problems = List.of(new Knapsack01(100).load(problemInfoDirectory + "KP-100.txt"),
				new Knapsack01(1000).load(problemInfoDirectory + "KP-1000.txt"),
				new MONKLand(50, 10).load(problemInfoDirectory + "NK-50-10.txt"),
				new MONKLand(200, 10).load(problemInfoDirectory + "NK-200-10.txt"),
				new MOTSP(50).load(problemInfoDirectory + "TSP-50.txt"),
				new MOTSP(500).load(problemInfoDirectory + "TSP-500.txt"),
				new MOQAP(50).load(problemInfoDirectory + "QAP-50.txt"),
				new MOQAP(200).load(problemInfoDirectory + "QAP-200.txt"));
		if (debug)
			System.out.println("  Done (" + (System.currentTimeMillis() - t0) + "ms)");
		return problems;
	}

	private static List<ZAlgorithm> setupTasks(List<Problem> problems) {
		var tasks = new ArrayList<ZAlgorithm>(60);
		for (int run = 0; run < INDEPENDENT_RUNS; run++) {
			for (Problem problem : problems) {
				int n = 100;
				int T = maxEval / n;
				RandomSearch alg0 = createRS(problem);
				NSGAII alg1 = createNSGAII(problem, T, n);
				SMSEMOA alg2 = createSMSEMOA(problem, T, n);
				MOEAD alg3 = createMOEAD(problem, T, n);
				PLS alg4 = createPLS(problem);
				SEMO alg5 = createSEMO(problem);
				ATPLS alg6 = createATPLS(problem);
				List<ZAlgorithm> algs = List.of(alg0, alg1, alg2, alg3, alg4, alg5, alg6);
				if (algID.contentEquals("A")) {
					tasks.addAll(algs);
				} else if (algID.contentEquals("M")) {
					tasks.addAll(List.of(alg1, alg2, alg3));
				} else {
					tasks.add(algs.get(Integer.valueOf(algID)));
				}
			}
		}
		return tasks;
	}

	public static void runExperiment(List<ZAlgorithm> tasks) {
		tasks.parallelStream().forEach(task -> runTask(task));
	}

	public static void runTask(ZAlgorithm alg) {
		Thread thread = new Thread(alg);
		var executor = Executors.newSingleThreadExecutor();
		var future = executor.submit(thread);
		long t0 = System.currentTimeMillis();
		long durationSecs = 0;

		if (debug)
			System.out.println("Start " + alg.getProblem().getName() + " " + alg.getName());

		try {
			var result = future.get(runTime, TimeUnit.SECONDS);
			durationSecs = System.currentTimeMillis() - t0;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			durationSecs = System.currentTimeMillis() - t0;
		}
		future.cancel(true);
		executor.shutdown();
		thread.stop();
		saveResult(alg, durationSecs);
		System.gc();
	}

	public static void saveResult(ZAlgorithm alg, long duration) {
		var problem = alg.getProblem();
		if (problem.getName().contains("KP") || problem.getName().contains("Knapsack")) {
			var algRes = Collections.synchronizedList(alg.getResult());
			var result = new ArrayList<BinarySolution>();
			for (int i = 0; i < algRes.size(); i++)
				result.add((BinarySolution) ((Solution<BinarySet>) algRes.get(i)).copy());
			saveFinalResult(alg, duration, (List<? extends Solution<?>>) result,
					experimentBaseDirectory + problem.getName() + "/" + alg.getName() + "/");
		} else if (problem.getName().contains("NK")) {
			var algRes = Collections.synchronizedList(alg.getResult());
			var result = new ArrayList<BinarySolution>();
			for (int i = 0; i < algRes.size(); i++)
				result.add((BinarySolution) ((Solution<BinarySet>) algRes.get(i)).copy());
			saveFinalResult(alg, duration, (List<? extends Solution<?>>) result,
					experimentBaseDirectory + problem.getName() + "/" + alg.getName() + "/");
			int s = result.size();
		} else {
			var algRes = Collections.synchronizedList(alg.getResult());
			var result = new ArrayList<PermutationSolution>();
			for (int i = 0; i < algRes.size(); i++)
				result.add((PermutationSolution) ((PermutationSolution) algRes.get(i)).copy());
			saveFinalResult(alg, duration, (List<? extends Solution<?>>) result,
					experimentBaseDirectory + problem.getName() + "/" + alg.getName() + "/");
		}
	}

	public static NSGAII createNSGAII(Problem problem, int T, int N) {
		CrossoverOperator crossover;
		MutationOperator mutation;
		SelectionOperator selection;

		int maxEvaluations = T * N;
		int matingPoolSize = N;
		int offSpringPoolSize = N;
		if (problem.createSolution() instanceof BinarySolution) {
			crossover = new UniformCrossover(1.0);
			mutation = new BitFlipMutation(1.0 / problem.getNumberOfVariables());
		} else {
			crossover = new OrderCrossover(1.0);
			mutation = new InversionMutation(0.05);
			if (problem.getName().contains("QAP")) {
				crossover = new CycleCrossover(1.0);
				mutation = new PermutationSwapMutation(0.05);
			}
		}

		selection = new BinaryTournamentSelection(new DominanceComparator());

		NSGAII nsgaii = new NSGAII(problem, maxEvaluations, N, matingPoolSize, offSpringPoolSize, crossover, mutation,
				selection, new SequentialSolutionListEvaluator<>());
		return nsgaii;
	}

	private static SMSEMOA createSMSEMOA(Problem problem, int T, int N) {
		int maxEvaluations = N * T;
		double offset = 100.0;

		var selection = new BinaryTournamentSelection(new DominanceComparator());
		CrossoverOperator crossover;
		MutationOperator mutation;

		if (problem.createSolution() instanceof BinarySolution) {
			crossover = new UniformCrossover(1.0);
			mutation = new BitFlipMutation(1.0 / problem.getNumberOfVariables());
		} else {
			crossover = new OrderCrossover(1.0);
			mutation = new InversionMutation(0.05);
			if (problem.getName().contains("QAP")) {
				crossover = new CycleCrossover(1.0);
				mutation = new PermutationSwapMutation(0.05);
			}
		}

		Hypervolume hv = new WFGHypervolume();

		SMSEMOA smsemoa = new SMSEMOA(problem, maxEvaluations, N, offset, crossover, mutation, selection,
				new DominanceComparator(), hv);

		return smsemoa;
	}

	public static MOEAD createMOEAD(Problem problem, int T, int N) {
		CrossoverOperator crossover;
		MutationOperator mutation;
		SelectionOperator selection;

		int maxEvaluations = T * N;

		if (problem.createSolution() instanceof BinarySolution) {
			crossover = new UniformCrossover(1);
			mutation = new BitFlipMutation(1.0 / problem.getNumberOfVariables());
		} else {
			crossover = new OrderCrossover(1.0);
			mutation = new InversionMutation(0.05);
			if (problem.getName().contains("QAP")) {
				crossover = new CycleCrossover(1.0);
				mutation = new PermutationSwapMutation(0.05);
			}
		}

		// In this setting, this 2009 MOEA/D falls back to the 2007 original verseion
		double neighborhoodSelectionProbability = 0.8;
		int neighborSize = 10;
		int maximumNumberOfReplacedSolutions = 2;

		MOEAD moead = new MOEAD(problem, N, N, maxEvaluations, mutation, crossover, AbstractMOEAD.FunctionType.TCHE, "",
				neighborhoodSelectionProbability, // 2009 MOEAD Setting, may replace solutions globally
				maximumNumberOfReplacedSolutions, // 2009 MOEAD Setting,
				neighborSize);
		moead.setOtherFunctionType(MOEAD.OtherFunctionType.TCHEdiv);

		return moead;
	}
	
	public static List<List<Integer>> neighbourIndices(int n, int d) {
		if (d == 1) 
	        return IntStream.range(0, n).mapToObj(i -> List.of(i)).collect(Collectors.toList());
	    if (d == 2) 
	        return IntStream.range(0, n).boxed().flatMap(i -> IntStream.range(i + 1, n)
	        		.mapToObj(j -> List.of(i, j))).collect(Collectors.toList());
	    return Collections.emptyList(); // Return an empty list for unsupported d values
	}
	
	public static Supplier<List<List<Integer>>> getIndexGenerator(Problem problem) {
		if (problem.getName().contains("NK")) 
			return (() -> neighbourIndices(problem.getNumberOfVariables(), 1));
		return (() -> neighbourIndices(problem.getNumberOfVariables(), 2));
	}
	
	public static BiFunction<Solution, List<Integer>, Solution> getNeighbourGenerator(Problem problem) {
		if (problem.createSolution() instanceof BinarySolution) {
			if (problem.getName().contains("NK")) {
				return ( (x, index) -> {
					BinarySolution s = (BinarySolution) x.copy();
					s.attributes().remove("neighbourIndices");
					s.attributes().remove("progress");
					s.variables().get( index.get(0) ).flip(0);
					return s;
				});
			}
			if (problem.getName().contains("Knapsack") || problem.getName().contains("KP")) {
				return ( (x, index) -> {
					BinarySolution s = (BinarySolution) x.copy();
					s.attributes().remove("neighbourIndices");
					s.attributes().remove("progress");
					s.variables().get( index.get(0) ).flip(0);
					s.variables().get( index.get(1) ).flip(0);
					return s;
				});
			}
		} else {
			if (problem.getName().contains("TSP")) {
				return ( (x, index) -> {
					PermutationSolution s = (PermutationSolution) x.copy();
					s.attributes().remove("neighbourIndices");
					s.attributes().remove("progress");
					Collections.reverse(s.variables().subList(index.get(0), index.get(1) + 1));
					return s;
				});
			}
			if (problem.getName().contains("QAP")) {
				return ( (x, index) -> {
					PermutationSolution s = (PermutationSolution) x.copy();
					s.attributes().remove("neighbourIndices");
					s.attributes().remove("progress");
					Collections.swap(s.variables(), index.get(0), index.get(1));
					return s;
				});
			}
		}
		return null;
	}

	public static SEMO createSEMO(Problem problem) {
		Supplier<List<List<Integer>>> indexGenerator = getIndexGenerator(problem);
		BiFunction<Solution, List<Integer>, Solution> neighbourGenerator = getNeighbourGenerator(problem);

		SEMO semo = new SEMO(problem, indexGenerator, neighbourGenerator, new DominanceComparator());
		semo.setPrompt(false);
		semo.setMaxEvaluations(maxEval);
		return semo;
	}

	public static PLS createPLS(Problem problem) {
		Supplier<List<List<Integer>>> indexGenerator = getIndexGenerator(problem);
		BiFunction<Solution, List<Integer>, Solution> neighbourGenerator = getNeighbourGenerator(problem);

		PLS pls = new PLS(problem, indexGenerator, neighbourGenerator, new DominanceComparator());
		pls.setPrompt(false);
		pls.setMaxEvaluations(maxEval);
		return pls;
	}

	public static ATPLS createATPLS(Problem problem) {
		Supplier<List<List<Integer>>> indexGenerator = getIndexGenerator(problem);
		BiFunction<Solution, List<Integer>, Solution> neighbourGenerator = getNeighbourGenerator(problem);
		
		ATPLS atpls = new ATPLS(problem, indexGenerator, neighbourGenerator, new DominanceComparator());
		atpls.setPrompt(false);
		atpls.setMaxEvaluations(maxEval);
		return atpls;
	}


	public static RandomSearch createRS(Problem problem) {
		RandomSearch rs = new RandomSearch(problem, maxEval);
		return rs;
	}


	public synchronized static void saveFinalResult(ZAlgorithm alg, long duration,
			List<? extends Solution<?>> population, String route) {
		Problem problem = alg.getProblem();

		File dir = new File(route);
		if (!dir.exists())
			dir.mkdirs();
		int id = findSaveID(route);
		if (debug)
			System.out.println("Saving " + problem.getName() + " " + alg.getName() + " run " + id);

		// output general information

		File file = new File(route + "INFO" + id + ".txt");
		try {
			PrintWriter writer = new PrintWriter(file);
			writer.println(problem.getName());
			writer.println(alg.getName());
			writer.println("Duration(s):" + duration);
			writer.println("Evaluations:" + getEval(alg));
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// output variables and objectives
		new SolutionListOutput(population)
				.setVarFileOutputContext(new DefaultFileOutputContext(route + "VAR" + id + ".csv", ","))
				.setFunFileOutputContext(new DefaultFileOutputContext(route + "FUN" + id + ".csv", ",")).print();
	}

	private static int findSaveID(String route) {
		int id = 0;
		while (true) {
			File dir = new File(route + "INFO" + id + ".txt");
			boolean exists = dir.exists();
			if (exists)
				id++;
			else
				break;
		}
		;
		return id;
	}

	public static int getEval(ZAlgorithm alg) {
		int evaluations = alg.getT();
		if (alg.getName().contains("NSGA") || alg.getName().contains("MOEAD"))
			evaluations *= 100;
		return evaluations;
	}

}
