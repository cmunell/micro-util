package edu.cmu.ml.rtw.generic.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.parse.CtxParsable;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationBinary;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationUnary;

public class RuleSetTest {
	@Test
	public void testRuleSetApplication() {
		Context context = Context.run("c", new DataTools(), 
				"rule r = (And(c1=TYPE1(r1=O(id=[id1]), r2=O(id=[id2])), c2=TYPE2(r1=O(id=${id2}), r2=O(id=[id3])), c3=Not(c1=Equals(${id1},${id3})))) -> (TYPE3(r1=O(id=${id1}), r2=O(id=${id3})));\n" +
				"rule_set rs = RuleSet(rules=(${r}));");
		
		RuleSet rs = context.getMatchRuleSet(Obj.curlyBracedValue("rs"));
		
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
		
		List<CtxParsable> rels = new ArrayList<CtxParsable>();
		rels.add(rel1);
		rels.add(rel2);
		
		Map<String, List<Obj>> results = rs.apply(rels);
		for (Entry<String, List<Obj>> entry : results.entrySet()) {
			for (Obj obj : entry.getValue()) {
				Assert.assertEquals("TYPE3(r1=O(id=\"o1\"), r2=O(id=\"o3\"))", obj.toString());
				break;
			}
		}
		
		
	}
}
