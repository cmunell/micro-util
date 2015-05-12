package edu.cmu.ml.rtw.generic.data.annotation.structure;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelMapping;

/**
 * DatumStructure represents a collection of datums (training/evaluation
 * examples from a data set) over which hard constraints can be imposed within
 * models.
 * 
 * @author Bill McDowell
 *
 * @param <D> Datum type
 * @param <L> Label type
 */
public abstract class DatumStructure<D extends Datum<L>, L> implements Collection<D> {
	/**
	 * DatumStructureOptimizer chooses a good set of labels to assign to 
	 * datums in a structure which satisfy some constraints.
	 */
	protected interface DatumStructureOptimizer<D extends Datum<L>, L> {
		/** 
		 *
		 * @param scoredDatumLabels represents a scoring function s:DxL->R
		 * that assigns a score to each datum-label pair.  
		 * 
		 * @param fixedDatumLabels constrains some datums in the structure 
		 * to have some labels.  The optimizer should adhere to these 
		 * constraints.
		 * 
		 * @param validLabels is the set of labels that the optimizer can
		 * assign to datums.
		 * 
		 * @param labelMapping represents a mapping from labels to labels that
		 * the optimizer should apply before returning the assignment of 
		 * labels to datums.
		 * 
		 * @return a mapping from datums to labels that maximizes the sum
		 * of scores across datums (according to scoredDatumLabels) subject
		 * to some constraints.
		 * 
		 */
		Map<D, L> optimize(Map<D, Map<L, Double>> scoredDatumLabels, Map<D, L> fixedDatumLabels, Set<L> validLabels, LabelMapping<L> labelMapping);
		String getGenericName();
	}
	
	protected String id;
	protected Map<String, DatumStructureOptimizer<D, L>> datumStructureOptimizers;
	
	public DatumStructure(String id) {
		this.id = id;
		this.datumStructureOptimizers = new HashMap<String, DatumStructureOptimizer<D, L>>();
	}
	
	public String getId() {
		return this.id;
	}
	
	public Map<D, L> getDatumLabels(LabelMapping<L> labelMapping) {
		Map<D, L> datumLabels = new HashMap<D, L>();
		
		for (D datum : this) {
			if (labelMapping == null)
				datumLabels.put(datum, datum.getLabel());
			else
				datumLabels.put(datum, labelMapping.map(datum.getLabel()));
		}
		
		return datumLabels;
	}
	
	@Override
	public int hashCode() {
		// FIXME: Make better
		return this.id.hashCode();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		DatumStructure<D, L> datumStructure = (DatumStructure<D, L>)o;
		return datumStructure.id.equals(this.id);
	}
	
	public Map<D, L> optimize(String optimizerName, Map<D, Map<L, Double>> scoredDatumLabels, Map<D, L> fixedDatumLabels, Set<L> validLabels, LabelMapping<L> labelMapping) {
		return this.datumStructureOptimizers.get(optimizerName).optimize(scoredDatumLabels, fixedDatumLabels, validLabels, labelMapping);
	}
	
	protected boolean addDatumStructureOptimizer(DatumStructureOptimizer<D, L> datumStructureOptimizer) {
		this.datumStructureOptimizers.put(datumStructureOptimizer.getGenericName(), datumStructureOptimizer);
		return true;
	}
	
	public abstract Map<String, Integer> constraintsHold(boolean useDisjunctiveConstraints);
}
