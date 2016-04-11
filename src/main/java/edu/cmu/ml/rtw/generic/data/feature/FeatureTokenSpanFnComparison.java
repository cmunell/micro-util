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
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.MathUtil;

public class FeatureTokenSpanFnComparison<D extends Datum<L>, L> extends Feature<D, L> {
	public enum Mode {
		EQUALITY,
		SIMILARITY
	}
	
	protected Datum.Tools.TokenSpanExtractor<D, L> tokenExtractor;
	protected Fn<TokenSpan, String> fn;
	protected Mode mode;
	

	protected String[] parameterNames = {"tokenExtractor", "fn", "mode"};
	
	public FeatureTokenSpanFnComparison() {
		
	}
	
	public FeatureTokenSpanFnComparison(DatumContext<D, L> context) {
		this.context = context;
		this.mode = Mode.EQUALITY;
	}
	
	@Override
	public boolean init(DataSet<D, L> dataSet) {
		return true;
	}

	private Map<String, Double> applyFnToSpan(TokenSpan span) {
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
	
	public double compare(Map<String, Double> first, Map<String, Double> second) {
		double result = 0.0;
		
		if (this.mode == Mode.EQUALITY) {
			result = 1.0;
			if (first.size() == second.size()) {
				for (Entry<String, Double> entry : first.entrySet()) {
					if (!second.containsKey(entry.getKey()) || Double.compare(entry.getValue(), second.get(entry.getKey())) != 0) {
						result = 0.0;
						break;
					}
				}
			}
		} else {
			for (Entry<String, Double> entry : first.entrySet()) {
				if (second.containsKey(entry.getKey())) {
					result += entry.getValue() * second.get(entry.getKey());
				}
			}
		}
		
		return result;
	}
	
	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		TokenSpan[] spans = this.tokenExtractor.extract(datum);

		List<Map<String, Double>> spanStrVectors = new ArrayList<>();
		for (TokenSpan span : spans) {
			spanStrVectors.add(applyFnToSpan(span));
		}
		
		double count = 0.0;
		double similarity = 0.0;
		for (int i = 0; i < spanStrVectors.size(); i++) {
			for (int j = 0; j < spanStrVectors.size(); j++) {
				if (i >= j)
					continue;
				similarity += compare(spanStrVectors.get(i), spanStrVectors.get(j));
				count++;
			}
		}
		
		similarity /= count;
		vector.put(offset, similarity);
		
		return vector;
	}

	public Integer getVocabularyIndex(String term) {
		return (term.equals("val")) ? 0 : null;
	}
	
	@Override
	public String getVocabularyTerm(int index) {
		return index == 0 ? "val" : null;
	}

	@Override
	protected boolean setVocabularyTerm(int index, String term) {
		return true;
	}

	@Override
	public int getVocabularySize() {
		return 1;
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
		return binaryFeature;
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
		return new FeatureTokenSpanFnComparison<D, L>(context);	
	}

	@Override
	public String getGenericName() {
		return "TokenSpanFnComparison";
	}

	@Override
	protected boolean cloneHelper(Feature<D, L> clone) {
		return true;
	}

}
