package edu.cmu.ml.rtw.generic.data;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.store.StoreReference;

public class SerializerGazetteerString extends Serializer<Gazetteer, String> {
	public static final String NAME_INDEX_FIELD = "name";
	private List<Serializer.Index<Gazetteer>> indices;
	
	public SerializerGazetteerString() {
		this.indices = new ArrayList<Serializer.Index<Gazetteer>>();
		
		this.indices.add(new Serializer.Index<Gazetteer>() {
			@Override
			public String getField() {
				return NAME_INDEX_FIELD;
			}

			@Override
			public Object getValue(Gazetteer item) {
				return item.getName();
			}
		});
	}
	
	@Override
	public String getName() {
		return "GazetteerString";
	}

	@Override
	public String serialize(Gazetteer item) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Gazetteer deserialize(String object, StoreReference storeReference) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String serializeToString(Gazetteer item) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Gazetteer deserializeFromString(String str,
			StoreReference storeReference) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Serializer.Index<Gazetteer>> getIndices() {
		return this.indices;
	}

}
