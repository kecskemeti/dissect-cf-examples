package hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.constraints.AlterableResourceConstraints;

	/**
	 * Instance of a ComponentType.
	 * 
	 * @author Rene Ponto
	 */

public class ComponentInstance {
	
	private String name;
	private boolean crit;
	private VirtualMachine vm;
	private ComponentType type;
    Set<Request> requests;
//	Multi_size size;
	private AlterableResourceConstraints cons;

	public ComponentInstance(String name, boolean crit, ComponentType componentType) {
		this.name = name;
		this.vm = null;
		this.crit = crit;
		this.type = componentType;
//		size=type->get_base_size();		replaced with AlterableResourceConstraints
		cons = type.getResources();
	}	
	
//	Multi_size get_size() {return size;}
	
	public AlterableResourceConstraints getResources() {
		return cons;
	}
	
	public VirtualMachine getVm() {
		return vm;
	}
	
	public void setVm(VirtualMachine vm) {
		this.vm = vm;
	}
	
	public String getName() {
		return name;
	}
	
	public ComponentType getType() {
		return type;
	}
	
	public boolean isCritical() {
		return crit;
	}	
	
	public void addRequest(Request r) {
		requests.add(r);
		cons.add(r.getResources());
		if(vm != null){
//			vm->size_increase(r->get_size());
		}
	}
	
	public void removeRequest(Request r) {
		requests.remove(r);
		cons.subtract(r.getResources());
		if(vm != null) {
//			vm->size_decrease(r->get_size());
		}
		if(requests.isEmpty())
			type.removeInstance(this);
	}


	public boolean mayBeUsedBy(String tenant, boolean critForTenant) {
		if((!crit)&&(!critForTenant))
			return true;
		
		boolean onlyServesTenant=true;
		for(Request r : requests) {
			if(r.getTenant() != tenant)
				onlyServesTenant = false;
		}
		return onlyServesTenant;
	}


	public List<String> getTenants() {
		List<String> result = new ArrayList<String>();
		for(Request r : requests) {
			result.add(r.getTenant());
		}
		return result;
	}


	public String getTenantsToString() {
		String s = "";
		for(Request r : requests) {
			if(s != "")
				s += ", ";
			s = s + r.getTenant();
		}
		return s;
	}
	
	
	
	
	class Request {
		
		private String tenant;
		private AlterableResourceConstraints cons;
		
		public Request(String tenant, AlterableResourceConstraints cons) {
			this.tenant = tenant;
			this.cons = cons;
		}
		
		public String getTenant() {
			return tenant;
		}
		
		public AlterableResourceConstraints getResources() {
			return cons;
		}
	}

}
