/*
 *  ========================================================================
 *  Helper classes to support simulations of large scale distributed systems
 *  ========================================================================
 *  
 *  This file is part of DistSysJavaHelpers.
 *  
 *    DistSysJavaHelpers is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *   DistSysJavaHelpers is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  (C) Copyright 2022, Gabor Kecskemeti (g.kecskemeti@ljmu.ac.uk)
 *  (C) Copyright 2022, Dilshad H. Sallo (sallo@iit.uni-miskolc.hu)
 *  (C) Copyright 2016, Gabor Kecskemeti (g.kecskemeti@ljmu.ac.uk)
 *  (C) Copyright 2012-2015, Gabor Kecskemeti (kecskemeti.gabor@sztaki.mta.hu)
 */
package hu.mta.sztaki.lpds.cloud.simulator.serverless;


import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import hu.mta.sztaki.lpds.cloud.simulator.energy.powermodelling.PowerState;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.IaaSService;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.pmscheduling.PhysicalMachineController;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmscheduling.Scheduler;
import hu.mta.sztaki.lpds.cloud.simulator.io.Repository;
import hu.mta.sztaki.lpds.cloud.simulator.util.PowerTransitionGenerator;

public class DataCenter {
	
	/**
	 * Hold current infrastructure configuration
	 */
	private static HashMap<String,Double> cip = new HashMap<String,Double>();

	
	public static IaaSService createDataCenter(Class<? extends Scheduler> vmsch, Class<? extends PhysicalMachineController> pmcont) throws Exception {
		
		cip = InfrastructureConfiguration.getCurrentInfrConfig();
		
		/**
		 * Specification of the power behavior
		 */
		final EnumMap<PowerTransitionGenerator.PowerStateKind, Map<String, PowerState>> transitions = PowerTransitionGenerator.
				generateTransitions(cip.get("minpower"), cip.get("idlepower"),cip.get("maxpower"), cip.get("diskDivider"), cip.get("netDivider"));
		final Map<String, PowerState> stTransitions = transitions.get(PowerTransitionGenerator.PowerStateKind.storage);
		final Map<String, PowerState> nwTransitions = transitions.get(PowerTransitionGenerator.PowerStateKind.network);
		
		/**
		 * Create cloud with scheduling and pm controller
		 */
		IaaSService iaas = new IaaSService(vmsch, pmcont);
		
		final String repoid = "Storage";
		HashMap<String, Integer> latencyMapRepo = new HashMap<String, Integer>((cip.get("Node").intValue() + 2));
		
		Repository mainStorage = new Repository(cip.get("capacity").longValue(), repoid, cip.get("maxInBW").longValue(), cip.get("maxOutBW").longValue(), 
				cip.get("diskBW").longValue(), latencyMapRepo, stTransitions, nwTransitions);
		iaas.registerRepository(mainStorage);
		
		/**
		 * Creating the PMs for the cloud
		 */
		final Map<String, PowerState> cpuTransitions = transitions.get(PowerTransitionGenerator.PowerStateKind.host);
		ArrayList<PhysicalMachine> completePMList = new ArrayList<PhysicalMachine>(cip.get("Node").intValue());
		HashMap<String, Integer> latencyMapMachine = new HashMap<String, Integer>(cip.get("Node").intValue() + 2);
		
		latencyMapMachine.put(repoid, cip.get("RepoLatency").intValue()); 
		final String machineid = "Node";
		
		for (int i = 1; i <= cip.get("Node").intValue(); i++) {
			String currid = machineid + i;
			
			PhysicalMachine pm = new PhysicalMachine(cip.get("CPU"), cip.get("perCorePocessing"), cip.get("Memory").longValue(), 
					new  Repository(cip.get("subCapacity").longValue(), currid, cip.get("subMaxInBW").longValue(),
							cip.get("subMaxOutBW").longValue(), cip.get("subDiskBW").longValue(), latencyMapMachine, stTransitions, nwTransitions), cip.get("onD").intValue(), cip.get("offD").intValue(), cpuTransitions);
			latencyMapRepo.put(currid, cip.get("RepoLatency").intValue());
			latencyMapMachine.put(currid,  cip.get("PMLatency").intValue());
			completePMList.add(pm);
		}
		
		iaas.bulkHostRegistration(completePMList);
		return iaas;
	}
}
