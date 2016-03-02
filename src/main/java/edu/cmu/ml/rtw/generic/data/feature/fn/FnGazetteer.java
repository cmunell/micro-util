package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * FnGazetteer computes the gazetteer ids with weights above a
 * given threshold on a collection of input strings.
 * 
 * Parameters:
 *  gazetteer - the gazetteer by which to compute the ids
 *  
 *  weightThreshold - the weight threshold above which gazetteer
 *  ids are included in the output.
 * 
 * @author Bill McDowell
 *
 */
public class FnGazetteer extends Fn<String, String> {
	private String[] parameterNames = { "gazetteer", "weightThreshold" };
	private Gazetteer gazetteer;
	private double weightThreshold;
	
	private Context context;

	public FnGazetteer() {
		
	}
	
	public FnGazetteer(Context context) {
		this.context = context;
	}
	
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("gazetteer"))
			return Obj.stringValue((this.gazetteer == null) ? "" : this.gazetteer.getName());
		else if (parameter.equals("weightThreshold"))
			return Obj.stringValue(String.valueOf(this.weightThreshold));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("gazetteer"))
			this.gazetteer = this.context.getDataTools().getGazetteer(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("weightThreshold"))
			this.weightThreshold = Double.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}
	
	@Override
	public <C extends Collection<String>> C compute(Collection<String> input, C output) {
		for (String str : input) {
			List<Pair<String, Double>> ids = this.gazetteer.getWeightedIds(str);
			if (ids == null)
				continue;
			
			for (Pair<String, Double> id : ids) {
				if (id.getSecond() >= this.weightThreshold) {
					output.add(id.getFirst());
				}
			}
		}
		
		return output;
	}

	@Override
	public Fn<String, String> makeInstance(Context context) {
		return new FnGazetteer(context);
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
		return "Gazetteer";
	} 

}
