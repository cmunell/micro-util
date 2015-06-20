package edu.cmu.ml.rtw.generic.data.feature;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;
import edu.cmu.ml.rtw.generic.util.CounterTable;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * FeatureNer computes a vector of indicators for 
 * whether a given datum has corresponding token spans with
 * some named-entity types.
 * 
 * Parameters:
 *  tokenExtractor - extractor for computing token spans from
 *  a datum
 *  
 *  useTypes - indicates whether there is a component in the 
 *  computed vector for each named-entity type (PERSON, 
 *  ORGANIZATION, etc)
 * 
 * @author Bill McDowell
 *
 * @param <D>
 * @param <L>
 */
public class FeatureNer<D extends Datum<L>, L> extends Feature<D, L> {
	protected BidirectionalLookupTable<String, Integer> vocabulary;
	
	protected Datum.Tools.TokenSpanExtractor<D, L> tokenExtractor;
	protected boolean useTypes;
	protected String[] parameterNames = { "tokenExtractor", "useTypes" };
	
	public FeatureNer() {
		
	}
	
	public FeatureNer(Context<D, L> context) {
		this.context = context;
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
		this.useTypes = true;
	}
	
	@Override
	public boolean init(FeaturizedDataSet<D, L> dataSet) {
		if (!this.useTypes)
			return true;
		
		CounterTable<String> counter = new CounterTable<String>();
		for (D datum : dataSet) {
			Set<String> types = getTypesForDatum(datum);
			for (String type : types) {
				counter.incrementCount(type);
			}
		}
		
		this.vocabulary = new BidirectionalLookupTable<String, Integer>(counter.buildIndex());
		
		return true;
	}
	
	private Set<String> getTypesForDatum(D datum){
		Set<String> types = new HashSet<String>();
		TokenSpan[] tokenSpans = this.tokenExtractor.extract(datum);
		
		for (TokenSpan span : tokenSpans) {
			DocumentNLP document = span.getDocument();
			List<Pair<TokenSpan, String>> nerAnnotations = document.getNer(span);
			for (Pair<TokenSpan, String> nerAnnotation : nerAnnotations) {
				types.add(nerAnnotation.getSecond());
			}
			
		}
		
		return types;
	}
	
	private boolean datumHasType(D datum){
		TokenSpan[] tokenSpans = this.tokenExtractor.extract(datum);
		
		for (TokenSpan span : tokenSpans) {
			DocumentNLP document = span.getDocument();
			List<Pair<TokenSpan, String>> nerAnnotations = document.getNer(span);
			if (nerAnnotations.size() > 0)
				return true;
		}
		
		return false;
	}
	
	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		if (this.useTypes) {
			Set<String> typesForDatum = getTypesForDatum(datum);
			for (String type : typesForDatum) {
				if (this.vocabulary.containsKey(type))
					vector.put(this.vocabulary.get(type) + offset, 1.0);		
			}	
		} else {
			vector.put(offset, (datumHasType(datum))? 1.0 : 0.0);
		}

		return vector;
	}


	@Override
	public String getGenericName() {
		return "Ner";
	}
	
	@Override
	public String getVocabularyTerm(int index) {
		return this.vocabulary.reverseGet(index);
	}

	@Override
	protected boolean setVocabularyTerm(int index, String term) {
		this.vocabulary.put(term, index);
		return true;
	}

	@Override
	public int getVocabularySize() {
		return this.vocabulary.size();
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("tokenExtractor"))
			return Obj.stringValue((this.tokenExtractor == null) ? "" : this.tokenExtractor.toString());
		else if (parameter.equals("useTypes"))
			return Obj.stringValue(String.valueOf(this.useTypes));
		return null;
	}
	
	
	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("tokenExtractor"))
			this.tokenExtractor = this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("useTypes"))
			this.useTypes = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public Feature<D, L> makeInstance(Context<D, L> context) {
		return new FeatureNer<D, L>(context);
	}

	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureNer<T, Boolean> binaryFeatureNer = (FeatureNer<T, Boolean>)binaryFeature;
		
		binaryFeatureNer.vocabulary = this.vocabulary;
		
		return binaryFeatureNer;
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		return internalAssignments;
	}

	@Override
	protected boolean cloneHelper(Feature<D, L> clone) {
		FeatureNer<D, L> cloneNer = (FeatureNer<D, L>)clone;
		
		cloneNer.vocabulary = this.vocabulary;
		
		return true;
	}
}
