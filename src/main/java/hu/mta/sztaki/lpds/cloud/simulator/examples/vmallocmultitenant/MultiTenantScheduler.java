package hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant;

import java.util.ArrayList;
import java.util.List;

import hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant.ComponentInstance.Request;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.IaaSService;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine.State;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VMManager.VMManagementException;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.constraints.ConstantConstraints;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmscheduling.Scheduler;
import hu.mta.sztaki.lpds.cloud.simulator.io.NetworkNode.NetworkException;
import hu.mta.sztaki.lpds.cloud.simulator.io.VirtualAppliance;

	/**
	 * 
	 * 
	 * @author Rene Ponto
	 */

public class MultiTenantScheduler extends Scheduler {
	
	private int vaCounter = 1;

	public MultiTenantScheduler(IaaSService parent) {
		super(parent);
	}

	@Override
	protected ConstantConstraints scheduleQueued() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Method to handle a new Request. The focus is on reusing existing instances instead of
	 * creating new ones, but if that is not possible, new instances are deployed.
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
			
			//get all existing vms
			List<VirtualMachine> vmList = new ArrayList<VirtualMachine>();
			for(PhysicalMachine pm : parent.runningMachines) {
				for(VirtualMachine vm : pm.listVMs()) {
					vmList.add(vm);
				}
			}
			
			//check if an existing vm can host the hostInstance
			VirtualMachine hostVm = null;
			for(VirtualMachine vm : vmList) {
				if(isVmAbleToHostInstance(vm, hostInstance)) {
					hostVm = vm;
					break;
				}
			}
			
			if(hostVm != null) {
				hostInstance.setVm(hostVm);
			}
			else {
				try {
					VirtualMachine[] vm = parent.requestVM(new VirtualAppliance(Integer.toString(vaCounter), 0, 0), 
							hostInstance.getResources(), parent.repositories.get(0), 1);	//TODO is this correct?
					hostVm = vm[0];
				} catch (VMManagementException | NetworkException e1) {
					e1.printStackTrace();
				} 
				hostInstance.setVm(hostVm);
				
				// sort PMs ?? TODO
				
				//check if an existing pm can host the hostVm
				PhysicalMachine hostPm = null;
				for(PhysicalMachine pm : parent.runningMachines) {
					if(isPmAbleToHostVm(pm, hostVm)) {
						hostPm = pm;
						if(pm.getState().equals(State.OFF))
							pm.turnon();
						
						break;
					}
				}
				if(hostPm != null) {
					try {
						hostPm.deployVM(hostVm, null, parent.repositories.get(0));		// TODO null, get(0)
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
	 * Terminates a given Request and removes it from the ComponentInstance. Further it is implemented with
	 * the intention to save energy, so it is checked if the instance has no other requests running in order to remove
	 * it. This is also done with the hosting vm and the host pm, which are shut down, too, if there are no tasks left
	 * for them.
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
			
			if(c.getVm().getResourceAllocation() == null) {	
				
				//remove the vm if there are no instances running
				PhysicalMachine host = c.getVm().getResourceAllocation().getHost();
				try {
					host.terminateVM(c.getVm(), true);
				} catch (VMManagementException e1) {
					e1.printStackTrace();
				}
				
				
				if(host.isHostingVMs() == false) {
					
					//if the host pm is now empty, switch it off
					try {
						host.switchoff(null);
					} catch (VMManagementException | NetworkException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
	}
	
	/**
	 * Checks if the given ComponentInstance is able to serve a given tenant. In order to determine that,
	 * the criticality is checked and the capacity of the PhysicalMachine which hosts the VirtualMachine with
	 * the ComponentInstance.
	 * 
	 * Should be finished.
	 * @param i
	 * 			The ComponentInstance which shall serve the tenant.
	 * @param r
	 * 			The Request of the tenant.
	 * @param crit
	 * 			Is the hosted data critical?
	 * @return true if there are enough resources on the hostPm to host the additional resources of the
	 * 		   ComponentInstance on its bounded Vm and both the instance and the to hosted data are not critical.
	 */
	private boolean isInstanceAbleToHostTenant(ComponentInstance i, Request r, boolean crit) {
		
		PhysicalMachine host = i.getVm().getResourceAllocation().getHost();		
		if(!(host.availableCapacities.getTotalProcessingPower() + r.getResources().getTotalProcessingPower() 
				<= host.getCapacities().getTotalProcessingPower()) && !(host.availableCapacities.getRequiredMemory() + 
				r.getResources().getRequiredMemory() <= host.getCapacities().getRequiredMemory())) {
			return false;
		}
		else {
			if(i.mayBeUsedBy(r.getTenant(), crit)) {
				return true;
			}
			
			return false;
		}
	}
	
	/**
	 * Not yet finished.
	 * @param vm
	 * 			The VirtualMachine which is going to be checked.
	 * @param i
	 * 			The ComponentInstance which shall be hosted.
	 * @return
	 */
	private boolean isVmAbleToHostInstance(VirtualMachine vm, ComponentInstance i) {
		
		PhysicalMachine host = vm.getResourceAllocation().getHost();		
		if(!(host.availableCapacities.getTotalProcessingPower() + i.getResources().getTotalProcessingPower() 
				<= host.getCapacities().getTotalProcessingPower()) && !(host.availableCapacities.getRequiredMemory() + 
				i.getResources().getRequiredMemory() <= host.getCapacities().getRequiredMemory())) {
			return false;
		}
		if(i.isCritical()) {
//			if(//TODO: Custom Components?) {
				return false;
//			}			
		}
		else {
//			if(//TODO: same) {
//				return false;
//			}
			
			return true;
		}
	}
	
	/**
	 * Not yet finished.
	 * @param pm
	 * 			The PhysicalMachine which is going to be checked.
	 * @param vm
	 * 			The VirtualMachine which shall be hosted.
	 * @return
	 */
	private boolean isPmAbleToHostVm(PhysicalMachine pm, VirtualMachine vm) {
		
		if(!(pm.availableCapacities.getTotalProcessingPower() + vm.getResourceAllocation().allocated.getTotalProcessingPower() 
				<= pm.getCapacities().getTotalProcessingPower()) && !(pm.availableCapacities.getRequiredMemory() + 
				vm.getResourceAllocation().allocated.getRequiredMemory() <= pm.getCapacities().getRequiredMemory())) {
			return false;
		}
//		if(//TODO) {
//			return false;
//		}
//		else {
//			return true;
//		}
		return false;
	}
	
}
