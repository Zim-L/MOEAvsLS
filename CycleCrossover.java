package gecco24;

import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.pseudorandom.BoundedRandomGenerator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CycleCrossover
 * @author Zimin Liang <zimin.liang@outlook.com>
 * 
 * An implementation for CycleCrossover, for position-based permutation problems
 */
@SuppressWarnings("serial")
public class CycleCrossover implements CrossoverOperator<PermutationSolution<Integer>> {
	private double crossoverProbability = 1.0;
	private BoundedRandomGenerator<Integer> cuttingPointRandomGenerator;
	private RandomGenerator<Double> crossoverRandomGenerator;

	/**
	 * Constructor
	 */
	public CycleCrossover(double crossoverProbability) {
		this(crossoverProbability, () -> JMetalRandom.getInstance().nextDouble(),
				(a, b) -> JMetalRandom.getInstance().nextInt(a, b));
	}

	/**
	 * Constructor
	 */
	public CycleCrossover(double crossoverProbability, RandomGenerator<Double> randomGenerator) {
		this(crossoverProbability, randomGenerator, BoundedRandomGenerator.fromDoubleToInteger(randomGenerator));
	}

	/**
	 * Constructor
	 */
	public CycleCrossover(double crossoverProbability, RandomGenerator<Double> crossoverRandomGenerator,
			BoundedRandomGenerator<Integer> cuttingPointRandomGenerator) {
		if ((crossoverProbability < 0) || (crossoverProbability > 1)) {
			throw new JMetalException("Crossover probability value invalid: " + crossoverProbability);
		}
		this.crossoverProbability = crossoverProbability;
		this.crossoverRandomGenerator = crossoverRandomGenerator;
		this.cuttingPointRandomGenerator = cuttingPointRandomGenerator;
	}

	/* Getters */
	@Override
	public double getCrossoverProbability() {
		return crossoverProbability;
	}

	/* Setters */
	public void setCrossoverProbability(double crossoverProbability) {
		this.crossoverProbability = crossoverProbability;
	}

	/**
	 * Executes the operation
	 *
	 * @param parents An object containing an array of two solutions
	 */
	public List<PermutationSolution<Integer>> execute(List<PermutationSolution<Integer>> parents) {
		if (null == parents) {
			throw new JMetalException("Null parameter");
		} else if (parents.size() != 2) {
			throw new JMetalException("There must be two parents instead of " + parents.size());
		}

		return doCrossover(crossoverProbability, parents);
	}

	/**
	 * Perform the crossover operation
	 *
	 * @param probability Crossover probability
	 * @param parents     Parents
	 * @return An array containing the two offspring
	 */
	public List<PermutationSolution<Integer>> doCrossover(double probability,
			List<PermutationSolution<Integer>> parents) {

		List<PermutationSolution<Integer>> offspring = new ArrayList<>(2);

		offspring.add((PermutationSolution<Integer>) parents.get(0).copy());
		offspring.add((PermutationSolution<Integer>) parents.get(1).copy());

		int n = parents.get(0).variables().size();

		if (crossoverRandomGenerator.getRandomValue() < probability) {
			// Initialize a boolean array to mark the visited elements
			boolean[] visited = new boolean[n];
			Arrays.fill(visited, false);

			// Initialize a variable to store the current cycle index
			int cycle = 0;
			// Loop through the elements of the permutations
			for (int i = 0; i < n; i++) {
				// If the current element is not visited, start a new cycle
				if (!visited[i]) {
					// Get the current element from the first parent
					int current = i;
					long t0 = System.currentTimeMillis();
					// Loop until the cycle is closed
					while (true) {
						// Mark the current element as visited
						visited[current] = true;

						// Copy the current element to the offspring according to the cycle index
						if (cycle % 2 == 0) {
							// Even cycle: copy from the first parent to the first offspring
							// and from the second parent to the second offspring
							offspring.get(0).variables().set(current, parents.get(0).variables().get(current));
							offspring.get(1).variables().set(current, parents.get(1).variables().get(current));
						} else {
							// Odd cycle: copy from the second parent to the first offspring
							// and from the first parent to the second offspring
							offspring.get(0).variables().set(current, parents.get(1).variables().get(current));
							offspring.get(1).variables().set(current, parents.get(0).variables().get(current));
						}

						// Get the next element from the second parent
						int next = parents.get(0).variables().indexOf(parents.get(1).variables().get(current));

						// If the next element is the same as the first element of the cycle, break the
						// loop
						if (next == i) {
							break;
						}
						if (System.currentTimeMillis() - t0 > 1000) System.out.println("Dead lock warning, i: "+i+", x[i]:"+parents.get(0).variables().get(i)+", next: "+next);
						// Otherwise, update the current element
						current = next;
					}

					// Increment the cycle index
					cycle++;
				}
			}
		}

		return offspring;
	}

	@Override
	public int getNumberOfRequiredParents() {
		return 2;
	}

	@Override
	public int getNumberOfGeneratedChildren() {
		return 2;
	}
	
}
