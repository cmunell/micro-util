package edu.cmu.ml.rtw.generic.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsable;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.rule.RuleSet;
import edu.cmu.ml.rtw.generic.util.Pair;

public class FnGreedyStructureRules<S extends WeightedStructure> extends Fn<S, S> {
	private Context context;
	
	private RuleSet rules;
	private Fn<S, ?> splitFn;
	private String[] parameterNames = { "rules", "splitFn" };
	
	public FnGreedyStructureRules() {
		
	}
	
	public FnGreedyStructureRules(Context context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("rules"))
			return (this.rules == null) ? null : Obj.curlyBracedValue(this.rules.getReferenceName());
		else if (parameter.equals("splitFn"))
			return (this.splitFn == null) ? null : Obj.curlyBracedValue(this.splitFn.getReferenceName());
		else 
			return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("rules"))
			this.rules = (parameterValue == null) ? null : this.context.getMatchRuleSet(parameterValue);
		else if (parameter.equals("splitFn"))
			this.splitFn = (parameterValue == null) ? null : (Fn<S, ?>)this.context.getMatchStructureFn(parameterValue);
		else 
			return false;
		return true;
	}

	@Override
	protected <C extends Collection<S>> C compute(Collection<S> input, C output) {
		for (S structure : input) {
			List<?> splitStructure = this.splitFn.listCompute(structure);
			System.out.println("Computed " + splitStructure.size() + " paths");
			List<Pair<List<CtxParsable>, Double>> orderedStructureParts = new ArrayList<Pair<List<CtxParsable>, Double>>();
			for (Object o : splitStructure) {
				WeightedStructure structurePart = (WeightedStructure)o;
				List<CtxParsable> structurePartList = structurePart.toList();
				double weight = 0.0;
				for (CtxParsable part : structurePartList) 
					weight += structurePart.getWeight(part);
				orderedStructureParts.add(new Pair<>(structurePartList, weight / (double)structurePartList.size()));
			}
			
			Collections.sort(orderedStructureParts, new Comparator<Pair<List<CtxParsable>, Double>>() {
				@Override
				public int compare(Pair<List<CtxParsable>, Double> o1, Pair<List<CtxParsable>, Double> o2) {
					return Double.compare(o2.getSecond(), o1.getSecond());
				}
				
			});
			
			for (Pair<List<CtxParsable>, Double> structurePart : orderedStructureParts) {
				Map<String, List<Obj>> objs = this.rules.apply(structurePart.getFirst());
				for (List<Obj> objList : objs.values())
					for (Obj obj : objList) {
						WeightedStructure newStructurePart = this.context.constructMatchWeightedStructure(obj);
						structure.add(newStructurePart, structurePart.getSecond());
					}
			}
			
			output.add(structure);
		}
		
		return output;
	}

	@Override
	public Fn<S, S> makeInstance(Context context) {
		return new FnGreedyStructureRules<S>(context);
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
		return "GreedyStructureRules";
	}
}
