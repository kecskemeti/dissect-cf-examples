package at.ac.uibk.dps.cloud.simulator.examples.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import at.ac.uibk.dps.cloud.simulator.test.IaaSRelatedFoundation;
import hu.mta.sztaki.lpds.cloud.simulator.Timed;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.IaaSService;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.pmscheduling.AlwaysOnMachines;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmscheduling.NonQueueingScheduler;
import hu.mta.sztaki.lpds.cloud.simulator.io.Repository;

public class MultiTenantPMSchedulerTest extends IaaSRelatedFoundation {
	IaaSService basic;
	Repository r;
	PhysicalMachine testPM1;
	PhysicalMachine testPM2;
	PhysicalMachine testPM3;
	PhysicalMachine testPM4;

	@Before
	public void resetSim() throws Exception {
		basic = new IaaSService(NonQueueingScheduler.class,
				AlwaysOnMachines.class);
		testPM1 = dummyPMcreator();
		testPM2 = dummyPMcreator();
		basic.registerHost(testPM1);
		basic.registerHost(testPM2);
		r = dummyRepoCreator(true);
		basic.registerRepository(r);
	}

	@Test(timeout = 100)
	public void turnonTest() {
		Assert.assertEquals("There should be no running machines at the beginning of the simulation",
				0, basic.runningMachines.size());
		Timed.simulateUntilLastEvent();
		testPM1.turnon();
		testPM2.turnon();
		Timed.simulateUntilLastEvent();
		
		Assert.assertEquals("Did not switch on all machines as expected",
				basic.machines.size(), basic.runningMachines.size());
	}
	
	@Test(timeout = 100)
	public void addMorePMsTest() {
		Timed.simulateUntilLastEvent();
		
		testPM3 = dummyPMcreator();
		testPM4 = dummyPMcreator();
		basic.registerHost(testPM3);
		basic.registerHost(testPM4);
		
		Timed.simulateUntilLastEvent();
		
		testPM3.turnon();
		testPM4.turnon();
		
		Timed.simulateUntilLastEvent();
		Assert.assertEquals("Did not switch on all machines as expected",
				basic.machines.size(), basic.runningMachines.size());
	}

}
