package edu.cmu.ml.rtw.generic.model.annotator.nlp.stanford;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;

public class JSONTokenizer implements Annotator {
	
	public JSONTokenizer() {
		
	}
	
	public JSONTokenizer(String name, Properties properties) {
		
	}
	
	@Override
	public void annotate(Annotation annotation) {
		try {
			DocumentNLP document = new DocumentNLPInMemory(null, new JSONObject(annotation.toString()));
			List<CoreMap> sentences = new ArrayList<CoreMap>();
			for (int i = 0; i < document.getSentenceCount(); i++) {
				CoreMap sentence = new ArrayCoreMap();
				List<CoreLabel> tokens = new ArrayList<CoreLabel>();
				for (int j = 0; j < document.getSentenceTokenCount(i); j++) {
					CoreLabel token = new CoreLabel();
					token.setBeginPosition(document.getToken(i, j).getCharSpanStart());
					token.setEndPosition(document.getToken(i, j).getCharSpanEnd());
					token.setSentIndex(i);
					token.set(TextAnnotation.class, document.getTokenStr(i, j));
					tokens.add(token);
				}
				sentence.set(TokensAnnotation.class, tokens);
				sentences.add(sentence);
			}
			
			annotation.set(SentencesAnnotation.class, sentences);
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Annotator.TOKENIZE_AND_SSPLIT;
	}

	@Override
	public Set<Requirement> requires() {
		return new TreeSet<Requirement>();
	}

}
