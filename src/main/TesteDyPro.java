package main;

import java.util.ArrayList;
import java.util.HashMap;

import heuristics.Cycle;
import heuristics.EdgeRandomization;
import heuristics.Patcher;
import ilog.concert.IloException;
import heuristics.FixOptPInt;
import heuristics.KCombinations;
import heuristics.OptPathPlan;
import heuristics.Pair;

public class TesteDyPro {
	
	// OPP_Cycles: 7 40 100 8 20 2 4 8 600 21600 15 5 2 (Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: -1)

	@SuppressWarnings("unlikely-arg-type")
	public static void main(String[] args) throws CloneNotSupportedException, IloException {
		
		//Parameters
		int networkSize = Integer.parseInt(args[0]); //size of the network (i.e., # of nodes)
		int capacityProbe = Integer.parseInt(args[1]); //available space in a given flow (e.g., # of bytes)	
		int maxProbes = Integer.parseInt(args[2]); //max ammount of probes allowed to solve the path generation
		int telemetryItemsRouter = Integer.parseInt(args[3]); //number of telemetry items per router 
		int maxSizeTelemetryItemsRouter = Integer.parseInt(args[4]); //max size of a given telemetry item (in bytes)
		int initSizeComb = Integer.parseInt(args[5]); // initial size of the combinations
		int maxSizeComb = Integer.parseInt(args[6]); // max size of the combinations
		int numThreads = Integer.parseInt(args[7]); //max number of threads allowed
		int subProblemTimeLimit = Integer.parseInt(args[8]); //maximum time to solve a subproblem
		int globalTimeLimit = Integer.parseInt(args[9]); //global time to cover the whole network
		int contIterNotImprovMax = Integer.parseInt(args[10]); //# of iterations without any improvement (i.e., no path reduction)
		int combSize = Integer.parseInt(args[11]); //combinations of size 'k', used on performed statistics
		int nodesOfFailure = Integer.parseInt(args[12]); //determines the number of "failure-nodes"
		
		double seed = 123;
		//double seed = System.currentTimeMillis();
		double timeER = 0;
		double timeOPPCycles = 0;
		
		NetworkInfrastructure infra = null; 
		EdgeRandomization modelER = null;
		OptPathPlan pathPlanCycles = null;
		FixOptPInt fixOpt = null;
		AlgorithmOpt opt = null;
		
		ArrayList<int[]> collectors = new ArrayList<int[]>();
		ArrayList<int[]> failures = new ArrayList<int[]>();
		KCombinations kComb = new KCombinations();
		int[] array = new int[networkSize];
		for(int i = 0; i < networkSize; i++) {
			array[i] = i;
		}
		
		String pathInstance = ""; //to parse a data file as input (if necessary)
		
		//creating infrastructure and generating a random topology
		infra = new NetworkInfrastructure(networkSize, pathInstance, telemetryItemsRouter, maxSizeTelemetryItemsRouter, (long) seed);
		infra.filePath = pathInstance;
		infra.generateRndTopology(0.7);
		
		//item size verification
		int itemSize = Integer.MIN_VALUE;
		for(int k = 0; k < infra.sizeTelemetryItems.length; k++) {
			if(infra.sizeTelemetryItems[k] > itemSize) {
				itemSize = infra.sizeTelemetryItems[k];
			}
		}
		
		if(itemSize > (capacityProbe - 2)) { //infeasible
			System.out.println("-0" + ";" + "NaN" + ";" 
					+ infra.size + ";" + infra.telemetryItemsRouter + ";" + infra.maxSizeTelemetryItemsRouter 
					+ ";" + infra.seed);
			System.out.println("-0" + ";" + "NaN" + ";" 
					+ infra.size + ";" + infra.telemetryItemsRouter + ";" + infra.maxSizeTelemetryItemsRouter 
					+ ";" + infra.seed);
			//System.out.println("Exception: CapkComb.enumKCombos(array, comb);acity is not enough to collect the items!");
			System.out.close();
		}else {
			//int minCyclesER = Integer.MAX_VALUE;
			//int maxCyclesER = Integer.MIN_VALUE;
			//double avgCyclesER = 0;
			int minCyclesOPP = Integer.MAX_VALUE;
			int maxCyclesOPP = Integer.MIN_VALUE;
			double avgCyclesOPP = 0;
			int minCyclesFixOpt = Integer.MAX_VALUE;
			int maxCyclesFixOpt = Integer.MIN_VALUE;
			double avgCyclesFixOpt = 0;
			
			
			if(nodesOfFailure > 0) {
				failures = kComb.enumKCombos(array, nodesOfFailure);
			}
			
			int n = 0;
			for(int l = 0; l < failures.size(); l++) {
			//for(int l = 0; l < 1; l++) {
				n++;
				//items' size
				/*for(int i = 0; i < infra.telemetryItemsRouter; i++) {
					System.out.println("item " + i + ": " + infra.sizeTelemetryItems[i]);	
				}*/
				
				//System.out.println("n: " + n);
				
				//ER
				int temp[] = new int[2];
				timeER = System.nanoTime();
				modelER = new EdgeRandomization(infra, capacityProbe, (long) seed, maxProbes, temp, false);
				/*modelER.runER();
				timeER = (System.nanoTime() - timeER)*0.000000001;
				System.out.println("ER before: " + modelER.cycles.size());
				
				//set IDs
				for(int i = 0; i < modelER.cycles.size(); i++) {
					modelER.cycles.get(i).id = i;
				}

				
				//deep clone
				ArrayList<Cycle> oldCyclesER = new ArrayList<Cycle>();
				for(int i = 0; i < modelER.cycles.size(); i++) {
					oldCyclesER.add(modelER.cycles.get(i).clone());
				}*/

				
				//INFOCOMM probe cycles
				timeOPPCycles = System.nanoTime(); 
				pathPlanCycles = new OptPathPlan(infra, capacityProbe, (long) seed, true);
				pathPlanCycles.adaptToLinks();
				timeOPPCycles = (System.nanoTime() - timeOPPCycles)*0.000000001;
				System.out.println("OPP_cycles before: " + pathPlanCycles.Q.size());

				
				//set IDs
				for(int i = 0; i < pathPlanCycles.Q.size(); i++) {
					pathPlanCycles.Q.get(i).id = i;
				}
				
				//deep clone
				ArrayList<Cycle> oldCyclesOPP_Cycles = new ArrayList<Cycle>();
				for(int i = 0; i < pathPlanCycles.Q.size(); i++) {
					oldCyclesOPP_Cycles.add(pathPlanCycles.Q.get(i).clone());
				}
				
				//FixOpt
				double timeFixOpt = System.nanoTime();
				ArrayList<Integer> sinks = new ArrayList<Integer>();
				for(int i = 0; i < networkSize; i++) {
					sinks.add(i);
				}
				fixOpt = new FixOptPInt(infra, capacityProbe, maxProbes, numThreads, (long) seed,
						subProblemTimeLimit, globalTimeLimit, initSizeComb, maxSizeComb, contIterNotImprovMax);
				double fixOptSol = fixOpt.run(pathPlanCycles.Q);
				timeFixOpt = (System.nanoTime() - timeFixOpt)*0.000000001;
				
				//set failure nodes
				ArrayList<Integer> failureNodes = new ArrayList<Integer>();
				//failureNodes.add(0);
				for(int k = 0; k < failures.get(l).length; k++) {
					failureNodes.add(failures.get(l)[k]);
				}
				int[] nodes = new int[2 + failureNodes.size()];
				for(int k = 2; k < nodes.length; k++) {
					nodes[k] = failureNodes.get(k - 2);
				}
				
				
				//System.out.println("failureNodes: " + failureNodes);
				
				//create sub-graph to (temporarily) exclude the nodes of failure; 
				ArrayList<Pair<Integer,Integer>> storeLinks = infra.setSubGraph(infra.graph, nodes);
				
				System.out.println("OPP here: " + pathPlanCycles.Q);
				System.out.println("FixOpt here: ");
				for(int p = 0; p < fixOpt.maxProbes; p++) {
					for(int q = 0; q < fixOpt.infra.size; q++) {
						for(int r = 0; r < fixOpt.infra.size; r++) {
							if(fixOpt.xMetrics[p][q][r] != 0) {
								System.out.printf("xMetrics[%d][%d][%d] = %d\n", p, q, r, fixOpt.xMetrics[p][q][r]);
							}
						}
					}
				}
				
				//Failure Injector
				Patcher failureInjector = new Patcher();
				//failureInjector.reconstructPaths(infra, modelER.cycles, failureNodes, storeLinks, nodes, capacityProbe);
				failureInjector.reconstructPaths(infra, pathPlanCycles.Q, failureNodes, storeLinks, nodes, capacityProbe);
				//failureInjector.reconstructPaths(infra, translateToArray, failureNodes, storeLinks, nodes, capacityProbe);
				
				//System.out.println("----------------------HEREAFTER IS ALL FAILURE-------------------");
				
				//ER
				/*timeER = System.nanoTime();
				modelER = new EdgeRandomization(infra, capacityProbe, (long) seed, maxProbes, nodes, true);
				modelER.runER();
				timeER = (System.nanoTime() - timeER)*0.000000001;				
				
				//INFOCOMM probe cycles
				timeOPPCycles = System.nanoTime(); 
				pathPlanCycles = new OptPathPlan(infra, capacityProbe, (long) seed, true);
				pathPlanCycles.adaptToLinks();
				timeOPPCycles = (System.nanoTime() - timeOPPCycles)*0.000000001;*/
				
				/*System.out.println("path 0 again: " + pathPlanCycles.Q.get(0).links + " id: " + pathPlanCycles.Q.get(0).id);
				System.out.println("path old again: " + oldCyclesOPP_Cycles.get(0).links + " id: " + oldCyclesOPP_Cycles.get(0).id);*/
				

				//get (min, max, avg) # of cycles
				/*if(modelER.cycles.size() < minCyclesER) {
					minCyclesER = modelER.cycles.size();
				}
				if(modelER.cycles.size() > maxCyclesER) {
					maxCyclesER = modelER.cycles.size();
				}*/
				
				if(pathPlanCycles.Q.size() < minCyclesOPP) {
					minCyclesOPP = pathPlanCycles.Q.size();
				}
				if(pathPlanCycles.Q.size() > maxCyclesOPP) {
					maxCyclesOPP = pathPlanCycles.Q.size();
				}				
				
				//print format nodes of failure
				/*String nodesFail_ER = "";
				for(int t = 0; t < failureNodes.size(); t++) {
					nodesFail_ER += String.format(";%s", failureNodes.get(t));
				}*/
				
				/*String nodesFail_OPP_Cycles = "";
				for(int t = 0; t < failureNodes.size(); t++) {
					nodesFail_OPP_Cycles += String.format(";%s", failureNodes.get(t));
				}*/
				
				
				//System.out.println("ER now: " + modelER.cycles.size());
				//System.out.println("OPP_Cycles now: " + pathPlanCycles.Q.size());
				System.out.println("OPP_Cycles now: " + fixOptSol);
				
				//Statistics
				Statistics sts = new Statistics();
				
				//compare to old solution and collect changes (# of items reallocation)
				//double[] changesER = new double[12];
				//changesER = sts.changesOnLinksAndItems(oldCyclesER, modelER.cycles);
				
				//double[] changesOPP_Cycles = new double[12];
				//changesOPP_Cycles = sts.changesOnLinksAndItems(oldCyclesOPP_Cycles, pathPlanCycles.Q);
				
				//System.exit(0);
				
				if(networkSize < 100) {
				
					for(int comb = 1; comb <= combSize; comb++) {
						collectors = null;
						collectors = kComb.enumKCombos(array, comb);
					
						// -Metric 1: Data overhead: collector's distance (minimum distance)
						//int bestMinDistCollectorER = Integer.MAX_VALUE;
						int bestMinDistCollectorOPP_Cycles = Integer.MAX_VALUE;
						int bestMinDistCollectorFixOpt = Integer.MAX_VALUE;
						//int minDistCollectorER = Integer.MAX_VALUE;
						int minDistCollectorOPP_Cycles = Integer.MAX_VALUE;
						int minDistCollectorFixOpt = Integer.MAX_VALUE;
						//int bestK_ER = Integer.MAX_VALUE;
						int bestK_OPP_Cycles = Integer.MAX_VALUE;
						int bestK_FixOpt = Integer.MAX_VALUE;
						
						//search for the best combination subset and set it as the set of collectors
						for(int k = 0; k < collectors.size(); k++) {
							//exclude invalid combinations (i.e., nodes of failure must not be considered)
							boolean invalidComb = false;
							for(int t = 0; t < collectors.get(k).length; t++) {
								for(int u = 2; u < nodes.length; u++) {
									if(collectors.get(k)[t] == nodes[u]) {
										invalidComb = true;
										break;
									}
								}
								if(invalidComb) {
									break;
								} 
							}
					
							if(!invalidComb) {
								/*System.out.println("failureNodes: " + failureNodes);
								for(int t = 0; t < collectors.get(k).length; t++) {
									System.out.println("collectors: " + collectors.get(k)[t]);
								}*/
								
								//minDistCollectorER = sts.transportationOverhead(modelER, modelER.cycles, fixOpt, opt, collectors.get(k), maxProbes, 2);
								//minDistCollectorOPP_Cycles = sts.transportationOverhead(modelER, pathPlanCycles.Q, fixOpt, opt, collectors.get(k), maxProbes, 2);
								minDistCollectorFixOpt = sts.transportationOverhead(modelER, pathPlanCycles.Q, fixOpt, opt, collectors.get(k), maxProbes, 1);
									
								/*if(minDistCollectorER < bestMinDistCollectorER) {
									bestMinDistCollectorER = minDistCollectorER;
									bestK_ER = k;
								}*/
								
								if(minDistCollectorOPP_Cycles < bestMinDistCollectorOPP_Cycles) {
									bestMinDistCollectorOPP_Cycles = minDistCollectorOPP_Cycles;
									bestK_OPP_Cycles = k;
								}
								
								if(minDistCollectorFixOpt < bestMinDistCollectorFixOpt) {
									bestMinDistCollectorFixOpt = minDistCollectorFixOpt;
									bestK_FixOpt = k;
								}
							}
							
						}
						
						//mapping (collector, # of probes)
						//HashMap<Integer,Integer> mapCollectorER = new HashMap<Integer,Integer>();
						//HashMap<Integer,Integer> mapCollectorOPP_Cycles = new HashMap<Integer,Integer>();
						HashMap<Integer,Integer> mapCollectorFixOpt = new HashMap<Integer,Integer>();
						
						/*for(Integer collector: collectors.get(bestK_ER)) {
							//System.out.println("bestK_ER: " + bestK_ER + " collector: " + collector + " collectors size: " + collectors.size());
							mapCollectorER.put(collector, 0);
						}*/
						
						/*for(Integer collector: collectors.get(bestK_OPP_Cycles)) {
							mapCollectorOPP_Cycles.put(collector, 0);
						}*/
						
						for(Integer collector: collectors.get(bestK_FixOpt)) {
							mapCollectorFixOpt.put(collector, 0);
						}
						
						
						// -Metric 2: Collector overhead: # of probes per collector
						//double collectorOverheadER[] = new double[3];
						//double collectorOverheadOPP_Cycles[] = new double[3];
						double collectorOverheadFixOpt[] = new double[3];
						//collectorOverheadER = sts.collectorOverhead(modelER, modelER.cycles, fixOpt, opt, collectors.get(bestK_ER), mapCollectorER, maxProbes, 2);
						//collectorOverheadOPP_Cycles = sts.collectorOverhead(modelER, pathPlanCycles.Q, fixOpt, opt, collectors.get(bestK_OPP_Cycles), mapCollectorOPP_Cycles, maxProbes, 2);
						collectorOverheadFixOpt = sts.collectorOverhead(modelER, pathPlanCycles.Q, fixOpt, opt, collectors.get(bestK_FixOpt), mapCollectorFixOpt, maxProbes, 1);
		
						//String mapCollectorER_collectors = new String();
						//String mapCollectorER_probes = new String();
						/*for(Integer collector: collectors.get(bestK_ER)) {
							mapCollectorER_collectors += ";" + collector;
							mapCollectorER_probes += ";" + mapCollectorER.get(collector);
							//System.out.println("collector: " + collector + " mapCollectorER.get(collector): " + mapCollectorER.get(collector));
						}*/
						
						/*String mapCollectorOPPcycles_collectors = new String();
						String mapCollectorOPPcycles_probes = new String();
						for(Integer collector: collectors.get(bestK_OPP_Cycles)) {
							mapCollectorOPPcycles_collectors += ";" + collector;
							mapCollectorOPPcycles_probes += ";" + mapCollectorOPP_Cycles.get(collector);
						}*/
						
						
						//////////// Fill with FixOpt //////////////
						String mapCollectorFixOpt_collectors = new String();
						String mapCollectorFixOpt_probes = new String();
						for(Integer collector: collectors.get(bestK_FixOpt)) {
							mapCollectorFixOpt_collectors += ";" + collector;
							mapCollectorFixOpt_probes += ";" + mapCollectorFixOpt.get(collector);
						}
						
						// -Metric 3: Average Probe Usage: (min, max, avg)
						//double[] probeUsageER = new double[3];
						//double[] probeUsageOPP_Cycles = new double[3];
						double[] probeUsageFixOpt = new double[3];
						//probeUsageER = sts.probeUsage(fixOpt, opt, maxProbes, modelER.cycles, capacityProbe, 2);
						//probeUsageOPP_Cycles = sts.probeUsage(fixOpt, opt, maxProbes, pathPlanCycles.Q, capacityProbe, 2);
						probeUsageFixOpt = sts.probeUsage(fixOpt, opt, maxProbes, pathPlanCycles.Q, capacityProbe, 1);
						
						// -Metric 4: Device overhead: # of probes/paths pass through devices (min, max, avg)
						//double[] devOverheadER = new double[3];
						//double[] devOverheadOPP_Cycles = new double[3];
						double[] devOverheadFixOpt = new double[3];
						//devOverheadER = sts.devOverhead(fixOpt, opt, modelER.cycles, maxProbes, networkSize, 2);
						//devOverheadOPP_Cycles = sts.devOverhead(fixOpt, opt, pathPlanCycles.Q, maxProbes, networkSize, 2);
						devOverheadFixOpt = sts.devOverhead(fixOpt, opt, pathPlanCycles.Q, maxProbes, networkSize, 1);
						
						// -Metric 5: Link overhead: # of probes/paths pass through links (min, max, avg)
						//double[] linkOverheadER = new double[3];
						//double[] linkOverheadOPP_Cycles = new double[3];
						double[] linkOverheadFixOpt = new double[3];
						//linkOverheadER = sts.linkOverhead(infra, fixOpt, opt, maxProbes, modelER.cycles, networkSize, 2);
						//linkOverheadOPP_Cycles = sts.linkOverhead(infra, fixOpt, opt, maxProbes, pathPlanCycles.Q, networkSize, 2);
						linkOverheadFixOpt = sts.linkOverhead(infra, fixOpt, opt, maxProbes, pathPlanCycles.Q, networkSize, 1);
						
						
						/*System.out.println("ER" + ";" + modelER.cycles.size() + ";" + timeER
								+ ";" + modelER.infra.size + ";" + modelER.infra.telemetryItemsRouter
								+ ";" + modelER.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe 
								+ ";" + minDistCollectorER + ";" + collectorOverheadER[0] + ";" + collectorOverheadER[1]
										+ ";" + collectorOverheadER[2] + ";" + probeUsageER[0] + ";" + probeUsageER[1] 
												+ ";" + probeUsageER[2] + ";" + devOverheadER[0] + ";" + devOverheadER[1]
														+ ";" + devOverheadER[2] + ";" + linkOverheadER[0] + ";" + linkOverheadER[1]
																+ ";" + linkOverheadER[2] + ";" + comb + ";" + modelER.seed + ";"
																+ "COL" + mapCollectorER_collectors + ";" + "#PRB" + mapCollectorER_probes
																+ ";" + minCyclesER + ";" + maxCyclesER + ";" + nodesOfFailure + ";" + "NODES_FAILURE" + nodesFail_ER
																+ ";" + "CHANGES-LINKS" + ";" + changesER[0] + ";" + changesER[1] + ";" + changesER[4]
																+ ";" + changesER[5] + ";" + changesER[8] + ";" + changesER[9] + ";" + "CHANGES-DEV-ITEMS"
																+ ";" + changesER[2] + ";" + changesER[3] + ";" + changesER[6] + ";" + changesER[7]
																+ ";" + changesER[10] + ";" + changesER[11]);*/
						
						
						/*System.out.println("OPPcycles" + ";" + pathPlanCycles.Q.size() + ";" + timeOPPCycles
								+ ";" + pathPlanCycles.infra.size + ";" + pathPlanCycles.infra.telemetryItemsRouter
								+ ";" + pathPlanCycles.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe 
								+ ";" + minDistCollectorOPP_Cycles + ";" + collectorOverheadOPP_Cycles[0] + ";" + collectorOverheadOPP_Cycles[1]
										+ ";" + collectorOverheadOPP_Cycles[2] + ";" + probeUsageOPP_Cycles[0] + ";" + probeUsageOPP_Cycles[1] 
												+ ";" + probeUsageOPP_Cycles[2] + ";" + devOverheadOPP_Cycles[0] + ";" + devOverheadOPP_Cycles[1]
														+ ";" + devOverheadOPP_Cycles[2] + ";" + linkOverheadOPP_Cycles[0] + ";" + linkOverheadOPP_Cycles[1]
																+ ";" + linkOverheadOPP_Cycles[2] + ";" + comb + ";" + pathPlanCycles.seed + ";"
																+ "COL" + mapCollectorOPPcycles_collectors + ";" + "#PRB" + mapCollectorOPPcycles_probes
																+ ";" + minCyclesOPP + ";" + maxCyclesOPP  + ";" + nodesOfFailure + ";" + "NODES_FAILURE" + nodesFail_OPP_Cycles
																+ ";" + "CHANGES-LINKS" + ";" + changesOPP_Cycles[0] + ";" + changesOPP_Cycles[1] + ";" + changesOPP_Cycles[4]
																		+ ";" + changesOPP_Cycles[5] + ";" + changesOPP_Cycles[8] + ";" + changesOPP_Cycles[9] + ";" + "CHANGES-DEV-ITEMS"
																		+ ";" + changesOPP_Cycles[2] + ";" + changesER[3] + ";" + changesOPP_Cycles[6]
																		+ ";" + changesOPP_Cycles[7] + ";" + changesOPP_Cycles[10] + ";" + changesOPP_Cycles[11]);*/
					
						
						System.out.println("FixOpt" + ";" + fixOptSol + ";" + timeFixOpt
								+ ";" + fixOpt.infra.size + ";" + fixOpt.infra.telemetryItemsRouter
								+ ";" + fixOpt.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe 
								+ ";" + minDistCollectorFixOpt + ";" + collectorOverheadFixOpt[0] + ";" + collectorOverheadFixOpt[1]
										+ ";" + collectorOverheadFixOpt[2] + ";" + probeUsageFixOpt[0] + ";" + probeUsageFixOpt[1] 
												+ ";" + probeUsageFixOpt[2] + ";" + devOverheadFixOpt[0] + ";" + devOverheadFixOpt[1]
														+ ";" + devOverheadFixOpt[2] + ";" + linkOverheadFixOpt[0] + ";" + linkOverheadFixOpt[1]
																+ ";" + linkOverheadFixOpt[2] + ";" + comb + ";" + fixOpt.seed + ";"
																+ "COL" + mapCollectorFixOpt_collectors + ";" + "#PRB" + mapCollectorFixOpt_probes);
				
					}
				}//end-if
				else {
					// -Metric 3: Average Probe Usage: (min, max, avg)
					//double[] probeUsageER = new double[3];
					//double[] probeUsageOPP_Cycles = new double[3];
					//probeUsageER = sts.probeUsage(fixOpt, opt, maxProbes, modelER.cycles, capacityProbe, 2);
					//probeUsageOPP_Cycles = sts.probeUsage(fixOpt, opt, maxProbes, pathPlanCycles.Q, capacityProbe, 2);
					
					// -Metric 4: Device overhead: # of probes/paths pass through devices (min, max, avg)
					//double[] devOverheadER = new double[3];
					//double[] devOverheadOPP_Cycles = new double[3];
					//devOverheadER = sts.devOverhead(fixOpt, opt, modelER.cycles, maxProbes, networkSize, 2);
					//devOverheadOPP_Cycles = sts.devOverhead(fixOpt, opt, pathPlanCycles.Q, maxProbes, networkSize, 2);
					
					// -Metric 5: Link overhead: # of probes/paths pass through links (min, max, avg)
					//double[] linkOverheadER = new double[3];
					//double[] linkOverheadOPP_Cycles = new double[3];
					//linkOverheadER = sts.linkOverhead(infra, fixOpt, opt, maxProbes, modelER.cycles, networkSize, 2);
					//linkOverheadOPP_Cycles = sts.linkOverhead(infra, fixOpt, opt, maxProbes, pathPlanCycles.Q, networkSize, 2);
					
					/*System.out.println("ER" + ";" + modelER.cycles.size() + ";" + timeER
							+ ";" + modelER.infra.size + ";" + modelER.infra.telemetryItemsRouter
							+ ";" + modelER.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe 
							+ ";" + "X" + ";" + "X" + ";" + "X"
									+ ";" + "X" + ";" + probeUsageER[0] + ";" + probeUsageER[1] 
											+ ";" + probeUsageER[2] + ";" + devOverheadER[0] + ";" + devOverheadER[1]
													+ ";" + devOverheadER[2] + ";" + linkOverheadER[0] + ";" + linkOverheadER[1]
															+ ";" + linkOverheadER[2] + ";" + "X" + ";" + modelER.seed + ";"
															+ "COL" + "X" + ";" + "#PRB" + "X"
															+ ";" + minCyclesER + ";" + maxCyclesER + ";" + nodesOfFailure + ";" + "NODES_FAILURE" + nodesFail_ER
															+ ";" + "CHANGES-LINKS" + ";" + changesER[0] + ";" + changesER[1] + ";" + changesER[4]
																	+ ";" + changesER[5] + ";" + changesER[8] + ";" + changesER[9] + ";" + "CHANGES-DEV-ITEMS"
																	+ ";" + changesER[2] + ";" + changesER[3] + ";" + changesER[6] + ";" + changesER[7]
																	+ ";" + changesER[10] + ";" + changesER[11]); */
					
					/*System.out.println("OPPcycles" + ";" + pathPlanCycles.Q.size() + ";" + timeOPPCycles
							+ ";" + pathPlanCycles.infra.size + ";" + pathPlanCycles.infra.telemetryItemsRouter
							+ ";" + pathPlanCycles.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe 
							+ ";" + "X" + ";" + "X" + ";" + "X"
									+ ";" + "X" + ";" + probeUsageOPP_Cycles[0] + ";" + probeUsageOPP_Cycles[1] 
											+ ";" + probeUsageOPP_Cycles[2] + ";" + devOverheadOPP_Cycles[0] + ";" + devOverheadOPP_Cycles[1]
													+ ";" + devOverheadOPP_Cycles[2] + ";" + linkOverheadOPP_Cycles[0] + ";" + linkOverheadOPP_Cycles[1]
															+ ";" + linkOverheadOPP_Cycles[2] + ";" + "X" + ";" + pathPlanCycles.seed + ";"
															+ "COL" + "X" + ";" + "#PRB" + "X"
															+ ";" + minCyclesOPP + ";" + maxCyclesOPP + ";" + nodesOfFailure + ";" + "NODES_FAILURE" + nodesFail_OPP_Cycles
															+ ";" + "CHANGES-LINKS" + ";" + changesOPP_Cycles[0] + ";" + changesOPP_Cycles[1] + ";" + changesOPP_Cycles[4]
																	+ ";" + changesOPP_Cycles[5] + ";" + changesOPP_Cycles[8] + ";" + changesOPP_Cycles[9] + ";" + "CHANGES-DEV-ITEMS"
																	+ ";" + changesOPP_Cycles[2] + ";" + changesER[3] + ";" + changesOPP_Cycles[6]
																	+ ";" + changesOPP_Cycles[7] + ";" + changesOPP_Cycles[10] + ";" + changesOPP_Cycles[11]); */
				}
				
				
				//reinsert failure nodes
				for(Pair<Integer,Integer> link : storeLinks) {
					//System.out.println("link: " + "(" + link.first + "," + link.second + ")");
					infra.graph[link.first][link.second] = 1;
				}
				
			}
			
			
		}
			
	}	
				
}

