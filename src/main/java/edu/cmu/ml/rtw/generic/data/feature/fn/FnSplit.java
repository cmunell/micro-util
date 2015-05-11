package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FnSplit extends Fn<String, String> {
	public enum From {
		FIRST,
		LAST
	}
	
	private String[] parameterNames = { "splitter", "chunkSize", "from", "limit" };
	private String splitter = "_";
	private int chunkSize = 1;
	private From from = From.FIRST;
	private int limit = 0;
	
	private Context<?, ?> context;

	public FnSplit() {
		
	}
	
	public FnSplit(Context<?, ?> context) {
		this.context = context;
	}
	
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("splitter"))
			return Obj.stringValue(this.splitter);
		else if (parameter.equals("chunkSize"))
			return Obj.stringValue(String.valueOf(this.chunkSize));
		else if (parameter.equals("from"))
			return Obj.stringValue(this.from.toString());
		else if (parameter.equals("limit"))
			return Obj.stringValue(String.valueOf(this.limit));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("splitter"))
			this.splitter = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("chunkSize"))
			this.chunkSize = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("from"))
			this.from = From.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("limit"))
			this.limit = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}
	
	@Override
	public <C extends Collection<String>> C compute(Collection<String> input, C output) {
		for (String str : input) {
			String[] parts = str.split(this.splitter);
			int numChunks = (this.limit == 0) ? 
									parts.length - this.chunkSize + 1 
									: Math.min(this.limit, parts.length - this.chunkSize + 1);
	
			
			for (int i = 0; i < numChunks; i++) {
				int chunkStartIndex = 0;
				if (this.from == From.FIRST)
					chunkStartIndex = i;
				else // From.LAST
					chunkStartIndex = parts.length - this.chunkSize - i;
				
				StringBuilder chunk = new StringBuilder();
				for (int j = chunkStartIndex; j < chunkStartIndex + this.chunkSize; j++) {
					chunk.append(parts[j]);
					chunk.append("_");
				}
				
				if (chunk.length() > 0)
					chunk.delete(chunk.length() - 1, chunk.length());
			
				output.add(chunk.toString());
			}
		}
			
		return output;
	}

	@Override
	public Fn<String, String> makeInstance(Context<?, ?> context) {
		return new FnSplit(context);
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
		return "Split";
	}

}
