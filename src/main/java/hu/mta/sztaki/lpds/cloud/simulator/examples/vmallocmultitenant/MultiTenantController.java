package hu.mta.sztaki.lpds.cloud.simulator.examples.vmallocmultitenant;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmconsolidation.ResourceVector;

public class MultiTenantController {
	
	//pm_size=[400 16000] TODO
	
	
	/** The existing tenants. */
//	private String[] tenants = new String[]{"A","B","C"};

	/** Used for creating all necessary component types. */
	private HashMap<String, ArrayList<String>> initialCompTypes;
	
	/** Used for creating all necessary requests. */
	private List<Request> initialRequests;
	Random random;
	
	public MultiTenantController() {
		random = new Random(123);
		
		initialCompTypes = new HashMap<String, ArrayList<String>>();
		initialRequests = new ArrayList<Request>();
	}
	
	public void main(int[] args) {	
		doOneRound();
	}
	
	/**
	 * Reads the comptypes.txt file to set the different component types inside the MultiTenantPMScheduler for
	 * initialising this scheduler to work properly. Otherwise there would be problems with arriving requests
	 * of those component types. The path is set relative, so there are no problems on different platforms.
	 */
	public void readCompTypes() {
		BufferedReader reader;
		try {
			Path path = Paths.get("src/main/java/hu/mta/sztaki/lpds/cloud/simulator/examples/vmallocmultitenant/comptypes.txt");			
			reader = new BufferedReader(new FileReader(path.toString()));
			
			String line = null;         
		    while ((line = reader.readLine()) != null) {
		    	if(line.startsWith("#"))
					continue;
		    	
		    	// we use the whitespace to seperate the different values
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
		    MultiTenantComponentScheduler.instantiateTypes(initialCompTypes);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("This path is not leading to the wanted file.", e);
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	}

	/**
	 * Reads the manually defined requests out of the requests.txt file to safe them in order to
	 * start one simulation. The path is set relative, so there are no problems on different 
	 * platforms. 
	 *  
	 * @return The total amount of requests.
	 */
	@SuppressWarnings("resource")
	public int readRequests() {
		int nrOfRequests = 0;
		
		BufferedReader reader;
		try {
			Path path = Paths.get("src/main/java/hu/mta/sztaki/lpds/cloud/simulator/examples/vmallocmultitenant/requests.txt");			
			reader = new BufferedReader(new FileReader(path.toString()));			
			
			String line = null;         
		    while ((line = reader.readLine()) != null) {
		    	if(line.startsWith("#"))
					continue;
		    	
		    	nrOfRequests++;		//increase the counter
		    	
		    	// we use the whitespace to seperate the different values
		    	String[] strings = line.split(" ");
		    	
		    	// now the string array above is used to create a new request
		    	String tenant = strings[0];
		    	String componentTypeName = strings[1];
		    	ComponentType ctype = null;
		    	for(ComponentType type: MultiTenantComponentScheduler.getTypes()) {
		    		if(MultiTenantComponentScheduler.getTypes().isEmpty())
		    			throw new NullPointerException("There are no types inside the pm scheduler!");
		    		
		    		// with this expression we can determine the component type to use
		    		if(type.getName().equals(componentTypeName)) {
		    			ctype = type;
		    			break;
		    		}
		    	}
		    	if(ctype == null) {
		    		throw new NullPointerException("There is no such ComponentType as " + componentTypeName + "!");
		    	}		    	
		    	
		    	boolean crit = Boolean.getBoolean(strings[2]);
		    	boolean custom = random.nextBoolean();		// we decide this factor randomly
		    	boolean supportsSecureEnclaves = random.nextBoolean();		// we decide this factor randomly
		    	
		    	double startTime = Double.parseDouble(strings[3]);
		    	double duration = Double.parseDouble(strings[4]);
		    	
		    	double first = Double.parseDouble(strings[5]);
				double second = 0.001;		// processing power
				long third = Long.parseLong(strings[6]);
				
				// a new ResourceVector is created for passing it to the new request
		    	ResourceVector cons = new ResourceVector(first, second, third);
		    	
		    	Request req = new Request(tenant, ctype, cons, crit, custom, supportsSecureEnclaves, startTime, duration);
			    initialRequests.add(req);
		    			    	
		    }
		    reader.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException("This path is not leading to the wanted file.", e);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return nrOfRequests;
	}
	
	// not necessary at the moment
	@SuppressWarnings("unused")
	private int generateRequests() {
		int nrOfRequests = 0;
		return nrOfRequests;
	}
	
	// not necessary at the moment
	@SuppressWarnings("unused")
	private int readGWFFile() {
		int jobsProcessed = 0;
		return jobsProcessed;
	}
	
	private void doOneRound() {
		
		// init comptypes and requests for the PMScheduler
//		readCompTypes();
//		int amountOfRequests = readRequests();
//		
//		int processedRequests = 0;
//		boolean changed = true;
//
//		int counter = 0;
//		while(initialRequests.get(counter) != null && processedRequests  < amountOfRequests) {
//			//TODO
//		}
	}
}
