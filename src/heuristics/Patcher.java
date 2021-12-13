package heuristics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import main.NetworkInfrastructure;

public class Patcher {
	
	public Patcher() {
		
	}
	
	public void reconstructPaths(NetworkInfrastructure infra, ArrayList<Cycle> Q, ArrayList<Integer> failureNodes,
			ArrayList<Pair<Integer,Integer>> storeLinks, int[] nodes, int capacityProbe) {
		
		//failure links
		ArrayList<Pair<Integer,Integer>> links_failure = new ArrayList<Pair<Integer, Integer>>();
		
		//failure dev-items
		ArrayList<Tuple> dev_items_failure = new ArrayList<Tuple>();
		
		//items unsatisfied to reallocate
		ArrayList<Tuple> dev_items_u = new ArrayList<Tuple>();
		
		
		//Step 0: Gather items and links that must be re-satisfied (pre-processing)
		//remove path links and items containing the identified failed nodes
		for(int n = 0; n < failureNodes.size(); n++) {
			
			//select a path
			for(int i = 0; i < Q.size(); i++) {
				int counter = 0;
				
				//traverse the links
				for(int j = 0; j < Q.get(i).links.size(); j++) {
					//System.out.println("failureNodes: " + failureNodes.get(n) + " (" + Q.get(i).links.get(j).first + "," + Q.get(i).links.get(j).second + ")");
					if(Q.get(i).links.get(j).first == failureNodes.get(n) || Q.get(i).links.get(j).second == failureNodes.get(n)) {
						counter++;
						
						if(!links_failure.contains(Q.get(i).links.get(j))) { //avoid duplicates
							links_failure.add(Q.get(i).links.get(j)); //unsatisfied link
						}
					}
				}				
				
				if(counter > 0) {
					//remove items from failure node
					for(int j = 0; j < Q.get(i).itemPerCycle.size(); j++) {
						if(Q.get(i).itemPerCycle.get(j).getDevice() == failureNodes.get(n)) {
							dev_items_failure.add(Q.get(i).itemPerCycle.get(j));
						}
					}
				}
				
			}
		}

		//pick up the weight of the lightest item
		int minItemWeight = Integer.MAX_VALUE;
		for(int i = 0; i < infra.sizeTelemetryItems.length; i++) {
			if(infra.sizeTelemetryItems[i] < minItemWeight) {
				minItemWeight = infra.sizeTelemetryItems[i];
			}
		}

		//remove links containing nodes of failure
		for(int i = 0; i < Q.size(); i++) {
			Q.get(i).links.removeAll(links_failure);
			Q.get(i).itemPerCycle.removeAll(dev_items_failure);
		}
		this.updateCapacity(infra, Q);
		
		//remove underused paths (in general, try to find another path which has all links from the first one. Then, exclude the first one and try to 
		//reallocate its items)
		ArrayList<Cycle> toRemove = new ArrayList<Cycle>();
		this.removeUnderusedPaths(infra, toRemove, Q, dev_items_u);
		
		//Step 1: Try to create amendments and reconstruct cycles
		//get start and end points besides the failure nodes
		this.createAmendments(infra, Q, failureNodes, nodes);
		updateCapacity(infra, Q);
		
		//order cycles in reverse order based on 'capacity_used' attribute
		//Collections.sort(Q, Collections.reverseOrder());
		
		//Step 2: Check capacity constraints and reconstruct cycles as long as necessary
		this.assureCapacityConstraints(Q, dev_items_u);
		updateCapacity(infra, Q);

		//Try to reinsert items
		this.reinsertItems(infra, Q, dev_items_u, minItemWeight);
		this.updateCapacity(infra, Q);
		toRemove.clear();

		//Check if another cycle visits all the links of the cycle collecting no items
		for(int i = 0; i < Q.size(); i++) {
			if(Q.get(i).itemPerCycle.size() > 0) {
				continue;
			}
			int hasAllLinksPath = 0;
			for(int j = 0; j < Q.size(); j++) {
				if(i != j) { //exclude cycles without items already satisfied by other cycles 
					//System.out.println("path i: " + Q.get(i).links + " path j: " + Q.get(j).links);

					for(int l = 0; l < Q.get(i).links.size(); l++) {
						if(Q.get(j).links.contains(Q.get(i).links.get(l))) {
							hasAllLinksPath++;
						}
					}
				}
				
				if(hasAllLinksPath == Q.get(i).links.size()) {
					//System.out.println("hasAllLinks: " + hasAllLinks + " links size: " + Q.get(i).links.size());
					break;
				}
			}
			
			toRemove.add(Q.get(i));
		}		
		Q.removeAll(toRemove);
		
		//System.out.println("unsatisfied items (dev_items_u) before: " + Arrays.asList(dev_items_u));
		
		//Step 3: If necessary, create new paths to satisfy items
		if(!dev_items_u.isEmpty()) {
			int maxSizePath = Integer.MIN_VALUE;
			for(int i = 0; i < Q.size(); i++) {
				if(Q.get(i).links.size() > maxSizePath) {
					maxSizePath = Q.get(i).links.size();
				}
			}
			
			this.createRemainingPaths(infra, Q, dev_items_u, failureNodes, capacityProbe, maxSizePath, minItemWeight, storeLinks, nodes);
		}
		
		//System.out.println("unsatisfied items (dev_items_u) after: " + Arrays.asList(dev_items_u));
		
		this.updateNodes(Q);
		
	}
	
	
	//get start and end points besides the failure nodes
	public void createAmendments(NetworkInfrastructure infra, ArrayList<Cycle> Q, ArrayList<Integer> failureNodes, int[] nodes) {
		
		//fetch amendments at the beginning and/or and of the cycle, besides amendments along the whole cycle
		for(int i = 0; i < Q.size(); i++) {
			
			if(Q.get(i).links.size() == 0) {
				continue;
			}
			
			int start = Q.get(i).links.get(0).first;
			int end = Q.get(i).links.get(Q.get(i).links.size() - 1).second;
			ArrayList<Integer> shortPath = new ArrayList<Integer>();
			
			//amendment at the beginning of the 'broken' cycle
			if(start != end) {
				nodes[0] = end;
				nodes[1] = start;
				shortPath = infra.getShortestPath2(nodes);
				
				int newStart = shortPath.indexOf(end);
				if(newStart != -1) {
					for(int k = newStart; k < shortPath.size() - 1; k++) {
						//System.out.println("-    " + shortPath.get(k) + " " + shortPath.get(k+1));
						Pair<Integer,Integer> link = Pair.create(shortPath.get(k), shortPath.get(k+1));
						Q.get(i).links.add(0, link);
					}
				}
				
				start = Q.get(i).links.get(0).first;
				end = Q.get(i).links.get(Q.get(i).links.size() - 1).second;
				
				//if it is not enough: fetch amendments at the end of the path
				if(start != end) {
					shortPath = new ArrayList<Integer>();
					nodes[0] = start;
					nodes[1] = end;
					shortPath = infra.getShortestPath2(nodes);
					newStart = shortPath.indexOf(start);
					if(newStart != -1) {
						for(int k = newStart; k < shortPath.size() - 1; k++) {
							Pair<Integer,Integer> link = Pair.create(shortPath.get(k), shortPath.get(k+1));
							Q.get(i).links.add(link);
						}
					}
				}
			}
			
			//verify cycles and search for remaining amendments (if any...)
			for(int j = 0; j < Q.get(i).links.size() - 1; j++) {
				if(Q.get(i).links.get(j).second != Q.get(i).links.get(j+1).first) {
					//shortPath = new ArrayList<Integer>();
					nodes[0] = Q.get(i).links.get(j).second;
					nodes[1] = Q.get(i).links.get(j+1).first;
					shortPath = infra.getShortestPath2(nodes);
					
					//reconstruct the amendment path backwards to insert it into the original path's ArrayList
					for(int k = shortPath.size() - 1; k > 0; k--) {
						//System.out.println("k: " + k);
						Pair<Integer,Integer> link = Pair.create(shortPath.get(k-1), shortPath.get(k));
						Q.get(i).links.add(j+1, link);
					}
				}
			}
		}
	}
	
	
	//Try to reinsert items
	public void reinsertItems(NetworkInfrastructure infra, ArrayList<Cycle> Q, ArrayList<Tuple> dev_items_u, int minItemWeight) {
		ArrayList<Tuple> resatisfiedItems = new ArrayList<Tuple>();
		for(int i = 0; i < Q.size(); i++) {
			if(Q.get(i).capacity - Q.get(i).capacity_used < minItemWeight) {
				continue;
			}else {
				for(Tuple di : dev_items_u) {
					boolean hasDev = false;
					
					//check whether or not the path passes through the device
					for(Pair<Integer,Integer> links : Q.get(i).links) {
						if(links.first == di.getDevice() || links.second == di.getDevice()) {
							//System.out.println("link: " + links.first + "," + links.second + " -> dev: " + di.getDevice());
							hasDev = true;
							break;
						}
					}
					
					if(hasDev && !Q.get(i).itemPerCycle.contains(di)) {
						if(Q.get(i).capacity_used + infra.sizeTelemetryItems[di.item] <= Q.get(i).capacity) {
							//System.out.println("before: " + Q.get(i).capacity_used + " after: " + (Q.get(i).capacity_used + infra.sizeTelemetryItems[di.item]));
							Q.get(i).capacity_used += infra.sizeTelemetryItems[di.item];
							Q.get(i).itemPerCycle.add(di);
							resatisfiedItems.add(di);
						}
					}
					
				}
				
				//remove satisfied items for next iteration (next cycle)
				for(int k = 0; k < Q.size(); k++) {
					dev_items_u.removeAll(resatisfiedItems);
				}
				
			}
		}
	}
	
	
	public void removeUnderusedPaths(NetworkInfrastructure infra, ArrayList<Cycle> toRemove, ArrayList<Cycle> Q, ArrayList<Tuple> dev_items_u) {
		Iterator<Cycle> iter1 = Q.iterator();
		while(iter1.hasNext()) {
		    Cycle e1 = iter1.next();
		    if(e1.links.size() < 2 && e1.itemPerCycle.size() == 0) { //if it is a completely empty cycle
		        iter1.remove();
		    }else if(e1.links.size() < 2 && e1.itemPerCycle.size() > 0) { //if there are no links but has items to reallocate
		    	for(int i = 0; i < e1.itemPerCycle.size(); i++) {
		    		if(!dev_items_u.contains(e1.itemPerCycle.get(i))) {
		    			dev_items_u.add(e1.itemPerCycle.get(i));
		    		}
		    	}
		    	iter1.remove();
		    }else if(e1.links.size() >= 2) { //check whether there is another cycle which passes through links that collect no items
				Iterator<Cycle> iter2 = Q.iterator();
				
				//iterate over other cycles
				int hasAllLinks = 0;
				Cycle e2 = null;
				
				while(iter2.hasNext()) {
					hasAllLinks = 0;
					e2 = iter2.next();
					
					if(e1 != e2) {
						//System.out.println("e1: " + e1.links + " e2: " + e2.links + " not equal!!!");
						for(int l = 0; l < e1.links.size(); l++) {
							if(e2.links.contains(e1.links.get(l))) { //if a longer cycle contains the links, exclude the items for further reallocation
								hasAllLinks++;
							}
						}
						
						if(hasAllLinks == e1.links.size() || hasAllLinks == e2.links.size()) { //try to put the items from the smaller cycle into the larger one
							//System.out.println("(if) hasAllLinks: " + hasAllLinks + " e1 links: " + e1.links + " e2 links: " + e2.links);
							
							if(e1.links.size() < e2.links.size()) {

								for(Tuple di : e1.itemPerCycle) {
									if(e2.capacity_used + infra.sizeTelemetryItems[di.item] <= e2.capacity) {
										if(!e2.itemPerCycle.contains(di)) {
											e2.itemPerCycle.add(di);	
										}
									}else {
										if(!dev_items_u.contains(di)) {
											dev_items_u.add(di);	
										}
									}
								}
								iter1.remove();
								break;
							}
							else if(e1.links.size() >= e2.links.size()) {

								for(Tuple di : e2.itemPerCycle) {
									if(e1.capacity_used + infra.sizeTelemetryItems[di.item] <= e1.capacity) {
										if(!e1.itemPerCycle.contains(di)) {
											e1.itemPerCycle.add(di);	
										}
									}else {
										if(!dev_items_u.contains(di)) {
											dev_items_u.add(di);	
										}
									}
								}
								
								toRemove.add(e2);
								//iter2.remove();
								break;
							}
						}
					}
					
				}//end while
		    }
		}
		
		Q.removeAll(toRemove);
	}
	
	public void assureCapacityConstraints(ArrayList<Cycle> Q, ArrayList<Tuple> dev_items_u) {
		//check capacity
		for(int i = 0; i < Q.size(); i++) {

			//exclude items exceeding capacity constraint in cycles
			while(Q.get(i).capacity_used > Q.get(i).capacity) {
				int min = Integer.MAX_VALUE;
				int minIndex = -1;
				
				//get the lightest item for exclusion
				for(int j = 0; j < Q.get(i).itemPerCycle.size(); j++) {
					if(Q.get(i).itemPerCycle.get(j).getItem() < min) {
						min = Q.get(i).itemPerCycle.get(j).getItem();
						minIndex = j;
					}
				}
				
				if(minIndex != -1) {
					if(!dev_items_u.contains(Q.get(i).itemPerCycle.get(minIndex))) {
						dev_items_u.add(Q.get(i).itemPerCycle.get(minIndex));
					}
					
					Q.get(i).itemPerCycle.remove(minIndex);	
				}else {
					break;
				}
				
			}
		}
	}
	
	public void updateCapacity(NetworkInfrastructure infra, ArrayList<Cycle> Q) {
		for(int i = 0; i < Q.size(); i++) {
			Q.get(i).capacity_used = 0;
		
			//links' cost
			for(Pair<Integer,Integer> l : Q.get(i).links) {
				Q.get(i).capacity_used += 1;
			}
			
			//device-items' cost
			for(Tuple di: Q.get(i).itemPerCycle) {
				Q.get(i).capacity_used += infra.sizeTelemetryItems[di.getItem()];
			}
		}
	}
	
	public void updateNodes(ArrayList<Cycle> Q) {
		for(int i = 0; i < Q.size(); i++) {
			Q.get(i).nodes.clear();
			for(Pair<Integer,Integer> link : Q.get(i).links) {
				Q.get(i).nodes.add(link.first);
			}
			Q.get(i).nodes.add(Q.get(i).links.get(0).first);
		}
	}
	
	public void createRemainingPaths(NetworkInfrastructure infra, ArrayList<Cycle> Q, ArrayList<Tuple> dev_items_u, ArrayList<Integer> failureNodes, 
			int capacityProbe, int maxSizePath, int minItemWeight, ArrayList<Pair<Integer,Integer>> storeLinks, int[] nodes) {
		
		//System.out.println("itemsFromNodes: " + itemsFromNodes);
		
		//generate subGraph
		nodes[0] = -1;
		nodes[1] = -1;
		//System.out.println("nodes[0]: " + nodes[0] + " nodes[1]: " + nodes[1]);
		
		//add nodes where it must collect as high-priority
		int[] collectPriority = new int[dev_items_u.size()];
		int t = 0;
		for(Tuple node : dev_items_u) {
			collectPriority[t] = node.getDevice();
			t++;
		}
		
		//collected items matrix
		boolean[][] collected = new boolean[infra.size][infra.telemetryItemsRouter];
		for(int i = 0; i < infra.size; i++) {
			for(int j = 0; j < infra.telemetryItemsRouter; j++) {
				collected[i][j] = true;
			}
		}
		for(Tuple di : dev_items_u) {
			//System.out.println("di: " + di);
			collected[di.getDevice()][di.getItem()] = false;
		}
	
		//System.exit(0);
		
		//System.out.println("itemsFromNodes: " + itemsFromNodes);
		
		//System.exit(0);

		
		int test = 0;
		while(!dev_items_u.isEmpty()) {
			test++;
			int w = 0;
			int nodeA = -1;
			int depot = -1;
			int lastNode = -1;
			
			if(!dev_items_u.isEmpty()){
				nodeA = dev_items_u.get(0).getDevice();
				depot = dev_items_u.get(0).getDevice();
			}
			
			boolean visited[][] = new boolean[infra.size][infra.size];
			Pair<Integer,Integer> edge = createEdge(infra, nodeA, lastNode, depot, collectPriority, visited);
			
			ArrayList<Pair<Integer,Integer>> pathLinks = new ArrayList<Pair<Integer,Integer>>();
			Cycle c = new Cycle();
			c.capacity = capacityProbe;
						
			int step = 0;
			w = 0;
			while(!dev_items_u.isEmpty() && step < maxSizePath - 1 && edge.second >= 0 && w < (capacityProbe - minItemWeight)) {
				step++;
				edge = createEdge(infra, nodeA, lastNode, depot, collectPriority, visited);
				nodeA = edge.first;
				
				
				//valid edge
				if(edge.first >= 0 && edge.second >= 0) {
					visited[edge.first][edge.second] = true;
					pathLinks.add(edge);
					w += infra.fixedEdgeCost;
					
					//iterate over telemetry items and collect them whenever possible
					for(int j = 0; j < infra.telemetryItemsRouter; j++) {
						if(infra.items[edge.first][j] == 1 && !collected[edge.first][j]) {
							if(w + infra.sizeTelemetryItems[j] <= capacityProbe) {
								
								w += infra.sizeTelemetryItems[j];
								Tuple di = new Tuple(edge.first, j);
								c.itemPerCycle.add(di);
								collected[edge.first][j] = true;
								
								int index = indexOfNode(collectPriority, edge.first); 
								if(index != -1){
									collectPriority[index] = 0;
								}
																
								if(!dev_items_u.isEmpty()) {
									dev_items_u.remove(di);	
								}
							}
						}
					}

					//iterate over telemetry items and collect them whenever possible
					for(int j = 0; j < infra.telemetryItemsRouter; j++) {
						if(infra.items[edge.second][j] == 1 && !collected[edge.second][j]) {
							if(w + infra.sizeTelemetryItems[j] <= capacityProbe) {
								w += infra.sizeTelemetryItems[j];
								Tuple di = new Tuple(edge.second, j);
								c.itemPerCycle.add(di);
								collected[edge.second][j] = true;
								
								int index = indexOfNode(collectPriority, edge.second); 
								if(index != -1){
									collectPriority[index] = 0;
								}
																
								if(!dev_items_u.isEmpty()) {
									dev_items_u.remove(di);	
								}
							}
						}
					}
					lastNode = edge.first;
					nodeA = edge.second;
				//invalid edge
				}else {
					break;
				}
			}
			
			if(edge.second == depot) {
				c.links = pathLinks;
				c.capacity_used = w;
				c.capacity = capacityProbe;
				Q.add(c);
			}else if(edge.second >= 0) {
				nodes[0] = edge.second;
				nodes[1] = depot;
				ArrayList<Integer> closeCycle = new ArrayList<Integer>();
				closeCycle = infra.getShortestPath2(nodes);
				
				//construct links
				for(int i = 0; i < closeCycle.size() - 1; i++) {
					Pair<Integer,Integer> link = Pair.create(closeCycle.get(i), closeCycle.get(i+1));
					pathLinks.add(link);
				}
				c.links = pathLinks;
				c.capacity_used = w;
				c.capacity = capacityProbe;
				Q.add(c);
			}else {
				nodes[0] = edge.first;
				nodes[1] = depot;
				ArrayList<Integer> closeCycle = new ArrayList<Integer>();
				closeCycle = infra.getShortestPath2(nodes);
				
				//construct links
				for(int i = 0; i < closeCycle.size() - 1; i++) {
					Pair<Integer,Integer> link = Pair.create(closeCycle.get(i), closeCycle.get(i+1));
					pathLinks.add(link);
				}
				c.links = pathLinks;
				c.capacity_used = w;
				c.capacity = capacityProbe;
				Q.add(c);
			}
			
			//System.out.println("pathLinks: " + pathLinks);
		}
		
			
		//reset subgraph
		if(!storeLinks.isEmpty()) {			
			for(Pair<Integer,Integer> link : storeLinks) {
				//System.out.println("link: " + "(" + link.first + "," + link.second + ")");
				infra.graph[link.first][link.second] = 1;
			}
		}
		
	}
	
	
	public Pair<Integer,Integer> createEdge(NetworkInfrastructure infra, int nodeA, int lastNode, int depot, int[] collectPriority, boolean[][] visited) {
		Pair<Integer,Integer> edge = Pair.create(nodeA, -2);
		for(int i = 0; i < infra.size; i++) {
			for(int j = 0; j < infra.size; j++) {
				if(infra.graph[i][j] == 1) {
					int indexNodeJ = indexOfNode(collectPriority, j); 
					if(indexNodeJ != -1 && j != nodeA && j != lastNode && j != depot && !visited[nodeA][j]) {
						edge = Pair.create(nodeA, j);
						return edge;
					}else if(j != nodeA && j != lastNode && j != depot && !visited[nodeA][j]){
						edge = Pair.create(nodeA, j);
						return edge;
					}
				}
			}
		}
		return edge;
	}
	
	
	public int indexOfNode(int[] collectPriority, int j) {
		for(int i = 0; i < collectPriority.length; i++) {
			if(collectPriority[i] == j) {
				return i;
			}
		}
		return -1;
	}
	
}
