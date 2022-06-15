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

public class Instance {
	private long date;
	private double instance_count;
	private double running_count;
	private double running_warm_count;
	private double idle_count;
	private float utilization;
	
	public Instance(long date) {
		this.date = date;
	}
	//setter
	public void setDate(long date) {
		this.date = date;
	}
	public void set_instance_count(double instance_count) {
		this.instance_count = instance_count;
	}
	public void set_running_count(double running_count) {
		this.running_count = running_count;
	}
	public void set_running_warm_count(double running_warm_count) {
		this.running_warm_count = running_warm_count;
	}
	public void set_idle_count(double idle_count) {
		this.idle_count = idle_count;
	}
	public void set_utilization(float utilization) {
		this.utilization = utilization;
	}
	//getter
	public long get_date() {
		return this.date;
	}
	public double get_instance_count() {
		return this.instance_count;
	}
	public double get_running_count() {
		return this.running_count;
	}
	public double get_running_warm_count() {
		return this.running_warm_count;
	}
	public double get_idle_count() {
		return this.idle_count;
	}
	public double get_utilization() {
		return this.utilization;
	}
}
