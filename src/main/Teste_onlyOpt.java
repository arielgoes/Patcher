package main;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.jws.WebParam.Mode;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import heuristics.Cycle;
import heuristics.EdgeRandomization;
import heuristics.FixOptPInt;
import heuristics.OptPathPlan;
import heuristics.Pair;
import heuristics.Tuple;
import heuristics.KCombinations;

public class Teste_onlyOpt {

	public static void main(String[] args) throws IloException, FileNotFoundException, CloneNotSupportedException{
		
		//Parameters
		int networkSize = Integer.parseInt(args[0]);
		int capacityProbe = Integer.parseInt(args[1]); //available space in a given flow (e.g., # of bytes)		
		int maxProbes = Integer.parseInt(args[2]); 
		int telemetryItemsRouter = Integer.parseInt(args[3]); //number of telemetry items per router 
		int maxSizeTelemetryItemsRouter = Integer.parseInt(args[4]); //max size of a given telemetry item (in bytes)
		int combSize = Integer.parseInt(args[5]);
		int n = Integer.parseInt(args[6]);
		
		String pathInstance = "/Users/mcluizelli/eclipse-workspace/INTelemetry/instances/hs/hs1.txt";
		double seed = 123;

		NetworkInfrastructure infra = null; 
		EdgeRandomization modelER = null;
		OptPathPlan pathPlanCycles = null;
		FixOptPInt fixOpt = null;
		AlgorithmOpt opt = null;
	
		infra = new NetworkInfrastructure(networkSize, pathInstance, telemetryItemsRouter, maxSizeTelemetryItemsRouter, (long)seed);
		infra.filePath = pathInstance;
		infra.generateRndTopology(0.5);
		
		int[] array = new int[networkSize];
		for(int i = 0; i < networkSize; i++) {
			array[i] = i;
		}
		
		ArrayList<Integer> sinks = new ArrayList<Integer>();
		for(int k = 0; k < networkSize; k++) {
			sinks.add(k);
		}
		
		Statistics sts = new Statistics();
		int temp[] = new int[2];
		modelER = new EdgeRandomization(infra, capacityProbe, (long) seed, maxProbes, temp, false);
		KCombinations kComb = new KCombinations();
		ArrayList<int[]> collectors = new ArrayList<int[]>();
		
		double timeOpt = System.nanoTime();
		
		opt = new AlgorithmOpt(infra, maxProbes);
		opt.buildCPPTelemetry(maxProbes, capacityProbe, sinks)
		
		System.out.println("Opt" + ";" + opt.cplex.getObjValue());
		System.exit(0);
		
		for(int comb = 1; comb <= combSize; comb++) {
			//item size verification
			int itemSize = Integer.MIN_VALUE;
			for(int k = 0; k < infra.sizeTelemetryItems.length; k++) {
				if(infra.sizeTelemetryItems[k] > itemSize) {
					itemSize = infra.sizeTelemetryItems[k];
				}
			}
			
			//if(itemSize > (capacityProbe - 2)) {
				/*System.out.println("-3" + ";" + "NaN" + ";" 
						+ infra.size + ";" + infra.telemetryItemsRouter + ";" + infra.maxSizeTelemetryItemsRouter 
						+ ";" + infra.seed);
				
				System.out.println("-3" + ";" + "NaN" + ";" 
						+ infra.size + ";" + infra.telemetryItemsRouter + ";" + infra.maxSizeTelemetryItemsRouter 
						+ ";" + infra.seed);
				
				System.out.println("-3" + ";" + "NaN" + ";" 
						+ infra.size + ";" + infra.telemetryItemsRouter + ";" + infra.maxSizeTelemetryItemsRouter 
						+ ";" + infra.seed);
				
				System.out.println("-3" + ";" + "NaN" + ";" 
						+ infra.size + ";" + infra.telemetryItemsRouter + ";" + infra.maxSizeTelemetryItemsRouter 
						+ ";" + infra.seed);
				
				System.out.println("-3" + ";" + "NaN" + ";" 
						+ infra.size + ";" + infra.telemetryItemsRouter + ";" + infra.maxSizeTelemetryItemsRouter 
						+ ";" + infra.seed);*/
			//}
				//else {
				//collectors = kComb.enumKCombos(array, comb);
			
			
				// -Métrica 1
				int bestMinDistCollectorOpt = Integer.MAX_VALUE;
				int minDistCollectorOpt = Integer.MAX_VALUE;
				int bestK_Opt = Integer.MAX_VALUE;
				
				for(int k = 0; k < collectors.size(); k++) {
					//minDistCollectorOpt = sts.transportationOverhead(modelER.cycles, opt, collectors.get(k), modelER, maxProbes, true);
					minDistCollectorOpt = sts.transportationOverhead(modelER, pathPlanCycles.Q, fixOpt, opt, collectors.get(k), maxProbes, 0);
					if(minDistCollectorOpt < bestMinDistCollectorOpt) {
						bestMinDistCollectorOpt = minDistCollectorOpt;
						bestK_Opt = k;
					}
				}
				
				HashMap<Integer,Integer> mapCollectorOpt = new HashMap<Integer,Integer>();
				for(Integer collector: collectors.get(bestK_Opt)) {
					mapCollectorOpt.put(collector, 0);
				}
				
				// -Métrica 2: (sobrecarga do coletor) números de probes por coletor
				double collectorOverheadOpt[] = new double[3];
				//collectorOverheadOpt = sts.collectorOverhead(opt, modelER.cycles, collectors.get(bestK_Opt), modelER, mapCollectorOpt, maxProbes, true);
				collectorOverheadOpt = sts.collectorOverhead(modelER, pathPlanCycles.Q, fixOpt, opt, collectors.get(bestK_Opt), mapCollectorOpt, maxProbes, 0);
				
				
				// -Métrica 4
				
				// -Métrica 5
				double[] probeUsageOpt = new double[3];
				//probeUsageOpt = sts.probeUsage(opt, maxProbes, modelER.cycles, capacityProbe, true);
				probeUsageOpt = sts.probeUsage(fixOpt, opt, maxProbes, pathPlanCycles.Q, capacityProbe, 0);
				
				// -Métrica 6
				double[] devOverheadOpt = new double[3];
				//devOverheadOpt = sts.devOverhead(opt, modelER.cycles, maxProbes, networkSize, true);
				devOverheadOpt = sts.devOverhead(fixOpt, opt, pathPlanCycles.Q, maxProbes, networkSize, 0);
				
				// -Métrica 7
				double[] linkOverheadOpt = new double[3];
				//linkOverheadOpt = sts.linkOverhead(infra, opt, maxProbes, modelER.cycles, networkSize, true);
				linkOverheadOpt = sts.linkOverhead(infra, fixOpt, opt, maxProbes, pathPlanCycles.Q, networkSize, 0);
				
				System.out.println("Opt" + ";" + opt.cplex.getObjValue() + ";" + (System.nanoTime() - timeOpt)*0.000000001 
						+ ";" + opt.infra.size + ";" + opt.infra.telemetryItemsRouter
						+ ";" + opt.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe 
						+ ";" + bestMinDistCollectorOpt + ";" + collectorOverheadOpt[0] + ";" + collectorOverheadOpt[1]
						+ ";" + collectorOverheadOpt[2] + ";" + probeUsageOpt[0] + ";" + probeUsageOpt[1] 
						+ ";" + probeUsageOpt[2] + ";" + devOverheadOpt[0] + ";" + devOverheadOpt[1]
						+ ";" + devOverheadOpt[2] + ";" + linkOverheadOpt[0] + ";" + linkOverheadOpt[1]
						+ ";" + linkOverheadOpt[2] + ";" + comb + ";" + opt.infra.seed);
				
				
				//opt.printSolution();		
			//}//else
		}//for
		
	}
}