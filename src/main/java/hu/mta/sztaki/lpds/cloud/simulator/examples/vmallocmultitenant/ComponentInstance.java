package hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant;

import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;

public class ComponentInstance {
	
//	class VM;
//	class CompType;
//	class Request;
	
	private String name;
	private boolean crit;
	private VirtualMachine vm;
	private ComponentType type;
//	std::set<Request*> requests;
//	Multi_size size;

	public ComponentInstance(String name, boolean crit, ComponentType componentType) {
		this.name = name;
		this.vm = null;
		this.crit = crit;
		this.type = componentType;
//		size=type->get_base_size();
	}	
	
//	Multi_size get_size() {return size;}	
	
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
	
	
//	public void CompInst::add_request(Request *r)
//	{
//		requests.insert(r);
//		size.increase(r->get_size());
//		if(vm!=NULL)
//			vm->size_increase(r->get_size());
//	}
//	
//	public void CompInst::remove_request(Request *r)
//	{
//		requests.erase(requests.find(r));
//		size.decrease(r->get_size());
//		if(vm!=NULL)
//			vm->size_decrease(r->get_size());
//		if(requests.empty())
//			type->remove_instance(this);
//	}


	public boolean mayBeUsedBy(String tenant, boolean critForTenant)
	{
		if((!crit)&&(!critForTenant))
			return true;
		
		boolean onlyServesTenant=true;
//		Set<Request*>::iterator it;
//		for(it=requests.begin();it!=requests.end();it++)
//		{
//			Request *r=*it;
//			if(r->get_tenant()!=tenant)
//				onlyServesTenant = false;
//		}
		return onlyServesTenant;
	}


//	public Set<String> CompInst::get_tenants()
//	{
//		std::set<std::string> result;
//		std::set<Request*>::iterator it_r;
//		for(it_r=requests.begin();it_r!=requests.end();it_r++)
//		{
//			Request * r=*it_r;
//			result.insert(r->get_tenant());
//		}
//		return result;
//	}


	public String getTenantsStr()
	{
		String s = "";
//		Set<Request*>::iterator it;
//		for(it=requests.begin();it!=requests.end();it++)
//		{
//			Request *r=*it;
//			if(s!="")
//				s+=", ";
//			s+=r->get_tenant();
//		}
		return s;
	}

}
