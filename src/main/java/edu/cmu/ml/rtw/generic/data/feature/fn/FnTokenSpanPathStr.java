package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

public class FnTokenSpanPathStr extends Fn<TokenSpan, String> {
	public enum Mode {
		ONLY_RELATIONS,
		ONLY_SPANS,
		RELATIONS_AND_FINAL_SPAN,
		FINAL_SPAN,
		ALL
	}
	
	private String[] parameterNames = { "mode", "pathLength", "spanFn1", "spanFn2", "spanFn3", "strFn", "multiRelation" };
	
	private Mode mode = Mode.ALL;
	private int pathLength = 1;
	private List<Fn<TokenSpan, TokenSpan>> spanFns;
	private Fn<TokenSpan, String> strFn;
	private boolean multiRelation = false;
	
	private Context context;
	
	public FnTokenSpanPathStr() {
		
	}
	
	public FnTokenSpanPathStr(Context context) {
		this.context = context;
		this.spanFns = new ArrayList<Fn<TokenSpan, TokenSpan>>();
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("mode"))
			return Obj.stringValue(this.mode.toString());
		else if (parameter.equals("pathLength"))
			return Obj.stringValue(String.valueOf(this.pathLength));
		else if (parameter.equals("spanFn1"))
			if (this.spanFns.size() < 1)
				return Obj.stringValue("");
			else
				return this.spanFns.get(0).toParse();
		else if (parameter.equals("spanFn2"))
			if (this.spanFns.size() < 2)
				return Obj.stringValue("");
			else
				return this.spanFns.get(1).toParse();
		else if (parameter.equals("spanFn3"))
			if (this.spanFns.size() < 3)
				return Obj.stringValue("");
			else
				return this.spanFns.get(2).toParse();
		else if (parameter.equals("strFn"))
			return this.strFn.toParse();
		else if (parameter.equals("multiRelation"))
			return Obj.stringValue(String.valueOf(this.multiRelation));
		else 
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("mode"))
			this.mode = Mode.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("pathLength"))
			this.pathLength = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("spanFn1") || parameter.equals("spanFn2") || parameter.equals("spanFn3")) {
			if (parameterValue.getObjType() != Obj.Type.VALUE || ((Obj.Value)parameterValue).getType() == Obj.Value.Type.CURLY_BRACED)
				this.spanFns.add(this.context.getMatchOrRunCommandTokenSpanFn(parameterValue));
		} else if (parameter.equals("multiRelation"))
			this.multiRelation = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("strFn"))
			this.strFn = this.context.getMatchOrConstructTokenSpanStrFn(parameterValue);
		else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<TokenSpan> input, C output) {
		List<String> initPaths = new ArrayList<String>();
		initPaths.add("");
		
		List<Pair<TokenSpan, List<String>>> currentSpans = new ArrayList<Pair<TokenSpan, List<String>>>();
		List<TokenSpan> singletonSpan = new ArrayList<TokenSpan>();
		Set<TokenSpan> visitedSpans = new HashSet<TokenSpan>();
		
		for (TokenSpan span : input)
			currentSpans.add(new Pair<TokenSpan, List<String>>(span, initPaths));
		
		for (int i = 0; i < this.pathLength; i++) {
			List<Pair<TokenSpan, List<String>>> nextSpans = new ArrayList<Pair<TokenSpan, List<String>>>(); 
			for (Pair<TokenSpan, List<String>> currentSpan : currentSpans) {
				singletonSpan.add(currentSpan.getFirst());

				nextSpans.addAll(computeTokenSpanStep(singletonSpan, currentSpan.getSecond(), visitedSpans, i == this.pathLength - 1));
				singletonSpan.clear();
			}
			
			currentSpans = nextSpans;
		}
		
		for (Pair<TokenSpan, List<String>> span : currentSpans) {
			output.addAll(span.getSecond());
		}
		
		return output;
	}

	private List<Pair<TokenSpan, List<String>>> computeTokenSpanStep(List<TokenSpan> singletonSpan, List<String> paths, Set<TokenSpan> visitedSpans, boolean finalSpan) {
		List<Pair<TokenSpan, List<String>>> nextPaths = new ArrayList<Pair<TokenSpan, List<String>>>();
		List<TokenSpan> allNextSpans = new ArrayList<>();
		for (Fn<TokenSpan, TokenSpan> spanFn : this.spanFns) {
			List<TokenSpan> nextSpans = spanFn.compute(singletonSpan, new ArrayList<TokenSpan>());	
			allNextSpans.addAll(nextSpans);
			for (TokenSpan nextSpan : nextSpans) {
				if (!this.multiRelation && visitedSpans.contains(nextSpan))
					continue;
				
				List<String> relationStrs = null;
				if (this.mode == Mode.ALL || this.mode == Mode.ONLY_RELATIONS || this.mode == Mode.RELATIONS_AND_FINAL_SPAN) {
					relationStrs = spanFn.computeRelations(singletonSpan.get(0), nextSpan);
				}
				
				List<String> nextSpanStrs = null;
				if (this.mode == Mode.ALL || this.mode == Mode.ONLY_SPANS 
						|| (finalSpan && (this.mode == Mode.FINAL_SPAN || this.mode == Mode.RELATIONS_AND_FINAL_SPAN))) {
					TokenSpan curSpan = singletonSpan.get(0);
					singletonSpan.clear();
					singletonSpan.add(nextSpan);
					nextSpanStrs = this.strFn.compute(singletonSpan, new ArrayList<String>());
					singletonSpan.clear();
					singletonSpan.add(curSpan);
				}
				
				List<String> nextPathSpanStrs = new ArrayList<String>();
				if (relationStrs != null && nextSpanStrs != null) {
					for (String pathStr : paths) {
						for (String relationStr : relationStrs) {
							for (String nextSpanStr : nextSpanStrs) {
								nextPathSpanStrs.add(pathStr + "_" + relationStr + "_" + nextSpanStr);
							}
						}
					}
				} else if (relationStrs != null) {
					for (String pathStr : paths) {
						for (String relationStr : relationStrs) {
							nextPathSpanStrs.add(pathStr + "_" + relationStr);
						}
					}
				} else if (nextSpanStrs != null) {
					for (String pathStr : paths) {
						for (String nextSpanStr : nextSpanStrs) {
							nextPathSpanStrs.add(pathStr + "_" + nextSpanStr);
						}
					}
				}
				
				nextPaths.add(new Pair<TokenSpan, List<String>>(nextSpan, nextPathSpanStrs));
			}
		}
		
		visitedSpans.addAll(allNextSpans);
		
		return nextPaths;
	}
	
	@Override
	public Fn<TokenSpan, String> makeInstance(Context context) {
		return new FnTokenSpanPathStr(context);
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
		return "TokenSpanPathStr";
	}
}
