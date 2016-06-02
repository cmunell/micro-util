package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;


public class FnRemoveLongStringParts extends Fn<String, String> {	
	private Context context;
	private int maxLength = 30;
	private String partSplit = "\\s+";
	private String partGlue = " ";
	private String[] parameterNames = { "maxLength", "partSplit", "partGlue" };
	
	public FnRemoveLongStringParts() {

	}
	
	public FnRemoveLongStringParts(Context context) {
		this.context = context;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("maxLength"))
			return Obj.stringValue(String.valueOf(this.maxLength));
		else if (parameter.equals("partSplit"))
			return Obj.stringValue(this.partSplit);
		else if (parameter.equals("partGlue"))
			return Obj.stringValue(this.partGlue);
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("maxLength"))
			this.maxLength = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("partSplit"))
			this.partSplit = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("partGlue"))
			this.partGlue = this.context.getMatchValue(parameterValue);
		else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<String> input, C output) {
		for (String str : input) {	
			String[] parts = str.split(this.partSplit);
			StringBuilder retStr = new StringBuilder();
			for (int i = 0; i < parts.length; i++) {
				if (parts[i].length() > this.maxLength) // remove long tokens
					continue;
				retStr = retStr.append(parts[i]).append(this.partGlue);
			}
			
			if (retStr.length() > 0)
				retStr.delete(retStr.length() - this.partGlue.length(), retStr.length());
			
			output.add(retStr.toString());
		}
		
		return output;
	}

	@Override
	public Fn<String, String> makeInstance(Context context) {
		return new FnRemoveLongStringParts(context);
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
		return "RemoveLongStringParts";
	}

}
