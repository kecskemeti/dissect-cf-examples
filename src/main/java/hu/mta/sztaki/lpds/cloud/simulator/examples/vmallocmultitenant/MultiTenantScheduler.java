package hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant.ComponentInstance.Request;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.IaaSService;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine.State;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VMManager.CapacityChangeEvent;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VMManager.VMManagementException;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.pmscheduling.PhysicalMachineController;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmscheduling.Scheduler.QueueingEvent;
import hu.mta.sztaki.lpds.cloud.simulator.io.NetworkNode.NetworkException;
import hu.mta.sztaki.lpds.cloud.simulator.io.Repository;
import hu.mta.sztaki.lpds.cloud.simulator.io.VirtualAppliance;

	/**
	 * A Scheduler supporting sgx. 
	 * 
	 * The algorithms in this class are taken out of the paper "Optimized Cloud 
	 * Deployment of Multi-tenant Software Considering Data Protection Concerns" 
	 * by Zoltan Adam Mann and Andreas Metzger, published in CCGrid 2017.
	 * 
	 * TODO for improvement:
	 * - Implement logic for scheduling
	 * - fill mapping while scheduling
	 * - use ProcessRequest and TerminateRequest while scheduling
	 * 
	 * @author Rene Ponto
	 */

public class MultiTenantScheduler extends PhysicalMachineController implements Helpers {
	
	/** Counter for creating virtual appliances*/
	private int vaCounter = 1;
	
	/** Contains the existing component types*/
	private List<ComponentType> types;
	
	/** Contains the mapping of VMs to their hosted component instances*/
	private HashMap<VirtualMachine, ArrayList<ComponentInstance>> mapping;		

	
	public MultiTenantScheduler(IaaSService parent) {
		super(parent);
		types = new ArrayList<ComponentType>();
	}
	
	@Override
	protected CapacityChangeEvent<PhysicalMachine> getHostRegEvent() {
		return null;
	}

	@Override
	protected QueueingEvent getQueueingEvent() {
		return null;
	}

	/**
	 * Method to handle a new Request. The focus is on reusing existing instances instead of
	 * creating new ones, but if that is not possible, new instances are deployed.
	 * 
	 * @param request
	 * 					The arriving Request.
	 * @param c
	 * 					The requested ComponentType.
	 * @param crit
	 * 					Determines if a secure pm is needed.
	 * @return true if the Request is handled correctly, false if there not enough capacities or
	 * similar problems.
	 */
	private boolean processRequest(Request request, ComponentType c, boolean crit) {
		
		//add the ComponentType to a list to work with them later
		if(!types.contains(c)) {
			types.add(c);
		}
		
		//check if an existing component instance of the given type can host this tenant
		ComponentInstance hostInstance = null;
		for(ComponentInstance instance : c.getInstances()) {
			if(isInstanceAbleToHostTenant(instance, request, crit)) {
				hostInstance = instance;
				break;
			}
		}
		if(hostInstance != null) {
			hostInstance.addRequest(request);
		}
		else {
			hostInstance = c.createInstance(crit);
			hostInstance.addRequest(request);
			
			//get all existing VMs
			List<VirtualMachine> vmList = new ArrayList<VirtualMachine>();
			vmList.addAll(mapping.keySet());
			
			//check if an existing VM can host the hostInstance
			VirtualMachine hostVm = null;
			for(VirtualMachine vm : vmList) {
				if(isVmAbleToHostInstance(vm, hostInstance, mapping)) {
					hostVm = vm;
					break;
				}
			}			
			if(hostVm != null) {
				hostInstance.setVm(hostVm);
			}
			else {
				
				//get a fitting repository
				Repository target = parent.repositories.get(0);				
				try {
					VirtualMachine[] vm = parent.requestVM(new VirtualAppliance(Integer.toString(vaCounter), 0, 0), 
							hostInstance.getResources(), target, 1);
					hostVm = vm[0];
				} catch (VMManagementException | NetworkException e1) {
					e1.printStackTrace();
				} 
				hostInstance.setVm(hostVm);
				
				// sort PMs
				parent.runningMachines.sort(nonsecureToSecure);		// no need to sort from running to off
				
				//check if an existing pm can host the hostVm
				PhysicalMachine hostPm = null;
				for(PhysicalMachine pm : parent.runningMachines) {
					if(isPmAbleToHostVm(pm, hostVm, mapping)) {
						hostPm = pm;
						if(pm.getState().equals(State.OFF))
							pm.turnon();						
						break;
					}
				}
				if(hostPm != null) {
					try {
						hostPm.deployVM(hostVm, hostPm.allocateResources(hostVm.getResourceAllocation().allocated, false, 
								PhysicalMachine.migrationAllocLen), parent.machines.get(0).localDisk);
					} catch (VMManagementException | NetworkException e) {
						e.printStackTrace();
					}
				}
				else
					return false;				
			}
		}		
		return true;
	}

	/**
	 * A PM comparator that orders PM from running to off. 
	 * 
	 * Should be finished.
	 */
	public static final Comparator<PhysicalMachine> runningToOffState = new Comparator<PhysicalMachine>() {
		@Override
		public int compare(PhysicalMachine o1, PhysicalMachine o2) {
			if(o1.isRunning()) {		
				return -o2.getState().compareTo(o1.getState());
			}
			else {
				return -o1.getState().compareTo(o2.getState());
			}			
		}
	};
	
	/**
	 * A PM comparator that orders PM from nonsecure to secure. 
	 * 
	 * Should be finished.
	 */
	public static final Comparator<PhysicalMachine> nonsecureToSecure = new Comparator<PhysicalMachine>() {
		@Override
		public int compare(PhysicalMachine o1, PhysicalMachine o2) {
			if(o1.isSecure()) {		
				if(o2.isSecure())
					return 0;
				else
					return -1;
			}
			else {
				if(o2.isSecure())
					return 1;
				else
					return 0;
			}			
		}
	};

	/**
	 * Terminates a given Request and removes it from the ComponentInstance. Further it is implemented with
	 * the intention to save energy, so it is checked if the instance has no other requests running in order to remove
	 * it. This is also done with the hosting VM and the host PM, which are shut down, too, if there are no tasks left
	 * for them.
	 * 
	 * @param r
	 * 			The given Request which is going to be terminated.
	 * @param c
	 * 			The hosting ComponentInstance of the Request.
	 */
	private void terminateRequest(Request r, ComponentInstance c) {
		
		//remove request		
		c.removeRequest(r);
		if(c.getRequests().isEmpty()) {
			
			//remove the instance which belongs to the request
			c.getType().removeInstance(c);
			
			//remove the VM if there are no instances running and because of that no
			//resources used
			if(c.getVm().getResourceAllocation() == null) {	
				PhysicalMachine host = c.getVm().getResourceAllocation().getHost();
				try {
					host.terminateVM(c.getVm(), true);
				} catch (VMManagementException e1) {
					e1.printStackTrace();
				}
				
				//if the host PM is now empty, switch it off
				if(!host.isHostingVMs()) {
					try {
						host.switchoff(null);
					} catch (VMManagementException | NetworkException e) {
						e.printStackTrace();
					}
				}
			}			
		}
	}
	
}
