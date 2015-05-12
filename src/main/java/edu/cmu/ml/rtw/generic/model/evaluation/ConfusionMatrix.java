package edu.cmu.ml.rtw.generic.model.evaluation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelMapping;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;

/**
 * ConfusionMatrix represents a confusion matrix for assignments
 * of labels to a data set.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class ConfusionMatrix<D extends Datum<L>, L> {
	// map from actual to predicted label to datums
	private Map<L, Map<L, List<D>>> actualToPredicted;
	private Set<L> validLabels;
	private LabelMapping<L> labelMapping;
	
	/**
	 * @param validLabels - labels to include in the matrix
	 */
	public ConfusionMatrix(Set<L> validLabels) {
		this(validLabels, null);
	}
	
	/**
	 * 
	 * @param validLabels - labels to include in the matrix
	 * @param labelMapping - mapping from all labels to validLabels
	 */
	public ConfusionMatrix(Set<L> validLabels, LabelMapping<L> labelMapping) {
		this.validLabels = validLabels;
		this.labelMapping = labelMapping;
		this.actualToPredicted = new HashMap<L, Map<L, List<D>>>();
	}
	
	/**
	 * @param otherMatrix
	 * @return true if otherMatrix values have been added to this matrix's values
	 */
	public boolean add(ConfusionMatrix<D, L> otherMatrix) {
		for (Entry<L, Map<L, List<D>>> otherMatrixEntryActual : otherMatrix.actualToPredicted.entrySet()) {
			L actual = otherMatrixEntryActual.getKey();
			if (!this.actualToPredicted.containsKey(actual))
				this.actualToPredicted.put(actual, new HashMap<L, List<D>>());
			for (Entry<L, List<D>> otherMatrixEntryPredicted : otherMatrixEntryActual.getValue().entrySet()) {
				L predicted = otherMatrixEntryPredicted.getKey();
				if (!this.actualToPredicted.get(actual).containsKey(predicted))
					this.actualToPredicted.get(actual).put(predicted, new ArrayList<D>());
				this.actualToPredicted.get(actual).get(predicted).addAll(otherMatrixEntryPredicted.getValue());
			}
		}
		
		return true;
	}
	
	/**
	 * @param classifiedData
	 * @return true if classifiedData has been added to the confusion matrix
	 */
	public boolean addData(Map<D, L> classifiedData) {
		for (L actual : this.validLabels) {
			this.actualToPredicted.put(actual, new HashMap<L, List<D>>());
			for (L predicted : this.validLabels) {
				this.actualToPredicted.get(actual).put(predicted, new ArrayList<D>());
			}
		}
		
		for (Entry<D, L> classifiedDatum : classifiedData.entrySet()) {
			if (classifiedDatum.getKey().getLabel() == null)
				continue;
			L actualLabel = mapValidLabel(classifiedDatum.getKey().getLabel());
			L predictedLabel = mapValidLabel(classifiedDatum.getValue());
			
			if (actualLabel== null || predictedLabel == null)
				continue;
			
			this.actualToPredicted.get(actualLabel).get(predictedLabel).add(classifiedDatum.getKey());
		}
		
		return true;
	}
	
	/**
	 * @param scale
	 * @return the matrix as map of maps scaled by scale
	 */
	public Map<L, Map<L, Double>> getConfusionMatrix(double scale) {
		if (this.actualToPredicted == null)
			return null;
		
		Map<L, Map<L, Double>> confusionMatrix = new HashMap<L, Map<L, Double>>();
		
		for (Entry<L, Map<L, List<D>>> entryActual : this.actualToPredicted.entrySet()) {
			confusionMatrix.put(entryActual.getKey(), new HashMap<L, Double>());
			for (Entry<L, List<D>> entryPredicted : entryActual.getValue().entrySet()) {
				confusionMatrix.get(entryActual.getKey()).put(entryPredicted.getKey(), entryPredicted.getValue().size()*scale);
			}
		}
		return confusionMatrix;
	}
	
	public Map<L, Map<L, Double>> getConfusionMatrix() {
		return getConfusionMatrix(1.0);
	}
	
	@SuppressWarnings("unchecked")
	public String toString(double scale) {
		Map<L, Map<L, Double>> confusionMatrix = getConfusionMatrix(scale);
		StringBuilder confusionMatrixStr = new StringBuilder();
		L[] validLabels = (L[])this.validLabels.toArray();
		
		confusionMatrixStr.append("\t");
		for (int i = 0; i < validLabels.length; i++) {
			confusionMatrixStr.append(validLabels[i]).append(" (P)\t");
		}
		confusionMatrixStr.append("Total\tIncorrect\t% Incorrect\n");
		
		DecimalFormat cleanDouble = new DecimalFormat("0"); // FIXME: Pass as argument
		
		double[] colTotals = new double[this.validLabels.size()];
		double[] colIncorrects = new double[this.validLabels.size()];
		for (int i = 0; i < validLabels.length; i++) {
			confusionMatrixStr.append(validLabels[i]).append(" (A)\t");
			double rowTotal = 0.0;
			double rowIncorrect = 0.0;
			for (int j = 0; j < validLabels.length; j++) {
				if (confusionMatrix.containsKey(validLabels[i]) && confusionMatrix.get(validLabels[i]).containsKey(validLabels[j])) {
					double value = confusionMatrix.get(validLabels[i]).get(validLabels[j]);
					String cleanDoubleStr = cleanDouble.format(value);
					confusionMatrixStr.append(cleanDoubleStr)
									  .append("\t");
				
					rowTotal += value;
					rowIncorrect += ((i == j) ? 0 : value);
					colTotals[j] += value;
					colIncorrects[j] += ((i == j) ? 0 : value);
				} else
					confusionMatrixStr.append("0.0\t");
			}
			
			confusionMatrixStr.append(cleanDouble.format(rowTotal))
							  .append("\t")
							  .append(cleanDouble.format(rowIncorrect))
							  .append("\t")
							  .append(rowTotal == 0 ? 0.0 : cleanDouble.format(100.0*rowIncorrect/rowTotal))
							  .append("\n");
		}
		
		confusionMatrixStr.append("Total\t");
		for (int i = 0; i < colTotals.length; i++)
			confusionMatrixStr.append(cleanDouble.format(colTotals[i])).append("\t");
		confusionMatrixStr.append("\n");
		
		confusionMatrixStr.append("Incorrect\t");
		for (int i = 0; i < colIncorrects.length; i++)
			confusionMatrixStr.append(cleanDouble.format(colIncorrects[i])).append("\t");
		confusionMatrixStr.append("\n");
		
		confusionMatrixStr.append("% Incorrect\t");
		for (int i = 0; i < colTotals.length; i++)
			confusionMatrixStr.append(colTotals[i] == 0 ? 0.0 : cleanDouble.format(100.0*colIncorrects[i]/colTotals[i])).append("\t");
		confusionMatrixStr.append("\n");
		
		return confusionMatrixStr.toString();
	}
	
	public String toString() {
		return toString(1.0);
	}
	
	/**
	 * @param tokenExtractor
	 * @return a description of data for each entry in the 
	 * confusion matrix.  The description currently just
	 * contains the sentences associated with data that
	 * contributed to the value of an entry of the matrix.
	 */
	public String getActualToPredictedDescription(Datum.Tools.TokenSpanExtractor<D, L> tokenExtractor) {
		StringBuilder description = new StringBuilder();
		
		for (Entry<L, Map<L, List<D>>> entryActual : this.actualToPredicted.entrySet()) {
			for (Entry<L, List<D>> entryPredicted : entryActual.getValue().entrySet()) {
				if (entryActual.getKey().equals(entryPredicted.getKey()))
					continue;
				for (D datum: entryPredicted.getValue()) {
					
					TokenSpan[] tokenSpans = null; 
					String sentence = null;
					if (tokenExtractor != null) {
						tokenSpans = tokenExtractor.extract(datum);
						if (tokenSpans != null && tokenSpans.length > 0 && tokenSpans[0].getSentenceIndex() >= 0)
							sentence = tokenSpans[0].getDocument().getSentence(tokenSpans[0].getSentenceIndex());
					}
					
					description.append("PREDICTED: ").append(entryPredicted.getKey()).append("\n");
					description.append("ACTUAL: ").append(entryActual.getKey()).append("\n");
					if (sentence != null)
						description.append("FIRST SENTENCE: ").append(sentence).append("\n");
					description.append(datum.toString()).append("\n\n");
					
				}
			}
		}
		
		return description.toString();
	}
	
	/**
	 * @param actual
	 * @return a mapping from predicted labels to the lists of datums for
	 * which they were predicted
	 */
	public Map<L, List<D>> getPredictedForActual(L actual) {
		return this.actualToPredicted.get(actual);
	}
	
	public Map<L, List<D>> getActualForPredicted(L predicted) {
		Map<L, List<D>> actual = new HashMap<L, List<D>>();
	
		for (Entry<L, Map<L, List<D>>> entry : this.actualToPredicted.entrySet()) {
			if (!actual.containsKey(entry.getKey()))
				actual.put(entry.getKey(), new ArrayList<D>());
			actual.get(entry.getKey()).addAll(entry.getValue().get(predicted));
		}
		
		return actual;
	}
	
	public List<D> getActualPredicted(L actual, L predicted) {
		return this.actualToPredicted.get(actual).get(predicted);
	}
	
	protected L mapValidLabel(L label) {
		if (label == null)
			return null;
		if (this.labelMapping != null)
			label = this.labelMapping.map(label);
		if (this.validLabels.contains(label))
			return label;
		else
			return null;
	}
}
