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

public class InstanceInfo {

	private String inst_name;
	double lifetime_mins;
	//this could be real or simulation time
	//I guess we are no longer need it start and end
	private long start;
	private long end;
	//counter to manage start and end
	// call by constructor or set will create object
	public InstanceInfo(long start){
		this.start = start;
	//	sequence.add(counter,new TrackVM(start));
	}
	public InstanceInfo(String name, long start) {
		this.inst_name = name;
		this.start = start;
	}
	public InstanceInfo(String inst_name, long StartTime, long EndTime) {
		this.inst_name = inst_name;
		this.lifetime_mins = (EndTime - StartTime) / 60d;//
		this.start = StartTime;
		this.end = EndTime;
	}
	//setter
	public void set_start(long start) {
		this.start = start;
	}
	public void set_end(long end) {
		this.end = end;

	}
	//getter
	public long get_start() {
		return this.start;
	}
	public long get_end() {
		return this.end;
	}
	public String get_inst_name() {
		return this.inst_name;
	}
	public double get_lifetime_mins() {
		return this.lifetime_mins; // this will also change
	}
}

