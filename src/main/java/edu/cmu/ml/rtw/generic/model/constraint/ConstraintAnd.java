package edu.cmu.ml.rtw.generic.model.constraint;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;

/**
 * ConstraintAnd represents a conjunction constraint.  Given
 * constaints firstConstraint and secondConstraint, a ConstraintAnd constraint 
 * is satisfied if both firstConstraint and secondConstraint are satisfied.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type 
 */
public class ConstraintAnd<D extends Datum<L>, L> extends Constraint<D, L> {
	private Constraint<D, L> firstConstraint;
	private Constraint<D, L> secondConstraint;
	
	public ConstraintAnd(Constraint<D,L> firstConstraint, Constraint<D, L> secondConstraint) {
		this.firstConstraint = firstConstraint;
		this.secondConstraint = secondConstraint;
	}

	@Override
	public boolean isSatisfying(FeaturizedDataSet<D, L> data, D datum) {
		return this.firstConstraint.isSatisfying(data, datum) 
				&& this.secondConstraint.isSatisfying(data, datum);
	}
	
	@Override
	public String toString() {
		return "And(" + this.firstConstraint.toString() + ", " + this.secondConstraint.toString() + ")";
	}
}
