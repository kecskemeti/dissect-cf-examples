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

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.serverless.workload.generator.FaaSJob;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption.ConsumptionEvent;
import uk.ac.ljmu.fet.cs.cloud.examples.autoscaler.TraceExhaustionCallback;

public class FaaS extends FaaSJob implements ConsumptionEvent{
	/**
	 * The number of jobs that have reached the infrastructure's VMs
	 */
	private static int jobsDispatched = 0;
	/**
	 * The number of jobs that have actually completed their tasks
	 */
	private static int jobsDone = 0;
	/**
	 * The total number of jobs this simulation has
	 */
	private static int jobCount = 0;
	/**
	 * Callback to terminate simulation
	 */
	private static TraceExhaustionCallback callback;

	public FaaS(String id, long submit, long queue, long exec, int nprocs, double ppCpu, long ppMem, String user,
			String group, String executable, Job preceding, long delayAfter) {
		super(id, submit, queue, exec, nprocs, ppCpu, ppMem, user, group, executable, preceding, delayAfter);
		jobCount++;
	}

	public void started() {
		this.registerDispatch();
	}
	@Override
	public void conComplete() {
		
		this.registerCompletion();
	}

	@Override
	public void conCancelled(ResourceConsumption problematic) {
		this.registerCompletion();
	}
	/**
	 * Remembers how many jobs were dispatched to their corresponding VMs. If all
	 * jobs have been dispatched, a message is printed on the screen.
	 */
	public void registerDispatch() {
		jobsDispatched++;
		if (jobsDispatched == jobCount) {
			System.out.println("Last job reached a VM");
		}
	}

	/**
	 * Remembers how many jobs finished their execution on their VMs. If no more
	 * jobs are coming, it will send the completion notification to the callback
	 * object.
	 */
	public void registerCompletion() {
		
		jobsDone++;
		// Finally let's see if there is any more need for the support mechanisms of the
		// simulation (e.g., autoscaler)
		if (jobsDone == jobCount) {
			callback.allJobsFinished();
		}
	}
	/**
	 * Get reference to terminate;
	 * @param call
	 */
	public static void reference(TraceExhaustionCallback call) {
		callback = call;
	}
}
