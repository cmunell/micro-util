package edu.cmu.ml.rtw.generic.data.feature.rule;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.TestDatum;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.OutputWriter;

public class RuleSetTest {
	// FIXME Needs refactoring
	public void testRuleSetApplication() {
		Context<TestDatum<String>, String> context = Context.deserialize(TestDatum.getStringTools(new DataTools(new OutputWriter())),
				"ts_fn head=Head();\n" +
				"ts_fn ins1=NGramInside(n=\"1\", noHead=\"true\");\n" +
				"ts_fn ins2=NGramInside(n=\"2\", noHead=\"true\");\n" +
				"ts_fn ins3=NGramInside(n=\"3\", noHead=\"true\");\n" +
				"ts_fn ctxb1=NGramContext(n=\"1\", type=\"BEFORE\");\n" +
				"ts_fn ctxa1=NGramContext(n=\"1\", type=\"AFTER\");\n" +
				"ts_fn sent1=NGramSentence(n=\"1\", noSpan=\"true\");\n" +
				"ts_fn doc2=NGramDocument(n=\"2\", noSentence=\"true\");\n" +
				"ts_str_fn pos=PoS();\n" +
				"ts_str_fn str=String(cleanFn=\"DefaultCleanFn\");\n" +
				"str_fn pre=Affix(type=\"PREFIX\", n=\"3\");\n" +
				"str_fn suf=Affix(type=\"SUFFIX\", n=\"3\");\n" +
				"str_fn filter=Filter(filter=\"\", type=\"SUBSTRING\");\n" +
				"str_fn filter_s=Filter(filter=\"\", type=\"SUFFIX\");\n" +
				"str_fn filter_p=Filter(filter=\"\", type=\"PREFIX\");\n" +
				"ts_str_fn headDoc2=(${str} o ${head} o ${doc2});\n" +
				"feature fsent1=TokenSpanFnDataVocab(scale=NORMALIZED_TFIDF, minFeatureOccurrence=2, tokenExtractor=TokenSpan, fn=(${filter} o ${str} o ${sent1}));\n" +
				"feature fsufh=TokenSpanFnDataVocab(scale=INDICATOR, minFeatureOccurrence=2, tokenExtractor=TokenSpan, fn=(${filter_s} o ${suf} o ${str} o ${head}));\n" +
				"rs rules=RuleSet() {\n" +
				
				"rule sentInc= (TokenSpanFnDataVocab(fn=(Filter() o ${str} o NGramSentence(n=[n],noSpan=true)))) " +
				"->" + 
				"(TokenSpanFnDataVocab(scale=NORMALIZED_TFIDF, minFeatureOccurrence=2, tokenExtractor=TokenSpan," +
				"fn=(Filter(type=SUBSTRING, filter=${FEATURE_STR}) o String(cleanFn=\"DefaultCleanFn\") o NGramSentence(n=${n++},noSpan=true))));\n" +
				
				"rule sentDoc= (TokenSpanFnDataVocab(fn=(Filter() o ${str} o NGramSentence(n=[n],noSpan=true)))) " +
				"->" + 
				"(TokenSpanFnDataVocab(scale=NORMALIZED_TFIDF, minFeatureOccurrence=2, tokenExtractor=TokenSpan, " +
				"fn=(Filter(type=SUBSTRING, filter=${FEATURE_STR}) o String(cleanFn=\"DefaultCleanFn\") o NGramDocument(n=${n},noSentence=true))));\n" +
				
				"rule posaInc= (TokenSpanFnDataVocab(fn=(Filter() o ${pos} o NGramContext(n=[n], type=\"AFTER\")))) " +
				"->" +
				"(TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"TokenSpan\", " + 
				"fn=(Filter(type=\"PREFIX\", filter=${FEATURE_STR}) o PoS() o NGramContext(n=${n++}, type=\"AFTER\"))));" +
				
				"rule docInc= (TokenSpanFnDataVocab(fn=(Filter() o ${str} o NGramDocument(n=[n],noSentence=\"true\"))))" + 
				"->" + 
				"(TokenSpanFnDataVocab(scale=\"NORMALIZED_TFIDF\", minFeatureOccurrence=\"2\", " +
				"fn=(Filter(type=SUBSTRING, filter=${FEATURE_STR}) o String(cleanFn=\"DefaultCleanFn\") o NGramDocument(n=${n++},noSentence=\"true\"))));" +
				
				"rule sentDoc1New = (TokenSpanFnDataVocab(fn=(Filter() o ${str} o ${sent1})))" +
				"->" +
				"(TokenSpanFnFilteredVocab(" +
					"vocabFeature=${fdoc1}, " +
					"vocabFilterFn=Filter(filter=${FEATURE_STR}, type=\"EQUAL\"), " +
					"vocabFilterInit=\"FULL\", " +
					"fn=(${str} o ${sent1}), " +
					"tokenExtractor=\"AllTokenSpans\") {" +
						"value referenceName = ${RULE};" +
					"});" +
				
				"};");
		
		Feature<TestDatum<String>, String> feature = context.getMatchFeature(Obj.curlyBracedValue("fsent1"));
		RuleSet<TestDatum<String>, String> rulesTemp = context.getMatchRuleSet(Obj.curlyBracedValue("rules"));
		RuleSet<TestDatum<String>, String> rules = new RuleSet<TestDatum<String>, String>(context);
		rules.fromParse(rulesTemp.toParse());
		
		Map<String, Obj> extraAssign = new HashMap<String, Obj>();
		extraAssign.put("FEATURE_STR", Obj.stringValue("some"));
		extraAssign.put("RULE", Obj.stringValue("new"));
		
		Map<String, Obj> resultingFeatureObjs = rules.applyRules(feature, extraAssign);
		Assert.assertEquals(3, resultingFeatureObjs.size());
		Assert.assertEquals("TokenSpanFnDataVocab(scale=\"NORMALIZED_TFIDF\", minFeatureOccurrence=\"2\", tokenExtractor=\"TokenSpan\", fn=Composite(Composite(Filter(type=\"SUBSTRING\", filter=\"some\"), String(cleanFn=\"DefaultCleanFn\")), NGramSentence(n=\"2\", noSpan=\"true\")))", resultingFeatureObjs.get("sentInc").toString());
		Assert.assertEquals("TokenSpanFnDataVocab(scale=\"NORMALIZED_TFIDF\", minFeatureOccurrence=\"2\", tokenExtractor=\"TokenSpan\", fn=Composite(Composite(Filter(type=\"SUBSTRING\", filter=\"some\"), String(cleanFn=\"DefaultCleanFn\")), NGramDocument(n=\"1\", noSentence=\"true\")))", resultingFeatureObjs.get("sentDoc").toString());
	
		rules.applyRules(feature, extraAssign);	
		Assert.assertEquals(3, resultingFeatureObjs.size());
		Assert.assertEquals("TokenSpanFnDataVocab(scale=\"NORMALIZED_TFIDF\", minFeatureOccurrence=\"2\", tokenExtractor=\"TokenSpan\", fn=Composite(Composite(Filter(type=\"SUBSTRING\", filter=\"some\"), String(cleanFn=\"DefaultCleanFn\")), NGramSentence(n=\"2\", noSpan=\"true\")))", resultingFeatureObjs.get("sentInc").toString());
		Assert.assertEquals("TokenSpanFnDataVocab(scale=\"NORMALIZED_TFIDF\", minFeatureOccurrence=\"2\", tokenExtractor=\"TokenSpan\", fn=Composite(Composite(Filter(type=\"SUBSTRING\", filter=\"some\"), String(cleanFn=\"DefaultCleanFn\")), NGramDocument(n=\"1\", noSentence=\"true\")))", resultingFeatureObjs.get("sentDoc").toString());
		Assert.assertEquals("TokenSpanFnFilteredVocab(" +
					"vocabFeature=${fdoc1}, " +
					"vocabFilterFn=Filter(filter=\"some\", type=\"EQUAL\"), " +
					"vocabFilterInit=\"FULL\", " +
					"fn=Composite(${str}, ${sent1}), " +
					"tokenExtractor=\"AllTokenSpans\") {\n" +
						"value referenceName=\"sentDoc1New\";\n" +
					"}", resultingFeatureObjs.get("sentDoc1New").toString());
	}
}
