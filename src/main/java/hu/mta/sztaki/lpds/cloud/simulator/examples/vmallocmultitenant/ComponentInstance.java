package hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.constraints.AlterableResourceConstraints;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption;
import hu.mta.sztaki.lpds.cloud.simulator.io.NetworkNode.NetworkException;

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
    private Set<Request> requests;

	private ResourceConsumption consumption;
	private AlterableResourceConstraints constraints;
	
	private boolean custom;

	/**
	 * The constructor.
	 * @param name
	 * 			The name of this instance.
	 * @param crit
	 * 			Determines if the instance is hosting critical data.
	 * @param componentType
	 * 			The underlying ComponentType.
	 * @param custom
	 * 			Determines if this instance is a custom implementation.
	 */
	public ComponentInstance(String name, boolean crit, ComponentType componentType, boolean custom) {
		this.name = name;
		this.vm = null;
		this.crit = crit;
		this.type = componentType;
		constraints = type.getResources();
		
		this.custom = custom;
		
		//create a new job on the host VM
		try {
			consumption = vm.newComputeTask(constraints.getTotalProcessingPower(), ResourceConsumption.unlimitedProcessing, null);	//TODO: null?
		} catch (NetworkException e) {
			e.printStackTrace();
		}
	}	
	
	public boolean isCustom() {
		return custom;
	}
	
	public Set<Request> getRequests() {
		return requests;
	}
	
	public AlterableResourceConstraints getResources() {
		return constraints;
	}
	
	public VirtualMachine getVm() {
		return vm;
	}
	
	public void setVm(VirtualMachine vm) {
		this.vm = vm;
		adjustTask();
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
	
	public ResourceConsumption getConsumption() {
		return consumption;
	}
	
	/**
	 * Cancels the previous declared resource consumption and creates a new task with the actual consumption.
	 * Gets called after a new Request arrives.
	 */
	private void adjustTask() {
		this.consumption.cancel();
		try {
			consumption = vm.newComputeTask(constraints.getTotalProcessingPower(), ResourceConsumption.unlimitedProcessing, null);	//TODO: null?
		} catch (NetworkException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds this Request to the set with the other ones. If a host VM exists, the resource consumption gets adjusted.
	 * @param r
	 * 			The Request which shall be added.
	 */
	public void addRequest(Request r) {
		requests.add(r);
		constraints.add(r.getResources());
		if(vm != null){
			adjustTask();
		}
	}
	
	/**
	 * Remove this Request from its set and the instance itself if it was the last Request. If a host VM exists, 
	 * the resource consumption gets adjusted.
	 * @param r
	 * 			The Request which shall be removed.
	 */
	public void removeRequest(Request r) {
		requests.remove(r);
		constraints.subtract(r.getResources());
		if(vm != null) {
			adjustTask();
		}
		if(requests.isEmpty())
			type.removeInstance(this);
	}

	/**
	 * Analyzes this instance and checks, if it can be used by a given tenant. This is the case,
	 * if it and the data of the tenant (which shall be hosted) are not critical or if it only serves 
	 * one tenant at all.
	 * @param tenant
	 * 				The tenant who wants to use this instance.
	 * @param critForTenant
	 * 				Defines if the data is critcal for the tenant.
	 * @return true if it and the data of the tenant (which shall be hosted) are not critical or if it only serves 
	 * 			one tenant at all, false otherwise.
	 */
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

	/**
	 * @return All tenants ordered inside an ArrayList.
	 */
	public List<String> getTenants() {
		List<String> result = new ArrayList<String>();
		for(Request r : requests) {
			result.add(r.getTenant());
		}
		return result;
	}

	/**
	 * @return All tenants inside one String.
	 */
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
		
		/**
		 * Defines a Request for a ComponentInstance from a tenant.
		 * @param tenant
		 * @param cons
		 */
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
