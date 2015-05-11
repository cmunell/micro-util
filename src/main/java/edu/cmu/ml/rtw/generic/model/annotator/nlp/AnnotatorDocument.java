package edu.cmu.ml.rtw.generic.model.annotator.nlp;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.model.annotator.Annotator;
import edu.cmu.ml.rtw.generic.util.Pair;

public interface AnnotatorDocument<T> extends Annotator<T> {
	Pair<T, Double> annotate(DocumentNLP document);
}
