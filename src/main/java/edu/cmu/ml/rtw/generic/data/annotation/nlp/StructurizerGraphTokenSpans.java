package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.Structurizer;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureGraph;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelation;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationBinary;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationUnary;

public class StructurizerGraphTokenSpans<L> extends StructurizerGraph<TokenSpansDatum<L>, L> {
	public StructurizerGraphTokenSpans() {
		super();
	}
	
	public StructurizerGraphTokenSpans(DatumContext<TokenSpansDatum<L>, L> context) {
		super(context);
	}
	
	@Override
	protected WeightedStructureRelation makeDatumStructure(TokenSpansDatum<L> datum, L label) {
		TokenSpan[] spans = datum.getTokenSpans();
		if (spans.length == 1) {
			return new WeightedStructureRelationUnary(
					label.toString(), 
					this.context, 
					getTokenSpanId(spans[0]));
		} else if (spans.length == 2) {
			return new WeightedStructureRelationBinary(
					label.toString(), 
					this.context, 
					String.valueOf(datum.getId()), 
					new WeightedStructureRelationUnary("TS", this.context, getTokenSpanId(spans[0])),
					new WeightedStructureRelationUnary("TS", this.context, getTokenSpanId(spans[1])),
					true);
		} else
			return null;
	}

	@Override
	protected String getStructureId(TokenSpansDatum<L> datum) {
		TokenSpan[] spans = datum.getTokenSpans();
		if (spans.length == 1) {
			return spans[0].getDocument().getName();
		} else if (spans.length == 2) {
			String id = spans[0].getDocument().getName();
			String id2 = spans[1].getDocument().getName();
			if (!id2.equals(id)) {
				if (id.compareTo(id2) < 0)
					id = id + "_" + id2;
				else 
					id = id2 + "_" + id;
			}
			return id;
		} else 
			return null;
	}

	@Override
	protected List<WeightedStructureRelation> getDatumRelations(TokenSpansDatum<L> datum, WeightedStructureGraph graph) {
		TokenSpan[] spans = datum.getTokenSpans();
		List<WeightedStructureRelation> rels = new ArrayList<>();
		if (spans.length == 1) {
			String id = getTokenSpanId(spans[0]);
			rels.addAll(graph.getNodes(id));
		} else if (spans.length == 2) {
			String id1 = getTokenSpanId(spans[0]);
			String id2 = getTokenSpanId(spans[1]);
			rels.addAll(graph.getEdges(id1, id2));
		} else 
			return null;
		
		return rels;
	}

	@Override
	public Structurizer<TokenSpansDatum<L>, L, WeightedStructureGraph> makeInstance(DatumContext<TokenSpansDatum<L>, L> context) {
		return new StructurizerGraphTokenSpans<L>(context);
	}

	@Override
	public String getGenericName() {
		return "GraphTokenSpans";
	}

	private String getTokenSpanId(TokenSpan span) {
		return span.getDocument().getName() + "_" + span.getSentenceIndex() + "_" + span.getStartTokenIndex() + "_" + span.getEndTokenIndex();
	}
}
