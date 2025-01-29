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

/**
 * MOTSP
 * @author Zimin Liang
 * This version is bi-objective ONLY.
 */
public class MOTSP extends AbstractIntegerPermutationProblem {

	public int n;
	int M = 2;
	double weightLimit;
	double opt1, opt2;
	double[][] x;
	double[][] y;
	public double[][][] map;
	public String matType = "RAN";

	public MOTSP() {
		initProblem(50);
	}

	public MOTSP(int n) {
		initProblem(n);
	}

	public void initProblem(int n) {
		this.n = n;
		setNumberOfVariables(n);
		setNumberOfObjectives(M);
		setName("TSP-" + n);

		if (matType.contentEquals("EUC")) {
			JMetalRandom random = JMetalRandom.getInstance();
			x = new double[M][n];
			y = new double[M][n];
			for (int j = 0; j < M; j++) {
				for (int i = 0; i < n; i++) {
					x[j][i] = random.nextDouble(0, 5000);
					y[j][i] = random.nextDouble(0, 5000);
				}
			}
			// calculate distance
			map = new double[M][n][n];
			for (int k = 0; k < M; k++) {
				for (int i = 0; i < n; i++) {
					for (int j = i; j < n; j++) {
						map[k][i][j] = Math.sqrt(
								(x[k][i] - x[k][j]) * (x[k][i] - x[k][j]) + (y[k][i] - y[k][j]) * (y[k][i] - y[k][j]));
						map[k][j][i] = map[k][i][j];
						// System.out.println(map[k][i][j]);
					}
				}
			}
		} else if (matType.contentEquals("RAN")) {
			JMetalRandom random = JMetalRandom.getInstance();
			map = new double[M][n][n];
			for (int k = 0; k < M; k++) {
				for (int i = 0; i < n; i++) {
					for (int j = i; j < n; j++) {
						map[k][i][j] = random.nextDouble();
						map[k][j][i] = map[k][i][j];
						// System.out.println(map[k][i][j]);
					}
				}
			}
		}
	}

	@Override
	public int getLength() {
		return n;
	}

	public MOTSP load(String path1, String path2) {
		double[][] matrix1 = loadMatrixFile(path1);
		double[][] matrix2 = loadMatrixFile(path2);
		map = new double[M][n][n];
		map[0] = matrix1;
		map[1] = matrix2;
		return this;
	}
	
	public MOTSP loadXiaofeng(String path) {
		int m = getNumberOfObjectives();
		int n = getNumberOfVariables();
		String d1file = path+"MOTSP-M"+m+"-D"+n+"_d1.txt";
		String d2file = path+"MOTSP-M"+m+"-D"+n+"_d2.txt";
		try {
			LineIterator reader1 = FileUtils.lineIterator(new File(d1file));
			LineIterator reader2 = FileUtils.lineIterator(new File(d2file));
			for (int i=0; i<n; i++) {
				String[] f1 = reader1.nextLine().split("   ");
				String[] f2 = reader2.nextLine().split("   ");
				for (int j=0; j<n; j++) {
					map[0][i][j] = Double.parseDouble(f1[j+1]);
					map[1][i][j] = Double.parseDouble(f2[j+1]);
				}
			}
		} catch (IOException e) {e.printStackTrace();}
		
		return this;
	}

	public double[][] loadMatrixFile(String path) {
		// System.out.println("Loading "+path);
		double[][] res = null;
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(path);
			DataInputStream reader = new DataInputStream(inputStream);
			String line;
			line = reader.readLine(); // line 1 problem name
			line = reader.readLine(); // line 2 problem type
			line = reader.readLine(); // line 3 problem comment
			line = reader.readLine(); // line 4 dimension
			String[] tokens = line.split(" ");
			this.n = Integer.valueOf(tokens[1]);
			res = new double[n][n];
			line = reader.readLine(); // line 5 edge type
			line = reader.readLine(); // line 6 edge format
			line = reader.readLine(); // line 7 edge weight section
			// line 8 to line 8+n(exclusive) matrix info
			for (int i = 0; i < this.n; i++) {
				String str = reader.readLine();
				for (int j = 0; j < this.n; j++) {
					str = str.strip();
					String[] strs = str.split(" ", 2);
					if (strs.length > 1)
						str = strs[1];
					res[i][j] = Double.valueOf(strs[0]);
					// System.out.print(res[i][j]+" ");
				}
				// System.out.println();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
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

			// save matrix
			for (int m = 0; m < M; m++) {
				for (int i = 0; i < n; i++) {
					for (int j = 0; j < n; j++) {
						writer.print(map[m][i][j] + " ");
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

	public MOTSP load(String path) {
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(path);
			DataInputStream reader = new DataInputStream(inputStream);
			this.n = Integer.valueOf(reader.readLine());
			this.M = Integer.valueOf(reader.readLine());
			setNumberOfVariables(n);
			setNumberOfObjectives(M);

			for (int m = 0; m < M; m++) {
				for (int i = 0; i < n; i++) {
					String[] str = reader.readLine().split(" ");
					for (int j = 0; j < n; j++) {
						map[m][i][j] = Double.valueOf(str[j]);
					}
				}
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}

	@Override
	public synchronized PermutationSolution<Integer> evaluate(PermutationSolution<Integer> solution) {
		double[] distance = new double[M];
		for (int j = 0; j < M; j++)
			distance[j] = 0;
		for (int j = 0; j < M; j++) {
			for (int i = 0; i < n - 1; i++) {
				int start = solution.variables().get(i);
				int dest = solution.variables().get(i + 1);
				distance[j] += map[j][start][dest];
			}
			distance[j] += map[j][solution.variables().get(n - 1)][solution.variables().get(0)];
		}
		for (int j = 0; j < M; j++) {
			solution.objectives()[j] = distance[j];
		}
		return solution;
	}

}
