package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnComposite.FnCompositeTokenSpan;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnComposite.FnCompositeStr;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnCompositeAppend applies a composition of two
 * functions to a collection of inputs, and then appends the 
 * inputs to the collection of outputs.
 * 
 * @author Bill McDowell
 *
 * @param <S>
 */
public abstract class FnCompositeAppend<S> extends Fn<S, S> {
	public static class FnCompositeAppendTokenSpan extends FnCompositeAppend<TokenSpan> {
		public FnCompositeAppendTokenSpan() {
			super();
		}
		
		public FnCompositeAppendTokenSpan(Context<?, ?> context) {
			super(context);
			this.compositeFn = new FnCompositeTokenSpan(context);
		}

		@Override
		public Fn<TokenSpan, TokenSpan> makeInstance(
				Context<?, ?> context) {
			return new FnCompositeAppendTokenSpan(context);
		}
	}
	
	public static class FnCompositeAppendStr extends FnCompositeAppend<String> {
		public FnCompositeAppendStr() {
			super();
		}
		
		public FnCompositeAppendStr(Context<?, ?> context) {
			super(context);
			this.compositeFn = new FnCompositeStr(context);
		}

		@Override
		public Fn<String, String> makeInstance(
				Context<?, ?> context) {
			return new FnCompositeAppendStr(context);
		}
	}
	
	private String[] parameterNames = { "f", "g" };
	protected FnComposite<S, S, S> compositeFn;
	protected Context<?, ?> context;
	
	public FnCompositeAppend() {
		
	}
	
	public FnCompositeAppend(Context<?, ?> context) {
		this.context = context;
	}
	
	@Override
	public <C extends Collection<S>> C compute(Collection<S> input, C output) {
		output = this.compositeFn.compute(input, output);
		output.addAll(input);
		return output;
	}

	@Override
	public String getGenericName() {
		return "CompositeAppend";
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("f"))
			return this.compositeFn.getParameterValue(parameter);
		else if (parameter.equals("g"))
			return this.compositeFn.getParameterValue(parameter);
		else 
			return null;
	}
	
	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("f")) {
			return this.compositeFn.setParameterValue(parameter, parameterValue);
		} else if (parameter.equals("g")) {
			return this.compositeFn.setParameterValue(parameter, parameterValue);
		} else 
			return false;
	}
	
	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {		
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		return null;
	}
	
}
