package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Predicate;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

public class FnPredicateArgument extends Fn<TokenSpan, TokenSpan> {
	private String tagFilter = null;
	private String[] parameterNames = { "tagFilter" };
	
	private Context context;

	
	public FnPredicateArgument() {
		
	}
	
	public FnPredicateArgument(Context context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("tagFilter"))
			return Obj.stringValue(this.tagFilter);
		else
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("tagFilter"))
			this.tagFilter = this.context.getMatchValue(parameterValue);
		else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<TokenSpan>> C compute(Collection<TokenSpan> input, C output) {		
		for (TokenSpan tokenSpan : input) {
			List<Pair<TokenSpan, Predicate>> preds = tokenSpan.getDocument().getTokenSpanAnnotations(AnnotationTypeNLP.PREDICATE, tokenSpan);
			for (Pair<TokenSpan, Predicate> pred : preds) {
				if (this.tagFilter != null) {
					TokenSpan[] spans = pred.getSecond().getArgument(this.tagFilter);
					if (spans != null) {
						for (TokenSpan span : spans)
							output.add(span);
					}
				} else {
					Set<String> argTags = pred.getSecond().getArgumentTags();
					for (String tag : argTags) {
						TokenSpan[] spans = pred.getSecond().getArgument(tag);
						for (TokenSpan span : spans)
							output.add(span);
					}
				}
			}
		}
		
		return output;
	}

	@Override
	public Fn<TokenSpan, TokenSpan> makeInstance(Context context) {
		return new FnPredicateArgument(context);
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
		return "PredicateArgument";
	}
	
	@Override
	public List<String> computeRelations(TokenSpan input, TokenSpan output) {
		List<String> relations = new ArrayList<String>();
		List<Pair<TokenSpan, Predicate>> preds = input.getDocument().getTokenSpanAnnotations(AnnotationTypeNLP.PREDICATE, input);
		for (Pair<TokenSpan, Predicate> pred : preds) {
			if (this.tagFilter != null) {
				TokenSpan[] spans = pred.getSecond().getArgument(this.tagFilter);
				if (spans != null) {
					for (TokenSpan span : spans) {
						if (span.equals(output)) {
							relations.add(this.tagFilter);
							return relations;
						}
					}
				}
			} else {
				Set<String> argTags = pred.getSecond().getArgumentTags();
				for (String tag : argTags) {
					TokenSpan[] spans = pred.getSecond().getArgument(tag);
					for (TokenSpan span : spans) {
						if (span.equals(output)) {
							relations.add(tag);
						}
					}
				}
			}
		}
		
		return relations;
	}
}
