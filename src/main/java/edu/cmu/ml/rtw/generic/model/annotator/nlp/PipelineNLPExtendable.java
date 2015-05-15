package edu.cmu.ml.rtw.generic.model.annotator.nlp;

import edu.cmu.ml.rtw.generic.model.annotator.Annotator;

public class PipelineNLPExtendable extends PipelineNLP {
	public PipelineNLPExtendable() {
		super();
	}
	
	public boolean extend(Annotator<?> annotator) {
		if (this.annotators.containsKey(annotator.produces()))
			return false;
		addAnnotator(annotator.produces(), annotator);
		return true;
	}
}
