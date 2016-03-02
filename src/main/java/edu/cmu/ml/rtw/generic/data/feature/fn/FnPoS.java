package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.cluster.ClustererTokenSpanPoSTag;
import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnPos takes a collection of token spans and computes
 * their corresponding part-of-speech tag
 * sequences.
 * 
 * @author Bill McDowell
 *
 */
public class FnPoS extends Fn<TokenSpan, String> {
	private ClustererTokenSpanPoSTag clusterer = new ClustererTokenSpanPoSTag();
	private String[] parameterNames = {  };
	
	public FnPoS() {
		
	}
	
	public FnPoS(Context context) {
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
	public <C extends Collection<String>> C compute(Collection<TokenSpan> input, C output) {
		for (TokenSpan tokenSpan : input) {
			output.addAll(this.clusterer.getClusters(tokenSpan));
		}
		
		return output;
	}

	@Override
	public Fn<TokenSpan, String> makeInstance(Context context) {
		return new FnPoS(context);
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
		return "PoS";
	}

}
