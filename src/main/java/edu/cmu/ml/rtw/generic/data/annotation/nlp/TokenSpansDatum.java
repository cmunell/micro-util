package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan.SerializationType;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureRelationUnary;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.WeightedStringList;

public class TokenSpansDatum<L> extends Datum<L> {
	private TokenSpan[] tokenSpans;

	public TokenSpansDatum(int id, TokenSpan[] tokenSpans, L label) {
		this.id = id;
		this.tokenSpans = tokenSpans;
		this.label = label;
	}
	
	public TokenSpansDatum(int id, List<TokenSpan> tokenSpans, L label) {
		this.id = id;
		this.tokenSpans = new TokenSpan[tokenSpans.size()];
		for (int i = 0; i < this.tokenSpans.length; i++)
			this.tokenSpans[i] = tokenSpans.get(i);
		this.label = label;
	}
	
	public TokenSpan[] getTokenSpans() {
		return this.tokenSpans;
	}
	
	public TokenSpan getFirstTokenSpan() {
		return this.tokenSpans[0];
	}
	
	public TokenSpan getLastTokenSpan() {
		return this.tokenSpans[this.tokenSpans.length - 1];
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(this.id).append(": ");
		
		for (TokenSpan tokenSpan : this.tokenSpans)
			str.append(tokenSpan.toString()).append(", ");
			
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
	
		return tools;
	}
	
	
	public static Tools<WeightedStringList> getWeightedStringListTools(DataTools dataTools) {
		Tools<WeightedStringList> tools =  new Tools<WeightedStringList>(dataTools) {
			@Override
			public WeightedStringList labelFromString(String str) {
				if (str == null)
					return null;
				WeightedStringList l = new WeightedStringList();
				if (!l.fromString(str))
					return null; // FIXME Throw exception
				
				return l;
			}
		};
		
		tools.addInverseLabelIndicator(new Datum.Tools.InverseLabelIndicator<WeightedStringList>() {
			public String toString() {
				return "Weighted";
			}
			
			@Override
			public WeightedStringList label(Map<String, Double> indicatorWeights, List<String> positiveIndicators) {
				return new WeightedStringList(indicatorWeights);
			}
		});
		
		tools.addInverseLabelIndicator(new Datum.Tools.InverseLabelIndicator<WeightedStringList>() {
			public String toString() {
				return "Unweighted";
			}
			
			@Override
			public WeightedStringList label(Map<String, Double> indicatorWeights, List<String> positiveIndicators) {
				List<Pair<String, Double>> weightedLabels = new ArrayList<Pair<String, Double>>(indicatorWeights.size());
				for (String positiveIndicator : positiveIndicators) {
					weightedLabels.add(new Pair<String, Double>(positiveIndicator, 1.0));
				}
				return new WeightedStringList(weightedLabels);
			}
		});
		
		return tools;
	}
	
	public static abstract class Tools<L> extends Datum.Tools<TokenSpansDatum<L>, L> { 
		public Tools(DataTools dataTools) {
			super(dataTools);

			dataTools.addGenericWeightedStructure(new WeightedStructureRelationUnary("TS"));
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "AllDocumentSentenceInitialTokens";
				}
				
				@Override
				public TokenSpan[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					Set<String> documents = new HashSet<String>();
					List<TokenSpan> sentenceInitialTokens = new ArrayList<TokenSpan>();
					
					for (TokenSpan tokenSpan : tokenSpansDatum.tokenSpans) {
						if (documents.contains(tokenSpan.getDocument().getName()))
							continue;
						DocumentNLP document = tokenSpan.getDocument();
						int sentenceCount = document.getSentenceCount();
						for (int i = 0; i < sentenceCount; i++) {
							if (document.getSentenceTokenCount(i) <= 0 || i == tokenSpan.getSentenceIndex())
								continue;
							sentenceInitialTokens.add(new TokenSpan(document, i, 0, 1));
						}
							
					}
					
					return sentenceInitialTokens.toArray(new TokenSpan[0]);
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "AllTokenSpans";
				}
				
				@Override
				public TokenSpan[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					return tokenSpansDatum.tokenSpans;
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "First";
				}
				
				@Override
				public TokenSpan[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					if (tokenSpansDatum.tokenSpans.length == 0)
						return null;
					return new TokenSpan[] { tokenSpansDatum.tokenSpans[0] };
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "Last";
				}
				
				@Override
				public TokenSpan[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					if (tokenSpansDatum.tokenSpans.length == 0)
						return null;
					return new TokenSpan[] { tokenSpansDatum.tokenSpans[tokenSpansDatum.tokenSpans.length - 1] };
				}
			});
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<TokenSpansDatum<L>, L>() {
				@Override
				public String toString() {
					return "FirstLast";
				}
				
				@Override
				public TokenSpan[] extract(TokenSpansDatum<L> tokenSpansDatum) {
					if (tokenSpansDatum.tokenSpans.length == 0)
						return null;
					return new TokenSpan[] { tokenSpansDatum.tokenSpans[0], tokenSpansDatum.tokenSpans[tokenSpansDatum.tokenSpans.length - 1] };
				}
			});
			
			this.addGenericStructurizer(new StructurizerDocumentNLPGraphTokenSpans<L>());
		}
		

		@Override
		public TokenSpansDatum<L> datumFromJSON(JSONObject json) {
			try {
				int id = json.getInt("id");
				L label = (json.has("label")) ? labelFromString(json.getString("label")) : null;
				JSONArray jsonTokenSpans = json.getJSONArray("tokenSpans");
				List<TokenSpan> tokenSpans = new ArrayList<TokenSpan>();
				for (int i = 0; i < jsonTokenSpans.length(); i++) {
					tokenSpans.add(TokenSpan.fromJSON(jsonTokenSpans.getJSONObject(i), this.dataTools.getStoredItemSetManager()));
				}
				
				return new TokenSpansDatum<L>(id, tokenSpans, label);
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		@Override
		public JSONObject datumToJSON(TokenSpansDatum<L> datum) {
			JSONObject json = new JSONObject();
			
			try {
				json.put("id", datum.id);
				if (datum.label != null)
					json.put("label", datum.label.toString());
				
				JSONArray tokenSpans = new JSONArray();
				for (TokenSpan tokenSpan : datum.tokenSpans) {
					tokenSpans.put(tokenSpan.toJSON(SerializationType.STORE_REFERENCE));
				}
				
				json.put("tokenSpans", tokenSpans);
				if (tokenSpans.length() > 0)
					json.put("str", datum.tokenSpans[0].toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return json;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <T extends Datum<Boolean>> T makeBinaryDatum(
				TokenSpansDatum<L> datum,
				LabelIndicator<L> labelIndicator) {
			
			TokenSpansDatum<Boolean> binaryDatum = new TokenSpansDatum<Boolean>(datum.getId(), datum.getTokenSpans(), (labelIndicator == null || datum.getLabel() == null) ? null : labelIndicator.indicator(datum.getLabel()));
			
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
			return (Datum.Tools<T, Boolean>)TokenSpansDatum.getBooleanTools(this.dataTools);
		}
	}
}
