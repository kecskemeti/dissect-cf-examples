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
import hu.mta.sztaki.lpds.cloud.simulator.iaas.constraints.ConstantConstraints;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.pmscheduling.PhysicalMachineController;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmscheduling.Scheduler;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmscheduling.Scheduler.QueueingEvent;
import hu.mta.sztaki.lpds.cloud.simulator.io.NetworkNode.NetworkException;
import hu.mta.sztaki.lpds.cloud.simulator.io.VirtualAppliance;

	/**
	 * 
	 * 
	 * @author Rene Ponto
	 */

public class MultiTenantScheduler extends PhysicalMachineController {
	
	private int vaCounter = 1;
	private List<ComponentType> types;
	
	private HashMap<ComponentInstance, VirtualMachine> mapping;		//TODO fill map while schedule is running

	public MultiTenantScheduler(IaaSService parent) {
		super(parent);
		types = new ArrayList<ComponentType>();
	}
	
	@Override
	protected CapacityChangeEvent<PhysicalMachine> getHostRegEvent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected QueueingEvent getQueueingEvent() {
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
			for(PhysicalMachine pm : parent.runningMachines) {
				for(VirtualMachine vm : pm.listVMs()) {
					vmList.add(vm);
				}
			}
			
			//check if an existing VM can host the hostInstance
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
				
				// sort PMs
//				parent.machines.sort(runningToOffState);
				parent.runningMachines.sort(nonsecureToSecure);
				
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
				
				//remove the VM if there are no instances running
				PhysicalMachine host = c.getVm().getResourceAllocation().getHost();
				try {
					host.terminateVM(c.getVm(), true);
				} catch (VMManagementException e1) {
					e1.printStackTrace();
				}
				
				
				if(!host.isHostingVMs()) {
					
					//if the host PM is now empty, switch it off
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
	 * Should be finished.
	 * @param vm
	 * 			The VirtualMachine which is going to be checked.
	 * @param i
	 * 			The ComponentInstance which shall be hosted.
	 * @return
	 */
	private boolean isVmAbleToHostInstance(VirtualMachine vm, ComponentInstance i) {
		
		//At first check the load of the PM which hosts the given VM. If there is not
		//enough capacity to host the given ComponentInstance, return false.
		PhysicalMachine host = vm.getResourceAllocation().getHost();		
		if(!(host.availableCapacities.getTotalProcessingPower() + i.getResources().getTotalProcessingPower() 
				<= host.getCapacities().getTotalProcessingPower()) && !(host.availableCapacities.getRequiredMemory() + 
				i.getResources().getRequiredMemory() <= host.getCapacities().getRequiredMemory())) {
			return false;
		}
		
		List<ComponentInstance> allInstancesOnVm = new ArrayList<ComponentInstance>();
		//if the instance is critical, there must not be a custom instance
		if(i.isCritical()) {			
			for(ComponentType type : types) {
				for(ComponentInstance instance : type.getInstances()) {
					if(instance.getVm() == vm) {
						allInstancesOnVm.add(instance);
					}
				}
			}
			
			for(ComponentInstance instance : allInstancesOnVm) {
				if(!instance.getType().getProvidedBy().equals("Provider") || instance.getTenants().size() > 1)
					return false;
			}
		}
		//if there are critical instances on the VM, this one must not be custom
		else {
			boolean critOnVm = false;
			String tenant = "";
			for(ComponentInstance instance : allInstancesOnVm) {
				if(instance.isCritical()) {
					critOnVm = true;
					tenant = instance.getTenants().get(0);
					break;
				}
			}
			if(critOnVm) {
				if(!i.getType().getProvidedBy().equals("Provider") && i.getTenants().get(0) != tenant) {
					return false;
				}
			}
		}
		
		return true;
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
		
		//ensures that the aggregate size of the VMs remains below the capacity of the PM
		if(!(pm.availableCapacities.getTotalProcessingPower() + vm.getResourceAllocation().allocated.getTotalProcessingPower() 
				<= pm.getCapacities().getTotalProcessingPower()) && !(pm.availableCapacities.getRequiredMemory() + 
				vm.getResourceAllocation().allocated.getRequiredMemory() <= pm.getCapacities().getRequiredMemory())) {
			return false;
		}
		
//		it is checked whether there is a component instance in the VM and another in the PM 
//		or vice versa that would violate the data protection constraint
		
//		if(//TODO) {
//			return false;
//		}
//		else {
//			return true;
//		}
		return false;
	}

	
	
}
