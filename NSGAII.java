package gecco24;

import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.RankingAndCrowdingSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.comparator.DominanceComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Zimin Liang <z.liang.1@bham.ac.uk>
 * 
 *         Adding an archive, stagnant stopping conditions to the NSGA-II
 *         implementation from @author Antonio J. Nebro <antonio@lcc.uma.es>
 * 
 */
@SuppressWarnings("serial")
public class NSGAII<S extends Solution<?>> extends AbstractGeneticAlgorithm<S, List<S>> implements ZAlgorithm<S> {
	protected final int maxEvaluations;

	protected final SolutionListEvaluator<S> evaluator;

	protected int evaluations;
	protected Comparator<S> dominanceComparator;
	protected int matingPoolSize;
	protected int offspringPopulationSize;
	protected int t = 0;
	private int stagnantStop = Integer.MAX_VALUE;

	private NonDominatedSolutionListArchive<S> archive;
	private int nonUpdateEvaluationSum = 0;

	protected String savePath = null;
	private Consumer<ZAlgorithm> monitor = null;
	public void setMonitor(Consumer<ZAlgorithm> m) {this.monitor=m;};
	public Consumer<ZAlgorithm> getMonitor() {return monitor;}

	private String name = "NSGA-II";

	/**
	 * Constructor
	 */
	public NSGAII(Problem<S> problem, int maxEvaluations, int populationSize, int matingPoolSize,
			int offspringPopulationSize, CrossoverOperator<S> crossoverOperator, MutationOperator<S> mutationOperator,
			SelectionOperator<List<S>, S> selectionOperator, SolutionListEvaluator<S> evaluator) {
		this(problem, maxEvaluations, populationSize, matingPoolSize, offspringPopulationSize, crossoverOperator,
				mutationOperator, selectionOperator, new DominanceComparator<S>(), evaluator);
	}

	/**
	 * Constructor
	 * 
	 * @param vis
	 */
	public NSGAII(Problem<S> problem, int maxEvaluations, int populationSize, int matingPoolSize,
			int offspringPopulationSize, CrossoverOperator<S> crossoverOperator, MutationOperator<S> mutationOperator,
			SelectionOperator<List<S>, S> selectionOperator, Comparator<S> dominanceComparator,
			SolutionListEvaluator<S> evaluator) {
		super(problem);
		this.maxEvaluations = maxEvaluations;
		setMaxPopulationSize(populationSize);
		;

		this.crossover = crossoverOperator;
		this.mutation = mutationOperator;
		this.selection = selectionOperator;

		this.evaluator = evaluator;
		this.dominanceComparator = dominanceComparator;

		this.matingPoolSize = matingPoolSize;
		this.offspringPopulationSize = offspringPopulationSize;
		this.archive = new NonDominatedSolutionListArchive<S>();
	}

	@Override
	public void run() {
		generationalInit();
		while (!isStoppingConditionReached()) {
			generationalRun();
		}
	}

	public void generationalInit() {
		population = createInitialPopulation();
		population = evaluatePopulation(population);
		initProgress();
	}

	public void generationalRun() {
		List<S> offspringPopulation;
		List<S> matingPopulation;

		matingPopulation = selection(population);
		offspringPopulation = reproduction(matingPopulation);
		offspringPopulation = evaluatePopulation(offspringPopulation);
		population = replacement(population, offspringPopulation);
		updateProgress();
	}

	@Override
	protected void initProgress() {
		evaluations = getMaxPopulationSize();
		this.t = 0;
		if (archive != null)
			archive.addAll(population);
		if (monitor != null) monitor.accept(this);
	}

	@Override
	protected void updateProgress() {
		evaluations += offspringPopulationSize;
		this.t++;
		if (monitor != null) monitor.accept(this);
	}

	@Override
	protected boolean isStoppingConditionReached() {
		boolean condition;
		condition = evaluations >= maxEvaluations || nonUpdateEvaluationSum >= stagnantStop;
		return condition;
	}

	@Override
	protected List<S> evaluatePopulation(List<S> population) {
		population = evaluator.evaluate(population, getProblem());
		return population;
	}

	/**
	 * This method iteratively applies a {@link SelectionOperator} to the population
	 * to fill the mating pool population.
	 *
	 * @param population
	 * @return The mating pool population
	 */
	@Override
	protected List<S> selection(List<S> population) {
		List<S> matingPopulation = new ArrayList<>(population.size());
		for (int i = 0; i < matingPoolSize; i++) {
			S solution = selection.execute(population);
			matingPopulation.add(solution);
		}

		List<S> newList = population.stream().map(solution -> (S) solution.copy()).collect(Collectors.toList());

		return matingPopulation;
	}

	/**
	 * This methods iteratively applies a {@link CrossoverOperator} a
	 * {@link MutationOperator} to the population to create the offspring
	 * population. The population size must be divisible by the number of parents
	 * required by the {@link CrossoverOperator}; this way, the needed parents are
	 * taken sequentially from the population.
	 *
	 * The number of solutions returned by the {@link CrossoverOperator} must be
	 * equal to the offspringPopulationSize state variable
	 *
	 * @param matingPool
	 * @return The new created offspring population
	 */
	@Override
	protected List<S> reproduction(List<S> matingPool) {
		int numberOfParents = crossover.getNumberOfRequiredParents();

		checkNumberOfParents(matingPool, numberOfParents);

		List<S> offspringPopulation = new ArrayList<>(offspringPopulationSize);
		for (int i = 0; i < matingPool.size(); i += numberOfParents) {
			List<S> parents = new ArrayList<>(numberOfParents);
			for (int j = 0; j < numberOfParents; j++) {
				parents.add(matingPool.get(i + j));
			}

			List<S> offspring = crossover.execute(parents);

			for (S s : offspring) {
				mutation.execute(s);
				offspringPopulation.add(s);
				if (offspringPopulation.size() >= offspringPopulationSize)
					break;
			}
		}
		return offspringPopulation;
	}

	@Override
	protected List<S> replacement(List<S> population, List<S> offspringPopulation) {
		List<S> jointPopulation = new ArrayList<>();
		jointPopulation.addAll(population);
		jointPopulation.addAll(offspringPopulation);
		if (archive != null) {
			for (S ind : offspringPopulation) {
				boolean updated = archive.add(ind);
				if (!updated)
					nonUpdateEvaluationSum++;
				else
					nonUpdateEvaluationSum = 0;
			}
		}
		RankingAndCrowdingSelection<S> rankingAndCrowdingSelection;
		rankingAndCrowdingSelection = new RankingAndCrowdingSelection<S>(getMaxPopulationSize(), dominanceComparator);

		return rankingAndCrowdingSelection.execute(jointPopulation);
	}

	@Override
	public List<S> getResult() {
		if (archive == null) {
			return SolutionListUtils.getNonDominatedSolutions(getPopulation());
		} else {
			return getArchive();
		}
	}

	@Override
	public List<S> getPopulation() {
		return population;
	}

	public void setArchive(NonDominatedSolutionListArchive archive) {
		this.archive = archive;
	}

	public List<S> getArchive() {
		return archive.getSolutionList();
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDescription() {
		return "Nondominated Sorting Genetic Algorithm version II, with non-dominated archive";
	}

	public int getT() {
		return t;
	}

	public int getStagnantStop() {
		return stagnantStop;
	}

	public void setStagnantStop(int stagnantStop) {
		this.stagnantStop = stagnantStop;
	}


}
