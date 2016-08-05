package edu.cmu.ml.rtw.generic.task.classify.meta;

import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;

public class PredictionClassificationDatum<L> extends Datum<L> {
	private PredictionClassification<?, ?> prediction;
	
	public PredictionClassificationDatum(int id, PredictionClassification<?, ?> prediction, L label) {
		this.id = id;
		this.prediction = prediction;
		this.label = label;
	}
	
	public PredictionClassification<?, ?> getPrediction() {
		return this.prediction;
	}

	@Override
	public String toString() {
		return this.id + ": " + this.prediction;
	}
	
	@SuppressWarnings("rawtypes")
	public static Tools<Boolean> getBooleanTools(Datum.Tools internalTools) {
		Tools<Boolean> tools = new Tools<Boolean>(internalTools) {
			@Override
			public Boolean labelFromString(String str) {
				if (str == null)
					return null;
				return str.toLowerCase().equals("true") || str.equals("1");
			}
		};
	
		tools.addGenericDataSetBuilder(new DataSetBuilderMetaClassification());
		
		tools.addGenericClassifyMethod(new MethodClassificationMetaScore());
		tools.addGenericClassifyMethod(new MethodClassificationMetaEvaluation());
		tools.addGenericClassifyMethod(new MethodClassificationMetaPerfect());
		
		return tools;
	}
	
	public static abstract class Tools<L> extends Datum.Tools<PredictionClassificationDatum<L>, L> { 
		public Tools(Datum.Tools<?, ?> internalTools) {
			super(internalTools.getDataTools());
			
			for (TokenSpanExtractor<?, ?> extractor : internalTools.getTokenSpanExtractors()) {
				this.addTokenSpanExtractor(new TokenSpanExtractor<PredictionClassificationDatum<L>, L>() {
					@SuppressWarnings({ "unchecked", "rawtypes" })
					@Override
					public TokenSpan[] extract(PredictionClassificationDatum<L> datum) {
						return ((TokenSpanExtractor)extractor).extract(datum.getPrediction().getDatum());
					}
				});
			}
		}
		

		@Override
		public PredictionClassificationDatum<L> datumFromJSON(JSONObject json) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public JSONObject datumToJSON(PredictionClassificationDatum<L> datum) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public <T extends Datum<Boolean>> T makeBinaryDatum(
				PredictionClassificationDatum<L> datum,
				LabelIndicator<L> labelIndicator) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T extends Datum<Boolean>> Datum.Tools<T, Boolean> makeBinaryDatumTools(
				LabelIndicator<L> labelIndicator) {
			throw new UnsupportedOperationException();
		}
	}
	
}
