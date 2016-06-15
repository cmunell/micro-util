package edu.cmu.ml.rtw.generic.data.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;
import edu.cmu.ml.rtw.generic.util.CounterTable;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;

/**
 * FeatureTokenSpanFnDataVocab computes a vector of weighted 
 * indicators of whether elements of a vocabulary of strings 
 * are associated with a datum.  The vocabulary of strings is
 * computed by running a function (constructed using the
 * edu.cmu.ml.rtw.generic.data.feature.fn package) on 
 * token spans extracted from the data in a given data set.
 * 
 * Parameters:
 *  minFeatureOccurrence - minimum number of times a string must 
 *  occur as the result of running fn on the data set for it 
 *  to be included in the vocabulary
 *  
 *  tokenExtractor - extractor of token spans from datums
 *  
 *  scale - method by which to scale the value of each component
 *  of vectors computed on data
 *
 *  fn - the function from token spans to strings that should
 *  be run on each data point to give an element of the 
 *  vocabulary
 *  
 *  initMode - determines whether idf and minFeatureOccurrence 
 *  filter are computed wiht respect to datums or documents
 * 
 * @author Bill McDowell
 *
 * @param <D>
 * @param <L>
 */
public class FeatureTokenSpanFnDataVocab<D extends Datum<L>, L> extends Feature<D, L> {
	/**
	 * 
	 * Scale gives possible functions by which to scale the computed
	 * feature vectors.  The INDICATOR function just returns 1 if an n-gram is 
	 * in F(T(d)) for datum d (F and T defined in documenation above).  The
	 * NORMALIZED_LOG function applies log(1+tf(F(T(d)), v) to n-gram v, where
	 * tf(x,v) computes the frequency of v in x.   Similarly, NORMALIZED_TFIDF
	 * applies tfidf for each n-gram.  Both NORMALIZED_LOG and NORMALIZED_TFIDF
	 * are normalized in the sense that the feature vector for n-gram v is 
	 * scaled to length 1.
	 * 
	 */
	public enum Scale {
		INDICATOR,
		NORMALIZED_LOG,
		NORMALIZED_TFIDF
	}
	
	public enum InitMode {
		BY_DATUM,
		BY_DOCUMENT
	}
	
	protected BidirectionalLookupTable<String, Integer> vocabulary;
	protected Map<Integer, Double> idfs; // maps vocabulary term indices to idf values to use in tfidf scale function
	
	protected int minFeatureOccurrence;
	protected Datum.Tools.TokenSpanExtractor<D, L> sourceTokenExtractor;
	protected Datum.Tools.TokenSpanExtractor<D, L> targetTokenExtractor;
	protected Datum.Tools.TokenSpanExtractor<D, L> tokenExtractor;
	protected Scale scale;
	protected Fn<TokenSpan, String> fn;
	protected InitMode initMode;
	protected String[] parameterNames = {"minFeatureOccurrence", "tokenExtractor", "sourceTokenExtractor", "targetTokenExtractor", "scale", "fn", "initMode"};
	
	public FeatureTokenSpanFnDataVocab() {
		
	}
	
	public FeatureTokenSpanFnDataVocab(DatumContext<D, L> context) {
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
		this.idfs = new HashMap<Integer, Double>();
		this.scale = Scale.INDICATOR;
		this.initMode = InitMode.BY_DATUM;
		this.context = context;
	}
	
	@Override
	public boolean init(DataSet<D, L> dataSet) {
		final CounterTable<String> counter = new CounterTable<String>();
		
		if (FeatureTokenSpanFnDataVocab.this.initMode == InitMode.BY_DATUM) { 
			dataSet.map(new ThreadMapper.Fn<D, Boolean>() {
				@Override
				public Boolean apply(D datum) {
					Map<String, Integer> gramsForDatum = applyFnToDatum(datum);
					for (String gram : gramsForDatum.keySet()) {
						counter.incrementCount(gram);
					}
					return true;
				}
			}, this.context.getMaxThreads());
		} else {
			final Map<String, Set<String>> gramsToDocuments = new ConcurrentHashMap<String, Set<String>>();
			dataSet.map(new ThreadMapper.Fn<D, Boolean>() {
				@Override
				public Boolean apply(D datum) {
					Map<String, Integer> gramsForDatum = applyFnToDatum(datum);
					String documentName = FeatureTokenSpanFnDataVocab.this.tokenExtractor.extract(datum)[0].getDocument().getName();
					for (String gram : gramsForDatum.keySet()) {
						synchronized (this) {
							if (!gramsToDocuments.containsKey(gram))
								gramsToDocuments.put(gram, new HashSet<String>());
							gramsToDocuments.get(gram).add(documentName);
						}
					}
					return true;
				}
			}, this.context.getMaxThreads());
			
			for (Entry<String, Set<String>> entry : gramsToDocuments.entrySet()) {
				counter.incrementCount(entry.getKey(), entry.getValue().size());
			}
		}
		
		counter.removeCountsLessThan(this.minFeatureOccurrence);
		
		this.vocabulary = new BidirectionalLookupTable<String, Integer>(counter.buildIndex());
		
		Map<String, Integer> counts = counter.getCounts();
		double N = dataSet.size();
		for (Entry<String, Integer> entry : counts.entrySet()) {
			this.idfs.put(this.vocabulary.get(entry.getKey()), Math.log(N/(1.0 + entry.getValue())));
		}
		
		return true;
	}

	public Map<String, Integer> applyFnToDatum(D datum) {
		Map<String, Integer> results = new HashMap<String, Integer>();
		if (this.tokenExtractor != null) {
			List<TokenSpan> spans = Arrays.asList(this.tokenExtractor.extract(datum));
			List<String> strs = this.fn.listCompute(spans);
			
			for (String str : strs) {
				if (!results.containsKey(str))
					results.put(str, 0);
				results.put(str, results.get(str) + 1);
			}
		} else {
			TokenSpan[] sourceSpans = this.sourceTokenExtractor.extract(datum);
			TokenSpan[] targetSpans = this.targetTokenExtractor.extract(datum);
			for (TokenSpan sourceSpan : sourceSpans) {
				for (TokenSpan targetSpan : targetSpans) {
					List<TokenSpan> sourceTarget = new ArrayList<>();
					sourceTarget.add(sourceSpan);
					sourceTarget.add(targetSpan);
					List<String> strs = this.fn.listCompute(sourceTarget);
				
					for (String str : strs) {
						if (!results.containsKey(str))
							results.put(str, 0);
						results.put(str, results.get(str) + 1);
					}
				}
			}
		}
		
		return results;
	}
	
	public Scale getScale() {
		return this.scale;
	}
	
	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		Map<String, Integer> gramsForDatum = applyFnToDatum(datum);
		
		if (this.scale == Scale.INDICATOR) {
			for (String gram : gramsForDatum.keySet()) {
				if (this.vocabulary.containsKey(gram))
					vector.put(this.vocabulary.get(gram) + offset, 1.0);		
			}
		} else if (this.scale == Scale.NORMALIZED_LOG) {
			double norm = 0.0;
			Map<Integer, Double> tempVector = new HashMap<Integer, Double>();
			for (Entry<String, Integer> entry : gramsForDatum.entrySet()) {
				if (!this.vocabulary.containsKey(entry.getKey()))
					continue;
				int index = this.vocabulary.get(entry.getKey());
				double value = Math.log(entry.getValue() + 1.0);
				norm += value*value;
				tempVector.put(index, value);
			}
			
			norm = Math.sqrt(norm);
			
			for (Entry<Integer, Double> entry : tempVector.entrySet()) {
				vector.put(entry.getKey() + offset, entry.getValue()/norm);
			}
		} else if (this.scale == Scale.NORMALIZED_TFIDF) {
			double norm = 0.0;
			Map<Integer, Double> tempVector = new HashMap<Integer, Double>();
			for (Entry<String, Integer> entry : gramsForDatum.entrySet()) {
				if (!this.vocabulary.containsKey(entry.getKey()))
					continue;
				int index = this.vocabulary.get(entry.getKey());
				double value = entry.getValue()*this.idfs.get(index);//Math.log(entry.getValue() + 1.0);
				norm += value*value;
				tempVector.put(index, value);
			}
			
			norm = Math.sqrt(norm);
			
			for (Entry<Integer, Double> entry : tempVector.entrySet()) {
				vector.put(entry.getKey() + offset, entry.getValue()/norm);
			}
		}

		return vector;
	}

	public Integer getVocabularyIndex(String term) {
		return this.vocabulary.get(term);
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
		if (parameter.equals("minFeatureOccurrence")) 
			return Obj.stringValue(String.valueOf(this.minFeatureOccurrence));
		else if (parameter.equals("fn")) {
			return this.fn.toParse();
		} else if (parameter.equals("tokenExtractor"))
			return (this.tokenExtractor == null) ? null : Obj.stringValue(this.tokenExtractor.toString());
		else if (parameter.equals("sourceTokenExtractor"))
			return (this.sourceTokenExtractor == null) ? null : Obj.stringValue(this.sourceTokenExtractor.toString());
		else if (parameter.equals("targetTokenExtractor"))
			return (this.targetTokenExtractor == null) ? null : Obj.stringValue(this.targetTokenExtractor.toString());
		else if (parameter.equals("scale"))
			return Obj.stringValue(this.scale.toString());
		else if (parameter.equals("initMode"))
			return Obj.stringValue(this.initMode.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("minFeatureOccurrence")) 
			this.minFeatureOccurrence = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("fn"))
			this.fn = this.context.getMatchOrConstructTokenSpanStrFn(parameterValue);
		else if (parameter.equals("tokenExtractor"))
			this.tokenExtractor = parameterValue == null ? null : this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("sourceTokenExtractor"))
			this.sourceTokenExtractor = parameterValue == null ? null : this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("targetTokenExtractor"))
			this.targetTokenExtractor = parameterValue == null ? null : this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("scale"))
			this.scale = Scale.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("initMode"))
			this.initMode = InitMode.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}
	
	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureTokenSpanFnDataVocab<T, Boolean> binaryFeatureTokenSpanFnDataVocab = (FeatureTokenSpanFnDataVocab<T, Boolean>)binaryFeature;
		
		binaryFeatureTokenSpanFnDataVocab.vocabulary = this.vocabulary;
		binaryFeatureTokenSpanFnDataVocab.idfs = this.idfs;
		
		return binaryFeatureTokenSpanFnDataVocab;
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		if (internalAssignments == null || !internalAssignments.contains("idfs"))
			return true;
		
		this.idfs = new HashMap<Integer, Double>();
		
		Obj.Array idfs = (Obj.Array)internalAssignments.get("idfs").getValue();
		for (int i = 0; i < idfs.size(); i++)
			this.idfs.put(i, Double.valueOf(idfs.get(i).getStr()));
		
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		if (this.vocabulary.size() == 0)
			return internalAssignments;
		
		Obj.Array idfs = Obj.array();
		for (int i = 0; i < this.vocabulary.size(); i++) {
			if (this.idfs.containsKey(i))
				idfs.add(Obj.stringValue(String.valueOf(this.idfs.get(i))));
			else
				idfs.add(Obj.stringValue("0.0"));
		}
		
		internalAssignments.add(Assignment.assignmentTyped(new ArrayList<String>(), Context.ObjectType.ARRAY.toString(), "idfs", idfs));
		
		return internalAssignments;
	}

	@Override
	public Feature<D, L> makeInstance(DatumContext<D, L> context) {
		return new FeatureTokenSpanFnDataVocab<D, L>(context);	
	}

	@Override
	public String getGenericName() {
		return "TokenSpanFnDataVocab";
	}

	@Override
	protected boolean cloneHelper(Feature<D, L> clone) {
		FeatureTokenSpanFnDataVocab<D, L> cloneData = (FeatureTokenSpanFnDataVocab<D, L>)clone;
		cloneData.vocabulary = this.vocabulary;
		cloneData.idfs = this.idfs;
		return true;
	}
}
