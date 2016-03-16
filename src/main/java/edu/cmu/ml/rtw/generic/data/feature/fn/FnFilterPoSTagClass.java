package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTagClass;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnHead computes the head tokens of a collection
 * of token spans
 * 
 * @author Bill McDowell
 *
 */
public class FnFilterPoSTagClass extends Fn<TokenSpan, TokenSpan> {
	private String tagClassName;
	private PoSTag[] tagClass;
	private String[] parameterNames = { "tagClass" };
	
	private Context context;
	
	public FnFilterPoSTagClass() {
		
	}
	
	public FnFilterPoSTagClass(Context context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("tagClass"))
			return Obj.stringValue(this.tagClassName);
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("tagClass")) {
			this.tagClassName = this.context.getMatchValue(parameterValue);
			this.tagClass = PoSTagClass.fromString(this.tagClassName);
		} else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<TokenSpan>> C compute(Collection<TokenSpan> input, C output) {
		for (TokenSpan span : input) {
			
			boolean passesFilter = true;
			for (int i = span.getStartTokenIndex(); i < span.getEndTokenIndex(); i++) {
				PoSTag tag = span.getDocument().getPoSTag(span.getSentenceIndex(), i);
				if (!PoSTagClass.classContains(this.tagClass, tag)) {
					passesFilter = false;
					break;
				}
			}
			
			if (passesFilter)
				output.add(span);
		}
		
		return output;
	}

	@Override
	public Fn<TokenSpan, TokenSpan> makeInstance(
			Context context) {
		return new FnFilterPoSTagClass(context);
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
		return "FilterPoSTagClass";
	}

}
