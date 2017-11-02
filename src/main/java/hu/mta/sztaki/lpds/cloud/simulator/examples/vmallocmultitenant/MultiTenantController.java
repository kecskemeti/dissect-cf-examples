package hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import hu.mta.sztaki.lpds.cloud.simulator.helpers.job.Job;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmconsolidation.ResourceVector;

public class MultiTenantController {
	
	//pm_size=[400 16000] TODO
	
	/** The existing tenants. */
	private String[] tenants = new String[]{"A","B","C"};

	/** Used for creating all necessary component types. */
	private HashMap<String, ArrayList<String>> initialCompTypes;
	
	/** Used for creating all necessary requests. */
	private List<Request> initialRequests;
	
	
	public MultiTenantController() {
		initialRequests = new ArrayList<Request>();
	}
	
	public void main(int[] args) {
		
		readCompTypes();
		readRequests();
		doOneRound();
	}
	
	/**
	 * Should be finished.
	 */
	private void readCompTypes() {
		String filePath = new File("").getAbsolutePath();
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader
					(filePath + "/src/main/java/hu/mta/sztaki/lpds/cloud/simulator/examples/vmallocmultitenant/comptypes.txt"));
			
			String line = null;         
		    while ((line = reader.readLine()) != null) {
		    	if(line.startsWith("#"))
					continue;
		    	
		    	String[] strings = line.split(" ");
		    	ArrayList<String> list = new ArrayList<String>();
		    	
		    	list.add(strings[1]);
		    	list.add(strings[2]);
		    	list.add("0.001");		// processing power
		    	list.add(strings[3]);
		    	list.add(strings[4]);		    	
		    	
		    	initialCompTypes.put(strings[0], list);
		    }
		    reader.close();
		    MultiTenantPMScheduler.instantiateTypes(initialCompTypes);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * 
	 * @return
	 */
	private int readRequests() {
		int nrOfRequests = 0;
		
		String filePath = new File("").getAbsolutePath();
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader
					(filePath + "/src/main/java/hu/mta/sztaki/lpds/cloud/simulator/examples/vmallocmultitenant/requests.txt"));
			
			String line = null;         
		    while ((line = reader.readLine()) != null) {
		    	if(line.startsWith("#"))
					continue;
		    	
		    	String[] strings = line.split(" ");
		    	
		    	String tenant = strings[0];
		    	String componentTypeName = strings[1];
		    	ComponentType ctype = null;
		    	for(ComponentType type: MultiTenantPMScheduler.getTypes()) {
		    		if(type.getName().equals(componentTypeName)) {
		    			ctype = type;
		    			break;
		    		}
		    	}
		    	double first = Double.parseDouble(strings[5]);
				double second = 0.001;		// processing power
				long third = Long.parseLong(strings[6]);
				
		    	ResourceVector cons = new ResourceVector(first, second, third);
		    	
		    	//TODO
		    	
		    	Request req = new Request(line, null, null, false, false, false);
		    	initialRequests.add(req);
		    }
		    reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return nrOfRequests;
	}
	
	private int generateRequests() {
		int nrOfRequests = 0;
		return nrOfRequests;
	}
	
	private int readGWFFile() {
		int jobsProcessed = 0;
		return jobsProcessed;
	}
	
	/**
	 * Maybe move this to the VMScheduler...
	 * 
	 * Has to be called after a new Job arrives. It generates a request with the
	 * given values, the size of the Job has to be used as component size increase,
	 * the finishing of a Job has to be mapped to a terminateRequest etc.
	 * 
	 * After that the request will be handled by the PMScheduler.
	 * 
	 * Since the workload trace does not contain all information we need, we 
	 * generated the missing information as follows:
	 * 
	 * - Each job gets marked as critical with probability pcrit.
	 * - Each job gets marked as custom with probability pcust.
	 * - Each job gets marked as capable of using secure enclaves with probability pcap.
	 * - Each job has to be assigned randomly to one of the tenants.
	 * 
	 * The marking actually happens in the generated request.
	 * 
	 * TODO
	 */
	private Request mappingRequestToJob(Job job) {	
		Random random = new Random(123);
		
		String tenant;
		boolean crit = false;
		boolean custom = false;
		boolean supportsSecureEnclaves = false;
		
		// determine the tenant who sends the job		
		double probability = random.nextDouble();
		if(probability < 0.33) {
			tenant = tenants[0];
		}
		else {
			if(probability > 0.67) {
				tenant = tenants[1];
			}
			else {
				tenant = tenants[2];
			}
		}
		
		// determine whether the tenant's request contains criticality, a custom component
		// instance or the possibilty to use secure enclaves.
		if(random.nextBoolean()) {
			crit = true;
		}
		if(random.nextBoolean()) {
			custom = true;
		}
		if(random.nextBoolean()) {
			supportsSecureEnclaves = true;
		}
		
		Request request = new Request(tenant, null, null, crit, custom, supportsSecureEnclaves);
		return request;
		// c++ code: main, read_gwf_file
	}
	
	private void doOneRound() {
		
	}
}
