package edu.cmu.ml.rtw.generic.model.annotator;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;

public interface Annotator<T> {
	String getName();
	AnnotationType<T> produces();
	AnnotationType<?>[] requires();
	boolean measuresConfidence();
}
