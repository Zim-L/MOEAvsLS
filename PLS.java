package gecco24;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.comparator.DominanceComparator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

public class PLS<S extends Solution> implements Algorithm<List<S>>, ZAlgorithm<S> {

	private static final long serialVersionUID = -4985021281052183946L;
	private int evaluations = 0;
	private Problem<S> problem;
	private Supplier<List<List<Integer>>> indexGenerator;
	private BiFunction<S, List<Integer>, S> neighbourGenerator;
	private DominanceComparator<S> dominance;
	private NonDominatedSolutionListArchive archive;
	private List<S> initSolutions = null;
	private Consumer<ZAlgorithm> monitor = null;

	public Consumer<ZAlgorithm> getMonitor() {
		return monitor;
	}

	public void setMonitor(Consumer<ZAlgorithm> monitor) {
		this.monitor = monitor;
	}

	private List<List<Integer>> neighbourIndices;

	private int maxEvaluations = Integer.MAX_VALUE;
	private boolean prompt = true;
	private S candidate;
	private NonDominatedSolutionListArchive<S> explore;

	public PLS(Problem<S> problem, Supplier<List<List<Integer>>> indexGenerator,
			BiFunction<S, List<Integer>, S> neighborGenerator, DominanceComparator<S> dominance) {
		this.problem = problem;
		this.indexGenerator = indexGenerator;
		this.neighbourGenerator = neighborGenerator;
		this.dominance = dominance;
		this.archive = new NonDominatedSolutionListArchive<S>();

		neighbourIndices = new ArrayList<List<Integer>>();
		indexGenerator.get().forEach(neighbourIndices::add);
	}

	@Override
	public String getName() {
		return "PLS";
	}

	@Override
	public String getDescription() {
		return "Pareto Local Search";
	}

	@Override
	public void run() {
		if (initSolutions == null) {
			initSolutions = new ArrayList<S>();
			evaluations = 0;
			while (initSolutions.size() == 0) {
				S initSol = problem.createSolution();
				problem.evaluate(initSol);
				evaluations++;
				if (Arrays.stream(initSol.constraints()).allMatch(c -> c == 0))
					initSolutions.add(initSol);
			}
		} else {
			initSolutions.forEach(i -> problem.evaluate(i));
			evaluations = initSolutions.size();
		}
		archive.addAll(initSolutions);

		explore = new NonDominatedSolutionListArchive<S>();
		explore.addAll(initSolutions);

		while (explore.size() > 0) {
			if (prompt)
				System.out.println("Explore list size: " + explore.size() + ";  \tevaluations: " + evaluations
						+ ";  \tarchive size: " + archive.size());

			// random selection
			candidate = (S) sample(explore.getSolutionList());

			List<List<Integer>> neighbourIndices;
			if (!candidate.attributes().containsKey("neighbourIndices")) {
				neighbourIndices = new ArrayList<List<Integer>>();
				this.neighbourIndices.forEach(neighbourIndices::add);
				candidate.attributes().put("neighbourIndices", neighbourIndices);
			}
			neighbourIndices = (List<List<Integer>>) candidate.attributes().get("neighbourIndices");

			// neighbourhood exploration
			for (int index = 0; index<neighbourIndices.size(); index++) {
				S nb = neighbourGenerator.apply(candidate, neighbourIndices.get(index));

				if (evaluations < maxEvaluations) {
					problem.evaluate(nb);
					evaluations++;
					boolean added = archive.add(nb);
					if (added)
						explore.add(nb);
					if (monitor != null)
						monitor.accept(this);
				} else
					break;
			}
			if (evaluations >= maxEvaluations)
				break;
		}
	}

	private S sample(List<S> l) {
		return l.get(JMetalRandom.getInstance().nextInt(0, l.size() - 1));
	}

	public List<S> getPhase1solutions() {
		return initSolutions;
	}

	@Override
	public List<S> getResult() {
		return archive.getSolutionList();
	}

	public List<S> getInitSolutions() {
		return initSolutions;
	}

	public void setInitSolutions(List<S> initSolutions) {
		this.initSolutions = initSolutions;
	}

	public int getMaxEvaluations() {
		return maxEvaluations;
	}

	public void setMaxEvaluations(int maxEvaluations) {
		this.maxEvaluations = maxEvaluations;
	}

	public boolean isPrompt() {
		return prompt;
	}

	public void setPrompt(boolean prompt) {
		this.prompt = prompt;
	}

	public int getEvaluations() {
		return evaluations;
	}

	@Override
	public List<S> getPopulation() {
		return archive.getSolutionList();
	}

	@Override
	public List<S> getArchive() {
		return archive.getSolutionList();
	}

	@Override
	public int getT() {
		return evaluations;
	}

	@Override
	public Problem getProblem() {
		return problem;
	}

	public S getCandidate() {
		return candidate;
	}

	public List<S> getExplore() {
		return explore.getSolutionList();
	}

}
