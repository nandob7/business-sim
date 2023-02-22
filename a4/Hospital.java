package assignments;

import java.util.Random;

import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.StatProbe;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.list.ListOfStatProbes;

/**
 * Models the hospital and its regions from Assignment 4.
 *
 * @author mctenthij
 * Edited by qvanderkaaij and jberkhout
 */
public class Hospital {

	// hospital variables
	public static double RESPONSE_TIME_TARGET = 15.0; // in minutes
	int numAmbulances;
	Ambulance[] ambulances;
	int numRegions;
	Region[] regions;
	double[] arrivalRates; // for all regions
	double serviceRate; // at accidents
	int[] ambulancePlacements; // ambulance placement strategy
	double stopTime;

	// RNG for seeds
	Random rng = new Random(); // for replication purposes you could set a seed

	// stats counters
	Tally serviceTimeTally;
	Tally waitTimeTally;
	Tally withinTargetTally;
	ListOfStatProbes<StatProbe> listStatsTallies;

	public Hospital(int numAmbulances, double[] arrivalRates, double serviceRate, double stopTime, int numRegions, boolean serveOutsideBaseRegion, int[] ambulancePlacements) {

		// set hospital variables
		this.numAmbulances = numAmbulances;
		ambulances = new Ambulance[numAmbulances];
		this.numRegions = numRegions;
		regions = new Region[numRegions];
		this.arrivalRates = arrivalRates;
		this.serviceRate = serviceRate;
		this.stopTime = stopTime;
		this.ambulancePlacements = ambulancePlacements;

		// create regions
		for (int j = 0; j < numRegions; j++) {
			double[] baseLocation = determineRegionLocation(j);
			RandomStream arrivalRandomStream = getStream();
			RandomStream locationRandomStream = getStream();
			regions[j] = new Region(j, baseLocation, arrivalRandomStream, arrivalRates[j], locationRandomStream, regions, numRegions);
		}

		// create and assign ambulances to regions
		for (int i = 0; i < numAmbulances; i++) {
			int region = determineBaseRegion(i);
			RandomStream serviceRandomStream = getStream();
			Ambulance ambulance = new Ambulance(i, regions[region], serviceRandomStream, serviceRate, serveOutsideBaseRegion);
			ambulances[i] = ambulance;
			regions[region].idleAmbulances.add(ambulance); // initially the ambulance is idle
		}

		// create Tallies
		waitTimeTally = new Tally("Waiting time");
		serviceTimeTally = new Tally("Service time");
		withinTargetTally = new Tally("Arrival within target");

		// add Tallies in ListOfStatProbes for later reporting
		listStatsTallies = new ListOfStatProbes<>("Stats for Tallies");
		listStatsTallies.add(waitTimeTally);
		listStatsTallies.add(serviceTimeTally);
		listStatsTallies.add(withinTargetTally);
	}

    // returns region index to which the ambulance should be assigned
    public int determineBaseRegion(int ambulanceNumber) {
        // this function must be adjusted

        // use ambulancePlacements to return the right base region index for
        // the ambulance with ambulanceNumber

        return 0;
    }

    // returns the location coordinates of the base of region j
    public double[] determineRegionLocation(int j) {

        // this function must be adjusted

        double[] location = new double[2];
        location[0] = 0.0; // X-Coordinate of accident location
        location[1] = 0.0; // Y-Coordinate of accident location
        return location;
    }

	public ListOfStatProbes simulateOneRun() {

		Sim.init();

		// reset stats counters
		listStatsTallies.init();

		// set first events
		for (int j = 0; j < numRegions; j++) {
			regions[j].arrivalProcess.init(); // schedules first arrival region j
		}
		new StopEvent().schedule(stopTime); // schedule stopping time

		// start simulation
		Sim.start();

		// combine results in the Hospital tallies
		for (int k = 0; k < numAmbulances; k++) {
			for (double obs: ambulances[k].serviceTimeTally.getArray()) {
				serviceTimeTally.add(obs);
			}
			for (double obs: ambulances[k].waitTimeTally.getArray()) {
				waitTimeTally.add(obs);
			}
			for (double obs: ambulances[k].withinTargetTally.getArray()) {
				withinTargetTally.add(obs);
			}
		}

		return listStatsTallies;
	}

	public void simulateOneRunAndReport() {
		ListOfStatProbes output = simulateOneRun();
		System.out.println(output.report());
	}

	// generate a random stream based on a random seed
	public MRG32k3a getStream() {
		long[] seed = new long[6];
		for (int i =0;i<seed.length;i++) {
			seed[i] = (long) rng.nextInt();
		}
		MRG32k3a myrng = new MRG32k3a();
		myrng.setSeed(seed);
		return myrng;
	}

	// stop simulation by scheduling this event
	class StopEvent extends Event {
		@Override
		public void actions() {
			Sim.stop();
		}
	}

    public static void main(String[] args) {

        // hospital variables
		int numAmbulances = 20;
		int numRegions = 7;
		double[] arrivalRates = {1./15, 1./15, 1./15, 1./15, 1./15, 1./15, 1./15}; // arrival rates per region
		double serviceRate = 1.0;
		double stopTime = 10000; // simulation endtime (minutes)
		boolean serveOutsideBaseRegion = true; // if true, ambulances serve outside their base regions, false otherwise

		// simulate ambulance placement 1
		int[] ambulancePlacements = {1, 4, 2, 4, 1, 3, 5}; // should be of the length numRegions and with a total sum of numAmbulances
		Hospital hospital = new Hospital(numAmbulances, arrivalRates, serviceRate, stopTime, numRegions, serveOutsideBaseRegion, ambulancePlacements);
		hospital.simulateOneRunAndReport();

		// simulate ambulance placement 2
		int[] ambulancePlacements2 = {1, 3, 3, 4, 1, 4, 4}; // should be of the length numRegions and with a total sum of numAmbulances
		hospital = new Hospital(numAmbulances, arrivalRates, serviceRate, stopTime, numRegions, serveOutsideBaseRegion, ambulancePlacements2);
		hospital.simulateOneRunAndReport();

		// further optimization experiments can be done here
    }
}
