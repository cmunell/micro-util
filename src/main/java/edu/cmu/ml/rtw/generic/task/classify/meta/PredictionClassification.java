package edu.cmu.ml.rtw.generic.task.classify.meta;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;

public class PredictionClassification<D extends Datum<L>, L> {
	private D datum;
	private L label;
	private double score;
	private MethodClassification<D, L> method;
	
	public PredictionClassification(D datum, L label, double score, MethodClassification<D, L> method) {
		this.datum = datum;
		this.label = label;
		this.score = score;
		this.method = method;
	}
	
	public D getDatum() {
		return this.datum;
	}
	
	public L getLabel() {
		return this.label;
	}
	
	public double getScore() {
		return this.score;
	}
	
	public MethodClassification<D, L> getMethod() {
		return this.method;
	}
	
	@Override
	public String toString() {
		return this.datum.toString() + " " + this.label.toString() + " " + this.method.getReferenceName();
	}
}
