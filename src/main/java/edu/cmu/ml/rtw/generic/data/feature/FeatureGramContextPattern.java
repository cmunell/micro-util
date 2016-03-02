package edu.cmu.ml.rtw.generic.data.feature;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTagClass;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FeatureGramContextPattern computes an indicator vector 
 * whose components represent sequences of tokens before 
 * or after mentions of a noun-phrase that match a given pattern 
 * in terms of token strings and their parts of speech.
 *
 * Parameters:
 *  beforePattern - pattern that the tokens before the noun-phrase token 
 *  span must match for a context window to be retrieved
 *  
 *  afterPattern - pattern that the tokens after the noun-phrase token 
 *  span must match for a context window to be retrieved
 *	
 *  capturePart - (can be BEFORE or AFTER) determines whether the sequence 
 *  of tokens to retrieve is before or after the noun-phrase
 *
 *  captureGroup - determines the group number within the regular expression 
 *  pattern to capture and return as the context token sequence
 *  
 *  scale - scaling method for components of the vector
 *  
 *  tokenExtractor - determines which token spans associated with the 
 *  noun-phrase to consider for the contexts (currently this just 
 *  returns the token spans containing the noun-phrase)
 *
 *  cleanFn - a function that transforms (cleans) each token in the 
 *  noun-phrase context (for example, this currently does things like 
 *  remove symbols, stemming, replacing digits...)
 *  
 *  minFeatureOccurrence - minimum number of times a context must occur 
 *  across training examples for it to be included as a feature
 *  scale (can be INDICATOR or NORMALIZED_IDF or NORMALIZED_TFIDF) determines 
 *  how the value of a context window feature is scaled
 *
 * Patterns used for beforePattern and afterPattern
 *  These are basically an extension of Java's regular expressions that allows 
 *  specification of part-of-speech tag and token sequences in a short-hand notation. 
 *  
 *  The extensions to Java's regular expressions are:
 *   A string in single quotes matches a token whose text is the string. For example, 
 *   'string' matches a single token whose text is string
 *   
 *   A string without quotes matches a token whose part-of-speech tag is that string. 
 *   For example, NNP matches a token whose part-of-speach tag is NNP.
 *
 *   <g:G> matches a token whose text is in gazetteer G
 *   
 *   <p:P1, P2,...,Pk> matches a token whose part-of-speech tag is in tag class P1, P2,... or Pk.
 * 
 *   {~R} matches anything that does not match pattern R, looking ahead.
 *
 *   <<~p:P1,P2,...,Pk> matches anything that does not have a token with part-of-speech in tag 
 *   class P1, P2,... or Pk, looking backward.
 * 
 * @author Bill McDowell
 *
 * @param <D>
 * @param <L>
 */
public class FeatureGramContextPattern<D extends Datum<L>, L> extends FeatureGram<D, L> {
	public enum CapturePart {
		BEFORE,
		AFTER
	}
	
	protected Pattern posTagClassPattern = Pattern.compile("<p:([^>]*)>");
	protected Pattern gazetteerPattern = Pattern.compile("<g:([^>]*)>");
	protected Pattern negationPattern = Pattern.compile("\\{~([^\\}]*)\\}");
	protected Pattern backwardNegationPattern = Pattern.compile("\\{<~([^\\}]*)\\}");
	protected Pattern backwardNegationPosTagClassPattern = Pattern.compile("<<~p:([^>]*)>");
	
	protected String beforePattern;
	protected String afterPattern;
	protected CapturePart capturePart;
	protected int captureGroup;
	
	protected Pattern convertedBeforePattern;
	protected Pattern convertedAfterPattern;

	public FeatureGramContextPattern() {
		
	}
	
	public FeatureGramContextPattern(DatumContext<D, L> context) {
		super(context);
		
		this.beforePattern = "";
		this.afterPattern = "";
		this.capturePart = CapturePart.BEFORE;
		this.captureGroup = 0;
		
		this.parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + 4);
		this.parameterNames[this.parameterNames.length - 4] = "beforePattern";
		this.parameterNames[this.parameterNames.length - 3] = "afterPattern";
		this.parameterNames[this.parameterNames.length - 2] = "capturePart";
		this.parameterNames[this.parameterNames.length - 1] = "captureGroup";
	}

	@Override
	public Obj getParameterValue(String parameter) {
		Obj parameterValue = super.getParameterValue(parameter);
		if (parameterValue != null)
			return parameterValue;
		else if (parameter.equals("beforePattern"))
			return Obj.stringValue(this.beforePattern);
		else if (parameter.equals("afterPattern"))
			return Obj.stringValue(this.afterPattern);
		else if (parameter.equals("capturePart"))
			return Obj.stringValue(this.capturePart.toString());
		else if (parameter.equals("captureGroup"))
			return Obj.stringValue(String.valueOf(this.captureGroup));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (super.setParameterValue(parameter, parameterValue))
			return true;
		else if (parameter.equals("beforePattern")) {
			String value = this.context.getMatchValue(parameterValue);
			this.beforePattern = value;
			this.convertedBeforePattern = Pattern.compile(convertPattern(value));
		} else if (parameter.equals("afterPattern")) {
			String value = this.context.getMatchValue(parameterValue);
			this.afterPattern = value;
			this.convertedAfterPattern = Pattern.compile(convertPattern(value));
		} else if (parameter.equals("captureGroup"))
			this.captureGroup = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("capturePart"))
			this.capturePart = CapturePart.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		
		return true;
	}

	@Override
	protected Map<String, Integer> getGramsForDatum(D datum) {
		Map<String, Integer> grams = new HashMap<String, Integer>();
		TokenSpan[] tokenSpans = this.tokenExtractor.extract(datum);
		for (TokenSpan tokenSpan : tokenSpans) {
			boolean beforeMatches = true;
			boolean afterMatches = true;
			String capture = null;
			
			if (this.beforePattern.length() > 0) {
				String beforeStr = buildContextString(tokenSpan, CapturePart.BEFORE);
				if (beforeStr.length() == 0) {
					beforeMatches = false;
				} else {
					Matcher beforeMatcher = this.convertedBeforePattern.matcher(beforeStr);
					beforeMatches = beforeMatcher.matches();
					if (beforeMatches && this.capturePart == CapturePart.BEFORE) {
						capture = beforeMatcher.group(this.captureGroup);
					}
					
				}
			}
			
			if (this.afterPattern.length() > 0) {
				String afterStr = buildContextString(tokenSpan, CapturePart.AFTER);
				if (afterStr.length() == 0) {
					afterMatches = false;
				} else {
					Matcher afterMatcher = this.convertedAfterPattern.matcher(afterStr);
					afterMatches = afterMatcher.matches();
					if (afterMatches && this.capturePart == CapturePart.AFTER) {
						capture = afterMatcher.group(this.captureGroup);
					}
				}	
			}
			
			if (beforeMatches && afterMatches && capture != null) {
				String gram = removePoSTags(capture);
				if (!grams.containsKey(gram))
					grams.put(gram, 0);
				grams.put(gram, grams.get(gram) + 1);
			}
		}
		
		return grams;
	}
	
	protected String buildContextString(TokenSpan tokenSpan, CapturePart capturePart) {
		StringBuilder str = new StringBuilder();
		DocumentNLP document = tokenSpan.getDocument();
		int sentenceIndex = tokenSpan.getSentenceIndex();
		if (capturePart == CapturePart.BEFORE) {
			for (int i = 0; i < tokenSpan.getStartTokenIndex(); i++) {
				str.append(this.cleanFn.transform(document.getTokenStr(sentenceIndex, i)))
				   .append("/")
				   .append(document.getPoSTag(sentenceIndex, i))
				   .append(" ");
			}
		} else {
			int numSentenceTokens = document.getSentenceTokenCount(sentenceIndex);
			for (int i = tokenSpan.getEndTokenIndex(); i < numSentenceTokens; i++) {
				str.append(this.cleanFn.transform(document.getTokenStr(sentenceIndex, i)))
				   .append("/")
				   .append(document.getPoSTag(sentenceIndex, i))
				   .append(" ");
			}
		}
		
		return str.toString();
	}
	
	protected String removePoSTags(String str) {
		String[] strParts = str.split("[\\s]+");
		StringBuilder strRemoved = new StringBuilder();
		
		for (int i = 0; i < strParts.length; i++) {
			String[] tokenParts = strParts[i].split("/");
			strRemoved.append(tokenParts[0]).append("_");
		}
		
		strRemoved = strRemoved.delete(strRemoved.length() - 1, strRemoved.length());
		return strRemoved.toString().trim();
	}
	
	protected String convertPattern(String inputPattern) {
		// Replace pos tag class references with disjunctions
		Matcher posTagClassMatcher = this.posTagClassPattern.matcher(inputPattern);
		while (posTagClassMatcher.find()) {
			StringBuilder posTagStr = new StringBuilder();
			String posTagClassesStr = posTagClassMatcher.group(1);
			String[] posTagClasses = posTagClassesStr.split(",");
			posTagStr.append("(");
			for (String posTagClass : posTagClasses) {
				PoSTag[] posTags = PoSTagClass.fromString(posTagClass);
				for (PoSTag posTag : posTags)
					posTagStr.append(posTag).append("|");
			}
			posTagStr.delete(posTagStr.length() - 1, posTagStr.length());
			posTagStr.append(")");
			inputPattern = inputPattern.replace(posTagClassMatcher.group(), posTagStr.toString());
			
			posTagClassMatcher = this.posTagClassPattern.matcher(inputPattern);
		}
		
		// Replace gazetteer references with disjunctions
		Matcher gazetteerMatcher = this.gazetteerPattern.matcher(inputPattern);
		while (gazetteerMatcher.find()) {
			StringBuilder termStr = new StringBuilder();
			String gazetteerName = gazetteerMatcher.group(1);
			Gazetteer gazetteer = this.context.getDatumTools().getDataTools().getGazetteer(gazetteerName);
			termStr.append("(");
			Set<String> terms = gazetteer.getValues();
			for (String term : terms) {
				termStr.append("'").append(term).append("'|");
			}
			termStr.delete(termStr.length() - 1, termStr.length());
			termStr.append(")");
			inputPattern = inputPattern.replace(gazetteerMatcher.group(), termStr.toString());
			
			gazetteerMatcher = this.gazetteerPattern.matcher(inputPattern);
		}
		
		// Replace negations with correct syntax
		Matcher negationMatcher = this.negationPattern.matcher(inputPattern);
		while (negationMatcher.find()) {
			String negatedStr = negationMatcher.group(1);
			String replacedNegation = "(?!.*" + negatedStr + ")";
			inputPattern = inputPattern.replace(negationMatcher.group(), replacedNegation);
			negationMatcher = this.negationPattern.matcher(inputPattern);
		}
		
		// Replace backward negations with correct syntax
		Matcher backwardNegationMatcher = this.backwardNegationPattern.matcher(inputPattern);
		while (backwardNegationMatcher.find()) {
			String negatedStr = backwardNegationMatcher.group(1);
			String replacedNegation = "(?<!" + negatedStr + ")";
			inputPattern = inputPattern.replace(backwardNegationMatcher.group(), replacedNegation);
			negationMatcher = this.backwardNegationPattern.matcher(inputPattern);
		}
		
		// Replace pos tag class references with disjunctions
		Matcher backwardNegationPosTagClassMatcher = this.backwardNegationPosTagClassPattern.matcher(inputPattern);
		while (backwardNegationPosTagClassMatcher.find()) {
			StringBuilder posTagStr = new StringBuilder();
			String posTagClassesStr = backwardNegationPosTagClassMatcher.group(1);
			String[] posTagClasses = posTagClassesStr.split(",");
			for (String posTagClass : posTagClasses) {
				PoSTag[] posTags = PoSTagClass.fromString(posTagClass);
				for (PoSTag posTag : posTags)
					posTagStr.append("(?<!/").append(posTag).append(")");
			}
			
			inputPattern = inputPattern.replace(backwardNegationPosTagClassMatcher.group(), posTagStr.toString());
			
			posTagClassMatcher = this.backwardNegationPosTagClassPattern.matcher(inputPattern);
		}
		
		inputPattern = inputPattern.replaceAll("([^'A-Z\\$/])([\\$A-Z]+)", "$1([^\\\\s]+/$2[\\\\s]+)");
		inputPattern = inputPattern.replaceAll("'([^']+)'", "$1/[^\\\\s]+[\\\\s]+");
		
		return inputPattern;
	}

	@Override
	public String getGenericName() {
		return "GramContextPattern";
	}

	@Override
	public Feature<D, L> makeInstance(DatumContext<D, L> context) {
		return new FeatureGramContextPattern<D, L>(context);
	}
}
