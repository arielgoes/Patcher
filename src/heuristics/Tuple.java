package heuristics;

import java.util.Objects;

public class Tuple {
	public int device;
	public int item;
	
	public Tuple(int device, int item) {
		this.device = device;
		this.item = item;
	}
	
	@Override
	public String toString() {
		return "(" + this.device + "," + this.item + ")";
	}
	
	public int getDevice() {
		return this.device;
	}
	
	public int getItem() {
		return this.item;
	}
	
	@Override
    public boolean equals(Object o) {
        if (!(o instanceof Tuple)) {
            return false;
        }
        Tuple t = (Tuple) o;
        return Objects.equals(t.device, device) && Objects.equals(t.item, item);
    }    

}

