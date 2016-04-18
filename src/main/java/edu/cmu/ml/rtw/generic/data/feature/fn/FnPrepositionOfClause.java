package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.ConstituencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.ConstituencyParse.Constituent;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnPrepositionOfClause computes the span of the preposition governing
 * a clause containing the head of a span.
 * 
 * @author Bill McDowell
 *
 */
public class FnPrepositionOfClause extends Fn<TokenSpan, TokenSpan> {
	private String[] parameterNames = {};
	
	public FnPrepositionOfClause() {
		
	}
	
	public FnPrepositionOfClause(Context context) {
		
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
	public <C extends Collection<TokenSpan>> C compute(Collection<TokenSpan> input, C output) {
		for (TokenSpan span : input) {
			if (span.getLength() == 0)
				continue;
			
			TokenSpan spanHead = span.getSubspan(span.getLength() - 1, span.getLength());
			ConstituencyParse parse = spanHead.getDocument().getConstituencyParse(span.getSentenceIndex());
			if (parse == null)
				continue;
			
			Constituent tokenConst = parse.getTokenConstituent(spanHead.getStartTokenIndex());
			Constituent parent = tokenConst.getParent();
			if (parent == null)
				continue;
			
			parent = parent.getParent();
			String pos = parent.getLabel();
			if (!pos.equals("PP")) {
				while (parent != null && parent.getLabel().equals(pos))
					parent = parent.getParent();
			}
			
			if (parent != null && parent.getLabel().equals("S")) {
				parent = parent.getParent();
				if (parent == null || !parent.getLabel().equals("PP"));
					continue;
			}
			
			if (parent != null && parent.getLabel().equals("PP")) {
				Constituent[] ppChildren = parent.getChildren();
				for (Constituent c : ppChildren) {
					if (c.getLabel().equals("IN")) {
						output.add(c.getTokenSpan());
						break;
					}	
				}
			}		
		}
		
		return output;
	}

	@Override
	public Fn<TokenSpan, TokenSpan> makeInstance(
			Context context) {
		return new FnPrepositionOfClause(context);
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
		return "PrepositionOfClause";
	}

}
