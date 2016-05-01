package edu.cmu.ml.rtw.generic.rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsable;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class RuleSet extends CtxParsableFunction {
	private List<Rule> rules;
	private String[] parameterNames = { "rules" };
	
	private Context context;
	
	public RuleSet() {
		this(null);
	}
	
	public RuleSet(Context context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("rules")) {
			if (this.rules == null)
				return null;
			Obj.Array rulesObj = new Obj.Array();
			for (Rule rule : this.rules) {
				rulesObj.add(Obj.curlyBracedValue(rule.getReferenceName()));
			}
			return rulesObj;
		} else {
			return null;
		}
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("rules")) {
			if (parameterValue == null) {
				this.rules = null;
				return true;
			}
			
			this.rules = new ArrayList<Rule>();
			Obj.Array rulesObj = (Obj.Array)parameterValue;
			for (int i = 0; i < rulesObj.size(); i++) {
				Rule r = this.context.getMatchRule(rulesObj.get(i));				
				this.rules.add(r);
			}		
		} else {
			return false;
		}
		
		return true;
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
		return "RuleSet";
	}

	public Map<String, List<Obj>> apply(CtxParsable sourceObj) {
		return applyToParse(sourceObj.toParse(), null);
	}
	
	public Map<String, List<Obj>> apply(CtxParsable sourceObj, Map<String, Obj> extraAssignments) {
		return applyToParse(sourceObj.toParse(), extraAssignments);
	}
	
	public <P extends CtxParsable> Map<String, List<Obj>> apply(List<P> sources) {
		List<Obj> objs = new ArrayList<Obj>();
		for (P sourceObj : sources)
			objs.add(sourceObj.toParse());
		return applyToParses(objs, null);
	}
	
	public <P extends CtxParsable> Map<String, List<Obj>> apply(List<P> sources, Map<String, Obj> extraAssignments) {
		List<Obj> objs = new ArrayList<Obj>();
		for (P sourceObj : sources)
			objs.add(sourceObj.toParse());
		return applyToParses(objs, extraAssignments);
	}
	
	public Map<String, List<Obj>> applyToParse(Obj sourceObj) {
		return applyToParse(sourceObj, null);
	}
	
	public Map<String, List<Obj>> applyToParse(Obj sourceObj, Map<String, Obj> extraAssignments) {
		List<Obj> sourceObjs = new ArrayList<Obj>();
		sourceObjs.add(sourceObj);
		return applyToParses(sourceObjs, extraAssignments);
	}
	
	public Map<String, List<Obj>> applyToParses(List<Obj> sourceObjs) {
		return applyToParses(sourceObjs, null);
	}
	
	
	public Map<String, List<Obj>> applyToParses(List<Obj> sourceObjs, Map<String, Obj> extraAssignments) {
		Map<String, List<Obj>> outputs = new HashMap<String, List<Obj>>();
		
		for (Rule rule : this.rules) {
			List<Obj> ruleOutputs = rule.applyToParses(sourceObjs, extraAssignments);
			if (ruleOutputs.size() > 0)
				outputs.put(rule.getReferenceName(), ruleOutputs);
		}
		
		return outputs;
	}
}
