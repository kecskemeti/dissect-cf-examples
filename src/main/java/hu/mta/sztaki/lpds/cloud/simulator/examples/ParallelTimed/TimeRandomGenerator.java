/*	
 * Author: Dilshad H. Sallo
 */

package hu.mta.sztaki.lpds.cloud.simulator.examples.ParallelTimed;

import hu.mta.sztaki.lpds.cloud.simulator.DeferredEvent;
import hu.mta.sztaki.lpds.cloud.simulator.Timed;
import hu.mta.sztaki.lpds.cloud.simulator.util.SeedSyncer;

/*
 * TimeRandomGenerator used to test performance of Timed class by generating
 * creating objects with random frequencies, using the same real workload that
 * realistic simulation does, on tick. 
 */
public class TimeRandomGenerator {
	
final public static int limit = 35000;
	
	public static class TimedRandom extends Timed {
		long myFires = 0;
		long computation = 0;
		/*
		 * subscribe all objects with different frequencies
		 */
		public TimedRandom(long i) {
			subscribe(i);
		}
		/*
		 * Using same real workload that realistic simulation does on tick
		 */
		@Override
		public void tick(long fires) {
			myFires++;
			for(int i=0; i<= 49; i ++) {
				computation += (long) (Math.exp(i) * 2 * Math.sqrt(i)) % (Math.abs(Math.floorDiv(i + 5 , i + 1))) ;
			}
		}
		
		public void display() {
			System.out.println("Total computations = " + computation);
		}
	}

	public static void main(String[] args) {

		final TimedRandom[]  timeRan = new TimedRandom[limit];

		/*
		 * Creating objects with random frequencies to test class under unexpected situations
		 */
		for (int i = 0; i <limit; i++) {
			timeRan[i] = new TimedRandom(SeedSyncer.centralRnd.nextInt(100) + 1) {
				
			};
			
			new DeferredEvent(SeedSyncer.centralRnd.nextInt(100)) {
			@Override
			protected void eventAction() {
			}
			};
		}
		timeRan[limit-1].display();

	}

}
