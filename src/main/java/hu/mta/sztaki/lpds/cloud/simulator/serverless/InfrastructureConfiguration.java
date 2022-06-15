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


import java.util.EnumMap;
import java.util.HashMap;

/**
 * Set defined plan for infrastructure based on selected option
 * 
 * @author Dilshad Sallo
 *
 */
public class InfrastructureConfiguration {
	/**
	 * Contains the current (selected) infrastructure plan
	 */
	private static  final EnumMap<InfrastructureConfiguration.MemorySize,HashMap<String,Double>> infrastructurePlan = new EnumMap<InfrastructureConfiguration.MemorySize,HashMap<String,Double>>(MemorySize.class);
	/**
	 * The actual selected provider
	 */
	private static Provider pro = null;
	/**
	 * The actual selected memory
	 */
	private static MemorySize planType = null;
	
	private static enum Provider {
		/**
		 * AWS Lambda
		 */
		AWS
	}
	private static enum MemorySize {
		m128
	}
	public static void getInfrastructurePlan() {
		
		/**
		 * Get selected provider
		 */
		pro = Provider.AWS;
		/**
		 * Get selected memory
		 */
		planType = MemorySize.m128;
		/**
		 * Determine selected provider
		 */
		switch(pro) {
		case AWS:
			switch(planType) {
			case m128:
				infrastructurePlan.put(InfrastructureConfiguration.MemorySize.m128, new HashMap<String,Double>(){
					{
						/**
						 * Price based on provider and memory size
						 */
						put("Price",0.000000832);
						/**
						 * Number of clouds
						 */
						put("cloud", 1.0);
						/**
						 * Number of nodes
						 */
						put("Node", 50.0);
						/**
						 * Number of cores
						 */
						put("CPU", 1006.0); 
						/**
						 * Amount of memory
						 */
						put("Memory", (double) 128000000000l);
						/**
						 * the power (in W) to be drawn by the PM while it is completely
						 * switched off (but plugged into the wall socket)
						 */
						put("minpower", 20.0);
						/**
						 * the power (in W) to be drawn by the PM's CPU while it is running
						 * but not doing any useful tasks.
						 */
						put("idlepower", 296.0);
						/**
						 * the power (in W) to be drawn by the PM's CPU if it's CPU is
						 * CPU's power draw values
						 */
						put("maxpower", 493.0);
						/**
						 * the ratio of the PM's disk power draw values compared to the it's
						 * CPU's power draw values
						 */
						put("diskDivider", 50.0);
						/**
						 * the ratio of the PM's network power draw values compared to the
						 * it's CPU's power draw values
						 */
						put("netDivider", 108.0);
						/**
						 * scaling the bandwidth according to the size of the cloud
						 */
						put("bandwidth", ((this.get("CPU") * this.get("Node")) / (7f * 64f)));
						/**
						 * the storage capacity of the repository
						 */
						put("capacity", (double) 36000000000000l);
						/**
						 * the input network bandwidth of the repository
						 */
						put("maxInBW",this.get("bandwidth") * 1250000);
						/**
						 * the output network bandwidth of the repository
						 */
						put("maxOutBW",this.get("bandwidth") * 1250000);
						/**
						 * the disk bandwidth of the repository
						 */
						put("diskBW",this.get("bandwidth") * 250000);
						/**
						 * Node bandwidth ratio
						 */
						put("pmBWRatio",Math.max(this.get("CPU") / 7f, 1));
						/**
						 * defines the processing capabilities of a single CPU core in this
						 * machine (in instructions/tick)
						 */
						put("perCorePocessing", 0.001);
						/**
						 * the sub-storage capacity of the repository
						 */
						put("subCapacity", (double) 5000000000000l);
						/**
						 * the input network bandwidth (sub) of the repository
						 */
						put("subMaxInBW",(this.get("pmBWRatio") * 250000));
						/**
						 * the output network bandwidth (sub) of the repository
						 */
						put("subMaxOutBW",(this.get("pmBWRatio") * 250000));
						/**
						 * the sub-disk bandwidth of the repository
						 */
						put("subDiskBW",(this.get("pmBWRatio") * 50000));
						/**
						 * defines the time delay between the machine's switch on and the
						 * first time it can serve VM requests
						 */
						put("onD",89000.0);
						/**
						 * defines the time delay the machine needs to shut down all of its
						 * operations while it does not serve any more VMs
						 */
						put("offD",29000.0);
						/**
						 * Latency for Repository
						 */
						put("RepoLatency", 5.0); 
						/**
						 * Latency for PM
						 */
						put("PMLatency", 3.0);
					}
				}); break;
			}
		default:
			//set default provider
			break;
		
		
		}//provider
		
	}
	
	public static HashMap<String,Double> getCurrentInfrConfig(){
			getInfrastructurePlan();
		return  infrastructurePlan.get(planType);
	}
	

}
