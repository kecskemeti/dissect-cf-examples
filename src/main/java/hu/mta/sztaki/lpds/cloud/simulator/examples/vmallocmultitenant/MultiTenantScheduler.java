package hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant;

import hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant.ComponentInstance.Request;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.IaaSService;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VMManager.NoSuchVMException;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VMManager.VMManagementException;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.constraints.ConstantConstraints;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmscheduling.Scheduler;
import hu.mta.sztaki.lpds.cloud.simulator.io.NetworkNode.NetworkException;

	/**
	 * 
	 * 
	 * @author Rene Ponto
	 */

public class MultiTenantScheduler extends Scheduler{

	public MultiTenantScheduler(IaaSService parent) {
		super(parent);
	}

	@Override
	protected ConstantConstraints scheduleQueued() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private boolean processRequest(Request request, ComponentType c, boolean crit) {
		
		//check if a component instance of the given type can host this tenant TODO
		boolean exist = false;	
		PhysicalMachine target;
		for(int i = 0; i < parent.machines.size(); i++) {
			if(parent.machines.get(i).isHostableRequest(c.getResources()) && crit == false) {
				target = parent.machines.get(i);
				exist = true;
				break;
			}
		}
		
//		if(exist) {
//			target.terminateVM(vm, true);
//		}
//		else {
//			//create new instance
//			
//			if() {	// any vm can host this comptype?
//				//mapping
//			}
//			else {
//				// create new vm and mapping
//				//sort pms
//				if() {	//can any pm of sorted pms host vm?
//					// take the first one
//					if(pm.isOff) {
//						// switch on
//					}
//					// put vm on this pm
//				}
//				else {
//					return false;
//				}
//			}
//		}
		return true;
		
	}

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
				} catch (NoSuchVMException e) {
					e.printStackTrace();
				} catch (VMManagementException e) {
					e.printStackTrace();
				}
				
				if(host.isHostingVMs() == false) {
					
					//if the host pm is now empty, switch it off
					try {
						host.switchoff(null);
					} catch (VMManagementException e) {
						e.printStackTrace();
					} catch (NetworkException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
	}
	
	/**
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
	 * @param i
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
	 * @param vm
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
