package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public abstract class FnRelationStr<S> extends Fn<S, String> {
	public static class FnRelationStrStr extends FnRelationStr<String> {
		public FnRelationStrStr() {
			super();
		}
		
		public FnRelationStrStr(Context<?, ?> context) {
			super(context);
		}

		@Override
		public Fn<String, String> makeInstance(
				Context<?, ?> context) {
			return new FnRelationStrStr(context);
		}
		
		@Override
		protected Fn<String, String> constructParameterF(Obj parameterValue) {
			return this.context.getMatchOrConstructStrFn(parameterValue);
		}
	}
	
	public static class FnRelationStrTokenSpan extends FnRelationStr<TokenSpan> {
		public FnRelationStrTokenSpan() {
			super();
		}
		
		public FnRelationStrTokenSpan(Context<?, ?> context) {
			super(context);
		}

		@Override
		public Fn<TokenSpan, String> makeInstance(
				Context<?, ?> context) {
			return new FnRelationStrTokenSpan(context);
		}

		@Override
		protected Fn<TokenSpan, String> constructParameterF(Obj parameterValue) {
			return this.context.getMatchOrConstructTokenSpanStrFn(parameterValue);
		}
	}

	private String[] parameterNames = { "f", "relationSymbol" };
	protected Fn<S, String> f;
	protected String relationSymbol;
	protected Context<?, ?> context;
	
	protected abstract Fn<S, String> constructParameterF(Obj parameterValue);
	
	public FnRelationStr() {
		
	}
	
	public FnRelationStr(Context<?, ?> context) {
		this.context = context;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<S> input, C output) {
		List<S> singletonInput = new ArrayList<S>();
		StringBuilder outputStr = new StringBuilder();
		for (S s : input) {
			singletonInput.add(s);
			List<String> tempOutputStrs = this.f.compute(input, new ArrayList<String>());
			
			for (String tempOutputStr : tempOutputStrs) {
				outputStr.setLength(0);
				outputStr.append(input);
				outputStr.append(this.relationSymbol);
				outputStr.append(tempOutputStr);
				output.add(outputStr.toString());
			}
			
			singletonInput.clear();
		}
	
		return output;
	}

	@Override
	public String getGenericName() {
		return "RelationStr";
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("f"))
			return this.f.toParse();
		else if (parameter.equals("relationSymbol"))
			return Obj.stringValue(this.relationSymbol);
		else 
			return null;
	}
	
	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("f")) {
			this.f = constructParameterF(parameterValue);
			if (this.f == null)
				return false;
		} else if (parameter.equals("relationSymbol")) {
			this.relationSymbol = this.context.getMatchValue(parameterValue);
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
