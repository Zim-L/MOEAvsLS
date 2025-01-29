package gecco24;

import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.pseudorandom.BoundedRandomGenerator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("serial")
public class OrderCrossover implements
    CrossoverOperator<PermutationSolution<Integer>> {
  private double crossoverProbability = 1.0;
  private BoundedRandomGenerator<Integer> cuttingPointRandomGenerator ;
  private RandomGenerator<Double> crossoverRandomGenerator ;

  /**
   * Constructor
   */
  public OrderCrossover(double crossoverProbability) {
	  this(crossoverProbability, () -> JMetalRandom.getInstance().nextDouble(), (a, b) -> JMetalRandom.getInstance().nextInt(a, b));
  }

  /**
   * Constructor
   */
  public OrderCrossover(double crossoverProbability, RandomGenerator<Double> randomGenerator) {
	  this(crossoverProbability, randomGenerator, BoundedRandomGenerator.fromDoubleToInteger(randomGenerator));
  }

  /**
   * Constructor
   */
  public OrderCrossover(double crossoverProbability, RandomGenerator<Double> crossoverRandomGenerator, BoundedRandomGenerator<Integer> cuttingPointRandomGenerator) {
    if ((crossoverProbability < 0) || (crossoverProbability > 1)) {
      throw new JMetalException("Crossover probability value invalid: " + crossoverProbability) ;
    }
    this.crossoverProbability = crossoverProbability;
    this.crossoverRandomGenerator = crossoverRandomGenerator ;
    this.cuttingPointRandomGenerator = cuttingPointRandomGenerator ;
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
      throw new JMetalException("Null parameter") ;
    } else if (parents.size() != 2) {
      throw new JMetalException("There must be two parents instead of " + parents.size()) ;
    }

    return doCrossover(crossoverProbability, parents) ;
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

		int permutationLength = parents.get(0).variables().size();

		if (crossoverRandomGenerator.getRandomValue() < probability) {
			// this version 
			int cuttingPoint1;
			int cuttingPoint2;

			cuttingPoint1 = cuttingPointRandomGenerator.getRandomValue(0, permutationLength - 1);
			cuttingPoint2 = cuttingPointRandomGenerator.getRandomValue(0, permutationLength - 1);
			while (cuttingPoint2 == cuttingPoint1)
				cuttingPoint2 = cuttingPointRandomGenerator.getRandomValue(0, permutationLength - 1);

			if (cuttingPoint1 > cuttingPoint2) {
				int swap;
				swap = cuttingPoint1;
				cuttingPoint1 = cuttingPoint2;
				cuttingPoint2 = swap;
			}
			Integer[] child0 = new Integer[permutationLength];
			Integer[] child1 = new Integer[permutationLength];
			HashSet<Integer> snippet0 = new HashSet<Integer>();
			HashSet<Integer> snippet1 = new HashSet<Integer>();
			for (int i = cuttingPoint1; i < cuttingPoint2; i++) {
				snippet0.add(offspring.get(0).variables().get(i));
				snippet1.add(offspring.get(1).variables().get(i));
				child0[i] = offspring.get(0).variables().get(i);
				child1[i] = offspring.get(1).variables().get(i);
			}

			// child0 get parent
			int i = cuttingPoint2;
			int j = cuttingPoint2;
			while (i < permutationLength) {
				if (!snippet0.contains(offspring.get(1).variables().get(j))) {
					child0[i] = offspring.get(1).variables().get(j);
					i += 1;
				}
				j = (j + 1) % permutationLength;
			}
			i = 0;
			while (i < cuttingPoint1) {
				if (!snippet0.contains(offspring.get(1).variables().get(j))) {
					child0[i] = offspring.get(1).variables().get(j);
					i += 1;
				}
				j = (j + 1) % permutationLength;
			}

			// child1 get parent
			i = cuttingPoint2;
			j = cuttingPoint2;
			while (i < permutationLength) {
				if (!snippet1.contains(offspring.get(0).variables().get(j))) {
					child1[i] = offspring.get(0).variables().get(j);
					i += 1;
				}
				j = (j + 1) % permutationLength;
			}
			i = 0;
			while (i < cuttingPoint1) {
				if (!snippet1.contains(offspring.get(0).variables().get(j))) {
					child1[i] = offspring.get(0).variables().get(j);
					i += 1;
				}
				j = (j + 1) % permutationLength;
			}

			for (i = 0; i < permutationLength; i++) {
				offspring.get(0).variables().set(i, child0[i]);
				offspring.get(1).variables().set(i, child1[i]);
			}
			/*
			 * int cuttingPoint1; int cuttingPoint2;
			 * 
			 * cuttingPoint1 = cuttingPointRandomGenerator.getRandomValue(0,
			 * permutationLength - 1); cuttingPoint2 =
			 * cuttingPointRandomGenerator.getRandomValue(0, permutationLength - 1); while
			 * (cuttingPoint2 == cuttingPoint1) cuttingPoint2 =
			 * cuttingPointRandomGenerator.getRandomValue(0, permutationLength - 1);
			 * 
			 * if (cuttingPoint1 > cuttingPoint2) { int swap; swap = cuttingPoint1;
			 * cuttingPoint1 = cuttingPoint2; cuttingPoint2 = swap; } Integer[] child0 = new
			 * Integer[permutationLength]; Integer[] child1 = new
			 * Integer[permutationLength]; HashSet<Integer> snippet0 = new
			 * HashSet<Integer>(); HashSet<Integer> snippet1 = new HashSet<Integer>(); for
			 * (int i=cuttingPoint1; i<=cuttingPoint2; i++) {
			 * snippet0.add(offspring.get(0).variables().get(i));
			 * snippet1.add(offspring.get(1).variables().get(i)); child0[i] =
			 * offspring.get(0).variables().get(i); child1[i] =
			 * offspring.get(1).variables().get(i); }
			 * 
			 * int i=0; int j=0; while (i<cuttingPoint1) { if
			 * (!snippet0.contains(offspring.get(1).variables().get(j))) { child0[i] =
			 * offspring.get(1).variables().get(j); i += 1; } j += 1; } i = cuttingPoint2+1;
			 * while (i<permutationLength) { if
			 * (!snippet0.contains(offspring.get(1).variables().get(j))) { child0[i] =
			 * offspring.get(1).variables().get(j); i += 1; } j += 1; }
			 * 
			 * i=0; j=0; while (i<cuttingPoint1) { if
			 * (!snippet1.contains(offspring.get(0).variables().get(j))) { child1[i] =
			 * offspring.get(0).variables().get(j); i += 1; } j += 1; } i = cuttingPoint2+1;
			 * while (i<permutationLength) { if
			 * (!snippet1.contains(offspring.get(0).variables().get(j))) { child1[i] =
			 * offspring.get(0).variables().get(j); i += 1; } j += 1; }
			 * 
			 * for (i=0; i<permutationLength; i++) { offspring.get(0).variables().set(i,
			 * child0[i]); offspring.get(1).variables().set(i, child1[i]); }
			 */
		}

		return offspring;
	}

  @Override
  public int getNumberOfRequiredParents() {
    return 2 ;
  }

  @Override
  public int getNumberOfGeneratedChildren() {
    return 2;
  }
}
