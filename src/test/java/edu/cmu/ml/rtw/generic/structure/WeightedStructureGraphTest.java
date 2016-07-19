package edu.cmu.ml.rtw.generic.structure;


import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.DataTools;

public class WeightedStructureGraphTest {
	@Test
	public void testEdgePaths() {
		Context context = Context.run("c", new DataTools(), 
				"rule r = (And(c1=TYPE1(r1=O(id=[id1]), r2=O(id=[id2])), c2=TYPE2(r1=O(id=${id2}), r2=O(id=[id3])), c3=Not(c1=Equals(${id1},${id3})))) -> (TYPE3(r1=O(id=${id1}), r2=O(id=${id3})));\n" +
				"rule_set rs = RuleSet(rules=(${r}));");
		
		WeightedStructureRelationBinary rel1 = new WeightedStructureRelationBinary(
				"TYPE1", 
				context, 
				"rel1", 
				new WeightedStructureRelationUnary("O", context, "o1"), 
				new WeightedStructureRelationUnary("O", context, "o2"), 
				true);
		
		WeightedStructureRelationBinary rel2 = new WeightedStructureRelationBinary(
				"TYPE2", 
				context, 
				"rel2", 
				new WeightedStructureRelationUnary("O", context, "o2"), 
				new WeightedStructureRelationUnary("O", context, "o3"), 
				true);
		
		WeightedStructureRelationBinary rel3 = new WeightedStructureRelationBinary(
				"TYPE3", 
				context, 
				"rel3", 
				new WeightedStructureRelationUnary("O", context, "o2"), 
				new WeightedStructureRelationUnary("O", context, "o3"), 
				true);
		
		WeightedStructureGraph graph = new WeightedStructureGraph(context);
		graph.add(rel1, 1.0);
		graph.add(rel2, 1.0);
		graph.add(rel3, 5.0);
		
		Assert.assertEquals(1, graph.getEdgePaths(2).size());
	}
	
	@Test
	public void testEdgePathsUnordered() {
		Context context = Context.run("c", new DataTools(), 
				"rule r = (And(c1=TYPE1(r1=O(id=[id1]), r2=O(id=[id2])), c2=TYPE2(r1=O(id=${id2}), r2=O(id=[id3])), c3=Not(c1=Equals(${id1},${id3})))) -> (TYPE3(r1=O(id=${id1}), r2=O(id=${id3})));\n" +
				"rule_set rs = RuleSet(rules=(${r}));");
		
		WeightedStructureRelationBinary rel1 = new WeightedStructureRelationBinary(
				"TYPE1", 
				context, 
				"rel1", 
				new WeightedStructureRelationUnary("O", context, "o1"), 
				new WeightedStructureRelationUnary("O", context, "o2"), 
				false);
		
		WeightedStructureRelationBinary rel2 = new WeightedStructureRelationBinary(
				"TYPE1", 
				context, 
				"rel2", 
				new WeightedStructureRelationUnary("O", context, "o2"), 
				new WeightedStructureRelationUnary("O", context, "o3"), 
				false);
		
		WeightedStructureGraph graph = new WeightedStructureGraph(context);
		graph.add(rel1, 1.0);
		graph.add(rel2, 1.0);
		
		List<WeightedStructureRelation> filter = new ArrayList<>();
		filter.add(rel1);
		filter.add(rel2);
		
		Assert.assertEquals(6, graph.getEdgePaths(2).size());
		Assert.assertEquals(6, graph.getEdgePaths(2, null, filter).size());
	}
}
