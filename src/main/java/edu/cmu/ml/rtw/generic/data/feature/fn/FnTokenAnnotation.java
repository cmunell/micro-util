package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnTokenAnnotation takes a collection of token spans and computes
 * their corresponding token annotations
 * sequences.
 * 
 * @author Bill McDowell
 *
 */
public class FnTokenAnnotation extends Fn<TokenSpan, String> {
	private AnnotationTypeNLP<?> annotationType;
	private String[] parameterNames = { "annotationType" };
	
	private Context context;
	
	public FnTokenAnnotation() {
		
	}
	
	public FnTokenAnnotation(Context context) {
		this.context = context;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("annotationType"))
			return Obj.stringValue(this.annotationType.getType());
		else
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("annotationType")) {
			this.annotationType = this.context.getDataTools()
									.getAnnotationTypeNLP(this.context.getMatchValue(parameterValue));
		} else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<TokenSpan> input, C output) {
		for (TokenSpan tokenSpan : input) {
			StringBuilder str = new StringBuilder();
			DocumentNLP document = tokenSpan.getDocument();
			for (int i = tokenSpan.getStartTokenIndex(); i < tokenSpan.getEndTokenIndex(); i++)
				str.append(
						document.getTokenAnnotation(this.annotationType, tokenSpan.getSentenceIndex(), i).toString())
				   .append("_");
			output.add(str.toString());
		}
		
		return output;
	}

	@Override
	public Fn<TokenSpan, String> makeInstance(Context context) {
		return new FnTokenAnnotation(context);
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
		return "TokenAnnotation";
	}

}
