package edu.cmu.ml.rtw.generic.model.annotator.nlp;

import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.model.annotator.Annotator;
import edu.cmu.ml.rtw.generic.util.Triple;

public interface AnnotatorTokenSpan<T> extends Annotator<T> {
	List<Triple<TokenSpan, T, Double>> annotate(DocumentNLP document);
}
