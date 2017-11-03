package at.ac.uibk.dps.cloud.simulator.examples.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant.ComponentInstance;
import hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant.ComponentType;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmconsolidation.ResourceVector;

public class ComponentTest {
	
	ComponentType ctype;
	ComponentInstance cinstance;
	String[] init = new String[]{"StoreMgr", "Provide", "0.2", "0.001", "20", "true"};

	@Before
	public void setUpBeforeClass() throws Exception {
		ctype = new ComponentType(init[0], init[1], new ResourceVector(Double.parseDouble(init[2]), 
				Double.parseDouble(init[3]), Long.parseLong(init[4])), Boolean.parseBoolean(init[5]));		
	}

	@Test
	public void testCreationOfComponentType() {
		Assert.assertNotNull(ctype);
	}
	
	@Test
	public void testCreationOfComponentInstance() {
		cinstance = ctype.createInstance(false);
		Assert.assertNotNull(cinstance);
		Assert.assertEquals(ctype, cinstance.getType());
		Assert.assertEquals(null, cinstance.getConsumption());
		Assert.assertEquals("StoreMgr0", cinstance.getName());
	}

}
