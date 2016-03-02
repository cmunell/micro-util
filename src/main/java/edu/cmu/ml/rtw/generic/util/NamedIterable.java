package edu.cmu.ml.rtw.generic.util;

public class NamedIterable<I extends Iterable<O>, O> {
	private String name;
	private I iterable;
	
	public NamedIterable(String name, I iterable) {
		this.name = name;
		this.iterable = iterable;
	}
	
	public I getIterable() {
		return this.iterable;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		for (O item : this.iterable)
			str.append(item.toString()).append("\n\n");
		
		return str.toString();
	}
}
