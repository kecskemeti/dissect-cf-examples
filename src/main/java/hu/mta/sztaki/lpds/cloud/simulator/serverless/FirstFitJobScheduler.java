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

import java.util.ArrayList;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption.ConsumptionEvent;
import hu.mta.sztaki.lpds.cloud.simulator.io.NetworkNode.NetworkException;
import uk.ac.ljmu.fet.cs.cloud.examples.autoscaler.JobLauncher;

/**
 * Offers a simple JobLauncher implementation based on the first fit principle.
 * If there are no VMs that can accommodate a particular VM at the time, it
 * rejects the job, it is then the caller's responsibility to decide what to do
 * with the job.
 * 
 * @author "Gabor Kecskemeti, Department of Computer Science, Liverpool John
 *         Moores University, (c) 2019"
 */
public class FirstFitJobScheduler implements JobLauncher {
	/**
	 * The virtual infrastructure this launcher will target with its jobs.
	 */
	private final VirtualInfrastructureManager vi;
	/**
	 * Constructs the scheduler and saves the input data so the scheduler will know
	 * where to submit jobs and where to report their statuses.
	 * 
	 * @param vi The infrastructure to use for the execution of the given tasks.
	 * @param pr The object to report the job statuses.
	 */
	public FirstFitJobScheduler(final VirtualInfrastructureManager vi) {
		this.vi = vi;
	}

	/**
	 * The actual job dispatching mechanism. If there is no VM to accommodate the
	 * job, this launcher tells the VI that it needs a VM of the kind which matches
	 * up with the executable of the job.
	 * 
	 * @param j the job to be sent to one of the VM's in the virtual infrastructure
	 * @return <i>true</i> if the job cannot be assigned to any of the
	 *         infrastruture's VMs. <i>false</i> if the job was taken care of and
	 *         there is no further action needed on it.
	 */
	@Override
	public boolean launchAJob(final Job j) {
		try {
			final ArrayList<VirtualMachine> vmset = vi.vmSetPerKind.get(j.executable);
			if (vmset != null) {
				final int vmsetsize = vmset.size();
				for (int i = 0; i < vmsetsize; i++) {
					final VirtualMachine vm = vmset.get(i);
					if (VirtualMachine.State.RUNNING.equals(vm.getState()) && vm.underProcessing.isEmpty() && vm.toBeAdded.isEmpty()) {					
						vm.newComputeTask((j.getExectimeSecs()) * vm.getPerTickProcessingPower(),ResourceConsumption.unlimitedProcessing, (ConsumptionEvent) j);
						j.started();
						return false;
						} else {
							vi.requestVM(j.executable, j);
							}
					}
				} else {
					vi.regNewVMKind(j.executable == null ? "default" : j.executable, j);
					}
			} catch (NetworkException ne) {
			ne.printStackTrace();
			// Not expected
			System.exit(1);
		}
		return true;
	}
}
