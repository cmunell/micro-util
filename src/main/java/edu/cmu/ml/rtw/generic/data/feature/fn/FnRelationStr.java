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
		
		public FnRelationStrStr(Context context) {
			super(context);
		}

		@Override
		public Fn<String, String> makeInstance(
				Context context) {
			return new FnRelationStrStr(context);
		}
		
		@Override
		protected Fn<String, String> constructParameterF(Obj parameterValue) {
			return this.context.getMatchOrRunCommandStrFn(parameterValue);
		}
	}
	
	public static class FnRelationStrTokenSpan extends FnRelationStr<TokenSpan> {
		public FnRelationStrTokenSpan() {
			super();
		}
		
		public FnRelationStrTokenSpan(Context context) {
			super(context);
		}

		@Override
		public Fn<TokenSpan, String> makeInstance(
				Context context) {
			return new FnRelationStrTokenSpan(context);
		}

		@Override
		protected Fn<TokenSpan, String> constructParameterF(Obj parameterValue) {
			return this.context.getMatchOrConstructTokenSpanStrFn(parameterValue);
		}
	}

	private String[] parameterNames = { "f1", "f2", "relationSymbol" };
	protected Fn<S, String> f1;
	protected Fn<S, String> f2;
	protected String relationSymbol;
	protected Context context;
	
	protected abstract Fn<S, String> constructParameterF(Obj parameterValue);
	
	public FnRelationStr() {
		
	}
	
	public FnRelationStr(Context context) {
		this.context = context;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<S> input, C output) {
		List<S> singletonInput = new ArrayList<S>();
		StringBuilder outputStr = new StringBuilder();
		for (S s : input) {
			singletonInput.add(s);
			List<String> tempOutputF1Strs = this.f1.compute(singletonInput, new ArrayList<String>());
			List<String> tempOutputF2Strs = this.f2.compute(singletonInput, new ArrayList<String>());
			
			for (String tempOutputF1Str : tempOutputF1Strs) {
				for (String tempOutputF2Str : tempOutputF2Strs) {
					outputStr.setLength(0);
					outputStr.append(tempOutputF1Str);
					outputStr.append(this.relationSymbol);
					outputStr.append(tempOutputF2Str);
					output.add(outputStr.toString());
				}
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
		if (parameter.equals("f1"))
			return this.f1.toParse();
		else if (parameter.equals("f2"))
			return this.f2.toParse();
		else if (parameter.equals("relationSymbol"))
			return Obj.stringValue(this.relationSymbol);
		else 
			return null;
	}
	
	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("f1")) {
			this.f1 = constructParameterF(parameterValue);
			if (this.f1 == null)
				return false;
		} else if (parameter.equals("f2")) {
			this.f2 = constructParameterF(parameterValue);
			if (this.f2 == null)
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
