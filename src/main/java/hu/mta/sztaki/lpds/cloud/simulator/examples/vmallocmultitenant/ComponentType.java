package hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant;

import java.util.Set;
import java.util.logging.Logger;

import hu.mta.sztaki.lpds.cloud.simulator.iaas.constraints.AlterableResourceConstraints;

	/**
	 * This class defines the type of a component, so there can be instances created of this type.
	 * 
	 * @author Rene Ponto
	 */

public class ComponentType {
	
	private String name;
	private String providedBy;
	//Multi_size base_size;		replaced with AlterableResourceConstraints
	private AlterableResourceConstraints cons;
	private Set<ComponentInstance> instances;
	private int instanceCounter;
	private boolean sgxSupport;

	public ComponentType(String name, String providedBy, /*Multi_size base_size, */ AlterableResourceConstraints cons, boolean sgxSupport) {
		this.name = name;
		this.cons = cons;
		this.providedBy = providedBy;
		//this.base_size = base_size;
		this.sgxSupport = sgxSupport;
		instanceCounter = 0;
	}
	
	public String getName() {
		return name;
	}
	
	public String getProvidedBy() {
		return providedBy;
	}
		
	//Multi_size get_base_size() {return base_size;}
	
	public AlterableResourceConstraints getResources() {
		return cons;
	}
	
	public Set<ComponentInstance> getInstances() {
		return instances;
	}
	
	public boolean isSgxSupported() {
		return sgxSupport;
	}
	
	public ComponentInstance createInstance(boolean crit) {
		ComponentInstance inst;
		String instanceName = name + Integer.toString(instanceCounter);
		inst = new ComponentInstance(instanceName, crit, this);
		instances.add(inst);
		instanceCounter ++;
		return inst;
	}

	public void removeInstance(ComponentInstance ci) {
		Logger.getGlobal().info("Removing an instance of type " + name);
		instances.remove(ci);
	}

}
