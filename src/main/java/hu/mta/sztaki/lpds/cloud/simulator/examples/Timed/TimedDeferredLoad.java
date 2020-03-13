/*
 * Author: Dilshad H. Sallo
 * Year: 2020
 */

package hu.mta.sztaki.lpds.cloud.simulator.examples.Timed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import hu.mta.sztaki.lpds.cloud.simulator.Timed;
import hu.mta.sztaki.lpds.cloud.simulator.examples.Timed.TimedDeferredLoad.TimedLoad.DefferedLoad;
import hu.mta.sztaki.lpds.cloud.simulator.util.SeedSyncer;

/*
 * This class allows to control the load size of simulation.
 */
public class TimedDeferredLoad {
	
	final static int limit = 10000;
	
	static HashMap<TimedLoad, ArrayList<DefferedLoad>> timeDefferedLoad = new HashMap<TimedLoad,ArrayList<DefferedLoad>>();
	
	public static class TimedLoad extends Timed {
		long myFires = 0;
		long frequency = 0;
		long fCounter = 0;
		
		public TimedLoad(int i) {
			subscribe(i+1);
		}	
		@Override
		public void tick(long fires) {
						
			final int size = (int) ((fires <= 0) ? 1 : fires);
			ArrayList<DefferedLoad> defferedLoadList = new ArrayList<DefferedLoad>();
			
			for(int i = 0; i < size; i++) {
				defferedLoadList.add(new DefferedLoad(SeedSyncer.centralRnd.nextInt(limit)));
			}
			 myFires++;
			 timeDefferedLoad.put(this, defferedLoadList);
			 this.fCounter = getFireCount();
		}
		
			public static class DefferedLoad{
				int totalLoad = 0;
				
				DefferedLoad(int l){
					
				final int level = limit / 10;
				final int load = (l % level == 0 ? level :  (l % level));
				
				for(int i = 0; i<= load; i++) {
					totalLoad = i * load * limit - (i * i) * (load * i);
					}
				}
			}
			
		protected void testObjectTickFires(){
			if(!(this.myFires == ((TimedDeferredLoad.limit -1) / this.frequency))) {
				System.out.println("Number of myfires is not correct");			
			}
		}
		
		protected void testTimedFireCounter() {
			if(!(this.myFires * this.frequency == this.fCounter)) {
				System.out.println("Number of Timed fires is not correct");
			}
		}
	}
	
	public static void main(String[] args) {
		
		final TimedLoad[]  timeLoad = new TimedLoad[limit];

		for (int i = 0; i <limit; i++) {
			timeLoad[i] = new TimedLoad(i);
			timeLoad[i].frequency = timeLoad[i].getFrequency();
		}
		Timed.simulateUntil(limit);
		 
		 for(Entry<TimedLoad, ArrayList<DefferedLoad>> listIter:timeDefferedLoad.entrySet()){      
		       if(!(listIter.getKey().myFires * listIter.getKey().frequency == listIter.getValue().size()))   
		    	   System.out.println("Number of objects is not correct");	
		    }
		 
		 for (int i = 0; i <limit; i++) {
			 timeLoad[i].testObjectTickFires();	
			 timeLoad[i].testTimedFireCounter();
			}
	}
}