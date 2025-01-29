package gecco24;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.uma.jmetal.problem.binaryproblem.impl.AbstractBinaryProblem;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class MONKLand extends AbstractBinaryProblem {
	
	/**
	 * This version of NKLandscape is to adapt Xiaofeng's MATLAB version, for GECCO 2024 Li et al
	 */
	int N;
	int M = 2;
	int K = 10;
	int matSize;
	int exponent = 1;
	private ArrayList<Integer> bitsPerVariable;
	
	double[][][] mat;
	
	int[][][] links;
	Random random = new Random();
	
	
	
	public MONKLand() {
		initProblem(50, 5);
	}

	public MONKLand(int n, int k){
		initProblem(n, k);
	}
	
	public void initProblem(int n, int k) {
		this.N = n;
		this.M = 2;
		this.K = k;
		setNumberOfVariables(n);
		setNumberOfObjectives(2);
	    setName("NK-"+N+"-"+K);
	    bitsPerVariable = new ArrayList<Integer>(n);
	    for (int i=0; i<n; i++) bitsPerVariable.add(1);
	  
	    links = new int[M][N][K];
	    for (int m=0; m<M; m++) {
	    	for (int i=0; i<N; i++) {
	    		ArrayList<Integer> loci = new ArrayList<Integer>();
	    		while (loci.size()<K) {
	    			int neighbour = random.nextInt(N);
	    			while (neighbour==i || loci.contains(neighbour)) {
	    				neighbour = random.nextInt(N);
	    			}
	    			loci.add(neighbour);
	    		}
	    		Collections.sort(loci);
	    		for (int j=0; j<K; j++) {
	    			links[m][i][j] = loci.get(j);
	    		}
	    	}
	    }
	    
	    // create matrix
	    matSize = intPow(2, k+1);
	    mat = new double[M][n][matSize];
	    for (int i=0; i<n; i++) {
	    	for (int j=0; j<matSize; j++) {
	    		for (int m=0; m<M; m++) {
	    			mat[m][i][j] = random.nextDouble();
	    		}
	    	}
	    }
	}
	
	private int intPow(int a, int b) {
		int res = 1;
		for (int i=0; i<b; i++) res*=a;
		return res;
	}

	
	
	@Override
	public synchronized BinarySolution evaluate(BinarySolution solution) {
		int[] bits = new int[N];
		for (int i=0; i<N; i++) {
			boolean bit = solution.variables().get(i).get(0);
			bits[i] = bit ? 1 : 0;
		}
		
		double[] obj = new double[M];
		for (int m=0; m<M; m++) obj[m]=0;
		
		int[] indexes = new int[K + 1];  // re-used across iterations
        int[] nicheBits = new int[K + 1]; // re-used to store bits in sorted order
		for (int i=0; i<N; i++) {
			for (int m=0; m<M; m++) {
				System.arraycopy(links[m][i], 0, indexes, 0, K);
                indexes[K] = i;

                Arrays.sort(indexes);
				
                for (int j = 0; j < K + 1; j++) {
                    nicheBits[j] = bits[indexes[j]];
                }
                int patternIndex = bits2int(nicheBits);
                obj[m] += mat[m][i][patternIndex];
			}
		}
		
		for (int m=0; m<M; m++) {
			solution.objectives()[m] = 1 - obj[m]/N;
		}
		
		return solution;
	}
	
	private int bits2int(int[] bits) {
        int index = 0;
        for (int b : bits) {
            index = (index << 1) | (b & 1);
        }
        return index;
    }

	@Override
	public void setName(String name) {
		super.setName(name);
	}
	
	@Override
	public List<Integer> getListOfBitsPerVariable() {
		return bitsPerVariable;
	}

	public void save(String path) {
		File file = new File(path);
		if (file.exists()) {
			System.out.println("Save failed, file exists");
			return;
		}
		try {
			PrintWriter writer = new PrintWriter(file);
			writer.println(N);
			writer.println(M);
			writer.println(K);
			// save neighbours
			
			for(int m=0; m<M; m++) {
				for (int i=0; i<N; i++) {
					for (int j=0; j<K; j++) {
						writer.print(links[m][i][j]);
						writer.print(' ');
					}
					writer.println();
				}
			}
			
			// save matrix
			for(int i=0; i<N; i++) {
				for(int j=0; j<matSize; j++) {
					for (int m=0; m<M-1; m++) {
						writer.print(mat[m][i][j]);
						writer.print(' ');
					}
					writer.print(mat[M-1][i][j]);
					writer.println();
				}
			}
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public MONKLand loadXiaofeng(String path) {
		String i1file = path+"MONKLand-M"+M+"-N"+N+"-K"+K+"_I1.txt";
		String i2file = path+"MONKLand-M"+M+"-N"+N+"-K"+K+"_I2.txt";
		String c1file = path+"MONKLand-M"+M+"-N"+N+"-K"+K+"_C1.txt";
		String c2file = path+"MONKLand-M"+M+"-N"+N+"-K"+K+"_C2.txt";
		
		// read neighbours
		try {
			LineIterator reader1 = FileUtils.lineIterator(new File(i1file));
			LineIterator reader2 = FileUtils.lineIterator(new File(i2file));
			for (int i=0; i<N; i++) {
				String[] nbs1 = reader1.nextLine().split("   ");
				String[] nbs2 = reader2.nextLine().split("   ");
				int k1=0, k2=0;
				for (int j=0; j<N; j++) {
					if (i!=j && Double.valueOf(nbs1[j+1])==1) {
						links[0][i][k1] = j;
						k1++;
					}
					if (i!=j && Double.valueOf(nbs2[j+1])==1) {
						links[1][i][k2] = j;
						k2++;
					}
				}
			}
		} catch (IOException e) {e.printStackTrace();}
		
		// read contributions
		try {
			LineIterator reader1 = FileUtils.lineIterator(new File(c1file));
			LineIterator reader2 = FileUtils.lineIterator(new File(c2file));
			for (int e=0; e<matSize; e++) {
				String[] nbs1 = reader1.nextLine().split("   ");
				String[] nbs2 = reader2.nextLine().split("   ");
				for (int i=0; i<N; i++) {
					mat[0][i][e] = Double.valueOf(nbs1[i+1]);
					mat[1][i][e] = Double.valueOf(nbs2[i+1]);
				}
			}
		} catch (IOException e) {e.printStackTrace();}
		
		return this;
	}
	
	public MONKLand load(String path) {
		FileInputStream inputStream;
		try {
			LineIterator reader = FileUtils.lineIterator(new File(path));
			this.N = Integer.valueOf(reader.nextLine());
			this.M = Integer.valueOf(reader.nextLine());
			this.K = Integer.valueOf(reader.nextLine());
			setNumberOfVariables(N);
			setNumberOfObjectives(2);
			this.matSize = intPow(2, this.K+1);
			
			for(int m=0; m<M; m++) {
				for (int i=0; i<N; i++) {
					String[] str = reader.nextLine().split(" ");
					for (int j=0; j<K; j++) {
						links[m][i][j] = Integer.valueOf(str[j]);
					}
				}
			}
			
			for (int i=0; i<N; i++) {
				for (int j=0; j<matSize; j++) {
					String[] str = reader.nextLine().split(" ");
					for (int m=0; m<M; m++) { 
						mat[m][i][j] = Double.valueOf(str[m]);
					}			
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("File not exist, creating one");
			save(path);
		} 
		
		return this;
	}
}
