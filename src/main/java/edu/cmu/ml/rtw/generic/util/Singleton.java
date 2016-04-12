package edu.cmu.ml.rtw.generic.util;

public class Singleton<T> {
	private T obj;
	
	public Singleton(T obj) {
		this.obj = obj;
	}
	
	public boolean set(T obj) {
		this.obj = obj;
		return true;
	}
	
	public T get() {
		return this.obj;
	}
}
