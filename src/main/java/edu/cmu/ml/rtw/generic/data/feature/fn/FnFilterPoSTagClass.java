package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTagClass;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;


public class FnFilterPoSTagClass extends Fn<TokenSpan, TokenSpan> {
	public enum Mode {
		ONLY,
		NONE
	}
	
	private List<String> tagClassNames;
	private PoSTag[] tagClass;
	private Mode mode = Mode.ONLY;
	private String[] parameterNames = { "tagClass", "mode" };
	
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
		if (parameter.equals("tagClass")) {
			if (this.tagClassNames.size() == 1)
				return Obj.stringValue(this.tagClassNames.get(0));
			else {
				return Obj.array(this.tagClassNames);
			}
		} else if (parameter.equals("mode"))
			return Obj.stringValue(this.mode.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("tagClass")) {
			if (this.context.getMatchArray(parameterValue) != null) {
				this.tagClassNames = this.context.getMatchArray(parameterValue);
				List<PoSTag> tagClassList = new ArrayList<>();
				for (String tagClassName : this.tagClassNames)
					tagClassList.addAll(Arrays.asList(PoSTagClass.fromString(tagClassName)));
				this.tagClass = tagClassList.toArray(new PoSTag[0]);
			} else {
				this.tagClassNames = new ArrayList<>();
				this.tagClassNames.add(this.context.getMatchValue(parameterValue));
				this.tagClass = PoSTagClass.fromString(this.tagClassNames.get(0));
			}
		} else if (parameter.equals("mode")) {
			this.mode = parameterValue == null ? Mode.ONLY : Mode.valueOf(this.context.getMatchValue(parameterValue));
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
				if ((this.mode == Mode.ONLY && !PoSTagClass.classContains(this.tagClass, tag))
						|| (this.mode == Mode.NONE && PoSTagClass.classContains(this.tagClass, tag))) {
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
