package edu.cmu.ml.rtw.generic.util;

public class Triple<F, S, T> extends Pair<F, S> {
	private T third;
	
	public Triple(F first, S second, T third) {
		super(first, second);
		this.third = third;
	}
	
	public boolean setThird(T third) {
		this.third = third;
		return true;
	}
	
	public T getThird() {
		return this.third;
	}
}
