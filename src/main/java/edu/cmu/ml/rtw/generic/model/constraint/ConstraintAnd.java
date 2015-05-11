/**
 * Copyright 2014 Bill McDowell 
 *
 * This file is part of theMess (https://github.com/forkunited/theMess)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 */

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
