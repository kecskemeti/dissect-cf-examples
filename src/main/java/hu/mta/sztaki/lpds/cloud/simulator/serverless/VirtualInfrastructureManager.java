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


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

import hu.mta.sztaki.lpds.cloud.simulator.Timed;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.IaaSService;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VMManager.VMManagementException;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine.State;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.constraints.ConstantConstraints;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.constraints.ResourceConstraints;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption;
import hu.mta.sztaki.lpds.cloud.simulator.io.Repository;
import hu.mta.sztaki.lpds.cloud.simulator.io.VirtualAppliance;



public class VirtualInfrastructureManager extends Timed implements VirtualMachine.StateChange{
	/**
	 * The virtual infrastructure for each executable. The keys of the map are the
	 * executable types for which we have a virtual infrastructure. The values of
	 * the map are the actual VMs that we have at hand for a given executable.
	 */
	public final HashMap<String, ArrayList<VirtualMachine>> vmSetPerKind = new HashMap<String, ArrayList<VirtualMachine>>();
	/**
	 * If a kind of VM is under preparation already, we remember it here so we can
	 * tell which VM kind should not have more VMs instantiated/destructued for the
	 * time being.
	 */
	public final HashMap<String, VirtualMachine> underPrepVMPerKind = new HashMap<String, VirtualMachine>();
	/**
	 * The cloud on which we will execute our virtual machines
	 */
	protected final IaaSService cloud;
	
	/**
	 * The cloud's VMI storage where we register the images of our executables
	 * allowing the instantiation of specific VMs that host the particular
	 * executable.
	 */
	protected final Repository storage;
	/**
	 * The number of cores the first PM (of the cloud) has. This is used to
	 * determine the maximum size of the VMs we will request.
	 */
	private final int pmCores;
	/**
	 * The processing capability of the first PM in the cloud.
	 */
	private final double pmProcessing;
	/**
	 * The amount of memory the first PM (of the cloud) has. This is used to
	 * determine the maximum size of the VMs we will request.
	 */
	private final long pmMem;

	/**
	 * Allows us to remember which VM kinds fell out of use
	 */
	private final ArrayDeque<String> obsoleteVAs = new ArrayDeque<String>();
	
	/**
	 * Initialises the auto scaling mechanism
	 * 
	 * @param cloud the physical infrastructure to use to rent the VMs from
	 */
	public VirtualInfrastructureManager(final IaaSService cloud) {
		this.cloud = cloud;
		storage = cloud.repositories.get(0);
		ResourceConstraints rcForMachine = cloud.machines.get(0).getCapacities();
		pmCores = (int) rcForMachine.getRequiredCPUs();
		pmProcessing = rcForMachine.getRequiredProcessingPower();
		pmMem = rcForMachine.getRequiredMemory();
	}
	/**
	 * If a new executable is encountered for which we might need a set of VMs, we
	 * can communicate this here.
	 * 
	 * @param kind the type of the executable that we need the VMs for
	 */
	public void regNewVMKind(final String kind, Job currJob) {
		if (vmSetPerKind.get(kind) == null) {
			vmSetPerKind.put(kind, new ArrayList<VirtualMachine>());
			requestVM(kind, currJob);
		}
	}

	/**
	 * Arranges a new VM request to be sent to the cloud. If needed it registers the
	 * corresponding VMI with the cloud. It also prepares all internal data
	 * structures used for monitoring the VM.
	 * 
	 * <b>Warning:</b> The VM's resource requirements are not determined
	 * realistically. If the biggest PM in the infrastructure is having less than 4
	 * CPU cores, then this could lead to VMs that cannot be run on the cloud.
	 * 
	 * @param vmKind The executable for which we want a new VM for.
	 */
	protected void requestVM(final String vmKind, Job currJob) {
		if (underPrepVMPerKind.containsKey(vmKind)) { 
			return;
		}
		VirtualAppliance va = (VirtualAppliance) storage.lookup(vmKind);
		if (va == null) {
			// A random sized VMI with a short boot procedure. The approximate size of the
			// VMI will be 1GB.
			va = new VirtualAppliance(vmKind, 10, 0, false, 1024 * 1024 * 1024);
			boolean regFail;
			do {
				regFail = !storage.registerObject(va);
				if (regFail) {
					// Storage ran out of space, remove the VA that became obsolete the longest time
					// in the past
					if (obsoleteVAs.isEmpty()) {
						throw new RuntimeException(
								"Configured with a repository not big enough to accomodate a the following VA: " + va);
					}
					String vaRemove = obsoleteVAs.pollFirst();
					storage.deregisterObject(vaRemove);
				}
			} while (regFail);
		}
		try {
			VirtualMachine vm = cloud.requestVM(va,
					new ConstantConstraints(currJob.nprocs, pmProcessing, currJob.nprocs * pmMem / pmCores), storage, 1)[0]; // memory must based on timeout of task like AWS

			currJob.is_cold = true;
			ArrayList<VirtualMachine> vmset = vmSetPerKind.get(vmKind);
			
			if (vmset.isEmpty()) {
				// VA became used again, no longer obsolete
				obsoleteVAs.remove(vmKind);
			}

			vmset.add(vm);
			underPrepVMPerKind.put(vmKind, vm);
			vm.subscribeStateChange(this);
		} catch (Exception vmm) {
			vmm.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Makes sure that the given VM is no longer having any local monitoring
	 * mechanism running and also that it is no longer present at the cloud.
	 * 
	 * @param vm The VM to be destroyed.
	 */
	protected void destroyVM(final VirtualMachine vm) {
	//	vmmonitors.remove(vm).finishMon();
		try {
			// Add memory size of this VM
			final String vmKind = vm.getVa().id;
			ArrayList<VirtualMachine> vms = vmSetPerKind.get(vmKind);
			
			vms.remove(vm);
			underPrepVMPerKind.remove(vmKind);
			if (VirtualMachine.State.DESTROYED.equals(vm.getState())) {
				// The VM was not even running when the decision about its destruction was made
				cloud.terminateVM(vm, true);
			} else {
				// The VM was initiated on the cloud, but we no longer need it
				vm.destroy(true);
			}
			if (vms.isEmpty()) {
				// Last use of the VA, make it obsolete now => enable it to be removed from the
				// central storage
				obsoleteVAs.add(vmKind);
			}
		} catch (VMManagementException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Asks Timed to notify us every two minutes. The auto scaling mechanism will be
	 * running with this frequency.
	 */
	public void startAutoScaling() {
		subscribe(1);
	}

	/**
	 * If there are no further tasks for the virtual infrastructure, it is
	 * completely dismantled and the two minute notifications from Timed are
	 * cancelled.
	 */
	public void terminateScalingMechanism() {
		for (ArrayList<VirtualMachine> vmset : vmSetPerKind.values()) {
			while (!vmset.isEmpty()) {
				destroyVM(vmset.get(vmset.size() - 1));
			}
		}
		unsubscribe();
		System.out.println("Autoscaling mechanism terminated.");
	}

	/**
	 * If a VM starts to run, we should enable further VMs to start or destroy
	 */
	@Override
	public void stateChanged(final VirtualMachine vm, final State oldState, final State newState) {

		if (VirtualMachine.State.RUNNING.equals(newState)) {
			underPrepVMPerKind.remove(vm.getVa().id);
			vm.unsubscribeStateChange(this);
		} else if (VirtualMachine.State.NONSERVABLE.equals(newState)) {
			underPrepVMPerKind.remove(vm.getVa().id);
			vm.unsubscribeStateChange(this);
		}
	}
	@Override
	public void tick(long fires) {
		this.unsubscribe();
	}
}
