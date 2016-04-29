package edu.cmu.ml.rtw.generic.task.classify.multi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Singleton;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.Triple;

public class MethodMultiClassificationSelfTrain extends MethodMultiClassification implements MultiTrainable {
	private MethodMultiClassification method;
	private List<DataSet<?, ?>> unlabeledData;
	private int trainIters = 1;
	private boolean trainOnInit = true;
	private boolean weightData = false;
	private double dataScoreThreshold = -1.0;
	private boolean incremental = false;
	private boolean incrementByLabel = true;
	private int incrementSize = 300;
	private List<EvaluationMultiClassificationMeasure> evaluations;
	private String[] parameterNames = { "method", "unlabeledData", "trainIters", "trainOnInit", "evaluations", "weightData", "dataScoreThreshold", "incremental", "incrementSize", "incrementByLabel" };
	
	private List<DataSet<?, ?>> trainData;
	private List<DataSet<?, ?>> initData;
	private List<DataSet<?, ?>> iterUnlabeledData;
	
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
		} else if (parameter.equals("incremental")) {
			return Obj.stringValue(String.valueOf(this.incremental));
		} else if (parameter.equals("incrementSize")) {
			return Obj.stringValue(String.valueOf(this.incrementSize));
		} else if (parameter.equals("incrementByLabel")) {
			return Obj.stringValue(String.valueOf(this.incrementByLabel));
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
		} else if (parameter.equals("incremental")) {
			this.incremental = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("incrementSize")) {
			this.incrementSize = Integer.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("incrementByLabel")) {
			this.incrementByLabel = Boolean.valueOf(this.context.getMatchValue(parameterValue));
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
		
		MultiTrainable trainable = this.method.getTrainable();
		if (this.initData == null)
			this.initData = trainable.getTrainData();
		
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
		for (EvaluationMultiClassificationMeasure evaluation : this.evaluations) {
			this.context.getDataTools().getOutputWriter().debugWriteln("Self training (init) " + evaluation.getReferenceName() + " " + evaluation.compute(true));
		}
		
		for (int i = 0; i < this.trainIters; i++) {
			this.context.getDataTools().getOutputWriter().debugWriteln("Self training iteration " + i);

			this.method.getTrainable().setTrainData(this.trainData);
			if (!this.method.getTrainable().train())
				return false;
			
			for (EvaluationMultiClassificationMeasure evaluation : this.evaluations) {
				this.context.getDataTools().getOutputWriter().debugWriteln("Self training iteration " + i + " " + evaluation.getReferenceName() + " " + evaluation.compute(true));
			}
			
			if (!makeTrainData())
				return false;
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
	
	private boolean makeTrainData() {
		if (this.incremental && this.incrementByLabel) {
			return makeTrainDataIncrementalByLabel();
		} else if (this.incremental && !this.incrementByLabel) {
			return makeTrainDataIncremental();
		} else {
			return makeTrainDataBatch();
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean makeTrainDataIncremental() {
		if (this.iterUnlabeledData == null) {
			this.iterUnlabeledData = this.unlabeledData;
		}
		
		MultiTrainable trainable = this.method.getTrainable();
		Map<Tools<?, ?>, DataSet<?, ?>> nextIterTrainData = new HashMap<>();
		for (DataSet<?, ?> d : trainable.getTrainData()) {
			if (!nextIterTrainData.containsKey(d.getDatumTools())) {
				nextIterTrainData.put(d.getDatumTools(), d.cloneUnbuildable());
			} else
				nextIterTrainData.get(d.getDatumTools()).addAll((DataSet)d);
		}
		
		Map<Tools<?, ?>, DataSet<?, ?>> nextIterUnlabeledData = new HashMap<>();
		for (DataSet<?, ?> d : this.iterUnlabeledData) {
			if (!nextIterUnlabeledData.containsKey(d.getDatumTools())) {
				DataSet<?, ?> dClone = d.cloneUnbuildable();
				dClone.clear();
				nextIterUnlabeledData.put(d.getDatumTools(), dClone);
			}
		}
		double lastScore = 0.0;
		int dataAdded = 0;
		List<Map<Datum<?>, Pair<?, Double>>> labels = this.method.classifyWithScore(this.iterUnlabeledData);
		for (int i = 0; i < this.iterUnlabeledData.size(); i++) {
			DataSet<?, ?> curUnlabeledData = this.iterUnlabeledData.get(i);
			Map<Datum<?>, Pair<?, Double>> dataLabels = labels.get(i);
			DataSet nextTrainData = nextIterTrainData.get(curUnlabeledData.getDatumTools());
			DataSet nextUnlabeledData = nextIterUnlabeledData.get(curUnlabeledData.getDatumTools());

			List<Triple<Datum, Object, Double>> ordering = getDataOrdering(dataLabels);
			
			for (int j = 0; j < ordering.size(); j++) {
				Triple<Datum, Object, Double> e = ordering.get(j);
				Object label = e.getSecond();
				Datum d = e.getFirst();
				double score = e.getThird();
				
				if (j < this.incrementSize && (this.dataScoreThreshold < 0 || score >= this.dataScoreThreshold)) {
					d.setLabel(label); // FIXME This should clone the datum
					if (this.weightData)
						d.setLabelWeight(label, score);
					else 
						d.setLabelWeight(label, 1.0);
					nextTrainData.add(d);
					lastScore = score;
					dataAdded++;
				} else {
					nextUnlabeledData.add(d);
				}
			
				
			}
		}
		
		this.trainData = new ArrayList<>();
		this.trainData.addAll(nextIterTrainData.values());
		
		this.iterUnlabeledData = new ArrayList<>();
		this.iterUnlabeledData.addAll(nextIterUnlabeledData.values());
		
		this.context.getDataTools().getOutputWriter().debugWriteln("Self training added " + dataAdded + " self-labeled data (lowest score: " + lastScore + ")");
		
		return true;
	}
	
	// (datum, label, score) list ordered descending
	@SuppressWarnings({ "rawtypes" })
	private List<Triple<Datum, Object, Double>> getDataOrdering(Map<Datum<?>, Pair<?, Double>> dataLabels) {
		List<Triple<Datum, Object, Double>> ordering = new ArrayList<>();
		for (Entry<Datum<?>, Pair<?, Double>> entry : dataLabels.entrySet()) {
			Object label = entry.getValue().getFirst();
			Datum<?> datum = entry.getKey();
			double score = entry.getValue().getSecond();
			ordering.add(new Triple<>(datum, label, score));
		}
		
		ordering.sort(new Comparator<Triple<Datum, Object, Double>>() {
			@Override
			public int compare(Triple<Datum, Object, Double> o1, Triple<Datum, Object, Double> o2) {
				return Double.compare(o2.getThird(), o1.getThird());
			}
		});
	
		return ordering;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean makeTrainDataIncrementalByLabel() {
		if (this.iterUnlabeledData == null) {
			this.iterUnlabeledData = this.unlabeledData;
		}
		
		MultiTrainable trainable = this.method.getTrainable();
		Map<Tools<?, ?>, DataSet<?, ?>> nextIterTrainData = new HashMap<>();
		for (DataSet<?, ?> d : trainable.getTrainData()) {
			if (!nextIterTrainData.containsKey(d.getDatumTools())) {
				nextIterTrainData.put(d.getDatumTools(), d.cloneUnbuildable());
			} else
				nextIterTrainData.get(d.getDatumTools()).addAll((DataSet)d);
		}
		
		Map<Tools<?, ?>, DataSet<?, ?>> nextIterUnlabeledData = new HashMap<>();
		for (DataSet<?, ?> d : this.iterUnlabeledData) {
			if (!nextIterUnlabeledData.containsKey(d.getDatumTools())) {
				DataSet<?, ?> dClone = d.cloneUnbuildable();
				dClone.clear();
				nextIterUnlabeledData.put(d.getDatumTools(), dClone);
			}
		}
		
		int dataAdded = 0;
		List<Map<Datum<?>, Pair<?, Double>>> labels = this.method.classifyWithScore(this.iterUnlabeledData);
		for (int i = 0; i < this.iterUnlabeledData.size(); i++) {
			DataSet<?, ?> curUnlabeledData = this.iterUnlabeledData.get(i);
			Map<Datum<?>, Pair<?, Double>> dataLabels = labels.get(i);
			DataSet nextTrainData = nextIterTrainData.get(curUnlabeledData.getDatumTools());
			DataSet nextUnlabeledData = nextIterUnlabeledData.get(curUnlabeledData.getDatumTools());
			Map labelCountsToAdd = getLabelCountsToAdd(nextTrainData);
			Map<Object, List<Pair<Datum, Double>>> labelsToDataOrderings = getDataOrderingsForLabels(dataLabels);
			
			for (Entry<Object, List<Pair<Datum, Double>>> entry : labelsToDataOrderings.entrySet()) {
				Object label = entry.getKey();
				int trainingCount = (Integer)labelCountsToAdd.get(label);
				for (int j = 0; j < entry.getValue().size(); j++) {
					Datum d = entry.getValue().get(j).getFirst();
					double score = entry.getValue().get(j).getSecond();
					
					if (j < trainingCount && (this.dataScoreThreshold < 0 || score >= this.dataScoreThreshold)) {
						d.setLabel(label); // FIXME This should clone the datum
						if (this.weightData)
							d.setLabelWeight(label, score);
						else 
							d.setLabelWeight(label, 1.0);
						nextTrainData.add(d);
						dataAdded++;
					} else {
						nextUnlabeledData.add(d);
					}
				}
				
			}
		}
		
		this.trainData = new ArrayList<>();
		this.trainData.addAll(nextIterTrainData.values());
		
		this.iterUnlabeledData = new ArrayList<>();
		this.iterUnlabeledData.addAll(nextIterUnlabeledData.values());
		
		this.context.getDataTools().getOutputWriter().debugWriteln("Self training added " + dataAdded + " self-labeled data");
		
		return true;
	}
	
	// Gets counts of each label to add by proportion in the train data
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map getLabelCountsToAdd(DataSet trainData) {
		Map counts = new HashMap();
		Set labels = trainData.getLabels();
		double size = trainData.labeledSize();
		
		for (Object l : labels)
			counts.put(l, (int)Math.floor((trainData.getDataSizeForLabel(l)/size)*this.incrementSize));
		
		return counts;
	}
	
	// Maps labels to (datum, score) list ordered descending
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map getDataOrderingsForLabels(Map<Datum<?>, Pair<?, Double>> dataLabels) {
		Map<Object, List> orderings = new HashMap();
		for (Entry<Datum<?>, Pair<?, Double>> entry : dataLabels.entrySet()) {
			Object label = entry.getValue().getFirst();
			Datum<?> datum = entry.getKey();
			double score = entry.getValue().getSecond();
			
			if (!orderings.containsKey(label))
				orderings.put(label, new ArrayList());
			orderings.get(label).add(new Pair<Datum, Double>(datum, score));
		}
		
		for (Entry<Object, List> entry : orderings.entrySet()) {
			entry.getValue().sort(new Comparator<Pair<Datum, Double>>() {
				@Override
				public int compare(Pair<Datum, Double> o1, Pair<Datum, Double> o2) {
					return Double.compare(o2.getSecond(), o1.getSecond());
				}
			});
		}
		
		return orderings;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean makeTrainDataBatch() {
		Map<Tools<?, ?>, DataSet<?, ?>> typeData = new HashMap<>();
		for (DataSet<?, ?> d : this.initData) {
			if (!typeData.containsKey(d.getDatumTools()))
				typeData.put(d.getDatumTools(), d.cloneUnbuildable());
			else
				typeData.get(d.getDatumTools()).addAll((DataSet)d);
		}
		
		Singleton<Integer> unlabeledAdded = new Singleton<Integer>(0);
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
						unlabeledAdded.set(unlabeledAdded.get() + 1);
					}
					
					return true;
				}
			}), this.context.getMaxThreads());
		}
		
		this.trainData = new ArrayList<>();
		this.trainData.addAll(typeData.values());
		this.context.getDataTools().getOutputWriter().debugWriteln("Self training added " + unlabeledAdded.get() + " self-labeled data");
		
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
