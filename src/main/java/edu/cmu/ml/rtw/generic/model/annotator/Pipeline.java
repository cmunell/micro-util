package edu.cmu.ml.rtw.generic.model.annotator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.Document;

public abstract class Pipeline {
	protected Map<AnnotationType<?>, Annotator<?>> annotators;
	protected List<AnnotationType<?>> annotationOrder;
	
	public Pipeline() {
		this.annotators = new HashMap<AnnotationType<?>, Annotator<?>>();
		this.annotationOrder = new ArrayList<AnnotationType<?>>();
	}
	
	public String getAnnotatorName(AnnotationType<?> annotationType) {
		if (this.annotators.containsKey(annotationType))
			return this.annotators.get(annotationType).getName();
		else 
			return null;
	}
	
	public boolean hasAnnotator(AnnotationType<?> annotationType) {
		return this.annotators.containsKey(annotationType);
	}
	
	public boolean annotatorMeasuresConfidence(AnnotationType<?> annotationType) {
		return this.annotators.get(annotationType).measuresConfidence();
	}
	
	public boolean meetsAnnotatorRequirements(AnnotationType<?> annotationType, Document document) {
		if (!hasAnnotator(annotationType))
			return false;
		return document.meetsAnnotatorRequirements(this.annotators.get(annotationType).requires());
	}
	
	public int annotatorCount() {
		return this.annotationOrder.size();
	}
	
	public AnnotationType<?> getAnnotationType(int index) {
		return this.annotationOrder.get(index);
	}
	
	protected void addAnnotator(AnnotationType<?> annotationType, Annotator<?> annotator) {
		this.annotators.put(annotationType, annotator);
		this.annotationOrder.add(annotationType);
	}
	
	protected void clearAnnotators() {
		this.annotators.clear();
		this.annotationOrder.clear();
	}
}
