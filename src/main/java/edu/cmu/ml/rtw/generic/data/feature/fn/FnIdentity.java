package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnIdentity take a collection and returns
 * it.
 * 
 * @author Bill McDowell
 *
 * @param <S>
 */
public class FnIdentity<S> extends Fn<S, S> {
	private String[] parameterNames = {};
	
	public FnIdentity() {
		
	}
	
	public FnIdentity(Context<?, ?> context) {
		
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		return false;
	}

	@Override
	public <C extends Collection<S>> C compute(Collection<S> input, C output) {
		for (S s : input)
			output.add(s);
		
		return output;
	}

	@Override
	public Fn<S, S> makeInstance(
			Context<?, ?> context) {
		return new FnIdentity<S>(context);
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
		return "Identity";
	}
}
