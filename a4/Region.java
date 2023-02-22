package assignments;

import java.util.LinkedList;
import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;

/**
 * @author A van Oostveen
 * Edited by J Berkhout
 */
public class Region {

    // region variables
	LinkedList<Accident> queue; // for the unaddressed accidents
	LinkedList<Ambulance> idleAmbulances;
	double[] baseLocation; // of the particular region
	ArrivalProcess arrivalProcess;
	int regionID;
	Region[] regions;
	int numRegions;

	RandomStream locationStream; // random number generator for locations
    
	public Region(int id, double[] baseLocation, RandomStream arrivalRandomStream, double arrivalRate, RandomStream locationRandomStream, Region[] regionArray, int numRegions) {

		// set region variables
		queue = new LinkedList<>();
		idleAmbulances = new LinkedList<>();
		this.baseLocation = baseLocation;
		regionID = id;
		this.regions = regionArray;
		this.numRegions = numRegions;

		// set random streams
		arrivalProcess = new ArrivalProcess(arrivalRandomStream, arrivalRate);
		locationStream = locationRandomStream;
	}
    
    public void handleArrival() {
        // create and process a new accident
    }

    // returns a random location inside the region
    public double[] drawLocation() {
        // determine the location of the accident
        double[] location = new double[2];
        location[0] = 0.0; // X-Coordinate of accident location
        location[1] = 0.0; // Y-Coordinate of accident location
        return location;
    }
    
	class ArrivalProcess extends Event {

		ExponentialGen arrivalTimeGen;
		double arrivalRate;

		public ArrivalProcess(RandomStream rng, double arrivalRate) {
			this.arrivalRate = arrivalRate;
			arrivalTimeGen = new ExponentialGen(rng, arrivalRate);
		}

		// event: new customer arrival at the store
		@Override
		public void actions() {
			handleArrival();
			double nextArrival = arrivalTimeGen.nextDouble();
			schedule(nextArrival); // schedule a new arrival event
		}

		public void init() {
			double nextArrival = arrivalTimeGen.nextDouble();
			schedule(nextArrival); // schedule a first new arrival
		}
	}
}
