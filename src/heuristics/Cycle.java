package heuristics;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Cycle implements Cloneable, Comparator<Cycle>, Comparable<Cycle>, Serializable{
	public ArrayList<Tuple> itemPerCycle;
	public ArrayList<Integer> nodes; //nodes in the cycle;
	public ArrayList<Pair<Integer,Integer>> links; //links in the cycle... sometimes this representations is easier
	public int capacity;
	public int capacity_used;
	public int transportationCost;
	public ArrayList<Integer> pathOverhead;   //for comparison between OptPathPlan and others
	public int id;

	public Cycle() {
		this.itemPerCycle = new ArrayList<Tuple>();
		this.nodes = new ArrayList<Integer>();
		this.links = new ArrayList<Pair<Integer,Integer>>();
		this.pathOverhead = new ArrayList<Integer>();
	}
	
	public void printCycle() {
		System.out.println("CIRCUIT:");
		System.out.println("path (nodes): " + this.nodes);
		System.out.println("path (links): " + this.links);
		System.out.println("DEVICE-ITEM:");
		System.out.println("device-item: " + this.itemPerCycle);
		
	}

	public void printCycleWithCapacity() {
		System.out.println("---------------------------------");
		System.out.println("CIRCUIT:");
		System.out.println("path (node): " + this.nodes);
		System.out.println("path (links): " + this.links);
		System.out.println("DEVICE-ITEMS:");
		System.out.println("device-items: " + this.itemPerCycle);
		System.out.println("Capacity used: " + this.capacity_used + ". Total Capacity: " + this.capacity);
	}
	
	@Override
	public Cycle clone() throws CloneNotSupportedException {
		//try{
			//Cycle clonedCycled = (Cycle)super.clone();
			Cycle clonedCycle = new Cycle();
			// if you have custom object, then you need create a new one in here
			
			clonedCycle.itemPerCycle = new ArrayList<Tuple>();
			for(int i = 0; i < itemPerCycle.size(); i++) {
				Tuple t = new Tuple(itemPerCycle.get(i).device, itemPerCycle.get(i).item);
				clonedCycle.itemPerCycle.add(t);
			}
			
			for(int i = 0; i < nodes.size(); i++) {
				Integer n = new Integer(nodes.get(i));
				clonedCycle.nodes.add(n);
			}
			
			for(int i = 0; i < links.size(); i++) {
				Pair<Integer, Integer> l = Pair.create(links.get(i).first, links.get(i).second);
				clonedCycle.links.add(l);
			}
			
			clonedCycle.capacity = capacity;
			clonedCycle.capacity_used = capacity_used;
			clonedCycle.transportationCost = transportationCost;
			
			for(int i = 0; i < pathOverhead.size(); i++) {
				Integer o = new Integer(pathOverhead.get(i));
				clonedCycle.pathOverhead.add(o);
			}
			
			clonedCycle.id = id;
			
			return clonedCycle;
		/*}catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return new Cycle();
		}*/
	}
	
	
	public int getCapacity_used() {
		return this.capacity_used;
	}

	@Override
	public int compare(Cycle c1, Cycle c2) {
		int capacity_used1 = c1.getCapacity_used();
		int capacity_used2 = c2.getCapacity_used();
		
		if(capacity_used1 == capacity_used2) {
			return 0;
		}else if(capacity_used1 > capacity_used2) {
			return 1;
		}else {
			return -1;
		}
	}

	@Override
	public int compareTo(Cycle c) {
		return Integer.compare(this.capacity_used, c.getCapacity_used());
	}


}
