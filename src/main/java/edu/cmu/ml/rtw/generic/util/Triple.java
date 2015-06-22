package edu.cmu.ml.rtw.generic.util;

/**
 * Triple represents a tuple containing three
 * objects.
 * 
 * @author Bill McDowell
 *
 * @param <F>
 * @param <S>
 * @param <T>
 */
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
