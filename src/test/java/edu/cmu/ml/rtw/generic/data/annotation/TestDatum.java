package edu.cmu.ml.rtw.generic.data.annotation;

import java.io.File;

import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassificationTernaryTest;
import edu.cmu.ml.rtw.generic.util.OutputWriter;

public class TestDatum<L> extends Datum<L> {
	private TokenSpan tokenSpan;
	
	public TestDatum(int id, TokenSpan tokenSpan, L label) {
		this.id = id;
		this.tokenSpan = tokenSpan;
		this.label = label;
	}
	
	public TokenSpan getTokenSpan() {
		return this.tokenSpan;
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(this.id).append(": ");
		str.append(this.tokenSpan.toString()).append(", ");
			
		return str.toString();
	}
	
	public static Tools<String> getStringTools(DataTools dataTools) {
		Tools<String> tools =  new Tools<String>(dataTools) {
			@Override
			public String labelFromString(String str) {
				return str;
			}
		};
	
		return tools;
	}
		
	public static Tools<Boolean> getBooleanTools(DataTools dataTools) {
		Tools<Boolean> tools =  new Tools<Boolean>(dataTools) {
			@Override
			public Boolean labelFromString(String str) {
				if (str == null)
					return null;
				return str.toLowerCase().equals("true") || str.equals("1");
			}
		};
		
		tools.addGenericDataSetBuilder(new TestDataSetBuilder());
	
		return tools;
	}
	
	public static Tools<TernaryLabel> getTernaryTools(DataTools dataTools) {
		Tools<TernaryLabel> tools =  new Tools<TernaryLabel>(dataTools) {
			@Override
			public TernaryLabel labelFromString(String str) {
				if (str == null)
					return null;
				return TernaryLabel.valueOf(str);
			}
		};
		
		tools.addGenericDataSetBuilder(new TernaryDataSetBuilder());
		tools.addGenericClassifyMethod(new MethodClassificationTernaryTest<>());
	
		return tools;
	}
	
	public static abstract class Tools<L> extends Datum.Tools<TestDatum<L>, L> { 
		public Tools(DataTools dataTools) {
			super(dataTools);
			
			
			this.addStringExtractor(new StringExtractor<TestDatum<L>, L>() {
				@Override
				public String toString() {
					return "TokenSpan";
				}
				
				@Override 
				public String[] extract(TestDatum<L> testDatum) {
					return new String[] { testDatum.tokenSpan.toString() };
				}
			});
			
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TestDatum<L>, L>() {
				@Override
				public String toString() {
					return "TokenSpan";
				}
				
				@Override
				public TokenSpan[] extract(TestDatum<L> testDatum) {
					return new TokenSpan[] { testDatum.tokenSpan };
				}
			});
		}
		
		@Override
		public TestDatum<L> datumFromJSON(JSONObject json) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public JSONObject datumToJSON(TestDatum<L> datum) {
			throw new UnsupportedOperationException();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <T extends Datum<Boolean>> T makeBinaryDatum(
				TestDatum<L> datum,
				LabelIndicator<L> labelIndicator) {
			
			TestDatum<Boolean> binaryDatum = new TestDatum<Boolean>(datum.getId(), datum.getTokenSpan(), (labelIndicator == null || datum.getLabel() == null) ? null : labelIndicator.indicator(datum.getLabel()));
			
			if (labelIndicator != null && datum.getLabel() != null) {
				double labelWeight = labelIndicator.weight(datum.getLabel());
				binaryDatum.setLabelWeight(true, labelWeight);
			}
			
			return (T)(binaryDatum);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Datum<Boolean>> Datum.Tools<T, Boolean> makeBinaryDatumTools(
				LabelIndicator<L> labelIndicator) {
			OutputWriter genericOutput = this.dataTools.getOutputWriter();
			OutputWriter output = new OutputWriter(
					(genericOutput.getDebugFilePath() != null) ? new File(genericOutput.getDebugFilePath() + "." + labelIndicator.toString()) : null,
					(genericOutput.getResultsFilePath() != null) ? new File(genericOutput.getResultsFilePath() + "." + labelIndicator.toString()) : null,
					(genericOutput.getDataFilePath() != null) ? new File(genericOutput.getDataFilePath() + "." + labelIndicator.toString()) : null,
					(genericOutput.getModelFilePath() != null) ? new File(genericOutput.getModelFilePath() + "." + labelIndicator.toString()) : null
				);
			
			DataTools dataTools = new DataTools(output);
			dataTools.setRandomSeed(this.dataTools.getGlobalRandom().nextLong());
			return (Datum.Tools<T, Boolean>)TestDatum.getBooleanTools(dataTools);
		}
	}
}
