package edu.cmu.ml.rtw.generic.model.annotator.nlp;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.model.annotator.Annotator;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * AnnotatorDocument produces a single annotation per
 * NLP document.
 * 
 * @author Bill McDowell
 *
 * @param <T>
 */
public interface AnnotatorDocument<T> extends Annotator<T> {
	/**
	 * @param document
	 * @return an annotation object for the given document
	 * along with a confidence score.
	 */
	Pair<T, Double> annotate(DocumentNLP document);
}
