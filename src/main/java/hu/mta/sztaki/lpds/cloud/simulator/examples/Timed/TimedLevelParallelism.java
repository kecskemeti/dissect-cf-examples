/*
 * Author: Dilshad H. Sallo
 * Year: 2020
 */

package hu.mta.sztaki.lpds.cloud.simulator.examples.Timed;

import java.util.Map;
import java.util.TreeMap;
import hu.mta.sztaki.lpds.cloud.simulator.DeferredEvent;
import hu.mta.sztaki.lpds.cloud.simulator.Timed;

public class TimedLevelParallelism {
	final public static int limit = 15000;
	
	static TreeMap<TimeLP,DeferredEventLP> tdePer = new TreeMap<TimeLP, DeferredEventLP>();
	
	public static class TimeLP extends Timed {
		 long myFires = 0;
		 long frequency = 0;
		 long fCounter = 0;
		 long totalTime = 0;
		
		public TimeLP(final int i) {
			/*
			 * Determine approximate level of parallelism using frequency
			 */
			final int freq = (i % 3 == 0 ? 3 :  (i % 3));
			/*
			 * Control delay time of DeferredEvent class
			 */
			final int wait = (i % 10 == 0 ? 10 :  (i % 10));
			subscribe(freq);
			this.frequency = freq;
			tdePer.put(this, new DeferredEventLP(wait));
		}
		
		@Override
		public void tick(long fires) {
		myFires++;
		this.fCounter = getFireCount();
		totalTime += calcTimeFire(fires);
		}
		
		public long calcTimeFire(long f) { 
			long result = 0;
			long startT = System.nanoTime();
			for(int i = 0; i <= f; i ++) {
				 result = result * i * f;
			}
			long endT = System.nanoTime();
			long durationInN = (endT - startT);
			return durationInN;
		}
	}
	
	public static class DeferredEventLP extends DeferredEvent{
		public boolean eventFired;

		public DeferredEventLP(long delay) {
			super(delay);
		}

		@Override
		protected void eventAction() {
			eventFired = true;	
		}
	}

	public static void main(String[] args) {

		final TimeLP[]  timePer = new TimeLP[limit];
		
		for (int i = 0; i <limit; i++) {
			timePer[i] = new TimeLP(i);
		}
		Timed.simulateUntil(limit);
		
		for (Map.Entry<TimeLP, DeferredEventLP> objects: tdePer.entrySet()) {
			if(!(objects.getKey().myFires * objects.getKey().frequency == objects.getKey().fCounter) && objects.getValue().eventFired == true) {
				System.out.println("Number of Timed fires is not correct and not all events arrived");
				}
			}
	}
}