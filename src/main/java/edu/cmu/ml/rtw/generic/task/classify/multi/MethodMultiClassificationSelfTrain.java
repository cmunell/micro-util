package edu.cmu.ml.rtw.generic.task.classify.multi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;

public class MethodMultiClassificationSelfTrain extends MethodMultiClassification implements MultiTrainable {
	private MethodMultiClassification method;
	private List<DataSet<?, ?>> unlabeledData;
	private int trainIters = 1;
	private boolean trainOnInit = true;
	private boolean weightData = false;
	private double dataScoreThreshold = -1.0;
	private List<EvaluationMultiClassificationMeasure> evaluations;
	private String[] parameterNames = { "method", "unlabeledData", "trainIters", "trainOnInit", "evaluations", "weightData", "dataScoreThreshold" };
	
	private List<DataSet<?, ?>> trainData;
	private List<DataSet<?, ?>> initData;
	
	public MethodMultiClassificationSelfTrain() {
		
	}
	
	public MethodMultiClassificationSelfTrain(Context context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("method")) {
			return (this.method == null) ? null : Obj.curlyBracedValue(this.method.getReferenceName());
		} else if (parameter.equals("unlabeledData")) {
			if (this.unlabeledData == null)
				return null;
			Obj.Array array = Obj.array();
			for (DataSet<?, ?> data : this.unlabeledData)
				array.add(Obj.curlyBracedValue(data.getReferenceName()));
			return array;			
		} else if (parameter.equals("trainIters")) {
			return Obj.stringValue(String.valueOf(this.trainIters));
		} else if (parameter.equals("trainOnInit")) 
			return Obj.stringValue(String.valueOf(this.trainOnInit));
		else if (parameter.equals("evaluations")) {
			if (this.evaluations == null)
				return null;
			Obj.Array array = Obj.array();
			for (EvaluationMultiClassificationMeasure evaluation : this.evaluations)
				array.add(Obj.curlyBracedValue(evaluation.getReferenceName()));
			return array;		
		} else if (parameter.equals("weightData")) {
			return Obj.stringValue(String.valueOf(this.weightData));
		} else if (parameter.equals("dataScoreThreshold")) {
			return Obj.stringValue(String.valueOf(this.dataScoreThreshold));
		}
		
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("method")) {
			this.method = (parameterValue == null) ? null : this.context.getMatchMultiClassifyMethod(parameterValue);
		} else if (parameter.equals("unlabeledData")) {
			if (parameterValue != null) {
				this.unlabeledData = new ArrayList<DataSet<?, ?>>();
				Obj.Array array = (Obj.Array)parameterValue;
				for (int i = 0; i < array.size(); i++)
					this.unlabeledData.add((DataSet<?, ?>)this.context.getAssignedMatches(array.get(i)).get(0));
			}			
		} else if (parameter.equals("trainIters")) {
			this.trainIters = (parameterValue == null) ? this.trainIters : Integer.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("trainOnInit")) {
			this.trainOnInit = (parameterValue == null) ? this.trainOnInit : Boolean.valueOf(this.trainOnInit);
		} else if (parameter.equals("evaluations")) {
			if (parameterValue != null) {
				this.evaluations = new ArrayList<EvaluationMultiClassificationMeasure>();
				Obj.Array array = (Obj.Array)parameterValue;
				for (int i = 0; i < array.size(); i++)
					this.evaluations.add((EvaluationMultiClassificationMeasure)this.context.getAssignedMatches(array.get(i)).get(0));
			}
		} else if (parameter.equals("weightData")) {
			this.weightData = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("dataScoreThreshold")) {
			this.dataScoreThreshold = Double.valueOf(this.context.getMatchValue(parameterValue));
		} else {
			return false;
		}
		return true;
	}

	@Override
	public List<Map<Datum<?>, ?>> classify(List<DataSet<?, ?>> data) {
		return this.method.classify(data);
	}
	
	@Override
	public List<Map<Datum<?>, Pair<?, Double>>> classifyWithScore(List<DataSet<?, ?>> data) {
		return this.method.classifyWithScore(data);
	}
	
	@Override
	public boolean init(List<DataSet<?, ?>> testData) {
		if (!this.method.init(testData))
			return false;
	
		if (!this.method.hasTrainable())
			return false;
		
		if (!makeTrainData())
			return false;
		
		if (this.trainOnInit)
			return train();
		else
			return true;
	}

	@Override
	public MethodMultiClassification clone() {
		MethodMultiClassificationSelfTrain clone = new MethodMultiClassificationSelfTrain(this.context);
		if (!clone.fromParse(this.getModifiers(), this.getReferenceName(), toParse()))
			return null;
		clone.method = this.method.clone();
		return clone;
	}

	@Override
	public MethodMultiClassification makeInstance(Context context) {
		return new MethodMultiClassificationSelfTrain(context);
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
		return "SelfTrain";
	}

	@Override
	public boolean train() {
		for (int i = 0; i < this.trainIters; i++) {
			this.context.getDataTools().getOutputWriter().debugWriteln("Self training iteration " + i);

			this.method.getTrainable().setTrainData(this.trainData);
			if (!this.method.getTrainable().train())
				return false;
			
			if (!makeTrainData())
				return false;
			
			for (EvaluationMultiClassificationMeasure evaluation : this.evaluations) {
				this.context.getDataTools().getOutputWriter().debugWriteln("Self training iteration " + i + " " + evaluation.getReferenceName() + " " + evaluation.compute(true));
			}
		}
		
		return true;
	}

	@Override
	public boolean setTrainData(List<DataSet<?, ?>> data) {
		this.trainData = data;
		return true;
	}

	@Override
	public List<DataSet<?, ?>> getTrainData() {
		return this.trainData;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean makeTrainData() {
		MultiTrainable trainable = this.method.getTrainable();
		if (this.initData == null)
			this.initData = trainable.getTrainData();
		Map<Tools<?, ?>, DataSet<?, ?>> typeData = new HashMap<>();
		for (DataSet<?, ?> d : this.initData) {
			if (!typeData.containsKey(d.getDatumTools()))
				typeData.put(d.getDatumTools(), d.cloneUnbuildable());
			else
				typeData.get(d.getDatumTools()).addAll((DataSet)d);
		}
		
		List<Map<Datum<?>, Pair<?, Double>>> labels = this.method.classifyWithScore(this.unlabeledData);
		for (int i = 0; i < this.unlabeledData.size(); i++) {
			DataSet<?, ?> unlabeledData = this.unlabeledData.get(i);
			Map<Datum<?>, Pair<?, Double>> dataLabels = labels.get(i);
			DataSet trainData = typeData.get(unlabeledData.getDatumTools());
			unlabeledData.map((ThreadMapper.Fn)(new ThreadMapper.Fn<Object, Boolean>() {
				@Override
				public Boolean apply(Object item) {
					Object label = null;
					double score = 0.0;
					synchronized (dataLabels) {
						if (dataLabels.containsKey(item)) {
							Pair<?, Double> labelScore = dataLabels.get(item);
							label = labelScore.getFirst();
							score = labelScore.getSecond();
						}
					}
					
					if (label == null || (dataScoreThreshold >= 0.0 && score < dataScoreThreshold))
						return true;
					
					Datum datum = (Datum)item;
					datum.setLabel(label); // FIXME This should clone the datum
					if (weightData)
						datum.setLabelWeight(label, score);
					else 
						datum.setLabelWeight(label, 1.0);
					
					synchronized (trainData) {
						trainData.add(datum);
					}
					
					return true;
				}
			}), this.context.getMaxThreads());
		}
		
		this.trainData = new ArrayList<>();
		this.trainData.addAll(typeData.values());
		
		return true;
	}

	@Override
	public boolean hasTrainable() {
		return true;
	}

	@Override
	public MultiTrainable getTrainable() {
		return this;
	}
}
