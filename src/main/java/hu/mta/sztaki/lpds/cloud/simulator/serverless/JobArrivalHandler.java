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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hu.mta.sztaki.lpds.cloud.simulator.Timed;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.JobListAnalyser;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.GenericTraceProducer;
import hu.mta.sztaki.lpds.cloud.simulator.helpers.trace.TraceManagementException;
import uk.ac.ljmu.fet.cs.cloud.examples.autoscaler.JobLauncher;
import uk.ac.ljmu.fet.cs.cloud.examples.autoscaler.QueueManager;


/**
 * Processes a trace and sends its jobs to a job launcher. If a job cannot be
 * launched at the moment, it will queue it with the help of a queue manager.
 * 
 * @author "Gabor Kecskemeti, Department of Computer Science, Liverpool John
 *         Moores University, (c) 2019"
 */
public class JobArrivalHandler extends Timed{
	//Complete jobs
	ArrayList<Job> jobsres;
	// Experiment information
	static ArrayList<InstanceInfo> instexperiments = new ArrayList<InstanceInfo>();
	// this hold information like parse_counting_info
	static ArrayList<Instance> instCounts = new ArrayList<Instance>();
	// store jobs in set with same executable
	HashMap<String, ArrayDeque<Job>> jobsSetExecutable = new HashMap<String, ArrayDeque<Job>>();
	// Store min and max values for each Unique VM
	HashMap<String, ArrayList<Job>> vmJobs = new HashMap<String, ArrayList<Job>>();
	
	VirtualInfrastructureManager vi;
	/**
	 * All jobs to be handled
	 */
	private  List<Job> jobs;
	
	private final int totaljobcount;
	/**
	 * The job scheduler to be used
	 */
	private final JobLauncher launcher;
	/**
	 * Queue where we can send jobs that cannot be executed right away
	 */
	private final QueueManager qm;
	/**
	 * The job to be executed next
	 */
	private int currIndex = 0;

	/**
	 * Loads the trace and analyses it to prepare all its jobs for scheduling.
	 * 
	 * @param trace    The trace to be process by this handler
	 * @param launcher The job scheduling mechanism to be used when a job is due to
	 *                 be ran.
	 * @param qm       The queueing mechanism if the job scheduler rejects the job
	 *                 at the moment.
	 * @param pr       The state exchange mechanism
	 * @throws TraceManagementException If there was an issue during the trace's
	 *                                  loading
	 */
	public JobArrivalHandler(final GenericTraceProducer trace, final JobLauncher launcher, final QueueManager qm) throws TraceManagementException {
		// Preparing the workload
		jobs = trace.getAllJobs();
		System.out.println("Number of loaded jobs: " + jobs.size());
		// Ensuring they are listed in submission order
		Collections.sort(jobs, JobListAnalyser.submitTimeComparator);
		// Analyzing the jobs for min and max submission time
		long minsubmittime = JobListAnalyser.getEarliestSubmissionTime(jobs);
		final long currentTime = Timed.getFireCount();
		final long msTime = minsubmittime;
		if (currentTime > msTime) {
			final long adjustTime = (long) Math.ceil((currentTime - msTime));
			minsubmittime += adjustTime;
			for (Job job : jobs) {
				job.adjust(adjustTime);
			}
		}
		// Workload prepared we need to move the timer till the first job is due.
		Timed.skipEventsTill(minsubmittime);
		this.launcher = launcher;
		this.qm = qm;
		totaljobcount = jobs.size();
	}

	/**
	 * Starts the trace processing mechanism
	 */
	public void processTrace() {
		tick(Timed.getFireCount());
	}

	/**
	 * Checks if a job is due at the moment, if so, it dispatches it. If it cannot
	 * be dispatched, it queues it.
	 */
	@Override
	public void tick(long currTime) {
		for (int i = currIndex; i < totaljobcount; i++) {
			final Job toprocess = jobs.get(i);
			final long submittime = toprocess.getSubmittimeSecs();
			if (currTime == submittime) {
				if (launcher.launchAJob(toprocess)) {
					qm.add(toprocess);
				}
				currIndex = i + 1;
			} else if (currTime < submittime) {
				updateFrequency(submittime - currTime);
				return;
			}
		}
		if (currIndex == totaljobcount) {
			// No further jobs, so no further dispatching
			System.out.println("Last job arrived, dispatching mechanism is terminated.");
			unsubscribe();
		}
	}
	/*
	 * Calculate arrival rate
	 */
	public float getArrivalRate() {
		long max = 0;
		long min = Long.MAX_VALUE;
		for(int i=0; i < totaljobcount; i++) {
			if((jobs.get(i).get_client_end_time()) > max) {
				max = jobs.get(i).get_client_end_time();
			}
			if((jobs.get(i).get_client_start_time()) < min) {
				min = jobs.get(i).get_client_start_time();
			}
		}
		double totalInvocation = jobs.size();
		double difference_In_Time = max - min;
		float mean_reqs_per_min = (float) (totalInvocation / (difference_In_Time / 60));
		float mean_reqs_per_sec = mean_reqs_per_min / 60;
		return mean_reqs_per_sec;
	}
	/*
	 * Calculate idle and running time of instance
	 */
	public void inst_count() {
	     jobsres = new ArrayList<Job>();
	     Collections.sort(jobs, JobListAnalyser.clientStartTimeComparator);
		long minSecond = jobs.get(0).get_client_start_time();
		long maxSecond = jobs.get(jobs.size()-1).get_client_end_time();

		long total = maxSecond - minSecond;
		long ignor = (total/60/4);
		long required = minSecond + ignor;
		for(int ii=0; ii< jobs.size(); ii++) {
			if(jobs.get(ii).get_client_start_time() >= required) {
			jobsres.add(jobs.get(ii));
			}
		}
		
		for(int i = 0; i < jobsres.size(); i++) {
			if(!vmJobs.containsKey(jobsres.get(i).executable)) {
				Job jfirst = jobsres.get(i);
				vmJobs.put(jfirst.executable, new ArrayList<Job>());
				ArrayList<Job> firstJob = vmJobs.get(jfirst.executable);
				firstJob.add(jfirst);  //here we have the exact value of submittime 	
			}else {
				Job jevetime = jobsres.get(i);
				ArrayList<Job> everytime = vmJobs.get(jevetime.executable);
				everytime.add(jevetime);
			}
		}

		for(Map.Entry vm : vmJobs.entrySet()){ 
			String vmName  = (String) vm.getKey();
			ArrayList<Job> currentList = (ArrayList<Job>) vm.getValue();
	
			Collections.sort(currentList, JobListAnalyser.clientStartTimeComparator);
				long min = currentList.get(0).get_client_start_time();
				long max = currentList.get(currentList.size()-1).get_client_end_time();
				instexperiments.add(new InstanceInfo(vmName, min, max));
				currentList.clear();
		}

		Collections.sort(jobsres, JobListAnalyser.clientStartTimeComparator);
		long minall = jobsres.get(0).get_client_start_time();
		long maxall = jobsres.get(jobsres.size()-1).get_client_end_time();
			for(long i = minall; i < maxall; i+=60) {
				instCounts.add(new Instance(i));
		}
			for(int i=0; i<instCounts.size(); i++) {
				double sum=0;
				for(int j=0; j<instexperiments.size(); j++) {
					if(instexperiments.get(j).get_start() < instCounts.get(i).get_date() && instexperiments.get(j).get_end() > instCounts.get(i).get_date()) {
						sum++;
					}
				}
				if(!Double.isNaN(sum))
				{
					instCounts.get(i).set_instance_count(sum);
					sum = 0;
				}else {
					instCounts.get(i).set_instance_count(0);
					sum = 0;
				}
			}
			//Instance count
			for(int k=0; k<instCounts.size(); k++) {				
				double sum = 0;
				ArrayList<String> uniqueInstance = new ArrayList<String>();
				for(int jj=0; jj< jobsres.size(); jj++) {
					if((jobsres.get(jj).get_client_start_time() < instCounts.get(k).get_date()) && (jobsres.get(jj).get_client_end_time() > instCounts.get(k).get_date())) {
						if(!uniqueInstance.contains(jobsres.get(jj).executable)) {
							sum++;
							uniqueInstance.add(jobsres.get(jj).executable);
						}
					}
				}
				uniqueInstance.clear();
				if(!Double.isNaN(sum)) {
					instCounts.get(k).set_running_count(sum);
					sum = 0;
				}else {
					instCounts.get(k).set_running_count(0);
					sum = 0;
				}
			}

			//Instance running
			for(int i=0; i<instCounts.size(); i++) {
				int sum = 0;
				ArrayList<String> uniqueInstance2 = new ArrayList<String>();
				for(int j=0; j< jobsres.size(); j++) {
					if((jobsres.get(j).get_client_start_time() < instCounts.get(i).get_date()) && (jobsres.get(j).get_client_end_time() > instCounts.get(i).get_date()) && (jobs.get(j).is_cold == false)) {
						if(!uniqueInstance2.contains(jobsres.get(j).executable)) {
							sum++;
							uniqueInstance2.add(jobsres.get(j).executable);
							}
						}
					}
				uniqueInstance2.clear();
				if(!Double.isNaN(sum)) {
					instCounts.get(i).set_running_warm_count(sum);
					sum = 0;
					}else {
						instCounts.get(i).set_running_warm_count(0);
						sum = 0;
						}
				}
			final int instSize = instCounts.size();
		
			// idle_count
			for(int i=0; i<instSize; i++) {
				double idle = 0;
				idle = instCounts.get(i).get_instance_count() - instCounts.get(i).get_running_count();
				if(!Double.isNaN(idle)) {
					instCounts.get(i).set_idle_count(idle);
					idle = 0;
					} else {
						instCounts.get(i).set_idle_count(0);
						idle = 0;
						}
				}
			// utilization
			for(int i=0; i<instSize; i++) {
				float util = 0;
				util = (float) (instCounts.get(i).get_running_warm_count() / instCounts.get(i).get_instance_count());
				if(!Double.isNaN(util) && !Double.isInfinite(util)) {
					instCounts.get(i).set_utilization(util);
					util = 0;
				}else {
					instCounts.get(i).set_utilization(0);
					util = 0;
				}				
			}
			
			// calculate average count of instances
			float sum = 0;
			for(int i=0; i<instSize; i++) {
				sum+= instCounts.get(i).get_instance_count();
			}			
			float avg_instance_count = (sum / instSize);
			
			// calculate averge running of instances
			float sumc = 0;
			for(int i=0; i<instSize; i++) {
				sumc+= instCounts.get(i).get_running_count();
			}
			float avg_running = (sumc / instSize);
			
			// calculate utilization
			float sumu = 0;
			for(int i=0; i<instSize; i++) {
				sumu+= instCounts.get(i).get_utilization();
			}
		
			float avg_util = (sumu / instSize) ;
			
			float avg_idle = (avg_instance_count - avg_running); 
			System.err.println("Ave idle count: " + avg_idle);
			System.err.println("Ave utilization: " + avg_util);

	}
	
	public double getColdStartProbability() {
		double trueCounter = 0;
		for (int i = 0; i < jobsres.size(); i++) {
			if(jobsres.get(i).is_cold == true) {
				trueCounter++;
			}
		}
		return trueCounter/jobsres.size();
	}
	/*
	 * Generate AWS trace
	 * Note: Generate trace may need to delete the offset (spaces at end of file) manually
	 */
	void generateAWSTrace() {
		try {
			RandomAccessFile raf = new RandomAccessFile("..//AWSTrace.csv", "rw");
			raf.writeBytes("" + "," + "is_cold" + "," + "cpu_info" + "," + "inst_id" + "," + "inst_priv_ip" + "," + "new_id" + "," +
					"exist_id" + "," +"uptime" + "," + "vm_id" + "," + "vm_priv_ip" + "," + "vm_pub_ip" + "," + "start_time" + "," +
					"end_time" + "," + "elapsed_time" + "," + "aws_duration" + "," + "aws_billed_duration" + "," +  "aws_max_mem" + "," +
					"io_speed" + "," +  "client_start_time" + "," + "client_end_time" + "," +   "client_elapsed_time" + "," + "\n");
	
			Collections.sort(jobs, JobListAnalyser.clientStartTimeComparator);
			for (int i = 0; i <jobs.size(); i++) {
				String vmid = jobs.get(i).executable;
				String newid = jobs.get(i).executable + Math.random();
				String cpu = String.format("\"%s,Intel(R) Xeon(R) Processor @ 2.50GHz\"", jobs.get(i).nprocs);
				String uptime = String.format("\"%s, %s\"", jobs.get(i).getMidExecInstanceSecs(), jobs.get(i).getMidExecInstanceSecs());
				raf.writeBytes(i + "," +
				//is_cold
				jobs.get(i).is_cold + "," +		
				//cpu_info
				cpu + "," +				
				//inst_id
				jobs.get(i).executable + "," +
				//inst_priv_ip
				"192.168. 0.1" + "," +	
				//new_id
				newid + "," +				
				//exist_id
				jobs.get(i).executable + "," +
				//up-time
				uptime + "," +			
				//vm_id
				vmid + "," +
				//vm_priv_ip
				"192.168. 0.1" + "," + 
				//vm_pub_ip
				"192.168. 0.1" + "," +				
				//start_time 
				jobs.get(i).getSubmittimeSecs() * 1000l  + "," +
				//end_time
				jobs.get(i).getStoptimeSecs() * 1000l + "," +
				//elapsed_time
				jobs.get(i).getExectimeSecs() * 1000l + "," +
				//aws_duration (in millisecond)
				((jobs.get(i).getExectimeSecs() * 1000l) + jobs.get(i).getQueuetimeSecs()) + "," +				
				//aws billed duration
				"200" + "," +
				//aws_max_mem
				jobs.get(i).usedMemory + "," +
				//io_speed
				"0" + "," +				
				//client_start_time
				jobs.get(i).get_client_start_time() + "," +		
				//client_end_time
				jobs.get(i).get_client_end_time() + "," +
				//client_elapsed_time
				jobs.get(i).getExectimeSecs() + "," + "\n");	
			}
			
			raf.close();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}	
}
