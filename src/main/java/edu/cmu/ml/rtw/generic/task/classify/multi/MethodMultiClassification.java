package edu.cmu.ml.rtw.generic.task.classify.multi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;

public abstract class MethodMultiClassification extends CtxParsableFunction {
	protected Context context;
	
	public MethodMultiClassification() {
		this(null);
	}
	
	public MethodMultiClassification(Context context) {
		this.context = context;
	}
	
	@Override
	public boolean equals(Object o) {
		return this.referenceName.equals(((MethodMultiClassification)o).getReferenceName());
	}
	
	@Override
	public int hashCode() {
		return this.referenceName.hashCode();
	}
	
	public List<Map<Datum<?>, ?>> classify(TaskMultiClassification task) {
		List<DataSet<?, ?>> data = new ArrayList<DataSet<?, ?>>();
		for (int i = 0; i < task.getTaskCount(); i++) {
			data.add(task.getTask(i).getData());
		}
		
		return classify(data);
	}
	
	
	public boolean init() {
		return init(null);
	}
	
	public abstract List<Map<Datum<?>, ?>> classify(List<DataSet<?, ?>> data);
	public abstract boolean init(List<DataSet<?, ?>> testData);
	public abstract MethodMultiClassification clone();
	public abstract MethodMultiClassification makeInstance(Context context);
	public abstract boolean hasTrainable();
	public abstract MultiTrainable getTrainable();
}
