package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.DataTools.StringPairIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelMapping;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.Structurizer;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.structure.WeightedStructure;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureGraph;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelation;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureGraph.OverwriteOperator;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureGraph.RelationMode;
import edu.cmu.ml.rtw.generic.util.Pair;

public abstract class StructurizerGraph<D extends Datum<L>, L> extends Structurizer<D, L, WeightedStructureGraph>  {
	protected LabelMapping<L> labelMapping;

	private RelationMode graphEdgeMode = RelationMode.SINGLE;
	private RelationMode graphNodeMode = RelationMode.SINGLE;
	private OverwriteOperator graphOverwriteOperator = OverwriteOperator.MAX;
	private StringPairIndicator graphEdgeMutexFn = null;
	
	protected String[] parameterNames = { "labelMapping", "graphEdgeMode", "graphNodeMode", "graphOverwriteOperator", "graphEdgeMutexFn" };
	
	public StructurizerGraph() {
		
	}
	
	public StructurizerGraph(DatumContext<D, L> context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("labelMapping"))
			return Obj.stringValue(this.labelMapping.toString());
		else if (parameter.equals("graphEdgeMode"))
			return Obj.stringValue(this.graphEdgeMode.toString());
		else if (parameter.equals("graphNodeMode"))
			return Obj.stringValue(this.graphNodeMode.toString());
		else if (parameter.equals("graphOverwriteOperator"))
			return Obj.stringValue(this.graphOverwriteOperator.toString());
		else if (parameter.equals("graphEdgeMutexFn"))
			return this.graphEdgeMutexFn == null ? null : Obj.stringValue(this.graphEdgeMutexFn.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("labelMapping"))
			this.labelMapping = this.context.getDatumTools().getLabelMapping(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("graphEdgeMode"))
			this.graphEdgeMode = RelationMode.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("graphNodeMode"))
			this.graphNodeMode = RelationMode.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("graphOverwriteOperator"))
			this.graphOverwriteOperator = OverwriteOperator.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("graphEdgeMutexFn"))
			this.graphEdgeMutexFn = parameterValue == null ? null : this.context.getDataTools().getStringPairIndicatorFn(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public boolean addToStructures(D datum, L label, double weight, Map<String, WeightedStructureGraph> structures, Map<String, Collection<WeightedStructure>> changes) {
		Pair<String, WeightedStructureGraph> pair = getOrConstructStructure(datum, structures);
		WeightedStructureGraph graph = pair.getSecond();
		WeightedStructureRelation rel = makeDatumStructure(datum, label);
		if (!changes.containsKey(pair.getFirst()))
			changes.put(pair.getFirst(), new HashSet<WeightedStructure>());
		
		boolean changed = false;
		if (rel != null) {
			Collection<WeightedStructure> c = changes.get(pair.getFirst());
			int changesStart = c.size();
			graph.add(rel, weight, c);
			changed = c.size() != changesStart;	
		}
		
		return changed;
	}

	@Override
	public String getStructureId(D datum, L label, Map<String, WeightedStructureGraph> structures) {
		return getOrConstructStructure(datum, structures).getFirst();
	}
	
	@Override
	public Map<String, WeightedStructureGraph> makeStructures() {
		return new HashMap<String, WeightedStructureGraph>();
	}

	@Override
	public Map<L, Double> getLabels(D datum, Map<String, WeightedStructureGraph> structures) {
		WeightedStructureGraph graph = getOrConstructStructure(datum, structures).getSecond();
		List<WeightedStructureRelation> rels = getDatumRelations(datum, graph);
		
		Map<L, Double> labels = new HashMap<L, Double>();
		for (WeightedStructureRelation rel : rels) {
			L label = this.context.getDatumTools().labelFromString(rel.getType());
			
			if (this.labelMapping != null)
				label = this.labelMapping.map(label);
			
			if (label == null)
				continue;

			labels.put(label, graph.getWeight(rel));
		}
		
		return labels;
	}
	
	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		return null;
	}
	
	private Pair<String, WeightedStructureGraph> getOrConstructStructure(D datum, Map<String, WeightedStructureGraph> structures) {
		String id = getStructureId(datum);
		if (!structures.containsKey(id)) {
			WeightedStructureGraph graph = new WeightedStructureGraph(this.context, this.graphEdgeMode, this.graphNodeMode, this.graphOverwriteOperator, this.graphEdgeMutexFn);
			structures.put(id, graph);
		}
		
		return new Pair<String, WeightedStructureGraph>(id, structures.get(id));
	}
	
	protected abstract WeightedStructureRelation makeDatumStructure(D datum, L label);
	protected abstract String getStructureId(D datum);
	protected abstract List<WeightedStructureRelation> getDatumRelations(D datum, WeightedStructureGraph graph);
}
