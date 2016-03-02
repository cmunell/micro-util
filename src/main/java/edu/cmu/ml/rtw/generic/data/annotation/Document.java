package edu.cmu.ml.rtw.generic.data.annotation;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;

/**
 * Document represents a bunch of text/data
 * that typically comes from a file on disk, and can be annotated
 * with annotations that are defined through instances of the
 * edu.cmu.ml.rtw.generic.data.annotation.AnnotationType
 * class.
 * 
 * @author Bill McDowell
 *
 */
public abstract class Document {
	protected DataTools dataTools;
	protected String name;
	protected StoreReference storeReference;
	
	public Document(DataTools dataTools) {
		this.dataTools = dataTools;
	}
	
	public Document(DataTools dataTools, String name) {
		this.dataTools = dataTools;
		this.name = name;
	}
	
	public Document(DataTools dataTools, String name, String storageName, String collectionName) {
		this.dataTools = dataTools;
		this.name = name;
		this.storeReference = new StoreReference(storageName, collectionName, SerializerDocument.NAME_INDEX_FIELD, collectionName);
	}
	
	public StoreReference getStoreReference() {
		return this.storeReference;
	}
	
	public String getName() {
		return this.name;
	}
	
	public boolean meetsAnnotatorRequirements(AnnotationType<?>[] requirements) {
		for (AnnotationType<?> type : requirements)
			if (!hasAnnotationType(type))
				return false;
		return true;
	}
	
	public DataTools getDataTools() {
		return this.dataTools;
	}
	
	public abstract String getAnnotatorName(AnnotationType<?> annotationType);
	public abstract boolean hasAnnotationType(AnnotationType<?> annotationType);
	public abstract boolean hasConfidence(AnnotationType<?> annotationType);
	public abstract Collection<AnnotationType<?>> getAnnotationTypes();
}
