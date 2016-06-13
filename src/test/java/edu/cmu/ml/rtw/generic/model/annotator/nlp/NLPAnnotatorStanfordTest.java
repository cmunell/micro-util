package edu.cmu.ml.rtw.generic.model.annotator.nlp;

import java.io.StringReader;

import org.bson.Document;
import org.junit.Assert;
import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.data.StoredItemSet;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.time.NormalizedTimeValue;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.time.TimeExpression;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.stanford.JSONTokenizer;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Properties;

public class NLPAnnotatorStanfordTest {
	@Test
	public void testJSONTokenizer() {
		PipelineNLPStanford pipeline = new PipelineNLPStanford();
		pipeline.initialize(null, new JSONTokenizer());
		
		DocumentNLPMutable document = new DocumentNLPInMemory(new DataTools(), 
													   "document", 
													   "{\"sentences\" : [ { \"tokens\": [ { \"str\" : \"the\", \"s\" : 0, \"e\" : 3 }, " +
																 "{ \"str\" : \"dog\", \"s\" : 4, \"e\" : 7 }, " +
																 "{ \"str\" : \"barks\", \"s\" : 8, \"e\" : 13 }," +
																 "{ \"str\" : \".\", \"s\" : 14, \"e\" : 15 }" +
																 " ] }, " +
													   "{ \"tokens\": ["  +
													   			 "{ \"str\" : \"the\", \"s\" : 16, \"e\" : 19 }, " +
																 "{ \"str\" : \"bee\", \"s\" : 20, \"e\" : 23 }, " +
																 "{ \"str\" : \"stings\", \"s\" : 24, \"e\" : 30 }," +		 
																 "{ \"str\" : \".\", \"s\" : 31, \"e\" : 32 }" +	
													   "] } ]" +
													   "}");
				
		pipeline.run(document);
		
		Assert.assertEquals("the", document.getTokenStr(0, 0));
		Assert.assertEquals("dog", document.getTokenStr(0, 1));
		Assert.assertEquals("barks", document.getTokenStr(0, 2));
		
		Assert.assertEquals(PoSTag.DT, document.getPoSTag(0, 0));
		Assert.assertEquals(PoSTag.NN, document.getPoSTag(0, 1));
		Assert.assertEquals(PoSTag.VBZ, document.getPoSTag(0, 2));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testSUTime() {
		DataTools dataTools = new DataTools(new OutputWriter(), 
				new Properties(new StringReader(
						"debug_dir=\n" +
						"storage_fs_bson_testBson=/test/bson\n" +
						"storage_fs_str_testStr=/test/str"
						)));
		StoredItemSet<NormalizedTimeValue, NormalizedTimeValue> timeValues = dataTools.getStoredItemSetManager().getItemSet("BSONMemory", "TimeValues", true, (Serializer<NormalizedTimeValue, Document>)dataTools.getSerializers().get("JSONBSONNormalizedTimeValue"));
		StoredItemSet<TimeExpression, TimeExpression> timeExpressions = dataTools.getStoredItemSetManager().getItemSet("BSONMemory", "TimeExpressions", true, (Serializer<TimeExpression, Document>)dataTools.getSerializers().get("JSONBSONTimeExpression"));

		PipelineNLPStanford pipeline = new PipelineNLPStanford();
		pipeline.initialize(null, null, timeExpressions, timeValues);
		
		DocumentNLPMutable document = new DocumentNLPInMemory(new DataTools(), 
				   "document", 
				   "Lunch will be served at 12:30, and dinner will be served at 5:00.");

		pipeline.run(document);
		
		//StoredItemSet<?, ?> outputEvals = dataTools.getStoredItemSetManager().getItemSet("BSONMemory", "TimeValues");
		//System.out.println(outputEvals.getStoredItems().toString());
	}
	
	/* FIXME Refactor later @Test
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
