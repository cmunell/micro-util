package edu.cmu.ml.rtw.generic.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.annotation.TestDatum;
import edu.cmu.ml.rtw.generic.data.annotation.TestDatum.Tools;
import edu.cmu.ml.rtw.generic.util.OutputWriter;

public class ContextTest {
	@Test
	public void testContextSerializationSelfValue() {
		testContextSerializationSelf("value x=\"1\";\n");
	}
	
	@Test
	public void testContextSerializationSelfBig() {
		String contextStr = "value randomSeed=\"1\";\n";
		contextStr +=       "value maxThreads=\"33\";\n";
		contextStr +=       "value trainOnDev=\"false\";\n";
		contextStr +=       "value errorExampleExtractor=\"FirstTokenSpan\";\n";
		contextStr +=       "(composite) evaluation accuracy=Accuracy(computeBaseline=\"false\");\n";
		contextStr +=       "evaluation accuracyBase=Accuracy(computeBaseline=\"true\");\n";
		contextStr +=       "evaluation f.5=F(mode=\"MACRO_WEIGHTED\", Beta=\"0.5\", filterLabel=\"true\");\n";
		contextStr +=       "evaluation f1=F(mode=\"MACRO_WEIGHTED\", Beta=\"1.0\", filterLabel=\"true\");\n";
		contextStr +=       "evaluation prec=Precision(weighted=\"false\", filterLabel=\"true\");\n";
		contextStr +=       "evaluation recall=Recall(weighted=\"false\", filterLabel=\"true\");\n";
		contextStr +=       "feature cpna1=GramContextPattern(minFeatureOccurrence=\"2\", cleanFn=\"DefaultCleanFn\", tokenExtractor=\"TokenSpan\", scale=\"INDICATOR\", beforePattern=\"\", afterPattern=\"((((<p:RB,VB>)*<p:VB>)|POS)(DT)?(<p:JJ,NN>)*<p:NN>).*\", capturePart=\"AFTER\", captureGroup=\"1\");\n";
		contextStr +=       "feature dep=NGramDep(minFeatureOccurrence=\"2\", cleanFn=\"DefaultCleanFn\", tokenExtractor=\"TokenSpan\", scale=\"INDICATOR\", clusterer=\"None\", n=\"1\", mode=\"ParentsAndChildren\", useRelationTypes=\"true\");\n";
		contextStr +=       "feature form=StringForm(stringExtractor=\"TokenSpan\", minFeatureOccurrence=\"2\");\n";
		contextStr +=       "feature fgnp=GazetteerContains(gazetteer=\"\", stringExtractor=\"TokenSpan\", includeIds=\"true\", includeWeights=\"true\", weightThreshold=\"0.9\");\n";

		contextStr +=       "ts_fn head=Head();\n";
		contextStr +=       "ts_fn ins1=NGramInside(n=\"1\", noHead=\"true\");\n";
		contextStr +=       "ts_fn ins2=NGramInside(n=\"2\", noHead=\"true\");\n";
		contextStr +=       "ts_fn ins3=NGramInside(n=\"3\", noHead=\"true\");\n";
		contextStr +=       "ts_fn ctxb1=NGramContext(n=\"1\", type=\"BEFORE\");\n";
		contextStr +=       "ts_fn ctxa1=NGramContext(n=\"1\", type=\"AFTER\");\n";
		contextStr +=       "ts_fn sent1=NGramSentence(n=\"1\", noSpan=\"true\");\n";
		contextStr +=       "ts_str_fn pos=PoS();\n";
		contextStr +=       "ts_str_fn strDef=String(cleanFn=\"DefaultCleanFn\");\n";
		contextStr +=       "ts_str_fn strStem=String(cleanFn=\"DefaultCleanFn\");\n";
		contextStr +=       "ts_str_fn strBoW=String(cleanFn=\"DefaultCleanFn\");\n";
		contextStr +=       "str_fn pre=Affix(type=\"PREFIX\", n=\"3\");\n";
		contextStr +=       "str_fn suf=Affix(type=\"SUFFIX\", n=\"3\");\n";
		contextStr +=       "str_fn filter=Filter(filter=\"\", type=\"SUBSTRING\");\n";
		contextStr +=       "str_fn filter_s=Filter(filter=\"\", type=\"SUFFIX\");\n";
		contextStr +=       "str_fn filter_p=Filter(filter=\"\", type=\"PREFIX\");\n";
		
		contextStr +=       "model lr=Areg(l1=\"0.0\", l2=\"0.0\", convergenceEpsilon=\"0.001\", maxEvaluationConstantIterations=\"500\", maxTrainingExamples=\"260001.0\", batchSize=\"100\", evaluationIterations=\"200\", weightedLabels=\"false\", classificationThreshold=\"0.5\", computeTestEvaluations=\"false\") {\n";
		contextStr +=       "array validLabels=(\"1\", \"2\");\n";
		contextStr +=       "};\n";
		
		testContextSerializationSelf(contextStr);
	}
	
	@Test
	public void testContextSerializationBig() {
		Map<String, String> variableMap = new HashMap<String, String>();
		variableMap.put("head", "Head()");
		variableMap.put("ins1", "NGramInside(n=\"1\", noHead=\"true\")");
		variableMap.put("ins2", "NGramInside(n=\"2\", noHead=\"true\")");
		variableMap.put("ins3", "NGramInside(n=\"3\", noHead=\"true\")");
		variableMap.put("ctxb1", "NGramContext(n=\"1\", type=\"BEFORE\")");
		variableMap.put("ctxa1", "NGramContext(n=\"1\", type=\"AFTER\")");
		variableMap.put("sent1", "NGramSentence(n=\"1\", noSpan=\"true\")");
		variableMap.put("pos", "PoS()");
		variableMap.put("strDef", "String(cleanFn=\"DefaultCleanFn\")");
		variableMap.put("strStem", "String(cleanFn=\"DefaultCleanFn\")");
		variableMap.put("strBoW", "String(cleanFn=\"DefaultCleanFn\")");
		variableMap.put("pre", "Affix(type=\"PREFIX\", n=\"3\")");
		variableMap.put("suf", "Affix(type=\"SUFFIX\", n=\"3\")");
		variableMap.put("filter", "Filter(filter=\"\", type=\"SUBSTRING\")");
		variableMap.put("filter_s", "Filter(filter=\"\", type=\"SUFFIX\")");
		variableMap.put("filter_p", "Filter(filter=\"\", type=\"PREFIX\")");
		variableMap.put("VALID_LABELS", "(\"0\", \"1\")");
		variableMap.put("rules", "RuleSet() {\n}");
		
		String inputContextStr =       "ts_fn head=" + variableMap.get("head") + ";\n";
		inputContextStr +=             "ts_fn ins1=" + variableMap.get("ins1") + ";\n";
		inputContextStr +=             "ts_fn ins2=" + variableMap.get("ins2") + ";\n";
		inputContextStr +=             "ts_fn ins3=" + variableMap.get("ins3") + ";\n";
		inputContextStr +=             "ts_fn ctxb1=" + variableMap.get("ctxb1") + ";\n";
		inputContextStr +=             "ts_fn ctxa1=" + variableMap.get("ctxa1") + ";\n";
		inputContextStr +=             "ts_fn sent1=" + variableMap.get("sent1") + ";\n";
		inputContextStr +=             "ts_str_fn pos=" + variableMap.get("pos") + ";\n";
		inputContextStr +=             "ts_str_fn strDef=" + variableMap.get("strDef") + ";\n";
		inputContextStr +=             "ts_str_fn strStem=" + variableMap.get("strStem") + ";\n";
		inputContextStr +=             "ts_str_fn strBoW=" + variableMap.get("strBoW") + ";\n";
		inputContextStr +=             "str_fn pre=" + variableMap.get("pre") + ";\n";
		inputContextStr +=             "str_fn suf=" + variableMap.get("suf") + ";\n";
		inputContextStr +=             "str_fn filter=" + variableMap.get("filter") + ";\n";
		inputContextStr +=             "str_fn filter_s=" + variableMap.get("filter_s") + ";\n";
		inputContextStr +=             "str_fn filter_p=" + variableMap.get("filter_p") + ";\n";
		inputContextStr +=             "feature fpos=TokenSpanFnDataVocab(minFeatureOccurrence=\"2\", tokenExtractor=\"TokenSpan\", scale=\"INDICATOR\", fn=${pos});\n";
		inputContextStr +=             "rs rules=" + variableMap.get("rules") + ";\n";
		inputContextStr +=             "array VALID_LABELS=" + variableMap.get("VALID_LABELS") + ";\n";
		inputContextStr +=             "evaluation accuracy=Accuracy(computeBaseline=\"false\");\n";

		inputContextStr +=             "model lg=LogistmarGramression(t=\"0.5\", rules=${rules}, weightedLabels=\"false\", l2=\"0.0\", convergenceEpsilon=\"0.001\", maxEvaluationConstantIterations=\"500\", maxTrainingExamples=\"260001.0\", batchSize=\"100\", evaluationIterations=\"200\", classificationThreshold=\"0.5\", computeTestEvaluations=\"false\") {\n";
		inputContextStr +=             "array validLabels=${VALID_LABELS};\n";
		inputContextStr +=             "};\n";

		inputContextStr +=             "gs g=GridSearch() {\n";
		inputContextStr +=             "dimension l2=Dimension(name=\"l2\", values=(\"0.000001\", \"0.00001\", \"0.0001\"), trainingDimension=\"true\");\n";
		inputContextStr +=             "dimension ct=Dimension(name=\"classificationThreshold\", values=(\"0.5\", \"0.6\", \"0.7\", \"0.8\", \"0.9\"), trainingDimension=\"false\");\n";
		inputContextStr +=             "dimension t=Dimension(name=\"t\", values=(\"0.5\", \"0.75\", \"1.0\"), trainingDimension=\"true\");\n";
		inputContextStr +=             "model model=${lg};\n";
		inputContextStr +=             "evaluation evaluation=${accuracy};\n";
		inputContextStr +=             "};\n";
		
		
		String outputContextStr = inputContextStr;
		for (Entry<String, String> entry : variableMap.entrySet()) {
			String key = "${" + entry.getKey() + "}";
			outputContextStr = outputContextStr.replace(
					key, 
					entry.getValue());
		}
		
		testContextSerialization(inputContextStr, outputContextStr);
	}
	
	private void testContextSerializationSelf(String contextStr) {
		DataTools tools = new DataTools(new OutputWriter());
		Tools<String> datumTools = TestDatum.getStringTools(tools);
		Context<TestDatum<String>, String> context = Context.deserialize(datumTools, contextStr);
		Assert.assertEquals(contextStr, context.toString());
	}
	
	private void testContextSerialization(String contextInputStr, String contextOutputStr) {
		DataTools tools = new DataTools(new OutputWriter());
		Tools<String> datumTools = TestDatum.getStringTools(tools);
		Context<TestDatum<String>, String> context = Context.deserialize(datumTools, contextInputStr);
		Assert.assertEquals(contextOutputStr, context.toString());
	}
}
