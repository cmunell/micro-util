package edu.cmu.ml.rtw.generic.model.evaluation;

import java.util.List;
import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.TokenSpanExtractor;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.util.OutputWriter;

public abstract class Validation<D extends Datum<L>, L> {
	protected String name;
	protected Datum.Tools<D, L> datumTools;
	protected int maxThreads;
	protected SupervisedModel<D, L> model;
	protected TokenSpanExtractor<D, L> errorExampleExtractor;
	protected List<SupervisedModelEvaluation<D, L>> evaluations;
	
	protected ConfusionMatrix<D, L> confusionMatrix;
	protected List<Double> evaluationValues;
	
	public Validation(String name, Context<D, L> context) {
		this.name = name;
		this.datumTools = context.getDatumTools();
		this.maxThreads = context.getIntValue("maxThreads");
		this.model = context.getModels().get(0);
		this.errorExampleExtractor = context.getDatumTools().getTokenSpanExtractor(context.getStringValue("errorExampleExtractor"));
		this.evaluations = context.getEvaluations();
	}
	
	public Validation(String name, Datum.Tools<D, L> datumTools, int maxThreads, SupervisedModel<D, L> model, List<SupervisedModelEvaluation<D, L>> evaluations, TokenSpanExtractor<D, L> errorExampleExtractor) {
		this.name = name;
		this.datumTools = datumTools;
		this.maxThreads = maxThreads;
		this.model = model;
		this.evaluations = evaluations;
		this.errorExampleExtractor = errorExampleExtractor;
	}
	
	public boolean runAndOutput() {
		return run() != null && outputAll();
	}
	
	public boolean outputAll() {
		return outputModel() && outputData() && outputResults();
	}
	
	public boolean outputResults() {
		OutputWriter output = this.datumTools.getDataTools().getOutputWriter();
		
		output.resultsWriteln("\nEvaluation results:");
		
		for (int i = 0; i < this.evaluations.size(); i++)
			output.resultsWriteln(this.evaluations.get(i).toString() + ": " + this.evaluationValues.get(i));
		
		output.resultsWriteln("\nConfusion matrix:\n" + this.confusionMatrix.toString());
		
		output.resultsWriteln("\nTime:\n" + this.datumTools.getDataTools().getTimer().toString());
		
		return true;
	}
	
	public boolean outputModel() {
		this.datumTools.getDataTools().getOutputWriter().modelWriteln(this.model.toString());
		return true;
	}
	
	public boolean outputData() {
		this.datumTools.getDataTools().getOutputWriter().dataWriteln(this.confusionMatrix.getActualToPredictedDescription(this.errorExampleExtractor));
		return true;
	}
	
	public List<SupervisedModelEvaluation<D, L>> getEvaluations() {
		return this.evaluations;
	}
	
	public SupervisedModel<D, L> getModel() {
		return this.model;
	}
	
	public ConfusionMatrix<D, L> getConfusionMatrix() {
		return this.confusionMatrix;
	}
	
	public List<Double> getEvaluationValues() {
		return this.evaluationValues;
	}
	
	public String getErrorExamples() {
		return this.confusionMatrix.getActualToPredictedDescription(this.errorExampleExtractor);
	}
	
	public abstract List<Double> run();
}
