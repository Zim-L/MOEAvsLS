package gecco24;

import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.pseudorandom.BoundedRandomGenerator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

/**
 * InversionMutation (2-opt)
 * @author Zimin Liang <zimin.liang@outlook.com>
 * An implementation of classical 2-opt operator, under the jMetal framework
 */
public class InversionMutation<T> implements MutationOperator<PermutationSolution<T>> {

	private double mutationProbability;
	private RandomGenerator<Double> mutationRandomGenerator;
	private BoundedRandomGenerator<Integer> positionRandomGenerator;

	public InversionMutation(double mutationProbability) {
		this(
		        mutationProbability,
		        () -> JMetalRandom.getInstance().nextDouble(),
		        (a, b) -> JMetalRandom.getInstance().nextInt(a, b));
	}
	
	 /** Constructor */
	  public InversionMutation(
	      double mutationProbability, RandomGenerator<Double> randomGenerator) {
	    this(
	        mutationProbability,
	        randomGenerator,
	        BoundedRandomGenerator.fromDoubleToInteger(randomGenerator));
	  }

	public InversionMutation(double mutationProbability, 
			RandomGenerator<Double> mutationRandomGenerator, 
			BoundedRandomGenerator<Integer> positionRandomGenerator) {
		Check.probabilityIsValid(mutationProbability);
	    this.mutationProbability = mutationProbability;
	    this.mutationRandomGenerator = mutationRandomGenerator;
	    this.positionRandomGenerator = positionRandomGenerator;
	}

	@Override
	public double getMutationProbability() {
		return mutationProbability;
	}

	@Override
	public PermutationSolution<T> execute(PermutationSolution<T> solution) {
		Check.notNull(solution);

	    doMutation(solution);
	    return solution;
	}

	private void doMutation(PermutationSolution<T> solution) {
		int permutationLength;
	    permutationLength = solution.variables().size();
	    
	    if ((permutationLength != 0) && (permutationLength != 1)) {
	    	if (mutationRandomGenerator.getRandomValue() < mutationProbability) {
	    		int pos1 = positionRandomGenerator.getRandomValue(0, solution.variables().size()-1);
	    		int pos2 = positionRandomGenerator.getRandomValue(0, solution.variables().size()-1);
	    		while (pos2==pos1) pos2 = positionRandomGenerator.getRandomValue(0, solution.variables().size()-1);
	    		if (pos2<pos1) {
	    			int temp = pos1;
	    			pos1 = pos2;
	    			pos2 = temp;
	    		}
	    		while (pos1<pos2) {
	    			T temp = solution.variables().get(pos1);
	    			solution.variables().set(pos1, solution.variables().get(pos2));
	    			solution.variables().set(pos2, temp);
	    			pos1++;
	    			pos2--;
	    		}
	    	}

	    }
		
	}

}
