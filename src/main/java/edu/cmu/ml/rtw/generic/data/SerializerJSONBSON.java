package edu.cmu.ml.rtw.generic.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bson.Document;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.JSONUtil;
import edu.cmu.ml.rtw.generic.util.StoredJSONSerializable;

public class SerializerJSONBSON<E extends StoredJSONSerializable> extends Serializer<StoredJSONSerializable, Document> {
	public static final String ID_INDEX_FIELD = "id";
	
	protected List<Index<StoredJSONSerializable>> indices;
	protected Collection<AnnotationType<?>> annotationTypes;
	protected StoredJSONSerializable genericObj;
	
	public SerializerJSONBSON(StoredJSONSerializable genericObj) {
		this.genericObj = genericObj;
		this.indices = new ArrayList<Serializer.Index<StoredJSONSerializable>>();
		this.indices.add(new Serializer.Index<StoredJSONSerializable>() {
			@Override
			public String getField() {
				return ID_INDEX_FIELD;
			}

			@Override
			public Object getValue(StoredJSONSerializable item) {
				return item.getId();
			}
		});
	}
	
	@Override
	public List<Index<StoredJSONSerializable>> getIndices() {
		return this.indices;
	}

	@Override
	public String getName() {
		return "JSONBSON";
	}

	@Override
	public Document serialize(StoredJSONSerializable item) {
		return JSONUtil.convertJSONToBSON(item.toJSON());
	}

	@Override
	public StoredJSONSerializable deserialize(Document object,
			StoreReference storeReference) {
		StoredJSONSerializable obj = this.genericObj.makeInstance(storeReference);
		if (!obj.fromJSON(JSONUtil.convertBSONToJSON(object)))
			return null;
		else
			return obj;
	}

	@Override
	public String serializeToString(StoredJSONSerializable item) {
		return serialize(item).toJson();
	}

	@Override
	public StoredJSONSerializable deserializeFromString(String str,
			StoreReference storeReference) {
		return deserialize(Document.parse(str));
	}
}
