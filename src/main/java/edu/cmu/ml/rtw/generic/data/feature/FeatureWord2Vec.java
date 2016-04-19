package edu.cmu.ml.rtw.generic.data.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Word2Vec;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;
import edu.cmu.ml.rtw.generic.util.MathUtil;

public class FeatureWord2Vec<D extends Datum<L>, L> extends Feature<D, L> {
	public enum Mode {
		SIMILARITY,
		VECTOR,
		DIFFERENCE
	}
	
	protected BidirectionalLookupTable<String, Integer> vocabulary;
	
	protected Datum.Tools.TokenSpanExtractor<D, L> tokenExtractor;
	protected Fn<TokenSpan, String> fn;
	protected Mode mode;
	

	protected String[] parameterNames = {"tokenExtractor", "fn", "mode"};
	
	public FeatureWord2Vec() {
		
	}
	
	public FeatureWord2Vec(DatumContext<D, L> context) {
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
		this.context = context;
		
		this.mode = Mode.VECTOR;
	}
	
	@Override
	public boolean init(DataSet<D, L> dataSet) {
		Word2Vec w2v = this.context.getDataTools().getWord2Vec();
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
		
		if (this.mode == Mode.VECTOR || this.mode == Mode.DIFFERENCE) {
			int vectorSize = w2v.getVectorSize();
			for (int i = 0; i < vectorSize; i++)
				this.vocabulary.put(String.valueOf(i), i);
		} else {
			this.vocabulary.put("sim", 0);
		}
		
		return true;
	}

	public Map<String, Double> applyFnToSpan(TokenSpan span) {
		List<String> strs = this.fn.listCompute(span);

		Map<String, Double> results = new HashMap<String, Double>();

		for (String str : strs) {
			if (!results.containsKey(str))
				results.put(str, 0.0);
			results.put(str, results.get(str) + 1.0);
		}
		
		results = MathUtil.normalize(results, strs.size());
		
		return results;
	}
	
	private double computeSimilarity(Word2Vec w2v, List<Map<String, Double>> spanStrVectors) {
		double count = 0.0;
		double similarity = 0.0;
		for (int i = 0; i < spanStrVectors.size(); i++) {
			for (int j = 0; j < spanStrVectors.size(); j++) {
				if (i >= j)
					continue;
				for (Entry<String, Double> entry1 : spanStrVectors.get(i).entrySet()) {
					for (Entry<String, Double> entry2 : spanStrVectors.get(j).entrySet()) {
						similarity += entry1.getValue() * entry2.getValue() * w2v.computeSimilarity(entry1.getKey(), entry2.getKey());
					}
				}

				count++;
			}
		}
		
		similarity /= count;
		return similarity;
	}
	
	private double[] computeDifference(Word2Vec w2v, List<Map<String, Double>> spanStrVectors) {
		double[] difference = new double[w2v.getVectorSize()];
		double count = 0.0;
		for (int i = 0; i < spanStrVectors.size(); i++) {
			for (int j = 0; j < spanStrVectors.size(); j++) {
				if (i >= j)
					continue;
				for (Entry<String, Double> entry1 : spanStrVectors.get(i).entrySet()) {
					for (Entry<String, Double> entry2 : spanStrVectors.get(j).entrySet()) {
						double[] vec1 = w2v.computeVector(entry1.getKey());
						double[] vec2 = w2v.computeVector(entry2.getKey());
						
						double[] diff1_2 = MathUtil.subtract(vec1, vec2);
						diff1_2 = MathUtil.normalize(diff1_2, MathUtil.computeMagnitude(diff1_2) / (entry1.getValue() * entry2.getValue()));
						difference = MathUtil.add(diff1_2, difference);					
					}
				}

				count++;
			}
		}
		
		
		return MathUtil.normalize(difference, count);
	}
	
	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		Word2Vec w2v = this.context.getDataTools().getWord2Vec();
		TokenSpan[] spans = this.tokenExtractor.extract(datum);
		if (this.mode == Mode.SIMILARITY || this.mode == Mode.DIFFERENCE) {
			List<Map<String, Double>> spanStrVectors = new ArrayList<>();
			for (TokenSpan span : spans) {
				spanStrVectors.add(applyFnToSpan(span));
			}
			
			if (this.mode == Mode.SIMILARITY)
				vector.put(offset, computeSimilarity(w2v, spanStrVectors));
			else {
				double[] diff = computeDifference(w2v, spanStrVectors);
				for (int i = 0; i < diff.length; i++)
					vector.put(i + offset, diff[i]);
			}
		} else if (this.mode == Mode.VECTOR) {
			Map<String, Double> spanStrs = new HashMap<String, Double>();
			for (TokenSpan span : spans) {
				Map<String, Double> thisSpanStrs = applyFnToSpan(span);
				for (Entry<String, Double> entry : thisSpanStrs.entrySet()) {
					if (!spanStrs.containsKey(entry.getKey()))
						spanStrs.put(entry.getKey(), 0.0);
					spanStrs.put(entry.getKey(), spanStrs.get(entry.getKey()) + entry.getValue());
				}
			}
			
			spanStrs = MathUtil.normalize(spanStrs, spans.length);
			
			double[] vec = new double[w2v.getVectorSize()];
			for (Entry<String, Double> entry : spanStrs.entrySet()) {
				double[] strVec = w2v.computeVector(entry.getKey());
				for (int i = 0; i < strVec.length; i++)
					vec[i] += strVec[i] * entry.getValue();
			}
			
			for (int i = 0; i < vec.length; i++)
				vector.put(i + offset, vec[i]);
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
		if (parameter.equals("fn")) {
			return this.fn.toParse();
		} else if (parameter.equals("tokenExtractor"))
			return Obj.stringValue((this.tokenExtractor == null) ? "" : this.tokenExtractor.toString());
		else if (parameter.equals("mode"))
			return Obj.stringValue(this.mode.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("fn"))
			this.fn = this.context.getMatchOrConstructTokenSpanStrFn(parameterValue);
		else if (parameter.equals("tokenExtractor"))
			this.tokenExtractor = this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("mode"))
			this.mode = Mode.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}
	
	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureWord2Vec<T, Boolean> binaryFeatureWord2Vec = (FeatureWord2Vec<T, Boolean>)binaryFeature;
		binaryFeatureWord2Vec.vocabulary = this.vocabulary;
		
		return binaryFeatureWord2Vec;
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
	public Feature<D, L> makeInstance(DatumContext<D, L> context) {
		return new FeatureWord2Vec<D, L>(context);	
	}

	@Override
	public String getGenericName() {
		return "Word2Vec";
	}

	@Override
	protected boolean cloneHelper(Feature<D, L> clone) {
		FeatureWord2Vec<D, L> cloneW2V = (FeatureWord2Vec<D, L>)clone;
		cloneW2V.vocabulary = this.vocabulary;
		return true;
	}

}
