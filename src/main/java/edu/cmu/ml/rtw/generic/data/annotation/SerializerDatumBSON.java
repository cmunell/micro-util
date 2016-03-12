package edu.cmu.ml.rtw.generic.data.annotation;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.JSONUtil;
import org.bson.Document;

public class SerializerDatumBSON<D extends Datum<L>, L> extends Serializer<D, Document> {
	public static final String ID_INDEX_FIELD = "id";
	private List<Serializer.Index<D>> indices;
	private Datum.Tools<D, L> datumTools;
	
	public SerializerDatumBSON(Tools<D, L> datumTools) {
		this.indices = new ArrayList<Serializer.Index<D>>();
		
		this.indices.add(new Serializer.Index<D>() {
			@Override
			public String getField() {
				return ID_INDEX_FIELD;
			}

			@Override
			public Object getValue(D item) {
				return String.valueOf(item.getId());
			}
		});
		
		this.datumTools = datumTools;
	}
	
	@Override
	public String getName() {
		return "DatumBSON";
	}

	@Override
	public Document serialize(D item) {
		return JSONUtil.convertJSONToBSON(this.datumTools.datumToJSON(item));
	}

	@Override
	public D deserialize(Document object, StoreReference storeReference) {
		return this.datumTools.datumFromJSON(JSONUtil.convertBSONToJSON(object));
	}

	@Override
	public String serializeToString(D item) {
		return serialize(item).toJson();
	}

	@Override
	public D deserializeFromString(String str,
			StoreReference storeReference) {
		return deserialize(Document.parse(str));
	}

	@Override
	public List<Serializer.Index<D>> getIndices() {
		return this.indices;
	}
}
