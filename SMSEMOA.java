package gecco24;

import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.legacy.qualityindicator.impl.hypervolume.Hypervolume;
import org.uma.jmetal.util.legacy.qualityindicator.impl.hypervolume.impl.WFGHypervolume;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.MergeNonDominatedSortRanking;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
@SuppressWarnings("serial")
public class SMSEMOA<S extends Solution<?>> extends AbstractGeneticAlgorithm<S, List<S>> implements ZAlgorithm<S> {
	protected final int maxEvaluations;
	protected final double offset;
	private int t;
	private int stagnantStop = Integer.MAX_VALUE;

	private NonDominatedSolutionListArchive<S> archive;
	private int nonUpdateEvaluationSum = 0;

	protected int evaluations;

	private Hypervolume<S> hypervolume;
	protected Comparator<S> dominanceComparator;


	private String savePath;
	private PrintWriter biObjHVwriter;

	private int sampleInterval = 20000;
	private double[][] refVector;

	private String name = "SMSEMOA";


	/**
	 * Constructor
	 */
	public SMSEMOA(Problem<S> problem, int maxEvaluations, int populationSize, double offset,
			CrossoverOperator<S> crossoverOperator, MutationOperator<S> mutationOperator,
			SelectionOperator<List<S>, S> selectionOperator, Comparator<S> dominanceComparator,
			Hypervolume<S> hypervolumeImplementation) {
		super(problem);
		this.maxEvaluations = maxEvaluations;
		setMaxPopulationSize(populationSize);

		this.offset = offset;

		this.crossover = crossoverOperator;
		this.mutation = mutationOperator;
		this.selection = selectionOperator;
		this.dominanceComparator = dominanceComparator;
		this.hypervolume = hypervolumeImplementation;
		this.archive = new NonDominatedSolutionListArchive<S>();
	}
	
	@Override
	public void run() {
		List<S> offspringPopulation;
		List<S> matingPopulation;

		population = createInitialPopulation();
		population = evaluatePopulation(population);
		initProgress();
		while (!isStoppingConditionReached()) {
			matingPopulation = selection(population);
			offspringPopulation = reproduction(matingPopulation);
			offspringPopulation = evaluatePopulation(offspringPopulation);
			population = replacement(population, offspringPopulation);
			updateProgress();
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
		if (archive != null) archive.addAll(population);
		if (biObjHVwriter != null) {
			
		}
	}


	@Override
	protected void updateProgress() {
		evaluations++;
		this.t++;
	}

	@Override
	protected boolean isStoppingConditionReached() {
		boolean condition = evaluations >= maxEvaluations || nonUpdateEvaluationSum >= stagnantStop;
		if (condition) {
			if (biObjHVwriter!=null) {
				biObjHVwriter.flush();
				biObjHVwriter.close();
			}
		}
		return condition;
	}

	@Override
	protected List<S> evaluatePopulation(List<S> population) {
		for (S solution : population) {
			getProblem().evaluate(solution);
		}
		return population;
	}

	@Override
	protected List<S> selection(List<S> population) {
		List<S> matingPopulation = new ArrayList<>(2);
		for (int i = 0; i < 2; i++) {
			S solution = selection.execute(population);
			matingPopulation.add(solution);
		}

		return matingPopulation;
	}

	@Override
	protected List<S> reproduction(List<S> population) {
		List<S> offspringPopulation = new ArrayList<>(1);

		List<S> parents = new ArrayList<>(2);
		parents.add(population.get(0));
		parents.add(population.get(1));

		List<S> offspring = crossover.execute(parents);

		mutation.execute(offspring.get(0));

		offspringPopulation.add(offspring.get(0));
		return offspringPopulation;
	}

	@Override
	protected List<S> replacement(List<S> population, List<S> offspringPopulation) {
		List<S> jointPopulation = new ArrayList<>();
		jointPopulation.addAll(population);
		jointPopulation.addAll(offspringPopulation);

		Ranking<S> ranking = new MergeNonDominatedSortRanking<>();
		ranking.compute(jointPopulation);

		List<S> lastSubfront = ranking.getSubFront(ranking.getNumberOfSubFronts() - 1);

		lastSubfront = hypervolume.computeHypervolumeContribution(lastSubfront, jointPopulation);

		List<S> resultPopulation = new ArrayList<>();
		for (int i = 0; i < ranking.getNumberOfSubFronts() - 1; i++) {
			for (S solution : ranking.getSubFront(i)) {
				resultPopulation.add(solution);
			}
		}

		for (int i = 0; i < lastSubfront.size() - 1; i++) {
			resultPopulation.add(lastSubfront.get(i));
		}
		if (archive != null) {
			for (S ind : offspringPopulation) {
				boolean updated = archive.add(ind);
				if (!updated) nonUpdateEvaluationSum++;
				else nonUpdateEvaluationSum = 0;
			}
		}

		return resultPopulation;
	}

	public List<S> getArchive() {
		return archive.getSolutionList();
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
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return "S metric selection EMOA";
	}
	
	public double[][] getRefVector() {
		return refVector;
	}

	public String getSavePath() {
		return savePath;
	}

	public void setSavePath(String savePath) {
		this.savePath = savePath;
		try {
			File dir = new File(savePath);
			if (!dir.exists()) {
				boolean mkd = dir.mkdirs();
				if (!mkd) Files.createDirectory(dir.toPath());
			}
			File file = new File(savePath+"hv.txt");
			System.out.println("savePath:"+file);
			this.biObjHVwriter = new PrintWriter(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int getT() {
		return t;
	}

	public void setT(int t) {
		this.t = t;
	}
	
	public int getStagnantStop() {
		return stagnantStop;
	}

	public void setStagnantStop(int stagnantStop) {
		this.stagnantStop = stagnantStop;
	}

	public void setName(String name) {
		this.name = name;
	}

}