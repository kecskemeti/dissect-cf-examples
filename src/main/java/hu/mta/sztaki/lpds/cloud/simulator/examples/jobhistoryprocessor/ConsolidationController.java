package hu.mta.sztaki.lpds.cloud.simulator.examples.jobhistoryprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmconsolidation.AbcConsolidator;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmconsolidation.GaConsolidator;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmconsolidation.PsoConsolidator;

/**
 * @author Rene Ponto
 * 
 *         This class manages the consolidation algorithms, which means do
 *         several test cases with different values for the constants of the
 *         consolidators. The results are going to be saved inside a seperate
 *         file. There are also methods to set the needed values for
 *         consolidation of some algorithms.
 */
public class ConsolidationController {
	private int propCounter = 0;
	Properties props; // the properties-file, contains the constants of the pso-, abc- and
						// ga-consolidator

	private String psoDefaultSwarmSize = "20";
	private String psoDefaultNrIterations = "50";
	private String psoDefaultC1 = "2";
	private String psoDefaultC2 = "2";
	private String abcDefaultPopulationSize = "10";
	private String abcDefaultNrIterations = "50";
	private String abcDefaultLimitTrials = "5";
	private String gaDefaultPopulationSize = "10";
	private String gaDefaultNrIterations = "50";
	private String gaDefaultNrCrossovers = "10";
	private String upperThreshold = "0.75";
	private String lowerThreshold = "0.25";
	private String mutationProb = "0.2";
	private String seed = "123";
	
	public static void main(String[] args) throws IOException {
		new ConsolidationController().runTestcaseOne(false);
	}

	/**
	 * Sets all default values (which are the origin ones) and reads the
	 * properties-file. The file is saved in .xml in the root of the simulator.
	 * 
	 * @param trace
	 *            The location of the trace-file.
	 * @throws IOException
	 */
	public ConsolidationController() throws IOException {

		props = new Properties();
		File file = new File("consolidationProperties.xml");
		FileInputStream fileInput = new FileInputStream(file);
		props.loadFromXML(fileInput);
		fileInput.close();

		// set the default values

		props.setProperty("upperThreshold", upperThreshold);
		props.setProperty("lowerThreshold", lowerThreshold);
		props.setProperty("mutationProb", mutationProb);
		props.setProperty("seed", seed);
		setPsoProperties(psoDefaultSwarmSize, psoDefaultNrIterations, psoDefaultC1, psoDefaultC2,true);
		setAbcProperties(abcDefaultPopulationSize, abcDefaultNrIterations, abcDefaultLimitTrials, mutationProb, "false",
				lowerThreshold,true);
		setGaProperties(gaDefaultPopulationSize, gaDefaultNrIterations, gaDefaultNrCrossovers, mutationProb, "false",
				lowerThreshold,true);

	}

	/**
	 * This testcase is to find the best configuration of the parameters of the
	 * consolidators. For that, we define a list of values to test and this method
	 * runs the appropriate consolidators for all possible combinations of the
	 * values. All results are saved inside a csv file.
	 * 
	 * @param test
	 *            If set to true, default values are taken, otherwise the lists get
	 *            filled and all combinations out of these values are taken
	 */
	public void runTestcaseOne(boolean test) {
		// defining lists with values for each parameter of each relevant algorithm
		List<Integer> psoSwarmSizeValues = new ArrayList<>();
		List<Integer> psoNrIterationsValues = new ArrayList<>();
		List<Integer> psoC1Values = new ArrayList<>();
		List<Integer> psoC2Values = new ArrayList<>();

		List<Integer> gaPopulationSizeValues = new ArrayList<>();
		List<Integer> gaNrIterationsValues = new ArrayList<>();
		List<Integer> gaNrCrossoversValues = new ArrayList<>();
		List<Double> gaMutationProbValues = new ArrayList<>();

		List<Integer> abcPopulationSizeValues = new ArrayList<>();
		List<Integer> abcNrIterationsValues = new ArrayList<>();
		List<Integer> abcLimitTrialsValues = new ArrayList<>();
		List<Double> abcMutationProbValues = new ArrayList<>();

		List<Boolean> doLocalSearchValues = new ArrayList<>();
		List<Double> lowerThresholdValues = new ArrayList<>();

		// fill the lists with values

		if (!test) {

			int i = 1;
			while (i < 21) {
				if(i==5) {
					i+=2;
					continue; // this was already done
				}
				psoSwarmSizeValues.add(i);
				gaPopulationSizeValues.add(i);
				abcPopulationSizeValues.add(i);
				psoNrIterationsValues.add(i);
				gaNrIterationsValues.add(i);
				abcNrIterationsValues.add(i);
				i = i + 2; // increase the variable by 2 to cover the <20 range better
			}

			i = 5;
			while (i < 101) {
				gaNrCrossoversValues.add(i);
				i = i + 10; //double the resolution of crossovers as before
			}

			i = 1;
			while (i < 15) {
				abcLimitTrialsValues.add(i);
				i += 3;
			}

			i = 1;
			while (i < 11) {
				psoC1Values.add(i);
				psoC2Values.add(i);
				i = i + 2;
			}

			double j = 0.1;
			while (j <= 1) {
				abcMutationProbValues.add(j);
				gaMutationProbValues.add(j);
				j = j + 0.2;
			}
			doLocalSearchValues.add(false);
			doLocalSearchValues.add(true);
			lowerThresholdValues.add(0.2);
			lowerThresholdValues.add(0.4);
			lowerThresholdValues.add(0.6);
		}

		// test values, only one run with defaults
		if (test) {
			psoSwarmSizeValues.add(20);
			psoNrIterationsValues.add(50);
			psoC1Values.add(2);
			psoC2Values.add(2);

			gaPopulationSizeValues.add(10);
			gaNrIterationsValues.add(50);
			gaNrCrossoversValues.add(10);
			gaMutationProbValues.add(0.2);

			abcPopulationSizeValues.add(10);
			abcNrIterationsValues.add(50);
			abcLimitTrialsValues.add(5);
			abcMutationProbValues.add(0.2);

			doLocalSearchValues.add(false);
			lowerThresholdValues.add(0.25);
		}

		// now run the consolidators with every possible combination of their parameters
		// and save the results afterwards

		// pso consolidator
		for (int first : psoSwarmSizeValues) {
			for (int second : psoNrIterationsValues) {
				for (int third : psoC1Values) {
					for (int fourth : psoC2Values) {

						setPsoProperties(psoSwarmSizeValues.get(psoSwarmSizeValues.indexOf(first)).toString(),
								psoNrIterationsValues.get(psoNrIterationsValues.indexOf(second)).toString(),
								psoC1Values.get(psoC1Values.indexOf(third)).toString(),
								psoC2Values.get(psoC2Values.indexOf(fourth)).toString(),false);

					}
				}
			}
		}

		// ga consolidator

		for (int first : gaPopulationSizeValues) {
			for (int second : gaNrIterationsValues) {
				for (int third : gaNrCrossoversValues) {
					for (double fourth : gaMutationProbValues) {
						for (boolean fifth : doLocalSearchValues) {
							for (double sixth : lowerThresholdValues) {
								setGaProperties(
										gaPopulationSizeValues.get(gaPopulationSizeValues.indexOf(first)).toString(),
										gaNrIterationsValues.get(gaNrIterationsValues.indexOf(second)).toString(),
										gaNrCrossoversValues.get(gaNrCrossoversValues.indexOf(third)).toString(),
										gaMutationProbValues.get(gaMutationProbValues.indexOf(fourth)).toString(),
										doLocalSearchValues.get(doLocalSearchValues.indexOf(fifth)).toString(),
										lowerThresholdValues.get(lowerThresholdValues.indexOf(sixth)).toString(),false);

								if (!fifth) // if no local search -> value of lowerThreshold plays no role -> there is
											// no point in testing more than one value
									break;
							}
						}
					}
				}
			}
		}

		// abc consolidator

		for (int first : abcPopulationSizeValues) {
			for (int second : abcNrIterationsValues) {
				for (int third : abcLimitTrialsValues) {
					for (double fourth : abcMutationProbValues) {
						for (boolean fifth : doLocalSearchValues) {
							for (double sixth : lowerThresholdValues) {
								setAbcProperties(
										abcPopulationSizeValues.get(abcPopulationSizeValues.indexOf(first)).toString(),
										abcNrIterationsValues.get(abcNrIterationsValues.indexOf(second)).toString(),
										abcLimitTrialsValues.get(abcLimitTrialsValues.indexOf(third)).toString(),
										abcMutationProbValues.get(abcMutationProbValues.indexOf(fourth)).toString(),
										doLocalSearchValues.get(doLocalSearchValues.indexOf(fifth)).toString(),
										lowerThresholdValues.get(lowerThresholdValues.indexOf(sixth)).toString(),false);
								if (!fifth) // if no local search -> value of lowerThreshold plays no role -> there is
											// no point in testing more than one value
									break;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Setter for the constant values of the pso algorithm.
	 * 
	 * @param swarmSize
	 *            This value defines the amount of particles.
	 * @param iterations
	 *            This value defines the number of iterations.
	 * @param c1
	 *            This value defines the first learning factor.
	 * @param c2
	 *            This value defines the second learning factor.
	 */
	private void setPsoProperties(String swarmSize, String iterations, String c1, String c2, boolean noWrite) {

		props.setProperty("psoSwarmSize", swarmSize);
		props.setProperty("psoNrIterations", iterations);
		props.setProperty("psoC1", c1);
		props.setProperty("psoC2", c2);

		this.saveProps(PsoConsolidator.class.getName(), noWrite);
	}

	/**
	 * Setter for the constant values of the abc algorithm.
	 * 
	 * @param populationSize
	 *            This value defines the amount of individuals in the population.
	 * @param iterations
	 *            This value defines the number of iterations.
	 * @param limitTrials
	 *            This value defines the maximum number of trials for improvement
	 *            before a solution is abandoned.
	 * @param mutationProb
	 */
	private void setAbcProperties(String populationSize, String iterations, String limitTrials, String mutationProb,
			String doLocalSearch, String lowerThreshold, boolean noWrite) {

		props.setProperty("abcPopulationSize", populationSize);
		props.setProperty("abcNrIterations", iterations);
		props.setProperty("abcLimitTrials", limitTrials);
		props.setProperty("mutationProb", mutationProb);
		props.setProperty("doLocalSearch", doLocalSearch);
		props.setProperty("lowerThreshold", lowerThreshold);

		this.saveProps(AbcConsolidator.class.getName(), noWrite);
	}

	/**
	 * Setter for the constant values of the ga algorithm.
	 * 
	 * @param populationSize
	 *            This value defines the amount of individuals in the population.
	 * @param iterations
	 *            This value defines the number of iterations.
	 * @param crossovers
	 *            This value defines the number of recombinations to perform in each
	 *            generation.
	 */
	private void setGaProperties(String populationSize, String iterations, String crossovers, String mutationProb,
			String doLocalSearch, String lowerThreshold, boolean noWrite) {

		props.setProperty("gaPopulationSize", populationSize);
		props.setProperty("gaNrIterations", iterations);
		props.setProperty("gaNrCrossovers", crossovers);
		props.setProperty("mutationProb", mutationProb);
		props.setProperty("doLocalSearch", doLocalSearch);
		props.setProperty("lowerThreshold", lowerThreshold);

		this.saveProps(GaConsolidator.class.getName(), noWrite);
	}

	/**
	 * Saves the properties in the data after changing them.
	 * 
	 * @param file
	 * @throws IOException
	 */
	private void saveProps(String consType, boolean noWrite) {
		if(noWrite) return;
		try {
			FileOutputStream fileOutput = new FileOutputStream(new File(consType + "-consolidationProperties" + propCounter++ + ".xml"));
			props.storeToXML(fileOutput, null);
			fileOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
