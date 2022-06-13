/*
 *  ========================================================================
 *  Helper classes to support simulations of large scale distributed systems
 *  ========================================================================
 *  
 *  This file is part of DistSysJavaHelpers.
 *  
 *    DistSysJavaHelpers is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *   DistSysJavaHelpers is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  (C) Copyright 2022, Gabor Kecskemeti (g.kecskemeti@ljmu.ac.uk)
 *  (C) Copyright 2022, Dilshad H. Sallo (sallo@iit.uni-miskolc.hu)
 *  (C) Copyright 2016, Gabor Kecskemeti (g.kecskemeti@ljmu.ac.uk)
 *  (C) Copyright 2012-2015, Gabor Kecskemeti (kecskemeti.gabor@sztaki.mta.hu)
 */
package hu.mta.sztaki.lpds.cloud.simulator.serverless;

import java.util.HashMap;

import hu.mta.sztaki.lpds.cloud.simulator.Timed;
import hu.mta.sztaki.lpds.cloud.simulator.energy.specialized.IaaSEnergyMeter;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.FileBasedTraceProducerFactory;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.IaaSService;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.pmscheduling.SchedulingDependentMachines;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmscheduling.FirstFitScheduler;
import uk.ac.ljmu.fet.cs.cloud.examples.autoscaler.JobLauncher;
import uk.ac.ljmu.fet.cs.cloud.examples.autoscaler.QueueManager;
import uk.ac.ljmu.fet.cs.cloud.examples.autoscaler.TraceExhaustionCallback;

public class ServerlessDemo implements TraceExhaustionCallback{
	//virtual infrastructure which receive the jobs
	private final VirtualInfrastructureManager vim;
	//cloud service which will accommodate the virtual infrastructure
	private final IaaSService cloud;
	//energy meter
	private final IaaSEnergyMeter energymeter;
	
	//current selected plan, if options it needed here.
	public static HashMap<String,Double> currentInfrast;
	
	//the job handler
	private final JobArrivalHandler jobhandler;

	/**
	 * The utilisation details of the physical machines before any jobs were
	 * dispatched to them.
	 */
	private HashMap<PhysicalMachine, Double> preProcessingRecords;
	
	/**
	 * Callback handler to do finalise the simulation once all jobs have completed.
	 */
	@Override
	public void allJobsFinished() {
		vim.terminateScalingMechanism();
		// There is no need to monitor the IaaS either
		energymeter.stopMeter();
	}
	
	public ServerlessDemo(String traceFileLoc) throws Exception {
		
		cloud = DataCenter.createDataCenter(FirstFitScheduler.class, SchedulingDependentMachines.class);
		vim = new VirtualInfrastructureManager(cloud);
		FaaS.reference(this);
		
		Timed.simulateUntilLastEvent();
		
		energymeter = new IaaSEnergyMeter(cloud);
		currentInfrast = InfrastructureConfiguration.getCurrentInfrConfig();
		vim.startAutoScaling();

		// Simple job dispatching mechanism which first prepares the workload
				JobLauncher launcher = new FirstFitJobScheduler(vim);
				QueueManager qm = new QueueManager(launcher);
				jobhandler = new JobArrivalHandler(FileBasedTraceProducerFactory.getProducerFromFile(traceFileLoc, 0, 5,
						false, currentInfrast.get("Node").intValue() * currentInfrast.get("CPU").intValue(), FaaS.class), launcher, qm);
				jobhandler.processTrace();
				
				// Collecting basic monitoring information
				preProcessingRecords = new HashMap<PhysicalMachine, Double>();
				for (PhysicalMachine pm : cloud.machines) {
					
					preProcessingRecords.put(pm, pm.getTotalProcessed());
				}
				
				// Collects energy related details in every hour
				energymeter.startMeter(3600000, true);
	}
	public void simulateAndprintStatistics() {
		long before = System.currentTimeMillis();
		long beforeSimu = Timed.getFireCount();
		// Now we can start the simulation
		Timed.simulateUntilLastEvent();

		// Simulation is done
		long totalExeution  = System.currentTimeMillis() - before;
		// Let's print out some basic statistics
		System.out.println("Simulation took: " + totalExeution + "ms");
		long simuTimespan = Timed.getFireCount() - beforeSimu;
		System.out.println("Simulated timespan: " + simuTimespan + " simulated ms");

		double totutil = 0;
		for (PhysicalMachine pm : cloud.machines) {			
			totutil += (pm.getTotalProcessed() - preProcessingRecords.get(pm))
					/ (simuTimespan * pm.getPerTickProcessingPower());
		}
		System.out.println("Average utilisation of PMs: " + 100 * totutil / cloud.machines.size() + " %");
		System.out.println("Total power consumption: " + energymeter.getTotalConsumption() / 1000 / 3600000 + " kWh");
		System.out.println("Number of virtual appliances registered at the end of the simulation: "
				+ cloud.repositories.get(0).contents().size());
		System.err.println("Arrival rate(s): " + jobhandler.getArrivalRate());
		jobhandler.inst_count();
		System.err.println("Cold start probabilty: " + jobhandler.getColdStartProbability() * 100 + " %");
		jobhandler.generateAWSTrace();
	}

	public static void main(String[] args) throws Exception {
		
		new ServerlessDemo(args[0]).simulateAndprintStatistics();
	}
}