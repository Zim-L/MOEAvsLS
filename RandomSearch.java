package gecco24;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;

import java.util.List;

/**
 * This class implements a simple random search algorithm.
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
@SuppressWarnings("serial")
public class RandomSearch<S extends Solution<?>> implements Algorithm<List<S>>, ZAlgorithm<S> {
	private Problem<S> problem;
	private int maxEvaluations;
	NonDominatedSolutionListArchive<S> nonDominatedArchive;
	private int evaluations;
	private int nonUpdateEvaluationSum = 0;
	private int stagnantStop = Integer.MAX_VALUE;

	/** Constructor */
	public RandomSearch(Problem<S> problem, int maxEvaluations) {
		this.problem = problem;
		this.maxEvaluations = maxEvaluations;
		nonDominatedArchive = new NonDominatedSolutionListArchive<S>();
		this.evaluations = 0;
	}

	/* Getter */
	public int getMaxEvaluations() {
		return maxEvaluations;
	}
	
	private boolean isStopConditionReached() {
		boolean condition = evaluations >= maxEvaluations || nonUpdateEvaluationSum >= stagnantStop;
		return condition;
	}

	@Override
	public void run() {
		while (!isStopConditionReached()) {
			S newSolution = problem.createSolution();
			problem.evaluate(newSolution);
			evaluations++;
			updateArchive(newSolution);
		}
	}
	
	private void updateArchive(S solution) {
		boolean updated = nonDominatedArchive.add(solution);
		if (!updated) nonUpdateEvaluationSum++;
		else nonUpdateEvaluationSum = 0;
	}

	@Override
	public List<S> getResult() {
		return nonDominatedArchive.getSolutionList();
	}

	@Override
	public String getName() {
		return "RS";
	}

	@Override
	public String getDescription() {
		return "Multi-objective random search algorithm";
	}

	@Override
	public List getPopulation() {
		return nonDominatedArchive.getSolutionList();
	}

	@Override
	public List getArchive() {
		return nonDominatedArchive.getSolutionList();
	}

	@Override
	public int getT() {
		return evaluations;
	}

	@Override
	public Problem getProblem() {
		return problem;
	}
	
	public int getStagnantStop() {
		return stagnantStop;
	}

	public void setStagnantStop(int stagnantStop) {
		this.stagnantStop = stagnantStop;
	}
}
