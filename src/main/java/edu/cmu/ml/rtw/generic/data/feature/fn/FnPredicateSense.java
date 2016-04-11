package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Predicate;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

public class FnPredicateSense extends Fn<TokenSpan, String> {
	private String[] parameterNames = {  };
	
	public FnPredicateSense() {
		
	}
	
	public FnPredicateSense(Context context) {

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
		return false;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<TokenSpan> input, C output) {		
		for (TokenSpan tokenSpan : input) {
			List<Pair<TokenSpan, Predicate>> preds = tokenSpan.getDocument().getTokenSpanAnnotations(AnnotationTypeNLP.PREDICATE, tokenSpan);
			for (Pair<TokenSpan, Predicate> pred : preds)
				output.add(pred.getSecond().getSense());
		}
		
		return output;
	}

	@Override
	public Fn<TokenSpan, String> makeInstance(Context context) {
		return new FnPredicateSense(context);
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
		return "PredicateSense";
	}
}
