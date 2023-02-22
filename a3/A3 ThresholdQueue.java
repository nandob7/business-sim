package assignments;

import java.util.LinkedList;

import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Accumulate;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.StatProbe;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.list.ListOfStatProbes;

/**
 * Class for simulating the threshold queue for Assignment 3, see main method
 * for examples on how to use it.
 *
 * @author mctenthij
 * Edited by qvdkaaij and jberkhout
 *
 */
public class ThresholdQueue {

    // threshold queue variables
    int k; // threshold value when server starts working again on normalServiceRate
    int K; // threshold value from which server starts working on highServiceRate
    double arrivalRate;
    double lowServiceRate; // normal service rate
    double highServiceRate;
    double costsLow;
    double costsHigh;
    double stopTime;

    // system variables
    LinkedList<Customer> queue;
    Server server;
    int numbCustomersInSystem;

    // RNGs
    ArrivalProcess arrivalProcess;
    MRG32k3a serviceTimeRNG; // random numbers to use when generating service times

    // statistics
    TallyStore totalTimeInSystemTally;
    TallyStore waitTimeTally;
    ListOfStatProbes<TallyStore> listStatsTallies;
    Accumulate utilizationAccumulate;
    Accumulate runningCostsAccumulate;
    Accumulate numbCustInSystemAccumulate;
    ListOfStatProbes<Accumulate> listStatsAccumulate;

    // other
    boolean printSystemState = false; // can be useful for debugging

    public ThresholdQueue(double arrivalRate, double lowServiceRate, double highServiceRate, double costsLow, double costsHigh, double stopTime, int k, int K, MRG32k3a arrivalRNG, MRG32k3a serviceRNG) {

        // set threshold queue variables
    	this.arrivalRate = arrivalRate;
        this.lowServiceRate = lowServiceRate;
        this.highServiceRate = highServiceRate;
        this.costsLow = costsLow;
        this.costsHigh = costsHigh;
        this.stopTime = stopTime;
        this.k = k;
        this.K = K;

        // initialize system parameters
        queue = new LinkedList<>();
        server = new Server();
        numbCustomersInSystem = 0;

        // set RNGs
        arrivalProcess = new ArrivalProcess(arrivalRNG, arrivalRate);
        serviceTimeRNG = serviceRNG;

        // for collecting Tallies and Accumulate
        listStatsAccumulate = new ListOfStatProbes<>("Stats for Accumulate");
        listStatsTallies = new ListOfStatProbes<>("Stats for Tallies");

        // create Tallies and add Tallies to listStatsTallies for later reporting
        waitTimeTally = new TallyStore("Waiting times");
        totalTimeInSystemTally = new TallyStore("Times spend in system");
        listStatsTallies.add(waitTimeTally);
        listStatsTallies.add(totalTimeInSystemTally);

        // create Accumulates and add them to listStatsAccumulate for later reporting
        utilizationAccumulate = new Accumulate("Server utilization");
        runningCostsAccumulate = new Accumulate("Running cost");
        numbCustInSystemAccumulate = new Accumulate("Number of customers in system");
        listStatsAccumulate.add(utilizationAccumulate);
        listStatsAccumulate.add(runningCostsAccumulate);
        listStatsAccumulate.add(numbCustInSystemAccumulate);
    }

    // after initializing ThresholdQueue, this starts the simulation
    // (only to be used once, i.e., it does not reset things)
    public void startSimulation() {

        // initialize simulation
		Sim.init();
		arrivalProcess.scheduleFirstArrival();
		new StopEvent().schedule(stopTime); // schedule stopping time

        // reset stats counters
		listStatsTallies.init();
		listStatsAccumulate.init();

		// start simulation
		Sim.start();
    }

    public StatProbe simulateRunningCosts() {
        startSimulation();
        return runningCostsAccumulate;
    }

    // use random number u to draw from exponential distribution with mu
    // (this way we can use one serviceTimeRNG to sample both low and high
    // service rate times)
    double drawExponentialValue(double u, double mu) {
        return -1 / mu * Math.log(u);
    }

	class ArrivalProcess extends Event {

		ExponentialGen arrivalTimeGen; // generates inter-arrival times
		double arrivalRate;

		public ArrivalProcess(RandomStream RNG, double arrivalRate) {
			this.arrivalRate = arrivalRate;
			arrivalTimeGen = new ExponentialGen(RNG, arrivalRate);
		}

		// event: new customer arrival at the store
		@Override
		public void actions() {

		    // check server regime
            numbCustomersInSystem += 1;
            server.checkAndChangeRegime();

            // generate new arriving customer
            Customer newCustomer = new Customer();

            // check whether new customer goes in service or in queue
            if (server.currentCustomerInService == null) {
                // no customer in service yet
                server.startService(newCustomer);
            } else {
                // customer enters the queue
                queue.add(newCustomer);
            }

            // schedule a new arriving customer
			schedule(arrivalTimeGen.nextDouble());

            // save statistics
            numbCustInSystemAccumulate.update(numbCustomersInSystem);

            if (printSystemState) {printSystemState("new arrival processed");}
		}

		public void scheduleFirstArrival() {
			schedule(arrivalTimeGen.nextDouble());
		}
	}

    class Customer {

        private double arrivalTime;
        private double serviceStartTime;
        private double completionTime;
        private double waitTime;
        private double timeSpendInSystem;

        public Customer() {
            // record arrival time when creating a new customer
            arrivalTime = Sim.time();
        }

        // call when customer starts service
        public void serviceStarted() {
            serviceStartTime = Sim.time();
            waitTime = serviceStartTime - arrivalTime;
            waitTimeTally.add(waitTime);
        }

        // call when service is completed
        public void completed() {
            completionTime = Sim.time();
            timeSpendInSystem = completionTime - serviceStartTime;
            totalTimeInSystemTally.add(timeSpendInSystem);
        }
    }

    class Server extends Event {

        static final double BUSY_CONSTANT = 1.0;
        static final double IDLE_CONSTANT = 0.0;
        boolean inHighRegime; // is true when server at high rate, false else
        Customer currentCustomerInService;

        public Server() {
            currentCustomerInService = null;
            inHighRegime = false;
        }

        // event: service completion
        @Override
        public void actions() {

            // check server regime
            numbCustomersInSystem -= 1;
            server.checkAndChangeRegime();

            // handle completed customer
            currentCustomerInService.completed();
            currentCustomerInService = null;

            // look for a new customer in queue
            if (!queue.isEmpty()) {
                Customer newCustomer = queue.removeFirst();
                server.startService(newCustomer);
            } else {
                // server becomes idle: update costs and statistics
                runningCostsAccumulate.update(0.0);
                utilizationAccumulate.update(IDLE_CONSTANT);
            }

            // save statistics
            numbCustInSystemAccumulate.update(numbCustomersInSystem);

            if (printSystemState) {printSystemState("service completed");}
        }

        public void startService(Customer customer) {

            // process customer
            currentCustomerInService = customer;
            customer.serviceStarted();

            // based on regime: generate service time & update costs
            double serviceTime;
            if (inHighRegime) {
                serviceTime = drawExponentialValue(serviceTimeRNG.nextDouble(), highServiceRate);
                runningCostsAccumulate.update(costsHigh);
            } else {
                serviceTime = drawExponentialValue(serviceTimeRNG.nextDouble(), lowServiceRate);
                runningCostsAccumulate.update(costsLow);
            }

            schedule(serviceTime); // schedule completion time

            // update statistics
            utilizationAccumulate.update(BUSY_CONSTANT);
        }

        // call this when regime needs to be changed
        public void changeRegime(boolean toHigh) {
            if (toHigh) {

                // system regime changes to high
                inHighRegime = true;

                // reschedule current service with faster service time
                // and update with higher operating costs
                reschedule(drawExponentialValue(serviceTimeRNG.nextDouble(), highServiceRate)); // note that the rescheduling is possible in this way due to memoryless property of exponential distributions
                runningCostsAccumulate.update(costsHigh);

            } else {
                // system regime changes to low (this can only happen at
                // customer departure so no need to reschedule service
                // completion because checkAndSetRegime() is called before
                // new service completion is scheduled, also runningCosts
                // do not need to be updated here: this will be taken care of
                // when starting a new customer)
                inHighRegime = false;
            }
        }

        // call this when a new customer arrives or a customer departs to check
        // and update the regime when necessary
        public void checkAndChangeRegime() {
            if (numbCustomersInSystem <= k && server.inHighRegime) {
                server.changeRegime(false);
            }
            if (numbCustomersInSystem >= K && !server.inHighRegime) {
                server.changeRegime(true);
            }
        }
    }

    // stop simulation by scheduling this event
    class StopEvent extends Event {
        @Override
        public void actions() {
            Sim.stop();
        }
    }

    // can be used to print the system state along the simulation
    void printSystemState(String extraInfo) {
        System.out.println("Time = " + Sim.time() + ", number in system = "
                + (this.queue.size() + utilizationAccumulate.getLastValue()) +
                ", high service regime = "
                + server.inHighRegime + ", running costs = "
                + runningCostsAccumulate.getLastValue()
                + ", " + extraInfo);
    }

    /**
     * Main method of the class to run some tests
     *
     * @param args Unused.
     */
    public static void main(String[] args) {

        // threshold queue variables
        int k = 9; // k-threshold for queue
        int K = 20; // K-threshold for queue
        double lambda = 1.5; // arrival rate
        double mu = 2.0; // service rate
        double muHigh = 6.0; // high service rate
        double costsLow = 5.0; // costs per time unit when operating at low rate
        double costsHigh = 10.0; // costs per time unit when operating at high rate
        double simTime = 10000; // simulation endtime (seconds)

        // test 1 (balanced system)
        ThresholdQueue thresholdQueue = new ThresholdQueue(lambda, mu, muHigh, costsLow, costsHigh, simTime, k, K, new MRG32k3a(), new MRG32k3a());
        thresholdQueue.startSimulation();
        System.out.println("Results for a balanced system:");
        System.out.println(thresholdQueue.listStatsAccumulate.report());
        System.out.println(thresholdQueue.listStatsTallies.report());

        // test 2 (overloaded system)
        thresholdQueue = new ThresholdQueue(10, mu, muHigh, costsLow, costsHigh, simTime, k, K, new MRG32k3a(), new MRG32k3a());
        thresholdQueue.startSimulation();
        System.out.println("Results for an overloaded system:");
        System.out.println(thresholdQueue.listStatsAccumulate.report());
        System.out.println(thresholdQueue.listStatsTallies.report());

        // test 3 (quiet system)
        thresholdQueue = new ThresholdQueue(0.01, mu, muHigh, costsLow, costsHigh, simTime, k, k, new MRG32k3a(), new MRG32k3a());
        thresholdQueue.startSimulation();
        System.out.println("Results in case of a quiet system:");
        System.out.println(thresholdQueue.listStatsAccumulate.report());
        System.out.println(thresholdQueue.listStatsTallies.report());
    }
}