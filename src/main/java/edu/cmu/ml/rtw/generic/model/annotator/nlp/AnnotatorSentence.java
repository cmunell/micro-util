package edu.cmu.ml.rtw.generic.model.annotator.nlp;

import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.model.annotator.Annotator;
import edu.cmu.ml.rtw.generic.util.Pair;

public interface AnnotatorSentence<T> extends Annotator<T> {
	Map<Integer, Pair<T, Double>> annotate(DocumentNLP document);
}
