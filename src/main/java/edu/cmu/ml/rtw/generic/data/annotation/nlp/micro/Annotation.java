package edu.cmu.ml.rtw.generic.data.annotation.nlp.micro;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

/**
 * An {@code Annotation} represents an arbitrary annotation associated
 * with a character span of a document. See the wiki page for more
 * information:
 * 
 * {@link http://rtw.ml.cmu.edu/wiki/index.php/Annotation}
 * 
 * {@code Annotation} is immutable.
 * 
 * @author jayant
 * 
 */
public class Annotation {

  private final int spanStart;
  private final int spanEnd;
  private final String slot;
  private final String annotator;
  private final String documentId;

  private final String stringValue;
  private final JSONObject jsonValue;

  private final Double confidence;
  private final DateTime annotationTime;
  private final String justification;

  public Annotation(int spanStart, int spanEnd, String slot, String annotator, String documentId,
	     JSONObject jsonValue, DateTime annotationTime) {
    this(spanStart, spanEnd, slot, annotator, documentId, jsonValue, null, annotationTime, null);
  }
  
  public Annotation(int spanStart, int spanEnd, String slot, String annotator, String documentId,
	      String stringValue, DateTime annotationTime) {
    this(spanStart, spanEnd, slot, annotator, documentId, stringValue, null, annotationTime, null);
  }
  
  public Annotation(int spanStart, int spanEnd, String slot, String annotator, String documentId,
	      String stringValue, Double confidence, DateTime annotationTime,
	      String justification) {
    this(spanStart, spanEnd, slot, annotator, documentId, stringValue, null, confidence, annotationTime, justification);
  }
  
  public Annotation(int spanStart, int spanEnd, String slot, String annotator, String documentId,
	      JSONObject jsonValue, Double confidence, DateTime annotationTime,
	      String justification) {
    this(spanStart, spanEnd, slot, annotator, documentId, null, jsonValue, confidence, annotationTime, justification);
  }
	  
  public Annotation(int spanStart, int spanEnd, String slot, String annotator, String documentId,
      String stringValue, JSONObject jsonValue, Double confidence, DateTime annotationTime,
      String justification) {
    this.spanStart = spanStart;
    this.spanEnd = spanEnd;
    Preconditions.checkArgument(spanEnd >= spanStart);

    this.slot = Preconditions.checkNotNull(slot);
    this.annotator = Preconditions.checkNotNull(annotator);
    this.documentId = documentId;

    // Exactly one of these must be null.
    this.stringValue = stringValue;
    this.jsonValue = jsonValue;
    Preconditions.checkArgument(stringValue != null ^ jsonValue != null);

    this.confidence = confidence;
    Preconditions.checkArgument(confidence == null || (confidence >= 0.0 && confidence <= 1.0));
    this.annotationTime = annotationTime;
    this.justification = justification;
  }

  /**
   * Produces an annotation from the given JSON object. The JSON
   * object is a dictionary that must include the following fields:
   * 
   * <ul>
   * <li>spanStart - starting character offset (inclusive) of the
   * annotated span
   * <li>spanEnd - ending character offset (exclusive) of the
   * annotated span
   * <li>slot - name of annotation. Each annotator has its own
   * vocabulary of slots (see below).
   * <li>annotator - name of the annotator
   * <li>value - string or JSON object whose format is slot-specific
   * <li>annotationTime - when the annotation was produced. This field
   * is automatically generated.
   * </ul>
   * 
   * Additionally, the following fields are optional:
   * <ul>
   * <li>justification - human-readable reason for annotation
   * <li>confidence - value between 0 and 1
   * </ul>
   * 
   * @param json
   * @return
   */
  public static Annotation fromJson(JSONObject json) {
    try {
      int spanStart = json.getInt("spanStart");
      int spanEnd = json.getInt("spanEnd");
      String slot = json.getString("slot");
      String annotator = json.getString("annotator");
      String documentId = json.optString("documentId", null);

      String stringValue = null;
      JSONObject jsonValue = null;
      Object value = json.get("value");
      if (value instanceof String) {
        stringValue = (String) value;
      } else if (value instanceof JSONObject) {
        jsonValue = (JSONObject) value;
      }

      Double confidence = json.optDouble("confidence");
      String annotationTimeString = json.getString("annotationTime");
      DateTime annotationTime = new DateTime(annotationTimeString);
      String justification = json.optString("justification", null);

      return new Annotation(spanStart, spanEnd, slot, annotator, documentId,
       stringValue, jsonValue, confidence, annotationTime, justification);
    } catch (JSONException e) {
	  return null;
    }
  }

  public static Annotation fromJsonString(String jsonString) {
    try {
      return fromJson(new JSONObject(jsonString));
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * Gets the character index of the start of the span covered by this
   * annotation. This index is inclusive.
   * 
   * @return
   */
  public int getSpanStart() {
    return spanStart;
  }

  /**
   * Gets the character index of the end of the span covered by this
   * annotation. This index is exclusive, i.e., it is the index of the
   * first character that is not covered by this annotation.
   * 
   * @return
   */
  public int getSpanEnd() {
    return spanEnd;
  }

  /**
   * Gets the name of the slot being annotated.
   * 
   * @return
   */
  public String getSlot() {
    return slot;
  }

  /**
   * Gets the name of the annotator that produced this annotation.
   * 
   * @return
   */
  public String getAnnotator() {
    return annotator;
  }

  /**
   * Returns the id of the document that this annotation applies to.
   * The character indexes returned by {@code getSpanStart()} and
   * {@code getSpanEnd()} are indexes within the contents of this
   * document.
   * 
   * <p>
   * If the returned value is {@code null} the annotation applies
   * to some unspecified document (that is presumably known from
   * context).
   * 
   * @return
   */
  public String getDocumentId() {
    return documentId;
  }

  /**
   * Gets the value of this annotation. Returns {@code null} if this
   * annotation does not have a string value.
   * 
   * @return
   */
  public String getStringValue() {
    return stringValue;
  }

  /**
   * Gets the value of this annotation. Returns {@code null} if this
   * annotation does not have a JSON value.
   * 
   * @return
   */
  public JSONObject getJsonValue() {
    return jsonValue;
  }

  /**
   * Gets the value of this annotation as an Object. This method
   * returns the same value as either {@code getJsonValue} or
   * {@code getStringValue}, depending on the type of value of this
   * annotation.
   * 
   * @return
   */
  public Object getValue() {
    if (stringValue != null) {
      return stringValue;
    } else {
      return jsonValue;
    }
  }

  /**
   * Gets a confidence value between 0 and 1 for this annotation. The
   * meaning of this number is annotator-specific.
   * 
   * @return
   */
  public Double getConfidence() {
    return confidence;
  }

  /**
   * Gets the time when this annotation was produced.
   * 
   * @return
   */
  public DateTime getAnnotationTime() {
    return annotationTime;
  }

  /**
   * Gets a string justification for this annotation. May be
   * {@code null}.
   * 
   * @return
   */
  public String getJustification() {
    return justification;
  }

  /**
   * Returns a JSON representation of this entire annotation. The
   * returned representation can be parsed with
   * {@link Annotation#fromJson}.
   * 
   * @return
   */
  public JSONObject toJson() {
    JSONObject result = new JSONObject();
    try {
		result.put("spanStart", spanStart);
	    result.put("spanEnd", spanEnd);
	    result.put("slot", slot);
	    result.put("annotator", annotator);
	
	    if (stringValue != null) {
	      result.put("value", stringValue);
	    } else {
	      result.put("value", jsonValue);
	    }
	    
	    if (documentId != null) {
	      result.put("documentId", documentId);
	    }
	
	    result.put("confidence", confidence);
	    result.put("annotationTime", annotationTime.toString());
	    
	    if (justification != null) {
	      result.put("justification", justification);
	    }
	} catch (JSONException e) {
		return null;
	}

    return result;
  }

  /**
   * Returns a JSON String representation of this entire annotation.
   * The returned representation can be parsed with
   * {@link Annotation#fromJsonString}.
   * 
   * @return
   */
  public String toJsonString() {
    return toJson().toString();
  }
}
