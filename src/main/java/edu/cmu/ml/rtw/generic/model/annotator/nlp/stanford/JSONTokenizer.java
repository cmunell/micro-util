package edu.cmu.ml.rtw.generic.model.annotator.nlp.stanford;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPJSONLegacy;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;

/**
 * JSONTokenizer is a tokenizer that can be placed in
 * the StanfordCoreNLP pipeline to create tokenized documents
 * from existing sentence split tokenizations stored in
 * a JSON representation.
 * 
 * @author Bill McDowell
 *
 */
public class JSONTokenizer implements Annotator {
	private DataTools dataTools = new DataTools();
	
	public JSONTokenizer() {
		
	}
	
	public JSONTokenizer(String name, Properties properties) {
		
	}
	
	@Override
	public void annotate(Annotation annotation) {
		try {
			DocumentNLPMutable document = new DocumentNLPInMemory(this.dataTools);
			SerializerDocumentNLPJSONLegacy serializer = new SerializerDocumentNLPJSONLegacy(document);
			document = serializer.deserialize(new JSONObject(annotation.toString()));
			List<CoreMap> sentences = new ArrayList<CoreMap>();
			int tokenOffset = 0;
			List<CoreLabel> allTokens = new ArrayList<CoreLabel>();
			for (int i = 0; i < document.getSentenceCount(); i++) {
				List<CoreLabel> tokens = new ArrayList<CoreLabel>();
				StringBuilder sentenceText = new StringBuilder();
				for (int j = 0; j < document.getSentenceTokenCount(i); j++) {
					CoreLabel token = new CoreLabel();
					token.setBeginPosition(document.getToken(i, j).getCharSpanStart());
					token.setEndPosition(document.getToken(i, j).getCharSpanEnd());
					token.setSentIndex(i);
					token.setIndex(j + 1);
					token.set(CoreAnnotations.TextAnnotation.class, document.getTokenStr(i, j));
					token.setValue(document.getTokenStr(i, j));
					tokens.add(token);
					sentenceText.append(document.getTokenStr(i, j)).append(" ");
				}
				
				Annotation sentence = new Annotation(sentenceText.toString().trim());
				sentence.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, document.getToken(i, 0).getCharSpanStart());
				sentence.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, document.getToken(i, document.getSentenceTokenCount(i) - 1).getCharSpanEnd());
				sentence.set(CoreAnnotations.TokensAnnotation.class, tokens);
				sentence.set(CoreAnnotations.TokenBeginAnnotation.class, tokenOffset);
				tokenOffset += tokens.size();
				sentence.set(CoreAnnotations.TokenEndAnnotation.class, tokenOffset);
				sentence.set(CoreAnnotations.SentenceIndexAnnotation.class, sentences.size());
				
				sentences.add(sentence);
				allTokens.addAll(tokens);
			}
			
			annotation.set(CoreAnnotations.SentencesAnnotation.class, sentences);
			annotation.set(CoreAnnotations.TokensAnnotation.class, allTokens);
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
