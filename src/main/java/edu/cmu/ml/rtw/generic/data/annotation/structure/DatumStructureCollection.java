package edu.cmu.ml.rtw.generic.data.annotation.structure;
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

import java.util.*;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.util.MathUtil;

/**
 * DatumStructureCollection represents a collection of datum structures
 * (edu.cmu.ml.rtw.generic.data.annotation.structure.DatumStructure). A particular domain-
 * specific datum structure collection should partition the datums of 
 * a data set into structures upon construction.  For example, one 
 * collection might partition the data into one structure per document,
 * whereas another might partition the data into one structure per
 * sentence.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> label type
 */
public abstract class DatumStructureCollection<D extends Datum<L>, L> implements Iterable<DatumStructure<D, L>> {
	protected List<DatumStructure<D, L>> datumStructures;
	
	public abstract String getGenericName();
	public abstract DatumStructureCollection<D, L> makeInstance(DataSet<D, L> data);
	
	public DatumStructureCollection() {
		this.datumStructures = new ArrayList<DatumStructure<D, L>>();
	}

	public boolean isEmpty() {
		return this.datumStructures.isEmpty();
	}

	public Iterator<DatumStructure<D, L>> iterator() {
		return this.datumStructures.iterator();
	}

	public int size() {
		return this.datumStructures.size();
	}
	
	public DatumStructure<D, L> getDatumStructure(int index) {
		return this.datumStructures.get(index);
	}

	public List<Integer> constructRandomDatumStructurePermutation(Random random) {
		List<Integer> permutation = new ArrayList<Integer>(this.datumStructures.size());
		for (int i = 0; i < this.datumStructures.size(); i++)
			permutation.add(i);
		
		return MathUtil.randomPermutation(random, permutation);
	}
	
	public List<Map<String, Integer>> checkConstraints(boolean useDisjunctiveConstraints){
		List<Map<String, Integer>> violations = new ArrayList<Map<String, Integer>>();
		for (DatumStructure<D,L> ds : datumStructures){
			violations.add(ds.constraintsHold(useDisjunctiveConstraints));
		}
		return violations;
	}
	
}
