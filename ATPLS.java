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
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

public class ATPLS<S extends Solution> implements Algorithm<List<S>>, ZAlgorithm<S> {

	private static final long serialVersionUID = -4985021281052183946L;
	private int evaluations = 0;
	private Problem<S> problem;
	private Supplier<List<List<Integer>>> indexGenerator;
	private BiFunction<S, List<Integer>, S> neighbourGenerator;
	private DominanceComparator<S> dominance;
	private NonDominatedSolutionListArchive<S> archive;
	private List<S> initSolutions = null;
	private Consumer<ZAlgorithm> monitor = null;
	
	private String selectionStrategy = "OHI";
	private String acceptanceStrategy = "><";
	private String exploreStrategy = "*";

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
	private NonDominatedSolutionListArchive<S> externalArchive;

	public ATPLS(Problem<S> problem, Supplier<List<List<Integer>>> indexGenerator,
			BiFunction<S, List<Integer>, S> neighborGenerator, DominanceComparator<S> dominance) {
		this.problem = problem;
		this.indexGenerator = indexGenerator;
		this.neighbourGenerator = neighborGenerator;
		this.dominance = dominance;
		this.archive = new NonDominatedSolutionListArchive<S>();
		this.externalArchive = new NonDominatedSolutionListArchive<S>();

		neighbourIndices = new ArrayList<List<Integer>>();
		indexGenerator.get().forEach(neighbourIndices::add);
	}

	@Override
	public String getName() {
		return "ATPLS";
	}

	@Override
	public String getDescription() {
		return "Anytime Pareto Local Search";
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
		externalArchive.addAll(initSolutions);

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

			boolean accept = false;
			if (acceptanceStrategy.equals("<")) {
				for (int index = 0; index<neighbourIndices.size(); index++) {
					S nb = neighbourGenerator.apply(candidate, neighbourIndices.get(index));
					if (evaluations<maxEvaluations) {
						problem.evaluate(nb); evaluations++; externalArchive.add(nb);
						boolean added = archive.add(nb);
						if (added) {
							explore.add(nb);
							accept = true;
							if (exploreStrategy.contains("1")) break;
						}
					} else break;
				}
			} else if (acceptanceStrategy.equals(">")) {
				for (int index = 0; index<neighbourIndices.size(); index++) {
					S nb = neighbourGenerator.apply(candidate, neighbourIndices.get(index));
					if (evaluations<maxEvaluations) {
						problem.evaluate(nb); evaluations++; externalArchive.add(nb);
						if (archive.getSolutionList().stream().anyMatch(s -> dominance.compare(nb, s)==-1)) {
							archive.add(nb);
							explore.add(nb);
							accept = true;
							if (exploreStrategy.contains("1")) break;
						}
					} else break;
				}
			} else if (acceptanceStrategy.equals("><")) {
				var nonDominatedSols = new ArrayList<S>();
				for (int index = 0; index<neighbourIndices.size(); index++) {
					S nb = neighbourGenerator.apply(candidate, neighbourIndices.get(index));
					if (evaluations<maxEvaluations) {
						problem.evaluate(nb); evaluations++; externalArchive.add(nb);
						if (archive.getSolutionList().stream().anyMatch(s -> dominance.compare(nb, s)==-1)) {
							archive.add(nb);
							explore.add(nb);
							accept = true;
							if (exploreStrategy.contains("1")) break;
						} else if (!archive.getSolutionList().stream().anyMatch(s -> dominance.compare(s, nb)==-1)) {
							nonDominatedSols.add(nb);
						}
					} else break;
				}
				if (!accept && nonDominatedSols.size()>0) {
					if (prompt) System.out.println("Cannot find dominating sols, non dominated sols: "+nonDominatedSols.size());
					//acceptanceStrategy = "<";
					accept = true;
					if (exploreStrategy.contains("1")) {
						boolean added = archive.add(nonDominatedSols.get(0));
						if (added) explore.add(nonDominatedSols.get(0));
					}
					else {
						nonDominatedSols.forEach(sol -> {
							boolean added = archive.add(sol);
							if (added) explore.add(sol);
						});
					}
				}
			} else {
				throw new JMetalException("Any Time PLS: Unknown acceptance criterion "+acceptanceStrategy);
			}
			if (!accept) {
				if (exploreStrategy.equals("1*")) {
					exploreStrategy = "*";
					explore.getSolutionList().clear();
					explore.getSolutionList().addAll(archive.getSolutionList());
				}
			}
			if (monitor != null) monitor.accept(this);
			explore.getSolutionList().remove(candidate);
			
			if (evaluations >= maxEvaluations)
				break;
		}
	}
	
	private S OHIselection(List<S> sols) {
		if (sols.size()==0) throw new JMetalException("Anytime PLS: Fail OHI selection, archive size=0");
		if (sols.size()==1) return sols.get(0);
		
		sols.sort( (s1, s2) -> Double.compare(s1.objectives()[0], s2.objectives()[0]));
		
		// case of leftmost
		int OHIbestIndex = 0;
		double OHIbest = 2*ohvc(sols.get(0), sols.get(1));
		sols.get(0).attributes().put("OHI", OHIbest);
		
		// OHI of middle solutions
		for (int i=1; i<sols.size()-1; i++) {
			double OHI = ohvc(sols.get(i-1), sols.get(i)) + ohvc(sols.get(i), sols.get(i+1));
			sols.get(i).attributes().put("OHI", OHI);
			if (OHI>OHIbest) {
				OHIbest = OHI;
				OHIbestIndex = i;
			}
		}
		
		double OHI = 2 * ohvc(sols.get(sols.size()-1), sols.get(sols.size()-2));
		sols.get(sols.size()-1).attributes().put("OHI", OHI);
		if (OHI>OHIbest) {
			OHIbest = OHI;
			OHIbestIndex = sols.size()-1;
		}
		
		return sols.get(OHIbestIndex);
	}
	
	private double ohvc(S s1, S s2) {
		return Math.abs(s1.objectives()[0]-s2.objectives()[0])*Math.abs(s1.objectives()[1]-s2.objectives()[1]);
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
