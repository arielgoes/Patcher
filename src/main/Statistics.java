package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import heuristics.Cycle;
import heuristics.EdgeRandomization;
import heuristics.FixOptPInt;
import heuristics.OptPathPlan;
import heuristics.Pair;
import ilog.concert.IloException;

public class Statistics {
	int maxProbes;
	int networkSize;
	int capacityProbe;
	NetworkInfrastructure infra;
	int collector;
	
	//results
	public String er = "";
	public String optimalER = "";
	public String infocomAriel = "";
	public String result;
	
	public Statistics() {
		
	}
	
	//parte do ariel
	// -Métrica 1: Sobrecarga de dados: distância coletor (mínima)
	//mode 0: opt, mode 1: fixOpt, mode 2: heur
	
	public int transportationOverhead(EdgeRandomization modelER, ArrayList<Cycle> paths, FixOptPInt fixOpt, AlgorithmOpt opt, int[] collectors, int maxProbes, int mode) {
		int minDistCollector = 0;
		
		if(mode < 0 || mode > 2) {
			System.out.println("ERROR: \"transportationOverHead\" invalid option!");
			return -1;
		}else if(mode == 0) { //opt case
			ArrayList<Cycle> pathsOpt = new ArrayList<Cycle>();
			for(int i = 0; i < maxProbes; i++) {
				boolean canAdd = false;
				Cycle c = new Cycle();
				HashSet<Integer> nodes = new HashSet<Integer>();
				
				for(int j = 0; j < opt.infra.size; j++) {
					for(int k = 0; k < opt.infra.size; k++) {
						if(opt.xMetrics[i][j][k] == 1) {
							canAdd = true;
							nodes.add(j);
							nodes.add(k);
							//System.out.printf("opt.xMetrics[%d][%d][%d]: %d", i, j, k, (int)opt.xMetrics[i][j][k]);
							//System.out.println();
						}
					}
				}
				
				if(canAdd) {
					ArrayList<Integer> cnodes = new ArrayList<>(nodes);
					c.nodes = cnodes;
					//System.out.println("c.nodes: " + c.nodes);
					pathsOpt.add(c);
					nodes = null;
					cnodes = null;
					c = null;
					canAdd = false;
				}
			}
		
			for(int p = 0; p < pathsOpt.size(); p++) {
				int minDist = Integer.MAX_VALUE;
				
				for(Integer collector: collectors) {
					for(int n = 0; n < pathsOpt.get(p).nodes.size(); n++) {
						int node = pathsOpt.get(p).nodes.get(n);
					
						if(node == collector) {
							minDist = 0;
						}
						else if(node != collector && modelER.costShortestPath[node][collector] < minDist) {
							minDist = modelER.costShortestPath[node][collector]; // metric 1
						}
					}				
				}
				minDistCollector += minDist;
			}

			pathsOpt = null;
			
		}else if(mode == 1) { //fixOpt case
			ArrayList<Cycle> pathsFixOpt = new ArrayList<Cycle>();
			for(int i = 0; i < fixOpt.maxProbes; i++) {
				boolean canAdd = false;
				Cycle c = new Cycle();
				HashSet<Integer> nodes = new HashSet<Integer>();
				
				for(int j = 0; j < fixOpt.infra.size; j++) {
					for(int k = 0; k < fixOpt.infra.size; k++) {
						if(fixOpt.xMetrics[i][j][k] == 1) {
							canAdd = true;
							nodes.add(j);
							nodes.add(k);
							//System.out.printf("opt.xMetrics[%d][%d][%d]: %d", i, j, k, (int)fixOpt.xMetrics[i][j][k]);
							//System.out.println();
						}
					}
				}
				
				if(canAdd) {
					ArrayList<Integer> cnodes = new ArrayList<>(nodes);
					c.nodes = cnodes;
					//System.out.println("c.nodes: " + c.nodes);
					pathsFixOpt.add(c);
					nodes = null;
					cnodes = null;
					c = null;
					canAdd = false;
				}
			}
		
			for(int p = 0; p < pathsFixOpt.size(); p++) {
				int minDist = Integer.MAX_VALUE;
				
				for(Integer collector: collectors) {
					for(int n = 0; n < pathsFixOpt.get(p).nodes.size(); n++) {
						int node = pathsFixOpt.get(p).nodes.get(n);
					
						if(node == collector) {
							minDist = 0;
						}
						else if(node != collector && modelER.costShortestPath[node][collector] < minDist) {
							minDist = modelER.costShortestPath[node][collector]; // metric 1
						}
					}				
				}
				minDistCollector += minDist;
			}

			pathsFixOpt = null;
			
		}else if(mode >= 2){ //heur case
			for(int p = 0; p < paths.size(); p++) {
				int minDist = Integer.MAX_VALUE;
				
				for(Integer collector: collectors) {
					for(int n = 0; n < paths.get(p).nodes.size(); n++) {
						int node = paths.get(p).nodes.get(n);
						
						if(node == collector) {
							minDist = 0;
						}
						else if(node != collector && modelER.costShortestPath[node][collector] < minDist) {
							minDist = modelER.costShortestPath[node][collector]; // metric 1
						}
					}				
				}
				minDistCollector += minDist;
			}
			
		}
		
		return minDistCollector;
	}
	
	
	// -Métrica 2: (sobrecarga do coletor) número de probes por coletor
	//mode 0: opt, mode 1: fixOpt, mode 2: heur
	public double[] collectorOverhead(EdgeRandomization modelER, ArrayList<Cycle> paths, FixOptPInt fixOpt, AlgorithmOpt opt, int[] collectors, HashMap<Integer, Integer> mapCollector, int maxProbes, int mode) {
		
		double[] collectorOverhead = new double[3];
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		double avg = 0;
		int closestDepot = -1;
		
		if(mode < 0 || mode > 2) {
			System.out.println("ERROR: \"collectorOverhead\" invalid option!");
			for(int i = 0; i < collectorOverhead.length; i++) {
				collectorOverhead[i] = -1;
				return collectorOverhead;
			}
		}
		else if(mode == 0) { //opt case
			ArrayList<Cycle> pathsOpt = new ArrayList<Cycle>();
			for(int i = 0; i < maxProbes; i++) {
				boolean canAdd = false;
				Cycle c = new Cycle();
				HashSet<Integer> nodes = new HashSet<Integer>();
				
				for(int j = 0; j < opt.infra.size; j++) {
					for(int k = 0; k < opt.infra.size; k++) {
						if(opt.xMetrics[i][j][k] == 1) {
							canAdd = true;
							nodes.add(j);
							nodes.add(k);
							//System.out.printf("opt.xMetrics[%d][%d][%d]: %d", i, j, k, (int)opt.xMetrics[i][j][k]);
							//System.out.println();
						}
					}
				}
				
				if(canAdd) {
					ArrayList<Integer> cnodes = new ArrayList<>(nodes);
					c.nodes = cnodes;
					//System.out.println("c.nodes: " + c.nodes);
					pathsOpt.add(c);
					nodes = null;
					cnodes = null;
					c = null;
					canAdd = false;
				}
			}
			
			
			for(int p = 0; p < pathsOpt.size(); p++) {	
				int minDist = Integer.MAX_VALUE;
				
				for(Integer collector: collectors) {
					for(int n = 0; n < pathsOpt.get(p).nodes.size(); n++) {
						int node = pathsOpt.get(p).nodes.get(n);
						//System.out.println("node: " + node);
						
						if(node == collector) {
							minDist = 0;
							closestDepot = collector;
							//System.out.println("node == collector: " + node);
						}
						else if(node != collector && modelER.costShortestPath[node][collector] < minDist) {
							minDist = modelER.costShortestPath[node][collector]; // metric 1
							closestDepot = collector;
						}
					}
				}
				mapCollector.put(closestDepot, mapCollector.get(closestDepot) + 1);
				
			}
			
			for(Map.Entry<Integer, Integer> entry: mapCollector.entrySet()) {
				if(entry.getValue() < min) {
					min = entry.getValue();
				}
				
				if(entry.getValue() > max) {
					max = entry.getValue();
				}
				
				avg += entry.getValue();
			}
			avg /= mapCollector.size();

			collectorOverhead[0] = min;
			collectorOverhead[1] = max;
			collectorOverhead[2] = avg;
			
		}
		else if (mode == 1) { //fixOpt case
			ArrayList<Cycle> pathsFixOpt = new ArrayList<Cycle>();
			for(int i = 0; i < fixOpt.maxProbes; i++) {
				boolean canAdd = false;
				Cycle c = new Cycle();
				HashSet<Integer> nodes = new HashSet<Integer>();
				
				for(int j = 0; j < fixOpt.infra.size; j++) {
					for(int k = 0; k < fixOpt.infra.size; k++) {
						if(fixOpt.xMetrics[i][j][k] == 1) {
							canAdd = true;
							nodes.add(j);
							nodes.add(k);
							//System.out.printf("opt.xMetrics[%d][%d][%d]: %d", i, j, k, (int)opt.xMetrics[i][j][k]);
							//System.out.println();
						}
					}
				}
				
				if(canAdd) {
					ArrayList<Integer> cnodes = new ArrayList<>(nodes);
					c.nodes = cnodes;
					//System.out.println("c.nodes: " + c.nodes);
					pathsFixOpt.add(c);
					nodes = null;
					cnodes = null;
					c = null;
					canAdd = false;
				}
			}
			
			
			for(int p = 0; p < pathsFixOpt.size(); p++) {	
				int minDist = Integer.MAX_VALUE;
				
				for(Integer collector: collectors) {
					
					for(int n = 0; n < pathsFixOpt.get(p).nodes.size(); n++) {
						int node = pathsFixOpt.get(p).nodes.get(n);
						
						if(node == collector) {
							minDist = 0;
							closestDepot = collector;
						}
						else if(node != collector && modelER.costShortestPath[node][collector] < minDist) {
							minDist = modelER.costShortestPath[node][collector]; // metric 1
							closestDepot = collector;
						}
					}
				}
				mapCollector.put(closestDepot, mapCollector.get(closestDepot) + 1);
				
			}
			
			for(Map.Entry<Integer, Integer> entry: mapCollector.entrySet()) {
				if(entry.getValue() < min) {
					min = entry.getValue();
				}
				
				if(entry.getValue() > max) {
					max = entry.getValue();
				}
				
				avg += entry.getValue();
			}
			avg /= mapCollector.size();

			collectorOverhead[0] = min;
			collectorOverhead[1] = max;
			collectorOverhead[2] = avg;
		}
		
		else if(mode == 2) { //heur case
			for(int p = 0; p < paths.size(); p++) {
								
				int minDist = Integer.MAX_VALUE;
				
				for(Integer collector: collectors) {
					for(int n = 0; n < paths.get(p).nodes.size(); n++) {
						int node = paths.get(p).nodes.get(n);
						
						if(node == collector) {
							minDist = 0;
							closestDepot = collector;
						}
						else if(node != collector && modelER.costShortestPath[node][collector] < minDist) {
							minDist = modelER.costShortestPath[node][collector]; // metric 1
							closestDepot = collector;
						}
					}
				}
				mapCollector.put(closestDepot, mapCollector.get(closestDepot) + 1);	
			}
			
			//System.out.println(Arrays.asList(mapCollector)); // method 1
			for(Map.Entry<Integer, Integer> entry: mapCollector.entrySet()) {
				if(entry.getValue() < min) {
					min = entry.getValue();
				}
				
				if(entry.getValue() > max) {
					max = entry.getValue();
				}
				
				avg += entry.getValue();
			}
			avg /= mapCollector.size();
			
			collectorOverhead[0] = min;
			collectorOverhead[1] = max;
			collectorOverhead[2] = avg;

		}
		
		return collectorOverhead;
	}
	
	
	// -Métrica 5: Utilização média dos probes (min, max, medio)
	//mode 0: opt, mode 1: fixOpt, mode 2: heur
	public double[] probeUsage(FixOptPInt fixOpt, AlgorithmOpt opt, int maxProbes, ArrayList<Cycle> paths, int capacityProbe, int mode) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		double avg = 0;
		double[] statistics = new double[3];
		
		if(mode == 0) { //opt case
			int probes = 0;
			for(int i = 0; i < maxProbes; i++) {
				int cap = 0;
				for(int j = 0; j < opt.infra.size; j++) {
					for(int k = 0; k < opt.infra.size; k++) {
						System.out.printf("opt.xMetrics[%d][%d][%d]: %d", i, j, k, (int)opt.xMetrics[i][j][k]);
						System.out.println();
						if(opt.xMetrics[i][j][k] == 1) {
							cap++;
						}
					}
				}
				
				for(int j = 0; j < opt.infra.telemetryItemsRouter; j++) {
					for(int k = 0; k < opt.infra.size; k++) {
						if(opt.zMetrics[i][j][k] == 1) {
							cap += opt.infra.sizeTelemetryItems[j];
						}
					}
				}
								
				if(cap != 0) {	
					probes++;
					avg += cap;
					if(cap > max) {
						max = cap;
					}
					if(cap < min) {
						min = cap;
					}
				}
			}
			avg = avg/(capacityProbe * probes);
		}
		else if(mode == 1) { //fixOpt case
			int probes = 0;
			for(int i = 0; i < fixOpt.maxProbes; i++) {
				int cap = 0;
				for(int j = 0; j < fixOpt.infra.size; j++) {
					for(int k = 0; k < fixOpt.infra.size; k++) {
						if(fixOpt.xMetrics[i][j][k] == 1) {
							cap++;
						}
					}
				}
				
				for(int j = 0; j < fixOpt.infra.telemetryItemsRouter; j++) {
					for(int k = 0; k < fixOpt.infra.size; k++) {
						if(fixOpt.zMetrics[i][j][k] == 1) {
							cap += fixOpt.infra.sizeTelemetryItems[j];
						}
					}
				}
								
				if(cap != 0) {	
					probes++;
					avg += cap;
					if(cap > max) {
						max = cap;
					}
					if(cap < min) {
						min = cap;
					}
				}
			}
			
			avg = avg/(capacityProbe * probes);
			
		}
		else if(mode == 2){ //heur case
			for(int p = 0; p < paths.size(); p++) {
				if(paths.get(p).capacity_used < min) {
					min = paths.get(p).capacity_used;
				}
				
				if(paths.get(p).capacity_used > max) {
					max = paths.get(p).capacity_used;
				}
				
				avg += paths.get(p).capacity_used;
			}
			
			avg = avg/(capacityProbe * paths.size());

		}
		
		statistics[0] = min;
		statistics[1] = max;
		statistics[2] = avg;
		
		return statistics;
	}
	
	
	// -Métrica 6: Sobrecarga de dispositivo: quantos probes/caminhos passam pelo dispositivo (min, max, medio)
	//mode 0: opt, mode 1: fixOpt, mode 2: heur
	public double[] devOverhead(FixOptPInt fixOpt, AlgorithmOpt opt, ArrayList<Cycle> paths, int maxProbes, int networkSize, 
			int mode) {
		int[] devicesUsage = new int[networkSize];
		double[] devOverhead = new double[3];
		
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		double avg = 0;

		
		if(mode == 0) { //opt case
			for(int i = 0; i < maxProbes; i++) {
				for(int j = 0; j < opt.infra.size; j++) {
					for(int k = 0; k < opt.infra.size; k++) {
						if(opt.xMetrics[i][j][k] == 1) {
							devicesUsage[k]++;
						}
					}
				}
			}
			
			for(int d = 0; d < devicesUsage.length; d++) {
				avg += devicesUsage[d];
				if(devicesUsage[d] < min) {
					min = devicesUsage[d];
				}
				if(devicesUsage[d] > max) {
					max = devicesUsage[d];
				}
			}
			avg /= devicesUsage.length;
			
		}
		
		else if(mode == 1){ //fixOpt case
			for(int i = 0; i < fixOpt.maxProbes; i++) {
				for(int j = 0; j < fixOpt.infra.size; j++) {
					for(int k = 0; k < fixOpt.infra.size; k++) {
						if(fixOpt.xMetrics[i][j][k] == 1) {
							devicesUsage[k]++;
						}
					}
				}
			}
			
			for(int d = 0; d < devicesUsage.length; d++) {
				avg += devicesUsage[d];
				if(devicesUsage[d] < min) {
					min = devicesUsage[d];
				}
				if(devicesUsage[d] > max) {
					max = devicesUsage[d];
				}
			}
			avg /= devicesUsage.length;
		}
		
		else if(mode == 2){ //heur case
			for(int p = 0; p < paths.size(); p++) {
				for(int d = 0; d < paths.get(p).nodes.size(); d++) {
					devicesUsage[paths.get(p).nodes.get(d)]++;
				}
			}
			
			for(int d = 0; d < devicesUsage.length; d++) {
				if(devicesUsage[d] < min) {
					min = devicesUsage[d];
				}
				if(devicesUsage[d] > max) {
					max = devicesUsage[d];
				}
				
				avg += devicesUsage[d];
			}
			avg /= devicesUsage.length; 
		}
		
		
		devOverhead[0] = min;
		devOverhead[1] = max;
		devOverhead[2] = avg;
		
		return devOverhead;
	}
	
	
	// -Métrica 7: Sobrecarga de enlace: quantos probes/caminhos passam pelo enlace (min, max, medio)
	
	public double[] linkOverhead(NetworkInfrastructure infra, FixOptPInt fixOpt, AlgorithmOpt opt, int maxProbes,
			ArrayList<Cycle> paths, int networkSize, int mode) {
		
		double [] linkOverhead = new double[3];
		int[][] linksUsage = new int[networkSize][networkSize];
		int numEdges = 0;

		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		double avg = 0;
		
		if(mode == 0) { //opt case
			for(int i = 0; i < maxProbes; i++) {
				for(int j = 0; j < opt.infra.size; j++) {
					for(int k = 0; k < opt.infra.size; k++) {
						if(opt.xMetrics[i][j][k] == 1) {
							numEdges++;
							linksUsage[j][k]++;
						}
					}
				}
			}
			
		}
		
		else if(mode == 1) { //fixOpt case
			for(int i = 0; i < fixOpt.maxProbes; i++) {
				for(int j = 0; j < fixOpt.infra.size; j++) {
					for(int k = 0; k < fixOpt.infra.size; k++) {
						if(fixOpt.xMetrics[i][j][k] == 1) {
							numEdges++;
							linksUsage[j][k]++;
						}
					}
				}
			}
		}
		
		else if(mode == 2){ //heur case
			for(int p = 0; p < paths.size(); p++) {
				for(Pair<Integer,Integer> e: paths.get(p).links) {
					linksUsage[e.first][e.second]++;
					linksUsage[e.second][e.first]++;
				}
			}
		}
		
		for(int i = 0; i < networkSize; i++) {
			for(int j = 0; j < networkSize; j++) {
				if(linksUsage[i][j] > 0) {	
					numEdges++;
					avg += linksUsage[i][j];
				}
				
				if(linksUsage[i][j] > 0 && linksUsage[i][j] < min) {
					min = linksUsage[i][j];
				}
				
				if(linksUsage[i][j] > 0 && linksUsage[i][j] > max) {
					max = linksUsage[i][j];
				}
			}
		}
		avg /= numEdges;	
		
		linkOverhead[0] = min;
		linkOverhead[1] = max;
		linkOverhead[2] = avg;
		
		return linkOverhead;		
	}
	
	
	//compare to old solution and collect changes (# of items reallocated)
	public double[] changesOnLinksAndItems(ArrayList<Cycle> Q_old, ArrayList<Cycle> Q) {
		double minDiffNumLinks = Integer.MAX_VALUE;
		double minDiffLinks = Integer.MAX_VALUE;
		double minDiffNumItems = Integer.MAX_VALUE;
		double minDiffItems = Integer.MAX_VALUE;
		
		double maxDiffNumLinks = Integer.MIN_VALUE;
		double maxDiffLinks = Integer.MIN_VALUE;
		double maxDiffNumItems = Integer.MIN_VALUE;
		double maxDiffItems = Integer.MIN_VALUE;
		
		double avgDiffNumLinks = 0;
		double avgDiffLinks = 0;
		double avgDiffNumItems = 0;
		double avgDiffItems = 0;
		
		
		double[] changesOnLinksAndItems = new double[12];
		
		double size = 0;
		for(int i = 0; i < Q_old.size(); i++) {
			boolean foundMyBrother = false;
			for(int j = 0; j < Q.size(); j++) {
				//System.out.println("old id: " + Q_old.get(i).id + " new id: " + Q.get(j).id);
				if(Q_old.get(i).id == Q.get(j).id) { //compare modifications to the cycle
					size++;
					
					//differences between the links (before and after failures)
					int diffNumLinks = 0;
					
					//compare links size
					if(Q_old.get(i).links.size() > Q.get(j).links.size()) {
						diffNumLinks = Q_old.get(i).links.size() - Q.get(j).links.size();	
					}else {
						diffNumLinks = Q.get(j).links.size() - Q_old.get(i).links.size();
					}
					
					//get min max avg
					if(diffNumLinks < minDiffNumLinks) {
						minDiffNumLinks = diffNumLinks;
					}
					if(diffNumLinks > maxDiffNumLinks) {
						maxDiffNumLinks = diffNumLinks;
					}
					avgDiffNumLinks += diffNumLinks;
					
					//compare link by link
					int diffLinks = 0;
					if(Q_old.get(i).links.size() > Q.get(j).links.size()) {
						for(int n = 0; n < Q.get(j).links.size(); n++) {
							if(Q_old.get(i).links.contains(Q.get(j).links.get(n))) {
								diffLinks++;
								continue;
							}
						}
						diffLinks = Q_old.get(i).links.size() - diffLinks;	
					}else {
						for(int n = 0; n < Q_old.get(i).links.size(); n++) {
							if(Q.get(j).links.contains(Q_old.get(i).links.get(n))) {
								diffLinks++;
								continue;
							}
						}
						diffLinks = Q.get(j).links.size() - diffLinks;
					}
					
					//get min max avg
					if(diffLinks < minDiffLinks) {
						minDiffLinks = diffLinks;
					}
					if(diffLinks > maxDiffLinks) {
						maxDiffLinks = diffLinks;
					}
					avgDiffLinks += diffLinks;
					
					//differences between the items (before and after failures)
					int diffNumItems = 0;
					
					//compare items size
					if(Q_old.get(i).itemPerCycle.size() > Q.get(j).itemPerCycle.size()) {
						diffNumItems = Q_old.get(i).itemPerCycle.size() - Q.get(j).itemPerCycle.size();	
					}else {
						diffNumItems = Q.get(j).itemPerCycle.size() - Q_old.get(i).itemPerCycle.size();
					}
					
					//get min max avg
					if(diffNumItems < minDiffNumItems) {
						minDiffNumItems = diffNumItems;
					}
					if(diffNumItems > maxDiffNumItems) {
						maxDiffNumItems = diffNumItems;
					}
					avgDiffNumItems += diffNumItems;
					
					//compare item by item
					int diffItems = 0;
					if(Q_old.get(i).itemPerCycle.size() > Q.get(j).itemPerCycle.size()) {						
						for(int n = 0; n < Q.get(j).itemPerCycle.size(); n++) {
							if(Q_old.get(i).itemPerCycle.contains(Q.get(j).itemPerCycle.get(n))) {
								diffItems++;
								continue;
							}
						}
						diffItems = Q_old.get(i).itemPerCycle.size() - diffItems;
					}else {
						for(int n = 0; n < Q_old.get(i).itemPerCycle.size(); n++) {
							if(Q.get(j).itemPerCycle.contains(Q_old.get(i).itemPerCycle.get(n))) {
								diffItems++;
								continue;
							}
						}
						diffItems = Q.get(j).itemPerCycle.size() - diffItems;
					}
					
					//get min max avg
					if(diffItems < minDiffItems) {
						minDiffItems = diffItems;
					}
					if(diffItems > maxDiffItems) {
						maxDiffItems = diffItems;
					}
					avgDiffItems += diffItems;
					
					
					/*System.out.println("Q_old links: " + Q_old.get(i).links);
					System.out.println("Q links: " + Q.get(j).links);
					System.out.println("diffNumLinks: " + diffNumLinks);
					System.out.println("diffLinks: " + diffLinks);
					System.out.println("-----");
					System.out.println("Q_old dev-items: " + Q_old.get(i).itemPerCycle);
					System.out.println("Q dev-items: " + Q.get(j).itemPerCycle);
					System.out.println("diffNumItems: " + diffNumItems);
					System.out.println("diffItems: " + diffItems);
					System.out.println();*/
					
					foundMyBrother = true;
					break;
				}
			}
			if(foundMyBrother) { //stop looking, because you are found its brother's cycle
				continue;
			}
		}
		
		//System.out.println("size: " + size);
		changesOnLinksAndItems[0] = minDiffNumLinks;
		changesOnLinksAndItems[1] = minDiffLinks;
		changesOnLinksAndItems[2] = minDiffNumItems;
		changesOnLinksAndItems[3] = minDiffItems;
		
		changesOnLinksAndItems[4] = maxDiffNumLinks;
		changesOnLinksAndItems[5] = maxDiffLinks;
		changesOnLinksAndItems[6] = maxDiffNumItems;
		changesOnLinksAndItems[7] = maxDiffItems;
		
		changesOnLinksAndItems[8] = avgDiffNumLinks/size;
		changesOnLinksAndItems[9] = avgDiffLinks/size;
		changesOnLinksAndItems[10] = avgDiffNumItems/size;
		changesOnLinksAndItems[11] = avgDiffItems/size;
		
		/*for(int i = 0; i < changesOnLinksAndItems.length; i++) {
			System.out.println("changesOnLinksAndItems[" + i + "]: " + changesOnLinksAndItems[i]);
		}*/
		
		
		return changesOnLinksAndItems;
	}
	
}
