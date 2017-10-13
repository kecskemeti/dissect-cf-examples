package hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant;

import java.util.Set;

import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;
import hu.mta.sztaki.lpds.cloud.simulator.io.VirtualAppliance;

public class VirtualMachine2 extends VirtualMachine {

	public VirtualMachine2(VirtualAppliance va) {
		super(va);
	}
	
	private Set<ComponentInstance> componentInstances;
	
	public Set<ComponentInstance> getInstances() {
		return componentInstances;
	}
	
	public void addInstance(ComponentInstance instance) {
		componentInstances.add(instance);
	}
	
	public void removeInstance(ComponentInstance instance) {
		componentInstances.remove(instance);
	}
	
	public boolean isHostingInstances() {
		return !componentInstances.isEmpty();
	}

}
