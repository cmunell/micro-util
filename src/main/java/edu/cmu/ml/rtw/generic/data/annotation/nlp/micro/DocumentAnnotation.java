package edu.cmu.ml.rtw.generic.data.annotation.nlp.micro;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import edu.cmu.ml.rtw.generic.util.FileUtil;

/**
 * A collection of annotations for a document. All of the annotations
 * within an instance of this class have the same value for their
 * {@code getDocumentId()} method.
 * 
 * <p>
 * {@code DocumentAnnotation} is immutable.
 * 
 * @author jayant
 * 
 */
public class DocumentAnnotation {

  private final String documentId;
  private final List<Annotation> annotations;

  public DocumentAnnotation(String documentId, List<Annotation> annotations) {
    this.documentId = documentId;
    this.annotations = ImmutableList.copyOf(annotations);
  }

  /**
   * Reads in the document annotations stored in {@code filename}.
   * This file is in the one-JSON-object-per-line format described on
   * the wiki:
   * 
   * {@link http://rtw.ml.cmu.edu/wiki/index.php/Annotation}
   * 
   * A single file may contain annotations for multiple documents. Each
   * document's annotations are returned aggregated into a single
   * DocumentAnnotation. Annotations with no document id are assigned
   * the id {@code null} and aggregated in the same fashion. 
   * 
   * @param filename
   * @return
   */
  public static List<DocumentAnnotation> fromFile(String filename) {
    Multimap<String, Annotation> documentAnnotations = HashMultimap.create();
    BufferedReader reader = FileUtil.getFileReader(filename);
    String line = null;
    try {
	  while ((line = reader.readLine()) != null) {
        Annotation a = Annotation.fromJsonString(line);
	    documentAnnotations.put(a.getDocumentId(), a);
	  }
	} catch (IOException e) {
      return null;
	}
    
    List<DocumentAnnotation> docAnnotations = Lists.newArrayList();
    for (String key : documentAnnotations.keySet()) {
      DocumentAnnotation docAnnotation = new DocumentAnnotation(key,
          Lists.newArrayList(documentAnnotations.get(key)));
      docAnnotations.add(docAnnotation);
    }
    return docAnnotations;
  }
  
  /**
   * Gets the id of the annotated document.
   */
  public String getDocumentId() {
    return documentId;
  }

  /**
   * Gets all of the annotations for this document.
   * 
   * @return
   */
  public List<Annotation> getAllAnnotations() {
    return annotations;
  }

  /**
   * Gets all annotations of a particular slot in this document.
   * 
   * @param slot
   * @return
   */
  public List<Annotation> getAnnotationsWithSlot(String slot) {
    List<Annotation> returnValue = Lists.newArrayList();
    for (Annotation annotation : annotations) {
      if (annotation.getSlot().equals(slot)) {
        returnValue.add(annotation);
      }
    }
    return returnValue;
  }

  /**
   * Gets all of the annotations for the given span, i.e., the
   * annotations for which {@code annotation.spanStart == spanStart}
   * and {@code annotation.spanEnd == spanEnd}.
   * 
   */
  public List<Annotation> getAnnotationsForSpan(int spanStart, int spanEnd) {
    List<Annotation> result = Lists.newArrayList();
    for (Annotation a : annotations) {
      if (a.getSpanStart() == spanStart && a.getSpanEnd() == spanEnd) {
        result.add(a);
      }
    }
    return result;
  }

  /**
   * Returns a new {@code DocumentAnnotation} containing all of the
   * annotations in both {@code this} and {@code other}.
   * 
   * @param other
   * @return
   */
  public DocumentAnnotation merge(DocumentAnnotation other) {
    Preconditions.checkArgument(other.documentId == this.documentId);
    
    List<Annotation> newAnnotations = Lists.newArrayList();
    newAnnotations.addAll(annotations);
    newAnnotations.addAll(other.getAllAnnotations());

    return new DocumentAnnotation(documentId, newAnnotations);
  }

  public void writeToFile(String filename) {
    PrintWriter writer = null;
    try {
      writer = new PrintWriter(filename, "UTF-8");
      for (Annotation a : annotations) {
        writer.println(a.toJsonString());
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } finally {
      if (writer != null) {
        writer.close();
      }
    }
  }
}
