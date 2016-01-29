package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.WeightedStringList;

/**
 * DocumentNLPDatum represents a single data instance consisting
 * of an NLP document
 * 
 * @author Bill McDowell
 *
 * @param <L>
 */
public class DocumentNLPDatum<L> extends Datum<L> {
	private DocumentNLP document;

	public DocumentNLPDatum(int id, DocumentNLP document, L label) {
		this.id = id;
		this.document = document;
		this.label = label;
	}
	
	public DocumentNLP getDocument() {
		return this.document;
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(this.id).append(": ").append(this.document.getName());
		
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
	
	public static abstract class Tools<L> extends Datum.Tools<DocumentNLPDatum<L>, L> { 
		public Tools(DataTools dataTools) {
			super(dataTools);
			
			this.addTokenSpanExtractor(new TokenSpanExtractor<DocumentNLPDatum<L>, L>() {
				@Override
				public String toString() {
					return "FirstTokenSpan";
				}
				
				@Override
				public TokenSpan[] extract(DocumentNLPDatum<L> documentDatum) {
					DocumentNLP document = documentDatum.getDocument();
					if (document.getSentenceCount() == 0 || document.getSentenceTokenCount(0) == 0)
						return new TokenSpan[0];
					
					return new TokenSpan[] {
						new TokenSpan(document, 0, 0, 1)	
					};
				}
			});
		}
		
		@Override
		public DocumentNLPDatum<L> datumFromJSON(JSONObject json) {
			try {
				int id = json.getInt("id");
				L label = (json.has("label")) ? labelFromString(json.getString("label")) : null;
				DocumentNLPMutable document = new DocumentNLPInMemory(this.dataTools);
				SerializerDocumentNLPJSONLegacy serializer = new SerializerDocumentNLPJSONLegacy(document);
				
				return new DocumentNLPDatum<L>(id, serializer.deserialize(json.getJSONObject("document")), label);
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		@Override
		public JSONObject datumToJSON(DocumentNLPDatum<L> datum) {
			JSONObject json = new JSONObject();
			
			try {
				json.put("id", datum.id);
				if (datum.label != null)
					json.put("label", datum.label.toString());
				DocumentNLPInMemory document = new DocumentNLPInMemory(datum.document);
				SerializerDocumentNLPJSONLegacy serializer = new SerializerDocumentNLPJSONLegacy(document);
				json.put("document", serializer.serialize(document));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return json;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <T extends Datum<Boolean>> T makeBinaryDatum(
				DocumentNLPDatum<L> datum,
				LabelIndicator<L> labelIndicator) {
			
			DocumentNLPDatum<Boolean> binaryDatum = new DocumentNLPDatum<Boolean>(datum.getId(), datum.getDocument(), 
					(labelIndicator == null || datum.getLabel() == null) ? null : labelIndicator.indicator(datum.getLabel()));
			
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
			DataTools dataTools = this.dataTools.makeInstance(output);
			Datum.Tools<T, Boolean> binaryTools = (Datum.Tools<T, Boolean>)DocumentNLPDatum.getBooleanTools(dataTools);
			
			return binaryTools;
			
		}
	}
}

