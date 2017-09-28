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
	private AlterableResourceConstraints cons;
	private Set<ComponentInstance> instances;
	private int instanceCounter;
	private boolean isSgxSupported;

	/**
	 * 
	 * @param name
	 * @param providedBy
	 * @param cons
	 * @param sgxSupport
	 */
	public ComponentType(String name, String providedBy, AlterableResourceConstraints cons, boolean sgxSupport) {
		this.name = name;
		this.cons = cons;
		this.providedBy = providedBy;
		this.isSgxSupported = sgxSupport;
		instanceCounter = 0;
	}
	
	public String getName() {
		return name;
	}
	
	public String getProvidedBy() {
		return providedBy;
	}
	
	public AlterableResourceConstraints getResources() {
		return cons;
	}
	
	public Set<ComponentInstance> getInstances() {
		return instances;
	}
	
	public boolean isSgxSupported() {
		return isSgxSupported;
	}
	
	/**
	 * Creates a new ComponentInstance of this ComponentType and adds it to the set of instances.
	 * Increases also the instanceCounter.
	 * @param crit
	 * 				Shall the instance host critical data?
	 * @return The created ComponentInstance.
	 */
	public ComponentInstance createInstance(boolean crit) {
		ComponentInstance inst;
		String instanceName = name + Integer.toString(instanceCounter);
		inst = new ComponentInstance(instanceName, crit, this);
		instances.add(inst);
		instanceCounter ++;
		return inst;
	}

	/**
	 * Removes the given ComponentInstance from the set.
	 * @param ci
	 * 			The ComponentInstance which shall be removed.
	 */
	public void removeInstance(ComponentInstance ci) {
		Logger.getGlobal().info("Removing an instance of type " + name);
		instances.remove(ci);
	}

}
