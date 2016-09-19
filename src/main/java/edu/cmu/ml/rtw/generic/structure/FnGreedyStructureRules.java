package edu.cmu.ml.rtw.generic.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsable;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.rule.RuleSet;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;

public class FnGreedyStructureRules<S extends WeightedStructure> extends FnStructure<S, S> {
	private static final double EPSILON = .1;
	
	private Context context;
	
	private Obj.Array rulesRefs;
	private List<RuleSet> rules;
	private Obj.Array splitFnsRefs;
	private List<FnStructure<S, ?>> splitFns;
	private int maxIterations = 0;
	private boolean singleRuleSetPerIteration = false;
	private int maxIterationSize = -1;
	private boolean backtracking = false;
	private String[] parameterNames = { "rules", "splitFns", "maxIterations", "singleRuleSetPerIteration", "maxIterationSize", "backtracking" };
	
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
		if (parameter.equals("rules")) {
			return (this.rulesRefs == null) ? null : this.rulesRefs;
		} else if (parameter.equals("splitFns")) {
			return (this.splitFnsRefs == null) ? null : this.splitFnsRefs;
		} else if (parameter.equals("maxIterations")) {
			return Obj.stringValue(String.valueOf(this.maxIterations));
		} else if (parameter.equals("singleRuleSetPerIteration")) {
			return Obj.stringValue(String.valueOf(this.singleRuleSetPerIteration));
		} else if (parameter.equals("maxIterationSize")) {
			return Obj.stringValue(String.valueOf(this.maxIterationSize));
		} else if (parameter.equals("backtracking")) {
			return Obj.stringValue(String.valueOf(this.backtracking));
		} else 
			return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("rules")) {
			this.rulesRefs = (Obj.Array)parameterValue;
			if (this.rulesRefs != null) {
				this.rules = new ArrayList<>();
				for (int i = 0; i < this.rulesRefs.size(); i++)
					this.rules.add(this.context.getMatchRuleSet(this.rulesRefs.get(i)));
			} else {
				this.rules = null;
			}
		} else if (parameter.equals("splitFns")) {
			this.splitFnsRefs = (Obj.Array)parameterValue;
			if (this.splitFnsRefs != null) {
				this.splitFns = new ArrayList<>();
				for (int i = 0; i < this.splitFnsRefs.size(); i++)
					this.splitFns.add((FnStructure<S, ?>)this.context.getMatchStructureFn(this.splitFnsRefs.get(i)));
			} else {
				this.splitFns = null;
			}
		} else if (parameter.equals("maxIterations")) {
			this.maxIterations = Integer.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("singleRuleSetPerIteration")) {
			this.singleRuleSetPerIteration = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("maxIterationSize")) {
			this.maxIterationSize = Integer.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("backtracking")) {
			this.backtracking = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		} else 
			return false;
		return true;
	}

	@Override
	protected <C extends Collection<S>, F extends WeightedStructure> C compute(Collection<S> input, C output, Collection<F> filter) {
		if (!this.singleRuleSetPerIteration)
			return computeDefault(input, output, filter);
		else
			return computeOneRuleSetPerIteration(input, output, filter);	
	}
	
	private <C extends Collection<S>, F extends WeightedStructure> C computeDefault(Collection<S> input, C output, Collection<F> filter) {
		for (S structure : input) {
			int iterations = 0;
			int prevFilterSize = (filter != null) ? filter.size() : 0;
			double weightChange = 0;
			do {
				filter = limitFilterSize(structure, filter, iterations);
				
				//long startTime = System.currentTimeMillis();
					
				List<Triple<List<CtxParsable>, Pair<Double, Object>, Integer>> orderedStructureParts = new ArrayList<Triple<List<CtxParsable>, Pair<Double, Object>, Integer>>();
				for (int i = 0; i < this.splitFns.size(); i++) {
					FnStructure<S, ?> splitFn = this.splitFns.get(i);
					
					List<?> splitStructure = splitFn.listCompute(structure, filter);
					
					for (Object o : splitStructure) {
						WeightedStructure structurePart = (WeightedStructure)o;
						List<CtxParsable> structurePartList = structurePart.toList();
						double weight = 0.0;
						StringBuilder sources = new StringBuilder();
						
						for (CtxParsable part : structurePartList) {
							weight += structurePart.getWeight(part);
							sources.append(structurePart.getSource(part)).append(";");
						}
		
						if (sources.length() > 0)
							sources.delete(sources.length() - 1, sources.length());
						
						orderedStructureParts.add(new Triple<>(structurePartList, 
								new Pair<Double, Object>(weight / (double)structurePartList.size(), sources.toString()), 
								i));
					}
				}
				
				Collections.sort(orderedStructureParts, new Comparator<Pair<List<CtxParsable>, Pair<Double, Object>>>() {
					@Override
					public int compare(Pair<List<CtxParsable>, Pair<Double, Object>> o1, Pair<List<CtxParsable>, Pair<Double, Object>> o2) {
						return Double.compare(o2.getSecond().getFirst(), o1.getSecond().getFirst());
					}
				});
				
				prevFilterSize = (filter != null) ? filter.size() : 0;
				filter = new HashSet<F>();
				double totalWeight = structure.getTotalWeight();
				
				//this.context.getDataTools().getOutputWriter().debugWriteln("Greedy inference running on iteration " + iterations + " trying to add " + orderedStructureParts.size() + " to graph ");
				
				for (Triple<List<CtxParsable>, Pair<Double, Object>, Integer> structurePart : orderedStructureParts) {
					Map<String, List<Obj>> objs = this.rules.get(structurePart.getThird()).apply(structurePart.getFirst());
					for (Entry<String, List<Obj>> objList : objs.entrySet()) {
						for (Obj obj : objList.getValue()) {
							WeightedStructure newStructurePart = this.context.constructMatchWeightedStructure(obj);
							
							int prevItemCount = structure.getItemCount();
							
							structure.add(newStructurePart, structurePart.getSecond().getFirst(), objList.getKey() + "(" + structurePart.getSecond().getSecond() + ")", filter);
							
							// FIXME This currently doesn't really work the way it should
							if (this.backtracking && prevItemCount == structure.getItemCount()) {
								double minWeight = Double.POSITIVE_INFINITY;
								CtxParsable minPart = null;
								for (CtxParsable part : structurePart.getFirst()) {
									double weight = structure.getWeight(part);
									if (Double.compare(weight, minWeight) < 0) {
										minWeight = weight;
										minPart = part;
									}
								}
								
								structure.remove(minPart);
							}
						}
					}
				}
				
				//this.context.getDataTools().getOutputWriter().debugWriteln("Greedy inference running iteration " + iterations + " on size " + iterFilterSize + " (" + (System.currentTimeMillis() - startTime) + ")");
				
				iterations++;
				weightChange = structure.getTotalWeight() - totalWeight;
			} while ((this.maxIterations == 0 || iterations <= this.maxIterations) && filter.size() > 0 && (filter.size() != prevFilterSize || weightChange > EPSILON));
			
			output.add(structure);
		}
		
		return output;
	}
	
	private <C extends Collection<S>, F extends WeightedStructure> C computeOneRuleSetPerIteration(Collection<S> input, C output, Collection<F> filter) {
		for (S structure : input) {
			int iterations = 0;
			int prevFilterSize = (filter != null) ? filter.size() : 0;
			double weightChange = 0;
			int splitFnIndex = 0;
			do {
				//System.out.println("Iter " + iterations + "\n" + structure);
				filter = limitFilterSize(structure, filter, iterations);
				
				//long startTime = System.currentTimeMillis();
					
				List<Triple<List<CtxParsable>, Pair<Double, Object>, Integer>> orderedStructureParts = new ArrayList<Triple<List<CtxParsable>, Pair<Double, Object>, Integer>>();
				
				splitFnIndex = iterations % this.splitFns.size();
				FnStructure<S, ?> splitFn = this.splitFns.get(splitFnIndex);
				
				/*System.out.println("FILTER");
				for (F f : filter)
					System.out.println(f);
				System.out.println("RULES STRUCTURE");*/
				//System.out.println(structure);
				
				List<?> splitStructure = splitFn.listCompute(structure, filter);
				
				for (Object o : splitStructure) {
					WeightedStructure structurePart = (WeightedStructure)o;
					List<CtxParsable> structurePartList = structurePart.toList();
					double weight = 0.0;
					StringBuilder sources = new StringBuilder();
					
					for (CtxParsable part : structurePartList) {
						weight += structurePart.getWeight(part);
						sources.append(structurePart.getSource(part)).append(";");
					}
	
					if (sources.length() > 0)
						sources.delete(sources.length() - 1, sources.length());
					
					orderedStructureParts.add(new Triple<>(structurePartList, 
							new Pair<Double, Object>(weight / (double)structurePartList.size(), sources.toString()), 
							splitFnIndex));
				}
				
				Collections.sort(orderedStructureParts, new Comparator<Pair<List<CtxParsable>, Pair<Double, Object>>>() {
					@Override
					public int compare(Pair<List<CtxParsable>, Pair<Double, Object>> o1, Pair<List<CtxParsable>, Pair<Double, Object>> o2) {
						return Double.compare(o2.getSecond().getFirst(), o1.getSecond().getFirst());
					}	
				});
				
				if (splitFnIndex == this.splitFns.size() - 1) {
					prevFilterSize = (filter != null) ? filter.size() : 0;
					filter = new HashSet<F>();
				}
				
				double totalWeight = structure.getTotalWeight();
				
				//this.context.getDataTools().getOutputWriter().debugWriteln("Greedy inference running on iteration " + iterations + " trying to add " + orderedStructureParts.size() + " to graph ");
				
				for (Triple<List<CtxParsable>, Pair<Double, Object>, Integer> structurePart : orderedStructureParts) {
					Map<String, List<Obj>> objs = this.rules.get(structurePart.getThird()).apply(structurePart.getFirst());
					for (Entry<String, List<Obj>> objList : objs.entrySet()) {
						for (Obj obj : objList.getValue()) {
							WeightedStructure newStructurePart = this.context.constructMatchWeightedStructure(obj);
							structure.add(newStructurePart, structurePart.getSecond().getFirst(), objList.getKey() + "(" + structurePart.getSecond().getSecond() + ")", filter);
						}
					}
				}
				
				//this.context.getDataTools().getOutputWriter().debugWriteln("Greedy inference running iteration " + iterations + " on size " + iterFilterSize + " (" + (System.currentTimeMillis() - startTime) + ")");
				
				iterations++;
				weightChange = Math.abs(structure.getTotalWeight() - totalWeight);
			} while (splitFnIndex != this.splitFns.size() - 1 || ((this.maxIterations == 0 || iterations < this.maxIterations) && filter.size() > 0 && (filter.size() != prevFilterSize || weightChange > EPSILON)));
			
			//System.out.println("FINAL\n" + structure);
			output.add(structure);
		}
		
		return output;
	}

	private <C extends Collection<S>, F extends WeightedStructure> Collection<F> limitFilterSize(S structure, Collection<F> filter, int iterations) {
		int iterFilterSize = (filter != null) ? filter.size() : 0;
		if (this.maxIterationSize > 0 && iterFilterSize > this.maxIterationSize) {
			this.context.getDataTools().getOutputWriter().debugWriteln("Greedy inference iteration " + iterations + " size exceeded max (" + iterFilterSize + ") choosing max weighted subset");
			List<F> filterList = new ArrayList<>();
			filterList.addAll(filter);
			Collections.sort(filterList, new Comparator<F>() {
				@Override
				public int compare(F o1, F o2) {
					// FIXME Note this will break if structure doesn't contain filter items
					return Double.compare(structure.getWeight(o2), structure.getWeight(o1));
				}
			});
			
			filter.clear();
			for (int i = 0; i < this.maxIterationSize; i++) {
				filter.add(filterList.get(i));
			}
		}
		
		return filter;
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
