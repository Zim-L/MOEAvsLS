package gecco24;

import org.uma.jmetal.algorithm.multiobjective.moead.AbstractMOEAD;
import org.uma.jmetal.algorithm.multiobjective.moead.util.MOEADUtils;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.DifferentialEvolutionCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.comparator.ConstraintViolationComparator;
import org.uma.jmetal.util.comparator.EqualSolutionsComparator;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.ArrayList;
import java.util.List;

/**
 * Class implementing the MOEA/D-DE algorithm described in : Hui Li; Qingfu
 * Zhang, "Multiobjective Optimization Problems With Complicated Pareto Sets,
 * MOEA/D and NSGA-II," Evolutionary Computation, IEEE Transactions on , vol.13,
 * no.2, pp.284,302, April 2009. doi: 10.1109/TEVC.2008.925798
 *
 * @author Antonio J. Nebro
 * @version 1.0
 */
@SuppressWarnings("serial")
public class MOEAD<S extends Solution<?>> extends AbstractMOEAD<S> implements ZAlgorithm<S> {
	public enum OtherFunctionType {
		TCHEdiv, TCHEnorm
	}

	protected DifferentialEvolutionCrossover differentialEvolutionCrossover;
	private NonDominatedSolutionListArchive<S> archive;
	private int nonUpdateEvaluationSum = 0;
	private OtherFunctionType otherFunctionType = null;

	private int t;
	private int stagnantStop = Integer.MAX_VALUE;
	
	private String name = "MOEAD";

	public MOEAD(Problem<S> problem, int populationSize, int resultPopulationSize, int maxEvaluations,
			MutationOperator<S> mutation, CrossoverOperator<S> crossover, FunctionType functionType,
			String dataDirectory, double neighborhoodSelectionProbability, int maximumNumberOfReplacedSolutions,
			int neighborSize) {
		super(problem, populationSize, resultPopulationSize, maxEvaluations, crossover, mutation, functionType,
				dataDirectory, neighborhoodSelectionProbability, maximumNumberOfReplacedSolutions, neighborSize);

		archive = new NonDominatedSolutionListArchive<S>();
	}

	@Override
	protected void initializeUniformWeight() {
		for (int n = 0; n < populationSize; n++) {
			double a = 1.0 * n / (populationSize - 1);
			lambda[n][0] = a;
			lambda[n][1] = 1 - a;
		}
	}

	public void init() {
		initializePopulation();
		if (problem.getNumberOfObjectives() == 2) {
			initializeUniformWeight();
		} else {
			super.initializeUniformWeight();
		}
		initializeNeighborhood();
		idealPoint.update(population);
		t = 0;
		evaluations = populationSize;
		if (archive != null) archive.addAll(population);
	}

	public void generationalRun() {
		t++;
		int[] permutation = new int[populationSize];
		MOEADUtils.randomPermutation(permutation, populationSize);

		for (int i = 0; i < populationSize; i++) {
			int subProblemId = permutation[i];

			NeighborType neighborType = chooseNeighborType();
			List<S> parents = parentSelection(subProblemId, neighborType);

			while (parents.size() > crossoverOperator.getNumberOfRequiredParents()) {
				parents.remove(parents.size()-1);
			}
			//int p1 = sample(neighborhood[subProblemId]);
			//int p2 = sample(neighborhood[subProblemId]);
			//while (p2==p1) p2 = sample(neighborhood[subProblemId]);
			
			//List<S> parents = List.of(population.get(p1), population.get(p2));
			//System.out.println(subProblemId+" recombine: "+population.indexOf(parents.get(0))+" "+population.indexOf(parents.get(1)));
			List<S> children = crossoverOperator.execute(parents);

			/*
			double fc1 = fitnessFunction(children.get(0), lambda[subProblemId]);
			double fc2 = fitnessFunction(children.get(1), lambda[subProblemId]);
			S child = fc1<fc2? children.get(0) : fc2>fc1? children.get(1) : children.get(JMetalRandom.getInstance().nextInt(0, 1));
			*/
			//S child = children.get(JMetalRandom.getInstance().nextInt(0, 1));
			S child = children.get(0);
			
			mutationOperator.execute(child);
			problem.evaluate(child);
			evaluations++;
			
			boolean updated = archive.add(child);
			if (!updated) nonUpdateEvaluationSum++;
			else nonUpdateEvaluationSum = 0;

			//if (ConstraintHandling.overallConstraintViolationDegree(child)>=0) 
				idealPoint.update(child.objectives());
			
			updateNeighborhood(child, subProblemId, neighborType);
		}
	}
	
	private Object sample(List<Object> list) {
		return list.get(JMetalRandom.getInstance().nextInt(0, list.size()-1));
	}
	
	private int sample(int[] list) {
		return list[JMetalRandom.getInstance().nextInt(0, list.length-1)];
	}

	@Override
	public void run() {
		init();
		do {
			generationalRun();
		} while (evaluations < maxEvaluations && nonUpdateEvaluationSum < stagnantStop);

	}

	@Override
	protected void updateNeighborhood(S child, int subProblemId, NeighborType neighborType)
			throws JMetalException {
		int size, time;
		time = 0;

		if (neighborType == NeighborType.NEIGHBOR) {
			size = neighborhood[subProblemId].length;
		} else {
			size = population.size();
		}
		int[] perm = new int[size];

		MOEADUtils.randomPermutation(perm, size);

		for (int i = 0; i < size; i++) {
			int k;
			if (neighborType == NeighborType.NEIGHBOR) {
				k = neighborhood[subProblemId][perm[i]];
				// 2007 Ver MOEAD where necessary
				if (maximumNumberOfReplacedSolutions==0) k = subProblemId;
			} else {
				//System.out.println("BANG");
				k = perm[i];
			}
			S neighbor = population.get(k);
			double fneighbor, fchild;
			
			if (new EqualSolutionsComparator<S>().compare(child, neighbor)==0) {
				continue;
			}
			
			//int vioCompare = new ConstraintViolationComparator<S>().compare(child, neighbor);
			//if (vioCompare == 0) {
			fneighbor = fitnessFunction(neighbor, lambda[k]);
			fchild = fitnessFunction(child, lambda[k]);
			if (fchild < fneighbor) {
				population.set(k, (S) child.copy());
				time++;
			}
			/*} else if (vioCompare == 1) {
				population.set(k, (S) child.copy());
				//replace.add(k);
				time++;
			}*/
			
			// 2007 Ver MOEAD where necessary
			if (maximumNumberOfReplacedSolutions==0) return;

			if (time >= maximumNumberOfReplacedSolutions) {
				return;
			}
		}
		//for (int k : replace) {
		//	population.set(k, (S) child.copy());
		//}
	}

	private double fitnessFunction(S individual, double[] lambda) throws JMetalException {
		double fitness;
		if (otherFunctionType != null) {
			if (OtherFunctionType.TCHEdiv.equals(otherFunctionType)) {
				double maxFun = -1.0e+30;

				for (int n = 0; n < problem.getNumberOfObjectives(); n++) {
					double diff = Math.abs(individual.objectives()[n] - idealPoint.getValue(n));

					double feval;
					if (lambda[n] == 0) {
						feval = 0.0001 * diff;
					} else {
						feval = diff / lambda[n];
					}
					if (feval > maxFun) {
						maxFun = feval;
					}
				}
				fitness = maxFun;
			} else {
				throw new JMetalException(" MOEAD.fitnessFunction: unknown type " + otherFunctionType);
			}
			
			return fitness;
		} else
		if (MOEAD.FunctionType.TCHE.equals(functionType)) {
			double maxFun = -1.0e+30;

			for (int n = 0; n < problem.getNumberOfObjectives(); n++) {
				double diff = Math.abs(individual.objectives()[n] - idealPoint.getValue(n));

				double feval;
				if (lambda[n] == 0) {
					feval = 0.0001 * diff;
				} else {
					feval = diff * lambda[n];
				}
				if (feval > maxFun) {
					maxFun = feval;
				}
			}

			fitness = maxFun;
		} else if (MOEAD.FunctionType.AGG.equals(functionType)) {
			double sum = 0.0;
			for (int n = 0; n < problem.getNumberOfObjectives(); n++) {
				sum += (lambda[n]) * individual.objectives()[n];
			}

			fitness = sum;

		} else if (MOEAD.FunctionType.PBI.equals(functionType)) {
			double d1, d2, nl;
			double theta = 5.0;

			d1 = d2 = nl = 0.0;

			for (int i = 0; i < problem.getNumberOfObjectives(); i++) {
				d1 += (individual.objectives()[i] - idealPoint.getValue(i)) * lambda[i];
				nl += Math.pow(lambda[i], 2.0);
			}
			nl = Math.sqrt(nl);
			d1 = Math.abs(d1) / nl;

			for (int i = 0; i < problem.getNumberOfObjectives(); i++) {
				d2 += Math.pow((individual.objectives()[i] - idealPoint.getValue(i)) - d1 * (lambda[i] / nl), 2.0);
			}
			d2 = Math.sqrt(d2);

			fitness = (d1 + theta * d2);
		} else {
			throw new JMetalException(" MOEAD.fitnessFunction: unknown type " + functionType);
		}
		return fitness;
	}

	public List<S> getPopulation() {
		return population;
	}

	protected void initializePopulation() {
		population = new ArrayList<>(populationSize);
		for (int i = 0; i < populationSize; i++) {
			S newSolution = (S) problem.createSolution();
			problem.evaluate(newSolution);
			population.add(newSolution);
		}
	}
	
	public OtherFunctionType getOtherFunctionType() {
		return otherFunctionType;
	}

	public void setOtherFunctionType(OtherFunctionType otherFunctionType) {
		this.otherFunctionType = otherFunctionType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return "Multi-Objective Evolutionary Algorithm based on Decomposition";
	}

	public List<S> getArchive() {
		return archive.getSolutionList();
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
	
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Problem getProblem() {
		return problem;
	}
	
	@Override
	public List<S> getResult() {
		if (archive == null) {
			return super.getResult();
		} else {
			return archive.getSolutionList();
		}
	}
}
