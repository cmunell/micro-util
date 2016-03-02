package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class EvaluationClassificationConfusionData<D extends Datum<L>, L> extends EvaluationClassification<D, L, Map<L, Map<L, List<D>>>> {
	private Datum.Tools.TokenSpanExtractor<D, L> tokenExtractor;
	private String[] parameterNames = { "tokenExtractor" };
	
	public EvaluationClassificationConfusionData() {
		this(null);
	}
	
	public EvaluationClassificationConfusionData(DatumContext<D, L> context) {
		super(context);
	}
	
	@Override
	public String[] getParameterNames() {
		String[] parentParameterNames = super.getParameterNames();
		String[] parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + parentParameterNames.length);
		for (int i = 0; i < parentParameterNames.length; i++)
			parameterNames[this.parameterNames.length + i] = parentParameterNames[i];
		return parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("tokenExtractor"))
			return (this.tokenExtractor == null) ? null : Obj.stringValue(this.tokenExtractor.toString());
		else
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("tokenExtractor"))
			this.tokenExtractor = (parameterValue == null) ? null : this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else 
			return super.setParameterValue(parameter, parameterValue);
		return true;
	}

	@Override
	public Type getType() {
		return Type.CONFUSION_DATA;
	}

	@Override
	public Map<L, Map<L, List<D>>> compute() {
		return this.task.computeActualToPredictedData(this.method);
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		return null;
	}

	@Override
	public String getGenericName() {
		return "ConfusionData";
	}

	@Override
	public String toString() {
		StringBuilder description = new StringBuilder();
		Map<L, Map<L, List<D>>> actualToPredicted = compute();
		
		for (Entry<L, Map<L, List<D>>> entryActual : actualToPredicted.entrySet()) {
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
						description.append("RELATED SENTENCE: ").append(sentence).append("\n");
					description.append(datum.toString()).append("\n\n");
					
				}
			}
		}
		
		return description.toString();
	}

	@Override
	public EvaluationClassification<D, L, ?> makeInstance(
			DatumContext<D, L> context) {
		return new EvaluationClassificationConfusionData<D, L>(context);
	}
}
