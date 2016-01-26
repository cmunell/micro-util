package edu.cmu.ml.rtw.generic.data.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Serializer;

public abstract class SerializerDocument<D extends Document, S> extends Serializer<D, S> {	
	protected List<Index<D>> indices;
	protected Collection<AnnotationType<?>> annotationTypes;
	protected D genericDocument;
	
	public SerializerDocument(Collection<AnnotationType<?>> annotationTypes) {
		this(null, annotationTypes);
	}
	
	public SerializerDocument(D genericDocument) {
		this(genericDocument, null);
	}
	
	public SerializerDocument(D genericDocument, Collection<AnnotationType<?>> annotationTypes) {
		this.annotationTypes = annotationTypes;
		this.genericDocument = genericDocument;
		this.indices = new ArrayList<Serializer.Index<D>>();
		this.indices.add(new Serializer.Index<D>() {
			@Override
			public String getField() {
				return "name";
			}

			@Override
			public Object getValue(D item) {
				return item.getName();
			}
		});
	}
	
	@Override
	public List<Index<D>> getIndices() {
		return this.indices;
	}
	
	public abstract SerializerDocument<D, S> makeInstance(D genericDocument, Collection<AnnotationType<?>> annotationTypes);
}
