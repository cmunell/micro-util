package edu.cmu.ml.rtw.generic.model.annotator.nlp;
/*
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraph.OutputFormat;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Token;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.stanford.JSONTokenizer;
*/
public class NLPAnnotatorStanfordTest {
	/* FIXME Refactor this @Test
	public void testJSONTokenizer() {
		NLPAnnotatorStanford annotator = new NLPAnnotatorStanford();
		annotator.disableConstituencyParses();
		annotator.disableDependencyParses();
		annotator.initializePipeline(new JSONTokenizer());
		annotator.setText("{\"sentences\" : [ { \"tokens\": [ { \"str\" : \"the\", \"s\" : 0, \"e\" : 3 }, " +
															 "{ \"str\" : \"dog\", \"s\" : 4, \"e\" : 7 }, " +
															 "{ \"str\" : \"barks\", \"s\" : 8, \"e\" : 13 }"
														+ " ] }, " +
											 "{ \"tokens\": [ ] } ]" +
						  "}");
		
		Token[][] tokens = annotator.makeTokens();
		Assert.assertEquals("the", tokens[0][0].getStr());
		Assert.assertEquals("dog", tokens[0][1].getStr());
		Assert.assertEquals("barks", tokens[0][2].getStr());
		
		PoSTag[][] pos = annotator.makePoSTags();
		Assert.assertEquals(PoSTag.DT, pos[0][0]);
		Assert.assertEquals(PoSTag.NN, pos[0][1]);
		Assert.assertEquals(PoSTag.VBZ, pos[0][2]);
	}
	
	@Test
	public void scratchSentenceParse() {
		NLPAnnotatorStanford annotator = new NLPAnnotatorStanford();
		annotator.initializePipeline();
		annotator.setText("Jim bakes the cookies.");
		
		List<CoreMap> sentences = annotator.annotatedText.get(SentencesAnnotation.class);
		for(int i = 0; i < sentences.size(); i++) {
			SemanticGraph sentenceDependencyGraph = sentences.get(i).get(CollapsedCCProcessedDependenciesAnnotation.class);
			Tree sentenceConstituencyParse = sentences.get(i).get(TreeAnnotation.class);
			System.out.println(sentenceDependencyGraph.toString(OutputFormat.LIST));
			System.out.println(sentenceConstituencyParse.toString());
		}
	}
	
	@Test
	public void scratchCoref() {
		NLPAnnotatorStanford annotator = new NLPAnnotatorStanford();
		annotator.enableNerAndCoref();
		annotator.initializePipeline();
		annotator.setText("Jim bakes the good cookies, and then he eats the cookies.  I eat them, too, and then I eat some cake.");
		Map<Integer, CorefChain> corefGraph = annotator.annotatedText.get(CorefChainAnnotation.class);
		for (Entry<Integer, CorefChain> entry : corefGraph.entrySet()) {
			CorefChain corefChain = entry.getValue();
			CorefMention representativeMention = corefChain.getRepresentativeMention();
			
			System.out.println(corefChain.getChainID() + " (" + representativeMention + ")");
			System.out.println("\tcluster " + representativeMention.corefClusterID + " " +
							   "sent: " + representativeMention.sentNum + " " +
							   "startIndex: " + representativeMention.startIndex + " " +
							   "endIndex: " + representativeMention.endIndex + " " +
							   "headIndex: " + representativeMention.headIndex + " " + 
							   "mentionId: " + representativeMention.mentionID+ " " +
							   "mentionSpan: " + representativeMention.mentionSpan);
			
			Map<IntPair, Set<CorefMention>> mentionMap = corefChain.getMentionMap();
			for (Entry<IntPair, Set<CorefMention>> spanEntry : mentionMap.entrySet()) {
				for (CorefMention mention : spanEntry.getValue()) {
					System.out.println("\t\t" + spanEntry.getKey().getSource() + "," + spanEntry.getKey().getTarget() + " " +
									   "cluster: " + mention.corefClusterID + " " +
									   "sent: " + mention.sentNum + " " +
									   "startIndex: " + mention.startIndex + " " +
									   "headIndex: " + mention.headIndex + " " + 
									   "endIndex: " + mention.endIndex + " " +
									   "mentionId: " + mention.mentionID + " " +
									   "mentionSpan: " + mention.mentionSpan);
				}
			}
			
		}
	}*/
}
