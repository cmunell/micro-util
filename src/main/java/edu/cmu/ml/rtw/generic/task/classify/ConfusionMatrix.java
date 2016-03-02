package edu.cmu.ml.rtw.generic.task.classify;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelMapping;

public class ConfusionMatrix<D extends Datum<L>, L> {
	private Map<L, Map<L, List<D>>> actualToPredicted;
	private Set<L> validLabels;
	private LabelMapping<L> labelMapping;

	public ConfusionMatrix(Map<L, Map<L, List<D>>> actualToPredicted) {
		this(actualToPredicted, actualToPredicted.keySet(), null);
	}

	public ConfusionMatrix(Map<L, Map<L, List<D>>> actualToPredicted, LabelMapping<L> labelMapping) {
		this(actualToPredicted, actualToPredicted.keySet(), labelMapping);
	}
	
	public ConfusionMatrix(Map<L, Map<L, List<D>>> actualToPredicted, Set<L> validLabels, LabelMapping<L> labelMapping) {
		this.actualToPredicted = actualToPredicted;
		this.validLabels = validLabels;
		this.labelMapping = labelMapping;
	}

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

	public Map<L, Map<L, Double>> asMap(double scale) {
		if (this.actualToPredicted == null)
			return null;
		
		Map<L, Map<L, Double>> confusionMatrix = new HashMap<L, Map<L, Double>>();
		
		for (Entry<L, Map<L, List<D>>> entryActual : this.actualToPredicted.entrySet()) {
			L actual = mapValidLabel(entryActual.getKey());
			if (actual == null)
				continue;
			confusionMatrix.put(actual, new HashMap<L, Double>());
			for (Entry<L, List<D>> entryPredicted : entryActual.getValue().entrySet()) {
				L predicted = mapValidLabel(entryPredicted.getKey());
				if (predicted == null)
					continue;
				confusionMatrix.get(actual).put(predicted, entryPredicted.getValue().size()*scale);
			}
		}
		
		return confusionMatrix;
	}

	public Map<L, Map<L, Double>> asMap() {
		return asMap(1.0);
	}

	@SuppressWarnings("unchecked")
	public String toString(double scale) {
		Map<L, Map<L, Double>> confusionMatrix = asMap(scale);
		StringBuilder confusionMatrixStr = new StringBuilder();
		L[] validLabels = (L[])this.validLabels.toArray();
		
		confusionMatrixStr.append("\t");
		for (int i = 0; i < validLabels.length; i++) {
			confusionMatrixStr.append(validLabels[i]).append(" (P)\t");
		}
		confusionMatrixStr.append("Total\tCorrect\tPercent\n");
		
		DecimalFormat cleanDouble = new DecimalFormat("0");
		
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
							  .append(cleanDouble.format(rowTotal - rowIncorrect))
							  .append("\t")
							  .append(rowTotal == 0 ? 0.0 : cleanDouble.format(100.0*(rowTotal - rowIncorrect)/rowTotal))
							  .append("\n");
		}
		
		confusionMatrixStr.append("Total\t");
		for (int i = 0; i < colTotals.length; i++)
			confusionMatrixStr.append(cleanDouble.format(colTotals[i])).append("\t");
		confusionMatrixStr.append("\n");
		
		confusionMatrixStr.append("Correct\t");
		for (int i = 0; i < colIncorrects.length; i++)
			confusionMatrixStr.append(cleanDouble.format(colIncorrects[i])).append("\t");
		confusionMatrixStr.append("\n");
		
		confusionMatrixStr.append("Percent\t");
		for (int i = 0; i < colTotals.length; i++)
			confusionMatrixStr.append(colTotals[i] == 0 ? 0.0 : cleanDouble.format(100.0*(colTotals[i] - colIncorrects[i])/colTotals[i])).append("\t");
		confusionMatrixStr.append("\n");
		
		return confusionMatrixStr.toString();
	}

	public String toString() {
		return toString(1.0);
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