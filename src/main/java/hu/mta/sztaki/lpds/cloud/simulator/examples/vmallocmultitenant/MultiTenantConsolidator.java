package hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import hu.mta.sztaki.lpds.cloud.simulator.iaas.IaaSService;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine.State;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VMManager.VMManagementException;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.consolidation.Consolidator;
import hu.mta.sztaki.lpds.cloud.simulator.io.NetworkNode.NetworkException;

	/**
	 * A consolidator which supports secure enclaves on physical machines.
	 * 
	 * The algorithms in this class are taken out of the paper "Optimized Cloud 
	 * Deployment of Multi-tenant Software Considering Data Protection Concerns" 
	 * by Zoltan Adam Mann and Andreas Metzger, published in CCGrid 2017.
	 * 
	 * TODO for improvement:
	 * - find a way to make the implementation'isPmAbleToHostVm' unique
	 * - write test class
	 * 
	 * @author Rene Ponto
	 */

public class MultiTenantConsolidator extends Consolidator {
	
	HashMap<VirtualMachine, ArrayList<ComponentInstance>> mapping;

	public MultiTenantConsolidator(IaaSService toConsolidate, long consFreq, HashMap<VirtualMachine, ArrayList<ComponentInstance>> mapping) {
		super(toConsolidate, consFreq);
		this.mapping = mapping;
	}

	@Override
	protected void doConsolidation(PhysicalMachine[] pmList) {
		try {
			reoptimize(pmList);
		} catch (VMManagementException e) {
			e.printStackTrace();
		} catch (NetworkException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("static-access")
	private void reoptimize(PhysicalMachine[] pms) throws VMManagementException, NetworkException {
		
		// first, check if the number of active PMs can be minimized
		for(PhysicalMachine actualPm : pms) {
			// collect the migrations and only commit them, if every VM on this PM can be migrated
			Map<VirtualMachine, PhysicalMachine> migrations = new HashMap<VirtualMachine, PhysicalMachine>();
			
			for(VirtualMachine vm : actualPm.publicVms) {
				// make sure the actual VM could be migrated to another PM
				PhysicalMachine target = null;
				for(PhysicalMachine pm : pms) {
					if(isPmAbleToHostVm(pm, vm, mapping)) {
						pm.allocateResources(vm.getResourceAllocation().allocated, true, pm.migrationAllocLen);
						target = pm;
						break;
					}	
				}
				// mark this VM as migrateable and move on to the next one
				if(target != null)
					migrations.put(vm, target);
			}
			
			if(actualPm.publicVms.size() == migrations.size()) {
				// commit the tentative migrations only if every VM can be migrated
				for(VirtualMachine vm : migrations.keySet()) {
					actualPm.migrateVM(vm, migrations.get(vm));
				}
				
				// switch this PM off to save energy
				actualPm.switchoff(null);
			}
			else {
				migrations.clear();
			}
		}
		
		// check if a secure PM can take load from two (non-secure) PMs		
		boolean changed = true;
		while(changed) {
			changed = false;
			PhysicalMachine securePm = null;
			// take one secure PM which is actually not running
			for(PhysicalMachine pm : pms) {
				// take the first PM which fits the criteria
				if(pm.isSecure() && pm.getState().equals(State.OFF)) {
					securePm = pm;
					break;
				}
			}
			if(securePm != null) {
				PhysicalMachine chosenPm1 = null;
				PhysicalMachine chosenPm2 = null;
				
				// make sure the chosen PMs are non-secure
				for(PhysicalMachine pm1 : pms) {
					if(pm1.isSecure()) {
						continue;
					}
					for(PhysicalMachine pm2 : pms) {
						if(pm2.isSecure()) {
							continue;
						}
						if(pm1 == pm2) {
							continue;
						}						
						// compare the resources if there are two different non-secure PMs
						double occupiedTotalProcessingPowerPm1 = pm1.getCapacities().getTotalProcessingPower() - 
								pm1.availableCapacities.getTotalProcessingPower() - pm1.freeCapacities.getTotalProcessingPower();
						long occupiedMemoryPm1 = pm1.getCapacities().getRequiredMemory() - pm1.availableCapacities.getRequiredMemory() 
								- pm1.freeCapacities.getRequiredMemory();
						double occupiedTotalProcessingPowerPm2 = pm2.getCapacities().getTotalProcessingPower() - 
								pm2.availableCapacities.getTotalProcessingPower() - pm2.freeCapacities.getTotalProcessingPower();
						long occupiedMemoryPm2 = pm2.getCapacities().getRequiredMemory() - pm2.availableCapacities.getRequiredMemory() 
								- pm2.freeCapacities.getRequiredMemory();
						
						// if the securePM can take the load of the two PMs, then set both chosen PMs
						if(occupiedTotalProcessingPowerPm1 + occupiedTotalProcessingPowerPm2 <= 
								securePm.getCapacities().getTotalProcessingPower() && occupiedMemoryPm1 + occupiedMemoryPm2 <= 
								securePm.getCapacities().getRequiredMemory()) {
							chosenPm1 = pm1;
							chosenPm2 = pm2;
						}
					}
				}
				
				if(chosenPm1 != null && chosenPm2 != null) {
					
					Logger.getGlobal().info("Secure PM " + securePm.hashCode() + " takes load from PMs " + chosenPm1.hashCode() + 
							" and " + chosenPm2.hashCode());
					
					//start the secure PM and migrate all VMs of chosenPM1 and chosenPM2 to the secure one
					securePm.turnon();
					for(VirtualMachine vm : chosenPm1.listVMs()) {
						chosenPm1.migrateVM(vm, securePm);
					}
					for(VirtualMachine vm : chosenPm2.listVMs()) {
						chosenPm2.migrateVM(vm, securePm);
					}
					
					chosenPm1.switchoff(null);
					chosenPm2.switchoff(null);
					changed = true;
				}
				
			}
		}
		
		// check if the load of a secure PM can be moved to a non-secure PM
		boolean changed2 = true;
		while(changed2) {
			changed2 = false;
			PhysicalMachine securePM = null;
			PhysicalMachine unsecurePM = null;
			
			// get a secure PM which is actually running (which means, it hosts VMs)
			for(PhysicalMachine pm : pms) {
				if(pm.isSecure() && pm.isRunning()) {
					securePM = pm;
					break;
				}
			}
			
			// get a non-secure PM which is actually not running
			for(PhysicalMachine pm : pms) {				
				if(!pm.isSecure() && !pm.isRunning()) {
					unsecurePM = pm;
					break;
				}
			}
			
			// check if there are critical instances on the VMs, which means, they cannot run on a non-secure PM			
			boolean hostsCriticals = false;			
			for(VirtualMachine vm: securePM.publicVms) {
				
				if(hostsCriticals)
					break;
				
				List<ComponentInstance> allInstancesOnVm = new ArrayList<ComponentInstance>();
				allInstancesOnVm.addAll(mapping.get(vm));
					
				//check if there are critical instances on this VM					
				for(ComponentInstance instance : allInstancesOnVm) {
					if(!instance.getType().getProvidedBy().equals("Provider") || instance.getTenants().size() > 1)
						hostsCriticals = true;
				}
								
			}
			
			// if there is no possible injury of the safety, migrate all VMs from the secure PM to the non-secure one
			if(!hostsCriticals) {
				unsecurePM.turnon();
				securePM.switchoff(unsecurePM);
				changed2 = true;
			}			
		}		
	}
	
	/**
	 * Should be finished.
	 * @param pm
	 * 			The PhysicalMachine which is going to be checked.
	 * @param vm
	 * 			The VirtualMachine which shall be hosted.
	 * @param mapping
	 * 			The actual mapping of VMs to ComponentInstances.
	 * @return
	 */
	private boolean isPmAbleToHostVm(PhysicalMachine pm, VirtualMachine vm, HashMap<VirtualMachine, ArrayList<ComponentInstance>> mapping) {
		
		//ensures that the aggregate size of the VMs remains below the capacity of the PM
		if(!(pm.availableCapacities.getTotalProcessingPower() + vm.getResourceAllocation().allocated.getTotalProcessingPower() 
				<= pm.getCapacities().getTotalProcessingPower()) && !(pm.availableCapacities.getRequiredMemory() + 
				vm.getResourceAllocation().allocated.getRequiredMemory() <= pm.getCapacities().getRequiredMemory())) {
			return false;
		}
		
		//it is checked whether there is a component instance in the VM and another in the PM 
		//or vice versa that would violate the data protection constraint		
		List<ComponentInstance> allInstancesOnVm = new ArrayList<ComponentInstance>();
		allInstancesOnVm.addAll(mapping.get(vm));
		
		//check if there are critical instances on this VM
		boolean hostsCriticals = false;
		for(ComponentInstance instance : allInstancesOnVm) {
			if(!instance.getType().getProvidedBy().equals("Provider") || instance.getTenants().size() > 1)
				hostsCriticals = true;
		}		
		
		//check if the PM supports secure enclaves, so there can be critical instances of different hosts be hosted
		if(hostsCriticals) {
			if(!pm.isSecure())
				return false;
			
			boolean cannotHost = false;
			for(ComponentInstance instance : allInstancesOnVm) {
				if(!instance.getType().isSgxSupported())
					cannotHost = true;
			}			
			return !cannotHost;	
		}
		else {
			return true;
		}
	}
	
}
