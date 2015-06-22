package edu.cmu.ml.rtw.generic.model.annotator;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;

/**
 * Annotator represents a method for annotating documents
 * 
 * 
 * @author Bill McDowell
 *
 * @param <T>
 */
public interface Annotator<T> {
	/**
	 * @return the name of the annotator
	 */
	String getName();
	
	/**
	 * @return the type of annotation the annotator
	 * produces
	 */
	AnnotationType<T> produces();
	
	/**
	 * @return the annotation types that the annotator must
	 * have access to (within a given document) to produce
	 * its annotations
	 */
	AnnotationType<?>[] requires();

	/**
	 * @return an indicator of whether the annotator produces
	 * meaningful confidence scores for its annotations.
	 */
	boolean measuresConfidence();
}
