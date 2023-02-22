package assignments;

import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.TallyStore;

/**
 * @author mctenthij
 * Edited by qvanderkaaij and jberkhout
 */
public class Ambulance extends Event {

	// ambulance variables
	int id;
	Region baseRegion;
	Accident currentAccident;
	boolean servesOutsideRegion;
	double drivingTimeHospitalToBase;

	// RNG
	ExponentialGen serviceTimeGen;

	// stats counters
	TallyStore waitTimeTally = new TallyStore("Waiting times");
	TallyStore serviceTimeTally = new TallyStore("Service times");
	TallyStore withinTargetTally = new TallyStore("Arrival within target");

	public Ambulance(int id, Region baseRegion, RandomStream serviceRandomStream, double serviceRate, boolean servesOutsideRegion) {
		this.id = id;
		currentAccident = null;
		this.baseRegion = baseRegion;
		serviceTimeGen = new ExponentialGen(serviceRandomStream, serviceRate);
		this.servesOutsideRegion = servesOutsideRegion;
		this.drivingTimeHospitalToBase = drivingTimeHospitalToBase();
	}

    public void startService(Accident accident, double arrivalTimeAtAccident) {
        currentAccident = accident;
        accident.serviceStarted(arrivalTimeAtAccident);
        double serviceTimeAtScene = serviceTimeGen.nextDouble();
        double busyServing = 0.0; // calculate the time needed to process the accident and drive back to the base
        schedule(busyServing); // after busyServing it becomes idle again
    }

    public void serviceCompleted() {
        // process the completed current accident: the ambulance brought the
        // patient to the hospital and is back at its base, what next?
    }

    // return Euclidean distance between accident and hospital
    public double drivingTimeToAccident(Accident cust) {
        // calculate the driving time from the baselocation of the ambulance to the accident location
        return 0.0;
    }

    // return Euclidean distance between accident and hospital
    public double drivingTimeToHostital(Accident cust) {
        // calculate the driving time from accident location to the hospital
        return 0.0;
    }

	// return Euclidean distance from the hospital to the base
	public double drivingTimeHospitalToBase() {
        // calculate the driving time from the hospital to the base
        return 0.0;
	}

    // event: the ambulance is back at its base after completing service
    @Override
    public void actions() {
        serviceCompleted();
    }
}