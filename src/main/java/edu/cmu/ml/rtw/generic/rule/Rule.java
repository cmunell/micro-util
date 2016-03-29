package edu.cmu.ml.rtw.generic.rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsable;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class Rule extends CtxParsable {
	private static enum SpecialFn {
		And,
		Or,
		Not,
		Equals;
		
		public static SpecialFn getFn(Obj obj) {
			Obj.Function fn = (Obj.Function)obj;			
			for (SpecialFn specialFn : SpecialFn.values())
				if (fn.getName().equals(specialFn.toString()))
					return specialFn;
			return null;
		}
	}
	
	private Obj.Rule parse;
	
	public Rule() {
		
	}
	
	@Override
	public Obj toParse() {
		return this.parse;
	}

	@Override
	protected boolean fromParseHelper(Obj obj) {
		this.parse = (Obj.Rule)obj;
		return true;
	}
	
	public List<Obj> apply(CtxParsable sourceObj) {
		return applyToParse(sourceObj.toParse(), null);
	}
	
	public List<Obj> apply(CtxParsable sourceObj, Map<String, Obj> extraAssignments) {
		return applyToParse(sourceObj.toParse(), extraAssignments);
	}
	
	public <P extends CtxParsable> List<Obj> apply(List<P> sources) {
		List<Obj> objs = new ArrayList<Obj>();
		for (P sourceObj : sources)
			objs.add(sourceObj.toParse());
		return applyToParses(objs, null);
	}
	
	public <P extends CtxParsable> List<Obj> apply(List<P> sources, Map<String, Obj> extraAssignments) {
		List<Obj> objs = new ArrayList<Obj>();
		for (P sourceObj : sources)
			objs.add(sourceObj.toParse());
		return applyToParses(objs, extraAssignments);
	}
	
	public List<Obj> applyToParse(Obj sourceObj) {
		return applyToParse(sourceObj, null);
	}
	
	public List<Obj> applyToParse(Obj sourceObj, Map<String, Obj> extraAssignments) {
		List<Obj> sourceObjs = new ArrayList<Obj>();
		sourceObjs.add(sourceObj);
		return applyToParses(sourceObjs, extraAssignments);
	}
	
	public List<Obj> applyToParses(List<Obj> sourceObjs) {
		return applyToParses(sourceObjs, null);
	}
	
	
	public List<Obj> applyToParses(List<Obj> sourceObjs, Map<String, Obj> extraAssignments) {
		List<Obj> outputs = new ArrayList<Obj>();
		List<Map<String, Obj>> matches = extendMatches(match(this.parse.getSource(), sourceObjs), extraAssignments);
		for (Map<String, Obj> match : matches) {
			Obj target = this.parse.getTarget().clone();
			target.resolveValues(match);
			outputs.add(target);
		}
		
		return outputs;
	}
	
	private List<Map<String, Obj>> match(Obj pattern, List<Obj> sourceObjs) {
		return match(pattern, sourceObjs, new HashMap<String, Obj>());
	}
	
	private List<Map<String, Obj>> match(Obj pattern, List<Obj> sourceObjs, Map<String, Obj> match) {
		SpecialFn specialFn = SpecialFn.getFn(pattern);
		if (specialFn == SpecialFn.And) {
			return matchAnd((Obj.Function)pattern, sourceObjs, match);
		} else if (specialFn == SpecialFn.Or) {
			return matchOr((Obj.Function)pattern, sourceObjs, match);
		} else if (specialFn == SpecialFn.Not) {
			return matchNot((Obj.Function)pattern, sourceObjs, match);
		} else if (specialFn == SpecialFn.Equals) {
			return matchEquals((Obj.Function)pattern, sourceObjs, match);
		} else {
			return matchAtom(pattern, sourceObjs, match);
		}
	}
	
	private List<Map<String, Obj>> extendMatches(List<Map<String, Obj>> matches, Map<String, Obj> extraAssignments) {
		List<Map<String, Obj>> retMatches = new ArrayList<Map<String, Obj>>();
		
		for (Map<String, Obj> match : matches) {
			// FIXME this is sort of a hack to allow incrementing numbers in rules... do this more
			// systematically later
			Map<String, Obj> incrementedObjs = new HashMap<String, Obj>();
			for (Entry<String, Obj> entry : match.entrySet()) {
				if (entry.getValue().getObjType() == Obj.Type.VALUE) {
					Obj.Value vObj = (Obj.Value)entry.getValue();
					if (vObj.getType() == Obj.Value.Type.STRING
							&& vObj.getStr().matches("[0-9]+")) {
						int matchValue = Integer.valueOf(vObj.getStr());
						// FIXME This is a hack to allow checking that the matched number is less than some specified number
						if (entry.getKey().contains("<")) { 
							String[] keyParts = entry.getKey().split("<");
							if (matchValue >= Integer.valueOf(keyParts[1].trim())) {
								match = new HashMap<String, Obj>();
								break;
							}
						}
						
						incrementedObjs.put(entry.getKey() + "++", Obj.stringValue(String.valueOf(matchValue + 1)));
					}
				}
			}
			
			if (match.size() == 0) // FIXME This happens when n<k match failed above (it's a hack)
				continue;
			
			match.putAll(incrementedObjs);
	
			if (extraAssignments != null)
				match.putAll(extraAssignments);
			
			match.put("RULE", Obj.stringValue(this.getReferenceName()));
			
			retMatches.add(match);
		}
		
		return retMatches;
	}
	
	private List<Map<String, Obj>> matchAnd(Obj.Function pattern, List<Obj> sourceObjs, Map<String, Obj> match) {
		AssignmentList args = pattern.getParameters();
		List<Map<String, Obj>> possibleMatches = new ArrayList<Map<String, Obj>>();
		possibleMatches.add(match);
		for (int i = 0; i < args.size(); i++) {
			List<Map<String, Obj>> nextPossibleMatches = new ArrayList<Map<String, Obj>>();
			for (Map<String, Obj> currentMatch : possibleMatches) {
				Obj argPattern = args.get(i).getValue().clone();
				argPattern.resolveValues(currentMatch);
				nextPossibleMatches.addAll(match(argPattern, sourceObjs, currentMatch));
			}
			
			possibleMatches = nextPossibleMatches;
			
			if (possibleMatches.size() == 0)
				return possibleMatches;
		}
		
		return possibleMatches;
	}
	
	private List<Map<String, Obj>> matchOr(Obj.Function pattern, List<Obj> sourceObjs, Map<String, Obj> match) {
		AssignmentList args = pattern.getParameters();
		List<Map<String, Obj>> possibleMatches = new ArrayList<Map<String, Obj>>();
		possibleMatches.add(match);
		for (int i = 0; i < args.size(); i++) {
			for (Map<String, Obj> currentMatch : possibleMatches) {
				Obj argPattern = args.get(i).getValue().clone();
				argPattern.resolveValues(currentMatch);
				possibleMatches.addAll(match(argPattern, sourceObjs, currentMatch));
			}
		}
		
		possibleMatches.remove(match);
		
		return possibleMatches;
	}
	
	private List<Map<String, Obj>> matchNot(Obj.Function pattern, List<Obj> sourceObjs, Map<String, Obj> match) {
		AssignmentList args = pattern.getParameters();
		if (args.size() != 1)
			throw new UnsupportedOperationException();
		
		Obj argPattern = args.get(0).getValue().clone();
		argPattern.resolveValues(match);
		
		List<Map<String, Obj>> matches = new ArrayList<Map<String, Obj>>();
		if (match(argPattern, sourceObjs, match).size() == 0) {
			matches.add(match);
		}
		
		return matches;
	}

	private List<Map<String, Obj>> matchEquals(Obj.Function pattern, List<Obj> sourceObjs, Map<String, Obj> match) {
		Obj.Function clonePattern = (Obj.Function)pattern.clone();
		clonePattern.resolveValues(match);
		
		AssignmentList args = clonePattern.getParameters();
		
		if (args.size() < 2)
			throw new UnsupportedOperationException();
		
		List<Map<String, Obj>> matches = new ArrayList<Map<String, Obj>>();
		
		Obj value = args.get(0).getValue();
		for (int i = 1; i < args.size(); i++) {
			if (!args.get(i).getValue().equals(value))
				return matches;
		}
		
		matches.add(match);
		
		return matches;
	}
	
	private List<Map<String, Obj>> matchAtom(Obj pattern, List<Obj> sourceObjs, Map<String, Obj> match) {
		List<Map<String, Obj>> matches = new ArrayList<Map<String, Obj>>();
		
		for (Obj sourceObj : sourceObjs) {
			Map<String, Obj> currentMatch = sourceObj.match(pattern);
			if (currentMatch.containsKey("")) {
				currentMatch.putAll(match);
				matches.add(currentMatch);
			}
		}
		
		return matches;
	}
}
