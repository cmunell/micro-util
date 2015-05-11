package edu.cmu.ml.rtw.generic.data.annotation.nlp.micro;

import java.util.List;

import org.joda.time.DateTime;
import org.json.JSONObject;

import com.google.common.collect.Lists;

/**
 * An editable collection of annotations for a document.
 * 
 * @author jayant
 *
 */
public class EditableDocumentAnnotation {

  private final String documentId;
  private final List<Annotation> annotations;
  
  public EditableDocumentAnnotation(String documentId, List<Annotation> annotations) {
    this.documentId = documentId;
    this.annotations = Lists.newArrayList(annotations);
  }

  public void addAnnotation(Annotation annotation) {
    annotations.add(annotation);
  }

  public void addAnnotation(int spanStart, int spanEnd, String slot, String annotator,
      String value, double confidence, DateTime annotationTime, String justification) {
    annotations.add(new Annotation(spanStart, spanEnd, slot, annotator, documentId, value, null,
        confidence, annotationTime, justification));
  }

  public void addAnnotation(int spanStart, int spanEnd, String slot, String annotator,
      JSONObject value, double confidence, DateTime annotationTime, String justification) {
    annotations.add(new Annotation(spanStart, spanEnd, slot, annotator, documentId, null, value, 
        confidence, annotationTime, justification));
  }

  public DocumentAnnotation toDocumentAnnotation() {
    return new DocumentAnnotation(documentId, annotations);
  }
}
