package hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant;

import hu.mta.sztaki.lpds.cloud.simulator.iaas.constraints.AlterableResourceConstraints;

/**
 * This class represents a request of a ComponentType. The variables of a request
 * are necessary to find/create a matching component instance.
 * 
 * @author Rene Ponto	 
 */
class Request {
	
	private String tenant;
	private AlterableResourceConstraints cons;
	private boolean crit;
	private boolean custom;
	private boolean supportsSecureEnclaves;
	/**
	 * Defines a Request for a ComponentInstance from a tenant.
	 * @param tenant
	 * 			The name of the requesting tenant.
	 * @param cons
	 * 			The requested resources.
	 */
	public Request(String tenant, AlterableResourceConstraints cons, boolean crit, boolean custom, boolean supportsSecureEnclaves) {
		this.tenant = tenant;
		this.cons = cons;
		this.crit = crit;
		this.custom = custom;
		this.supportsSecureEnclaves = supportsSecureEnclaves;
	}
	
	/**
	 * 
	 * @return The name of the tenant.
	 */
	public String getTenant() {
		return tenant;
	}
	
	/**
	 * 
	 * @return The AlterableResourceConstraints with the requested resources.
	 */
	public AlterableResourceConstraints getResources() {
		return cons;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isCrit() {
		return crit;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isCustom() {
		return custom;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean supportsSecureEnclaves() {
		return supportsSecureEnclaves;
	}
}
