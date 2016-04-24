package edu.cmu.ml.rtw.generic.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;

public abstract class FnStructure<S extends WeightedStructure, T extends WeightedStructure> extends Fn<S, T> {
	public <F extends WeightedStructure> List<T> listCompute(Collection<S> input, Collection<F> filter) {
		return this.compute(input, new ArrayList<T>(), filter);
	}
	
	public <F extends WeightedStructure> Set<T> setCompute(Collection<S> input, Collection<F> filter) {
		return this.compute(input, new HashSet<T>(), filter);
	}
	
	public <F extends WeightedStructure> List<T> listCompute(S input, Collection<F> filter) {
		return this.compute(Collections.singletonList(input), new ArrayList<T>(), filter);
	}
	
	public <F extends WeightedStructure> Set<T> setCompute(S input, Collection<F> filter) {
		return this.compute(Collections.singletonList(input), new HashSet<T>(), filter);
	}
	
	protected <C extends Collection<T>> C compute(Collection<S> input, C output) {
		return compute(input, output, null);
	}
	
	protected abstract <C extends Collection<T>, F extends WeightedStructure> C compute(Collection<S> input, C output, Collection<F> filter);
}
