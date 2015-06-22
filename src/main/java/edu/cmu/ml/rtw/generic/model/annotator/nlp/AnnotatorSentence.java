package edu.cmu.ml.rtw.generic.model.annotator.nlp;

import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.model.annotator.Annotator;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * AnnotatorSentence produces a single annotation per 
 * sentence within an NLP document.
 * 
 * @author Bill McDowell
 *
 * @param <T>
 */
public interface AnnotatorSentence<T> extends Annotator<T> {
	/**
	 * @param document
	 * @return a mapping from sentence index to annotation
	 * and confidence
	 */
	Map<Integer, Pair<T, Double>> annotate(DocumentNLP document);
}
