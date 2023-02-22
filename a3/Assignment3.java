package assignments;

import java.util.HashSet;
import java.util.Random;

import umontreal.ssj.probdist.StudentDist;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.stat.TallyStore;

/**
 * Template Assignment 3.
 *
 * @author mctenthij Edited by qvanderkaaij and jberkhout
 */
public class Assignment3 {

	// optimization variables
	Solution[] solutionsList;
	int numbSolutions;
	int budget;
	int kMin;
	int kMax;
	int KMin;
	int KMax;

	// threshold queue variables
	double arrivalRate;
	double lowServiceRate;
	double highServiceRate;
	double stopTime;
	double costsLow;
	double costsHigh;

	Random RNG = new Random(0L); // in case a random number is needed

	public Assignment3(int kMin, int kMax, int KMin, int KMax, int budget, double arrivalRate, double muLow,
			double muHigh, double costsLow, double costsHigh, double stopTime) {

		// check how many solutions are possible
		int kRange = kMax - kMin + 1;
		int KRange = KMax - KMin + 1;
		numbSolutions = kRange * KRange;

		// create all solutions and store them in solutionsList[]
		solutionsList = new Solution[numbSolutions];
		for (int i = 0; i < kRange; i++) {
			for (int j = 0; j < KRange; j++) {
				Solution solution = new Solution(kMin + i, KMin + j);
				solutionsList[KRange * i + j] = solution;
			}
		}

		// set optimization variables
		this.budget = budget;
		this.KMin = KMin;
		this.kMin = kMin;
		this.KMax = KMax;
		this.kMax = kMax;

		// set threshold queue variables
		this.arrivalRate = arrivalRate;
		this.lowServiceRate = muLow;
		this.highServiceRate = muHigh;
		this.stopTime = stopTime;
		this.costsLow = costsLow;
		this.costsHigh = costsHigh;
	}

	// represents a threshold setting and its simulation results
	class Solution {

		int kValue;
		int KValue;
		TallyStore simResults;

		public Solution(int k, int K) {
			this.kValue = k;
			this.KValue = K;
			this.simResults = new TallyStore("Simulation results (" + k + ", " + K + ")");
		}
	}

	// calculates the index position of solution (k, K) in the solutionsList
	public int calcSolutionPosition(int k, int K) {
		return (k - kMin) * (KMax - KMin + 1) + K - KMin;
	}

	// returns the solution object corresponding to (k, K)
	public Solution getSolution(int k, int K) {
		return solutionsList[calcSolutionPosition(k, K)];
	}

	// prints the simulation results of solution
	public void printSolutionResults(Solution solution) {
		System.out.print("Solution (");
		System.out.print(solution.kValue);
		System.out.print(", ");
		System.out.print(solution.KValue);
		System.out.print(") is simulated ");
		System.out.print(solution.simResults.numberObs());
		if (solution.simResults.numberObs() > 1) {
			// we can calculate average and (unbiased) standard deviation
			System.out.print(" times and has an average costs of ");
			System.out.print(solution.simResults.average());
			System.out.print(" with standard deviation of ");
			System.out.print(solution.simResults.standardDeviation());
			System.out.println(".");
		} else if (solution.simResults.numberObs() == 1) {
			// only one sample: only report sample = average
			System.out.print(" time and got a value of ");
			System.out.print(solution.simResults.average());
			System.out.println(".");
		} else {
			System.out.println(" times and therefore has no statistics.");
		}
	}

	// prints simulation results of all solutions
	public void printAllSolutionsResults() {
		for (int i = 0; i < numbSolutions; i++) {
			printSolutionResults(solutionsList[i]);
		}
	}

	// returns best simulated solution (only simulated solutions considered)
	public Solution getBestSimulatedSolution() {
		double minimum = Double.POSITIVE_INFINITY;
		Solution optSolution = null;
		for (int i = 0; i < numbSolutions; i++) {
			if (solutionsList[i].simResults.numberObs() > 0) {
				if (solutionsList[i].simResults.average() < minimum) {
					minimum = solutionsList[i].simResults.average();
					optSolution = solutionsList[i];
				}
			}
		}
		return optSolution;
	}

	// returns a random seed for replication purposes
	public long[] generateSeed() {
		long[] seed = new long[6];

		for (int i = 0; i < seed.length; i++) {
			seed[i] = RNG.nextInt();
		}

		return seed;
	}

	// returns a RNG that starts from a random seed
	public MRG32k3a getRNG() {

		// TO DO: create a random seed, for examplel using RNG
		long[] seed = generateSeed();

		// create RNG based on seed
		MRG32k3a RNG = new MRG32k3a();
		RNG.setSeed(seed);

		return RNG;
	}

	// returns a RNG for a given seed (useful for replication purposes)
	public MRG32k3a getRNG(long[] seed) {
		MRG32k3a randomStream = new MRG32k3a();
		randomStream.setSeed(seed);
		return randomStream;
	}

	// simulates solution (k, K) and returns the average costs
	public double simulateSolution(int k, int K) {

		// init random sources
		MRG32k3a arrivalRNG = getRNG();
		MRG32k3a serviceRNG = getRNG();

		// create threshold queue model
		ThresholdQueue model = new ThresholdQueue(arrivalRate, lowServiceRate, highServiceRate, costsLow, costsHigh,
				stopTime, k, K, arrivalRNG, serviceRNG);

		return model.simulateRunningCosts().average();
	}

	// performs ranking and selection and returns best solution:
	// simulates all solutions initialRuns times, and afterwards evenly divides
	// the remaining budget over all solutions that are not significantly
	// outperformed with level alpha
	// public Solution runRankingSelection(int initialRuns, double alpha) {
	//
	// // TO DO: perform initial runs
	//
	// HashSet<State> I = selectCandidateSolutions(alpha);
	//
	// // TO DO: perform rest of the runs
	//
	// return getBestSimulatedSolution();
	// }

	// returns not significantly outperformed solutions with level alpha
	// public HashSet<Solution> selectCandidateSolutions(double alpha) {
	//
	// HashSet<State> I = new HashSet(); // set of candidate solutions
	//
	// // TO DO: find all candidate solutions for the ranking and selection
	// // method and add them to I
	//
	// return I;
	// }

	// performs the local search algorithm starting from random solution
	public Solution runLocalSearch() {

		// TO DO: perform local search here

		return getBestSimulatedSolution();
	}

	// returns solution with lowest average costs
	public Solution compareTwoSolutions(Solution sol1, Solution sol2) {

		// TO DO: return the best solution

		return (sol1.simResults.average() < sol2.simResults.average()) ? sol1 : sol2;
	}

	// returns a random solution
	// public Solution getRandomSolution() {
	//
	// Solution randomSolution;
	//
	// // TO DO: generate a random solution
	//
	// return randomSolution;
	// }

	// return a random solution from the neighborhood of given solution
	// public Solution getRandomNeighbor (Solution solution) {
	//
	// Solution randomNeighbor;
	//
	// // TO DO: generate a random neighbor of solution
	//
	// return randomNeighbor;
	// }

	// simulates (k, K) and (k2, K2) with CRN and return their average costs
	public double[] simulateTwoSolutionsWithCRN(int k, int K, int k2, int K2, boolean verbose) {

		// init
		double[] averageCosts = new double[2];
		long[] arrivalSeed = generateSeed();
		long[] serviceSeed = generateSeed();

		// TO DO: simulate (k, K) and (k2, K2) with CRN and store their average
		// costs in averageCosts

		// create threshold queue model
		ThresholdQueue model = new ThresholdQueue(arrivalRate, lowServiceRate, highServiceRate, costsLow, costsHigh,
				stopTime, k, K, getRNG(arrivalSeed), getRNG(serviceSeed));
		ThresholdQueue model2 = new ThresholdQueue(arrivalRate, lowServiceRate, highServiceRate, costsLow, costsHigh,
				stopTime, k2, K2, getRNG(arrivalSeed), getRNG(serviceSeed));

		averageCosts[0] = model.simulateRunningCosts().average();
		averageCosts[1] = model2.simulateRunningCosts().average();

		if (verbose) {
			System.out.println("Average costs for (" + k + ", " + K + ") = " + averageCosts[0]);
			System.out.println("Average costs for (" + k2 + ", " + K2 + ") = " + averageCosts[1]);
		}

		return averageCosts;
	}

	// simulates numbSims-times (k, K) and (k2, K2) without and with CRN
	// and prints the results for their differences
	public void simulateTwoSolutionsMultipleTimes(int numbSims, int k, int K, int k2, int K2, boolean verbose) {

		// init
		TallyStore diffAverCostsNormal = new TallyStore("Difference average costs for normal simulation");
		TallyStore diffAverCostsCRN = new TallyStore("Difference average costs for CRN");

		// TO DO: simulate (k, K) and (k2, K2) without and with CRN multiple
		// times to get their differences and report their Tallystores
		for (int i = 0; i < numbSims; i++) {
			diffAverCostsNormal.add(simulateSolution(k, K) - simulateSolution(k2, K2));

			double[] crnCosts = simulateTwoSolutionsWithCRN(k, K, k2, K2, verbose);
			diffAverCostsCRN.add(crnCosts[0] - crnCosts[1]);
		}

		// report results
		System.out.println(diffAverCostsNormal.report());
		System.out.println(diffAverCostsCRN.report());
	}

	public static void main(String[] args) {

		// threshold queue variables
		double lambda = 3. / 2; // arrival rate
		double muHigh = 6; // average service time
		double muLow = 2; // average service time
		double costsLow = 5; // costs per time unit when operating at low rate
		double costsHigh = 10; // costs per time unit when operating at high rate
		double stopTime = 10000; // simulation endtime (seconds)

		// optimization variables
		int kMin = 5; // lowest possible value for k
		int kMax = 9; // highest possible value for k
		int KMin = 10; // lowest possible value for K
		int KMax = 20; // highest possible value for K
		int budget = 5000; // budget for the initial runs
		int initialRuns = 2500; // initial runs for the Ranking and selection method
		double alpha = 0.05; // alpha value for the Ranking and selection method
		int k = 5; // k-threshold for queue
		int K = 20; // K-threshold for queue
		int k2 = 9; // k-threshold for alternative queue
		int K2 = 20; // K-threshold for alternative queue

		// Question 1
		Assignment3 CRN = new Assignment3(kMin, kMax, KMin, KMax, budget, lambda, muLow, muHigh, costsLow, costsHigh,
				stopTime);
		CRN.simulateTwoSolutionsWithCRN(k, K, k2, K2, true);

		// Question 2
		Assignment3 CRNMultipleTimes = new Assignment3(kMin, kMax, KMin, KMax, budget, lambda, muLow, muHigh, costsLow,
				costsHigh, stopTime);
		CRNMultipleTimes.simulateTwoSolutionsMultipleTimes(100, k, K, k2, K2, false);
		//
		// // Question 3
		// Assignment3 localSearch = new Assignment3(kMin, kMax, KMin, KMax, budget,
		// lambda, muLow, muHigh, costsLow, costsHigh, stopTime);
		// Solution localSearchSolution = localSearch.runLocalSearch();
		// localSearch.printSolutionResults(localSearchSolution);
		//
		// // Question 4
		// Assignment3 rankingAndSelection = new Assignment3(kMin, kMax, KMin, KMax,
		// budget, lambda, muLow, muHigh, costsLow, costsHigh, stopTime);
		// rankingAndSelection.runRankingSelection(initialRuns, alpha, true);
		// rankingAndSelection.printSolutionResults(rankingAndSelection.getBestSimulatedSolution());
	}
}
