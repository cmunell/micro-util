package edu.cmu.ml.rtw.generic.opt.search;

import edu.cmu.ml.rtw.generic.parse.Parameterizable;

public interface ParameterSearchable extends Parameterizable {
	int getStageCount();
	boolean runStage(int stageIndex);
	double evaluate();
	boolean lastStageRequiresCloning();
	ParameterSearchable clone();
}
