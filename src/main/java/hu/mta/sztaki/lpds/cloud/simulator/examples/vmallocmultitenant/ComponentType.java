package hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant;

import java.util.Set;
import java.util.logging.Logger;

public class ComponentType {
	
	private String name;
	private String providedBy;
	//Multi_size base_size;
	private Set<ComponentInstance> instances;
	private int inst_ctr;
	private boolean sec_hw_capable;

	public ComponentType(String name, String providedBy, /*Multi_size base_size, */ boolean sec_hw_capable) {
		this.name = name;
		this.providedBy = providedBy;
		//this.base_size = base_size;
		this.sec_hw_capable = sec_hw_capable;
		inst_ctr = 0;
	}
	
	public String getName() {
		return name;
	}
	
	public String getProvidedBy() {
		return providedBy;
	}
		
	//Multi_size get_base_size() {return base_size;}
	
	public Set<ComponentInstance> getInstances() {
		return instances;
	}
	
	public boolean is_sec_hw_capable() {
		return sec_hw_capable;
	}
	
	public ComponentInstance CreateInstance(boolean crit) {
		ComponentInstance inst;
		String instanceName = name + Integer.toString(inst_ctr);
		inst = new ComponentInstance(instanceName, crit, this);
		instances.add(inst);
		inst_ctr ++;
		return inst;
	}


	public void remove_instance(ComponentInstance ci) {
		Logger.getGlobal().info("Removing an instance of type " + name);
		instances.remove(ci);
	}

}
