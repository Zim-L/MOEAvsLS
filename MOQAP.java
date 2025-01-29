package gecco24;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.uma.jmetal.problem.permutationproblem.impl.AbstractIntegerPermutationProblem;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

public class MOQAP extends AbstractIntegerPermutationProblem {

	int n;
	int M = 2;
	double weightLimit;
	double opt1, opt2;
	double[] x;
	double[] y;
	double[][] distance;

	private double[][][] flows; // flow matrix

	public MOQAP() {
		initProblem(50);
	}

	public MOQAP(int n) {
		initProblem(n);
	}

	public void initProblem(int n) {
		this.n = n;
		setNumberOfVariables(n);
		setNumberOfObjectives(M);
		setName("QAP-" + n);

		JMetalRandom random = JMetalRandom.getInstance();
		x = new double[n];
		y = new double[n];
		for (int i = 0; i < n; i++) {
			x[i] = random.nextDouble(0, 5000);
			y[i] = random.nextDouble(0, 5000);
		}
		// calculate distance matrix
		distance = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = i; j < n; j++) {
				distance[i][j] = Math.sqrt((x[i] - x[j]) * (x[i] - x[j]) + (y[i] - y[j]) * (y[i] - y[j]));
				distance[j][i] = distance[i][j];
			}
		}

		// create flow matrix
		flows = new double[M][n][n];
		for (int m = 0; m < M; m++) {
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					flows[m][i][j] = random.nextDouble(0, 100);
				}
			}
		}
	}

	@Override
	public int getLength() {
		return n;
	}

	public void save(String path) {
		File file = new File(path);
		if (file.exists()) {
			System.out.println("Save failed, file exists");
			return;
		}
		try {
			PrintWriter writer = new PrintWriter(file);
			writer.println(n);
			writer.println(M);
			// save distance
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					writer.print(distance[i][j] + " ");
				}
				writer.println();
			}

			// save matrix
			for (int m = 0; m < M; m++) {
				for (int i = 0; i < n; i++) {
					for (int j = 0; j < n; j++) {
						writer.print(flows[m][i][j] + " ");
					}
					writer.println();
				}
			}
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public MOQAP load(String path) {
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(path);
			DataInputStream reader = new DataInputStream(inputStream);
			this.n = Integer.valueOf(reader.readLine());
			this.M = Integer.valueOf(reader.readLine());
			setNumberOfVariables(n);
			setNumberOfObjectives(M);

			for (int i = 0; i < n; i++) {
				String[] str = reader.readLine().split(" ");
				for (int j = 0; j < n; j++) {
					distance[i][j] = Double.valueOf(str[j]);
				}
			}

			for (int m = 0; m < M; m++) {
				for (int i = 0; i < n; i++) {
					String[] str = reader.readLine().split(" ");
					for (int j = 0; j < n; j++) {
						flows[m][i][j] = Double.valueOf(str[j]);
					}
				}
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return this;
	}
	
	public MOQAP loadXiaofeng(String path) {
		int m = getNumberOfObjectives();
		int n = getNumberOfVariables();
		String afile = path+"mQAP-M"+m+"-D"+n+"_a.txt";
		String b1file = path+"mQAP-M"+m+"-D"+n+"_b1.txt";
		String b2file = path+"mQAP-M"+m+"-D"+n+"_b2.txt";
		// read distance
		try {
			LineIterator reader = FileUtils.lineIterator(new File(afile));
			for (int i=0; i<n; i++) {
				String[] d = reader.nextLine().split("   ");
				for (int j=0; j<n; j++) {
					distance[i][j] = Double.parseDouble(d[j+1]);
				}
			}
		} catch (IOException e) {e.printStackTrace();}
		// read flows
		try {
			LineIterator reader1 = FileUtils.lineIterator(new File(b1file));
			LineIterator reader2 = FileUtils.lineIterator(new File(b2file));
			for (int i=0; i<n; i++) {
				String[] f1 = reader1.nextLine().split("   ");
				String[] f2 = reader2.nextLine().split("   ");
				for (int j=0; j<n; j++) {
					flows[0][i][j] = Double.parseDouble(f1[j+1]);
					flows[1][i][j] = Double.parseDouble(f2[j+1]);
				}
			}
		} catch (IOException e) {e.printStackTrace();}
		
		return this;
	}

	@Override
	public synchronized PermutationSolution<Integer> evaluate(PermutationSolution<Integer> solution) {
		double[] cost = new double[M];
		for (int k = 0; k < M; k++)
			cost[k] = 0;
		for (int k = 0; k < M; k++) {
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					cost[k] += distance[i][j] * flows[k][solution.variables().get(i)][solution.variables().get(j)];
				}
			}
		}
		for (int k = 0; k < M; k++) {
			solution.objectives()[k] = cost[k];
		}
		return solution;
	}

}
