package edu.cmu.ml.rtw.generic.task.classify.multi;

import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;

public interface MultiTrainable {
	boolean train();
	boolean setTrainData(List<DataSet<?, ?>> data);
	List<DataSet<?, ?>> getTrainData();
}
