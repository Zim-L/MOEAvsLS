package gecco24;

import java.util.List;

import org.uma.jmetal.problem.Problem;

public interface ZAlgorithm<S> extends Runnable {
	public List<S> getPopulation();
	public List<S> getArchive();
	public int getT();
	public String getName();
	public Problem getProblem();
	public List<S> getResult();
}
