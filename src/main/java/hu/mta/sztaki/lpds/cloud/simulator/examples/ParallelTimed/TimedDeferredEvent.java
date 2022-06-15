/*	
 * Author: Dilshad H. Sallo
 */

package hu.mta.sztaki.lpds.cloud.simulator.examples.ParallelTimed;


import hu.mta.sztaki.lpds.cloud.simulator.Timed;
import hu.mta.sztaki.lpds.cloud.simulator.DeferredEvent;
import java.util.TreeMap;
import java.util.Map;

/*
 * TimedDeferredEvent class used Timed class and DeferredEvent class to 
 * test performance of both together to balance execution time for each object.
 */
public class TimedDeferredEvent {
	
	final static int limit = 35000;	
	static boolean  fired = true;
	
	/*
	 * Store Objects of TimePerformance and DEPerformance in ordered manner.
	 */
	static TreeMap<TimePerformance,DEPerformance> tdePer = new TreeMap<TimePerformance, DEPerformance>();
	
	public static class TimePerformance extends Timed {
		 long myFires = 0;
		 long frequency = 0;
		 long fCounter;
		 
		public TimePerformance(final int i) {
			subscribe(i+1);
			tdePer.put(this, new DEPerformance(i));
		}
		
		@Override
		public void tick(long fires) {
			myFires++;
			this.fCounter = getFireCount();
		}
		
		protected void testObjectTickFires(){
			if(!(this.myFires == ((TimedDeferredEvent.limit -1) / this.frequency))) {
				System.out.println("Number of myfires is not correct");			
			}
		}
		
		protected void testTimedFireCounter() {
			if(!(this.myFires * this.frequency == this.fCounter)) {
				System.out.println("Number of Timed fires is not correct");
			}
		}
	}
	
	public static class DEPerformance extends DeferredEvent {
		public boolean eventFired;
		
		public DEPerformance(long delay) {
			super(delay);
		}

		@Override
		protected void eventAction() {
			eventFired = true;	
		}	
	}
	
	public static void main(String[] args) {

		final TimePerformance[]  timePer = new TimePerformance[limit];
		
		for (int i = 0; i <limit; i++) {
			timePer[i] = new TimePerformance(i);
			timePer[i].frequency = timePer[i].getFrequency();
		}
		Timed.simulateUntil(limit);
			
    for (Map.Entry<TimePerformance, DEPerformance> objects: tdePer.entrySet()) {
     	fired &= objects.getValue().eventFired;
		   if(!fired) {
			   System.out.println("Not all events arrived");
	        }
		}
    
	for (int i = 0; i <limit; i++) {
		timePer[i].testObjectTickFires();	
		timePer[i].testTimedFireCounter();
		}	
	}
}