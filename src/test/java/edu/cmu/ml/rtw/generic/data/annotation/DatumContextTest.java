package edu.cmu.ml.rtw.generic.data.annotation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPDatum;
import edu.cmu.ml.rtw.generic.data.feature.FeatureSet;

public class DatumContextTest {
	@Test
	public void testContextFeatures() {
		BufferedReader ctxReader = new BufferedReader(new StringReader(getContextStr()));
		
		DatumContext<DocumentNLPDatum<Boolean>, Boolean> context = DatumContext.run(DocumentNLPDatum.getBooleanTools(new DataTools()), ctxReader);
		try {
			ctxReader.close();
		} catch (IOException e) {
			return;
		}
		
		FeatureSet<DocumentNLPDatum<Boolean>, Boolean> features = new FeatureSet<>(context, context.getFeatures());
		features.getFeatureVocabularySize();
	}
	
	private static String getContextStr() {
		String contextStr = "";
		contextStr += "value cleanFn=BuildCleanFn(name=\"BagOfWordsCleanFn\", fns=(\"Trim\", \"RemoveSymbols\", \"ReplaceNumbers\", \"UnderscoreToSpace\", \"Trim\", \"RemoveLongTokens\", \"Stem\", ";
		contextStr += "\"SpaceToUnderscore\"));\n";
		contextStr += "feature_set f=FeatureSet(features=(${fvbDoc1}), initData=(${trainData})) {\n";
		contextStr += "value vocabularySize=\"2\";\n";
		contextStr += "feature fvbDoc1=TokenSpanFnDataVocab(minFeatureOccurrence=\"8\", tokenExtractor=\"FirstTokenSpan\", scale=\"NORMALIZED_TFIDF\","; 
		contextStr +=  "fn=Composite(f=Composite(f=String(cleanFn=\"BagOfWordsCleanFn\", splitTokens=\"true\") {\n";
		contextStr +=  "value referenceName=\"strDef\";\n";
		contextStr +=  "}, g=FilterPoSTagClass(tagClass=\"VB\") {\n";
		contextStr +=  "value referenceName=\"posFilter\";\n";
		contextStr +=  "}) {\n";
		contextStr +=  "}, g=NGramDocument(n=\"1\", noSentence=\"false\") {\n";
		contextStr +=  "value referenceName=\"doc1\";\n";
		contextStr +=  "}) {\n";
		contextStr +=  "}, initMode=\"BY_DATUM\") {\n";
		contextStr +=  "array vocabulary=(\"cancel\", \"spoke\");\n";
		contextStr +=  "array idfs=(\"5.048412658430285\", \"3.1314900462482247\");\n";
		contextStr +=  "value referenceName=\"fvbDoc1\";\n";
		contextStr +=  "};\n";
		contextStr +=  "value referenceName=\"f\";\n";
		contextStr +=  "};";
		return contextStr;
	}
}
