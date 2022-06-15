/*	
 * Author: Dilshad H. Sallo
 */

package hu.mta.sztaki.lpds.cloud.simulator.examples.ParallelTimed;

import hu.mta.sztaki.lpds.cloud.simulator.Timed;
import hu.mta.sztaki.lpds.cloud.simulator.examples.ParallelTimed.TimeRandomGenerator.TimedRandom;

/*
 * TimedDegreeParallelism class to test Timed class using four
 * different degrees of parallelism
 */
public class TimedDegreeParallelism {
	
final public static int limit = 35000;
	
	public static class TimeLP extends TimedRandom {
		 long frequency = 0;
		
		public TimeLP(final long i) {
			super(i);
			this.frequency = i;
		}
	}
	
	public static void main(String[] args) {

		/*
		 * Degree of Parallelism 25%
		 */
		final TimeLP[]  timePer1 = new TimeLP[limit];
		
		for (int i = 0; i <limit; i++) {
			final long freq = (i % 11 == 0) ? 11 :  (i % 11);
			timePer1[i] = new TimeLP(freq);
		}
		Timed.simulateUntil(limit);
		/*
		 * Reset Timed class
		 */
		Timed.resetTimed();
		/*
		 * Degree of Parallelism 50%
		 */
		final TimeLP[]  timePer2 = new TimeLP[limit];

		for (int i = 0; i <limit; i++) {
			final long freq = (i % 4 == 0) ? 4 :  (i % 4);
			timePer2[i] = new TimeLP(freq);
		}
		Timed.simulateUntil(limit);
		/*
		 * Reset Timed class
		 */
		Timed.resetTimed();
		/*
		 * Degree of Parallelism 75%
		 */
		final TimeLP[]  timePer3 = new TimeLP[limit];

		for (int i = 0; i <limit; i++) {
			final long freq = (i % 2 == 0) ? 2 :  (i % 2);
			timePer3[i] = new TimeLP(freq);
		}
		Timed.simulateUntil(limit);
		/*
		 * Rest Timed class
		 */
		Timed.resetTimed();
		/*
		 *  Degree of Parallelism 100%
		 */
		final TimeLP[]  timePer4 = new TimeLP[limit];

		for (int i = 0; i <limit; i++) {
			final long freq = (i % 1 == 0) ? 1 :  (i % 1);
			timePer4[i] = new TimeLP(freq);
		}
		Timed.simulateUntil(limit);
	}
}
