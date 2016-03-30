package edu.cmu.ml.rtw.generic.model;

import java.io.StringReader;

import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.StoredItemSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.TestDatum;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Properties;

public class YADLLTest {
	@Test
	public void testYADLL() {
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
		contextStr +=           "model yadll=YADLL(numEpochs=\"300\", stepSize=\".6\", ";
		contextStr +=           "fnNodes=(\"Softmax\", \"NegativeLogLoss\"), ";
		contextStr +=           "fnParameters=(\"FanIn\", \"Zeros\"), ";
		contextStr +=           "Softmax_0_input=\"FanIn_0*x+Zeros_1\", "; 
		contextStr +=           "Softmax_0_size=\"2\", "; 
		contextStr +=           "NegativeLogLoss_1_input=\"Softmax_0\", "; 
		contextStr +=           "NegativeLogLoss_1_size=\"1\", "; 
		contextStr +=           "targetFnNode=\"Softmax_0\", ";
		contextStr +=           "lossFnNode=\"NegativeLogLoss_1\") { ";
		contextStr +=           "array validLabels=(\"true\", \"false\");\n";
		contextStr +=           "};\n";
		contextStr +=           "evaluation modelF1=F(filterLabel=\"true\", Beta=\"1\");\n";
		contextStr +=           "classify_method yadllMethod = SupervisedModel(model=${yadll}, data=${trainMatrix}, trainEvaluation=${modelF1});\n";
		contextStr +=           "search yadllSearch=Grid() {\n";
		contextStr +=           "dimension classificationThreshold=Enumerated(values=(\"0.1\", \"0.3\", \"0.5\", \"0.7\", \"0.9\"), stageIndex=\"1\");\n";
		contextStr +=           "};\n";
		contextStr +=           "classify_task devTask = Classification(data=${devData});\n";
		contextStr +=           "classify_eval devEval = F(task=${devTask}, method=${yadllMethod}, Beta=\"1\", filterLabel=\"true\");\n";
		contextStr +=           "classify_method bestMethod = RunClassifyMethodSearch(fn=${devEval}, search=${yadllSearch});\n";
		contextStr +=           "classify_task testTask = Classification(data=${testData});\n";
		contextStr +=           "classify_eval testF = F(task=${testTask}, method=${bestMethod}, Beta=\"1\", mode=\"MICRO\");\n";
		contextStr +=           "classify_eval testPrecision = Precision(task=${testTask}, method=${bestMethod}, mode=\"MICRO\");\n";
		contextStr +=           "classify_eval testRecall = Recall(task=${testTask}, method=${bestMethod}, mode=\"MICRO\");\n";
		contextStr +=           "value strEvals = OutputStrings(id=\"TestEvals\", storage=\"StringMemory\", collection=\"ExperimentEvaluationOutput\", refs=(${testF}, ${testPrecision}, ${testRecall}));\n";
		contextStr +=       "};\n";
		
		return contextStr;
	}
}
