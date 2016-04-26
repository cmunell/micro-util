package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;

public interface Trainable<D extends Datum<L>, L> {
	boolean train();
	boolean setTrainData(DataSet<D, L> data);
	boolean setDevData(DataSet<D, L> data);
	boolean iterateTraining(Map<D, L> constrainedData);
	DataSet<D, L> getTrainData();
}
