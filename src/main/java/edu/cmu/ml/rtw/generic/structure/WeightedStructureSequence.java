package edu.cmu.ml.rtw.generic.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsable;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class WeightedStructureSequence extends WeightedStructure {
	private Context context;
	private List<CtxParsable> items;
	private List<Double> weights;
	
	private static String[] parameterNames = { };
	
	
	public WeightedStructureSequence() {
		this(null);
	}
	
	public WeightedStructureSequence(Context context) {
		this.items = new ArrayList<CtxParsable>();
		this.weights = new ArrayList<Double>();
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		return true;
	}

	public int size() {
		return this.items.size();
	}
	
	public CtxParsable get(int index) {
		return this.items.get(index);
	}
	
	@Override
	public boolean remove(CtxParsable item) {
		int index = this.items.indexOf(item);
		this.items.remove(index);
		this.weights.remove(index);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public WeightedStructure add(CtxParsable item, double w, Collection<?> changes) {
		this.items.add(item);
		this.weights.add(w);
		if (changes != null)
			((Collection<CtxParsable>)changes).add(item);
		return this;
	}

	@Override
	public double getWeight(CtxParsable item) {
		int index = this.items.indexOf(item);
		return this.weights.get(index);
	}

	@Override
	public WeightedStructure merge(WeightedStructure s) {
		if (!(s instanceof WeightedStructureSequence))
			throw new IllegalArgumentException(); 
		
		WeightedStructureSequence seq = (WeightedStructureSequence)s;
		this.items.addAll(seq.items);
		this.weights.addAll(seq.weights);
		
		return this;
	}

	@Override
	public WeightedStructure makeInstance(Context context) {
		return new WeightedStructureSequence(context);
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		return null;
	}

	@Override
	public String getGenericName() {
		return "Sequence";
	}

	@Override
	public List<CtxParsable> toList() {
		return this.items;
	}
	
	public WeightedStructureSequence clone() {
		WeightedStructureSequence seq = new WeightedStructureSequence(this.context);
		if (!seq.fromParse(toParse()))
			return null;
		
		for (int i = 0; i < this.items.size(); i++) {
			seq.items.add(this.items.get(i));
			seq.weights.add(this.weights.get(i));
		}
		
		return seq;
	}
	
	@Override
	public int getItemCount() {
		return this.items.size();
	}
}
