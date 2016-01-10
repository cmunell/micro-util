package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnComposite computes the composition (f o g) of
 * two functions f and g.
 * 
 * Parameters:
 *  f - second function in composition
 *  g - first function in composition
 * 
 * @author Bill McDowell
 *
 * @param <S>
 * @param <T>
 * @param <U>
 */
public abstract class FnComposite<S, T, U> extends Fn<S, T> {
	public static class FnCompositeTokenSpan extends FnComposite<TokenSpan, TokenSpan, TokenSpan> {
		public FnCompositeTokenSpan() {
			super();
		}
		
		public FnCompositeTokenSpan(Context<?, ?> context) {
			super(context);
		}

		@Override
		protected Fn<TokenSpan, TokenSpan> constructParameterF(Obj parameterValue) {
			return this.context.getMatchOrConstructTokenSpanFn(parameterValue);
		}

		@Override
		protected Fn<TokenSpan, TokenSpan> constructParameterG(Obj parameterValue) {
			return this.context.getMatchOrConstructTokenSpanFn(parameterValue);
		}

		@Override
		public Fn<TokenSpan, TokenSpan> makeInstance(
				Context<?, ?> context) {
			return new FnCompositeTokenSpan(context);
		}
	}
	
	public static class FnCompositeStr extends FnComposite<String, String, String> {
		public FnCompositeStr() {
			super();
		}
		
		public FnCompositeStr(Context<?, ?> context) {
			super(context);
		}

		@Override
		protected Fn<String, String> constructParameterF(Obj parameterValue) {
			return this.context.getMatchOrConstructStrFn(parameterValue);
		}

		@Override
		protected Fn<String, String> constructParameterG(Obj parameterValue) {
			return this.context.getMatchOrConstructStrFn(parameterValue);
		}

		@Override
		public Fn<String, String> makeInstance(
				Context<?, ?> context) {
			return new FnCompositeStr(context);
		}
	}
	
	public static class FnCompositeTokenSpanTokenSpanStr extends FnComposite<TokenSpan, String, TokenSpan> {
		public FnCompositeTokenSpanTokenSpanStr() {
			super();
		}
		
		public FnCompositeTokenSpanTokenSpanStr(Context<?, ?> context) {
			super(context);
		}

		@Override
		protected Fn<TokenSpan, String> constructParameterF(Obj parameterValue) {
			return this.context.getMatchOrConstructTokenSpanStrFn(parameterValue);
		}

		@Override
		protected Fn<TokenSpan, TokenSpan> constructParameterG(Obj parameterValue) {
			return this.context.getMatchOrConstructTokenSpanFn(parameterValue);
		}

		@Override
		public Fn<TokenSpan, String> makeInstance(
				Context<?, ?> context) {
			return new FnCompositeTokenSpanTokenSpanStr(context);
		}
	}
	
	public static class FnCompositeTokenSpanStrStr extends FnComposite<TokenSpan, String, String> {
		public FnCompositeTokenSpanStrStr() {
			super();
		}
		
		public FnCompositeTokenSpanStrStr(Context<?, ?> context) {
			super(context);
		}

		@Override
		protected Fn<String, String> constructParameterF(Obj parameterValue) {
			return this.context.getMatchOrConstructStrFn(parameterValue);
		}

		@Override
		protected Fn<TokenSpan, String> constructParameterG(Obj parameterValue) {
			return this.context.getMatchOrConstructTokenSpanStrFn(parameterValue);
		}

		@Override
		public Fn<TokenSpan, String> makeInstance(
				Context<?, ?> context) {
			return new FnCompositeTokenSpanStrStr(context);
		}
	}
	
	private String[] parameterNames = { "f", "g" };
	protected Fn<U, T> f;
	protected Fn<S, U> g;
	protected Context<?, ?> context;
	
	protected abstract Fn<U, T> constructParameterF(Obj parameterValue);
	protected abstract Fn<S, U> constructParameterG(Obj parameterValue);

	public FnComposite() {
		
	}
	
	public FnComposite(Context<?, ?> context) {
		this.context = context;
	}
	
	@Override
	public <C extends Collection<T>> C compute(Collection<S> input, C output) {
		return this.f.compute(this.g.listCompute(input), output);
	}

	@Override
	public String getGenericName() {
		return "Composite";
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("f"))
			return this.f.toParse();
		else if (parameter.equals("g"))
			return this.g.toParse();
		else 
			return null;
	}
	
	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("f")) {
			this.f = constructParameterF(parameterValue);
			if (this.f == null)
				return false;
		} else if (parameter.equals("g")) {
			this.g = constructParameterG(parameterValue);
			if (this.g == null)
				return false;
		} else 
			return false;
		
		return true;
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
