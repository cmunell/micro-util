package edu.cmu.ml.rtw.generic.data;

import java.io.StringReader;

import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.TernaryLabel;
import edu.cmu.ml.rtw.generic.data.annotation.TestDatum;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Properties;


public class ContextTest {
	@Test
	public void testBinaryContext() {
		DataTools dataTools = new DataTools(new OutputWriter(), 
				new Properties(new StringReader(
						"debug_dir=\n" +
						"storage_fs_bson_testBson=/test/bson\n" +
						"storage_fs_str_testStr=/test/str"
						)));
		

		dataTools.addGenericContext(new DatumContext<TestDatum<Boolean>, Boolean>(TestDatum.getBooleanTools(dataTools), "TestBoolean"));
		Context.run("test", dataTools, makeBinaryContextString());
		
		StoredItemSet<?, ?> outputEvals = dataTools.getStoredItemSetManager().getItemSet("StringMemory", "ExperimentEvaluationOutput");
		System.out.println(outputEvals.getStoredItems().toString());
	}
	
	private String makeBinaryContextString() {
		String contextStr = "value maxThreads=\"2\";\n";
		contextStr +=       "value debug=Debug();\n";
		contextStr +=       "value randomSeed=SetRandomSeed(seed=\"6\");\n";
		contextStr +=       "context testBooleanCtx=TestBoolean() {\n";
		contextStr +=       	"data trainData = Test(storage=\"BSONMemory\", collection=\"TrainDocuments\");\n";
		contextStr +=       	"data devData = Test(storage=\"BSONMemory\", collection=\"DevDocuments\");\n";
		contextStr +=       	"data testData = Test(storage=\"BSONMemory\", collection=\"TestDocuments\");\n";
		contextStr +=           "value testSize = SizeData(data=${testData});\n";
		contextStr +=           "value sizeDataDebug = OutputDebug(refs=(${testSize}));\n";
		contextStr +=           "ts_fn doc1=NGramDocument(n=\"1\", noSentence=\"false\");\n";
		contextStr +=           "ts_str_fn strDef=String(cleanFn=\"DefaultCleanFn\");\n";
		contextStr +=           "feature fdoc1=TokenSpanFnDataVocab(scale=\"NORMALIZED_TFIDF\", minFeatureOccurrence=\"2\", tokenExtractor=\"TokenSpan\", fn=(${strDef} o ${doc1}));\n";
		contextStr +=           "feature_set f = FeatureSet(features=(${fdoc1}), initData=(${trainData}));\n";
		contextStr +=           "data_features trainMatrix = DataFeatureMatrix(data=${trainData}, features=${f});\n";
		contextStr +=           "model weka=WekaSVMOneClass(gamma=\".001\", targetLabel=\"true\", defaultOutlierLabel=\"false\", kernelType=\"RBF\")\n";
		contextStr +=           "{\n";
		contextStr +=           "array validLabels=(\"true\", \"false\");\n";
		contextStr +=           "};\n";
		contextStr +=           "evaluation modelF1=F(filterLabel=\"true\", Beta=\"1\");\n";
		contextStr +=           "classify_method wekaMethod = SupervisedModel(model=${weka}, data=${trainMatrix}, trainEvaluation=${modelF1});\n";
		contextStr +=           "search trr=Grid() {\n";
		contextStr +=           "dimension gamma=Enumerated(values=(\".1\",\".01\",\".0001\",\".00001\"), stageIndex=\"0\");\n";
		contextStr +=           "};\n";
		contextStr +=           "classify_task devTask = Classification(data=${devData});\n";
		contextStr +=           "classify_eval devEval = F(task=${devTask}, method=${wekaMethod}, Beta=\"1\", filterLabel=\"true\");\n";
		contextStr +=           "classify_method bestMethod = RunClassifyMethodSearch(fn=${devEval}, search=${trr});\n";
	
		contextStr +=           "classify_task testTask = Classification(data=${testData});\n";
		contextStr +=           "classify_eval testF = F(task=${testTask}, method=${bestMethod}, Beta=\"1\", mode=\"MICRO\");\n";
		contextStr +=           "classify_eval testPrecision = Precision(task=${testTask}, method=${bestMethod}, mode=\"MICRO\");\n";
		contextStr +=           "classify_eval testRecall = Recall(task=${testTask}, method=${bestMethod}, mode=\"MICRO\");\n";
		contextStr +=           "value strEvals = OutputStrings(id=\"TestEvals\", storage=\"StringMemory\", collection=\"ExperimentEvaluationOutput\", refs=(${testF}, ${testPrecision}, ${testRecall}));\n";
		contextStr +=       "};\n";
		
		return contextStr;
	}
	
	
	@Test
	public void testTernaryContext() {
		DataTools dataTools = new DataTools(new OutputWriter(), 
				new Properties(new StringReader(
						"debug_dir=\n" +
						"storage_fs_bson_testBson=/test/bson\n" +
						"storage_fs_str_testStr=/test/str"
						)));
		

		dataTools.addGenericContext(new DatumContext<TestDatum<TernaryLabel>, TernaryLabel>(TestDatum.getTernaryTools(dataTools), "TestTernary"));
		Context.run("test", dataTools, makeTernaryContextString());
		
		//StoredItemSet<?, ?> outputEvals = dataTools.getStoredItemSetManager().getItemSet("StringMemory", "ExperimentEvaluationOutput");
		//StoredItemSet<?, ?> outputParses = dataTools.getStoredItemSetManager().getItemSet("StringMemory", "ExperimentParseOutput");
		//System.out.println(outputEvals.getStoredItems().toString());
		//System.out.println(outputParses.getStoredItems().toString());
		
	}
	
	private String makeTernaryContextString() {
		String contextStr = "value maxThreads=\"2\";\n";
		contextStr +=       "value debug=Debug();\n";
		contextStr +=       "value randomSeed=SetRandomSeed(seed=\"6\");\n";
		contextStr +=       "context testTernaryCtx=TestTernary() {\n";
		contextStr +=       	"data trainData = Ternary(storage=\"BSONMemory\", collection=\"TrainDocuments\");\n";
		contextStr +=       	"data devData = Ternary(storage=\"BSONMemory\", collection=\"DevDocuments\");\n";
		contextStr +=       	"data testData = Ternary(storage=\"BSONMemory\", collection=\"TestDocuments\");\n";
		contextStr +=           "value trainPartitioned = PartitionData(data=${trainData}, distribution=(\".8\", \".1\", \".1\"));\n";
		contextStr +=           "data unionedData = UnionData(data=(${trainData_0}, ${trainData_1}, ${trainData_2}));";
		contextStr +=           "value trainSize = SizeData(data=${trainData});\n";
		contextStr +=           "value trainSize0 = SizeData(data=${trainData_0});\n";
		contextStr +=           "value trainSize1 = SizeData(data=${trainData_1});\n";
		contextStr +=           "value trainSize2 = SizeData(data=${trainData_2});\n";
		contextStr +=           "value unionedSize = SizeData(data=${unionedData});\n";
		contextStr +=           "value testSize = SizeData(data=${testData});\n";
		contextStr +=           "value sizeDataDebug = OutputDebug(refs=(${testSize}));\n";
		contextStr +=           "ts_fn doc1=NGramDocument(n=\"1\", noSentence=\"false\");\n";
		contextStr +=           "ts_str_fn strDef=String(cleanFn=\"DefaultCleanFn\");\n";
		contextStr +=           "feature fdoc1=TokenSpanFnDataVocab(scale=\"INDICATOR\", minFeatureOccurrence=\"2\", tokenExtractor=\"TokenSpan\", fn=(${strDef} o ${doc1}));\n";
		contextStr +=           "feature_set f = FeatureSet(features=(${fdoc1}), initData=(${trainData}));\n";
		contextStr +=           "data_features trainMatrix = DataFeatureMatrix(data=${trainData}, features=${f});\n";
		contextStr +=           "classify_method testMethod = TernaryTest(incorrect=\".2\", correct=\".2\");\n";
		contextStr +=           "classify_task testTask = Classification(data=${testData});\n";
		contextStr +=           "classify_eval testF = F(task=${testTask}, method=${testMethod}, Beta=\"1\", mode=\"MICRO\");\n";
		contextStr +=           "classify_eval testPrecision = Precision(task=${testTask}, method=${testMethod}, mode=\"MICRO\");\n";
		contextStr +=           "classify_eval testRecall = Recall(task=${testTask}, method=${testMethod}, mode=\"MICRO\");\n";
		contextStr +=           "value strEvals = OutputStrings(id=\"TestEvals\", storage=\"StringMemory\", collection=\"ExperimentEvaluationOutput\", refs=(${testF}, ${testPrecision}, ${testRecall}));\n";
		contextStr +=       "};\n";
		
		/*contextStr +=           "model lr=WekaOneClass(targetRejectionRate=\".1\", targetLabel=\"true\", defaultOutlierLabel=\"false\")\n";
		//contextStr +=           "model lr=Areg(l1=\"0\", l2=\"0\", convergenceEpsilon=\".00001\", maxTrainingExamples=\"1000001\", batchSize=\"200\", evaluationIterations=\"200\", maxEvaluationConstantIterations=\"500\", weightedLabels=\"false\", computeTestEvaluations=\"false\")\n";
		contextStr +=           "{\n";
		contextStr +=                "array validLabels=(\"true\", \"false\");\n";
		contextStr +=           "};\n";
		contextStr +=           "evaluation modelF1=F(filterLabel=\"true\", Beta=\"1\");\n";
		contextStr +=           "classify_method lrMethod = SupervisedModel(model=${lr}, data=${trainMatrix}, trainEvaluation=${modelF1});";
		
		contextStr +=           "search l2=Grid() {\n";
		contextStr +=                "dimension targetRejectionRate=Enumerated(values=(\".1\", \".3\", \".5\", \".7\", \".9\"), stageIndex=\"0\");\n";
		contextStr +=           "};\n";
		//contextStr +=           "search l2=Grid() {\n";
		//contextStr +=                "dimension l2=Enumerated(values=(\"0\", \".0000001\"), stageIndex=\"0\");\n";
		//contextStr +=                "dimension classificationThreshold=Enumerated(values=(\".5\", \".6\"), stageIndex=\"1\");\n";
		//contextStr +=           "};\n";
		contextStr +=           "classify_task devTask = Classification(data=${devMatrix});\n";
		contextStr +=           "classify_eval devEval = F(task=${devTask}, method=${lrMethod}, Beta=\"1\", filterLabel=\"true\");\n";
		contextStr +=           "classify_method bestLrMethod = RunClassifyMethodSearch(fn=${devEval}, search=${l2});\n";
		contextStr +=           "classify_task testTask = Classification(data=${testMatrix});\n";
	    contextStr +=           "classify_eval testAccuracy = Accuracy(task=${testTask}, method=${bestLrMethod});\n";       
		contextStr +=           "classify_eval testF = F(task=${testTask}, method=${bestLrMethod}, Beta=\"1\", filterLabel=\"true\");\n";
		contextStr +=           "classify_eval testPrecision = Precision(task=${testTask}, method=${bestLrMethod}, filterLabel=\"true\");\n";
		contextStr +=           "classify_eval testRecall = Recall(task=${testTask}, method=${bestLrMethod}, filterLabel=\"true\");\n";
		contextStr +=           "classify_eval testConfusionMatrix = ConfusionMatrix(task=${testTask}, method=${bestLrMethod});\n";
		contextStr +=           "classify_eval testConfusionData = ConfusionData(task=${testTask}, method=${bestLrMethod});\n";
		contextStr +=           "value strEvals = OutputStrings(id=\"TestEvals\", storage=\"StringMemory\", collection=\"ExperimentEvaluationOutput\", refs=(${testAccuracy}, ${testF}, ${testPrecision}, ${testRecall}, ${testConfusionMatrix}, ${l2}));";
		contextStr +=           "value strData = OutputStrings(id=\"TestEvalData\", storage=\"StringMemory\", collection=\"ExperimentEvaluationOutput\", refs=(${testConfusionData}));";
		contextStr +=           "value parseFeatures = OutputParses(id=\"TestFeatures\", storage=\"StringMemory\", collection=\"ExperimentParseOutput\", types=(\"features\"), fns=(${f}));";
		contextStr +=           "value parseModel = OutputParses(id=\"TestModel\", storage=\"StringMemory\", collection=\"ExperimentParseOutput\", types=(\"model\"), fns=(${bestLrMethod}), params=(\"modelInternal\"));";
		contextStr +=           "value ut=OutputDebug(refs=(${trainSize}));";
		contextStr +=           "value ut0=OutputDebug(refs=(${trainSize0}));";
		contextStr +=           "value ut1=OutputDebug(refs=(${trainSize1}));";
		contextStr +=           "value ut2=OutputDebug(refs=(${trainSize2}));";
		contextStr +=           "value ut3=OutputDebug(refs=(${unionedSize}));";
		contextStr +=       "};\n";*/
		return contextStr;
	}
	
	/* FIXME Needs refactored @Test
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
	}*/
}
