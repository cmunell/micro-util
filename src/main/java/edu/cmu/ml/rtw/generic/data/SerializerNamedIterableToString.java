package edu.cmu.ml.rtw.generic.data;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.NamedIterable;

public class SerializerNamedIterableToString extends Serializer<NamedIterable<?, ?>, String> {
	private List<Serializer.Index<NamedIterable<?, ?>>> indices;
	
	public SerializerNamedIterableToString() {
		this.indices = new ArrayList<Serializer.Index<NamedIterable<?, ?>>>();
		
		this.indices.add(new Serializer.Index<NamedIterable<?, ?>>() {
			@Override
			public String getField() {
				return "name";
			}

			@Override
			public Object getValue(NamedIterable<?, ?> item) {
				return item.getName();
			}
		});
	}
	
	@Override
	public String getName() {
		return "NamedIterableToString";
	}

	@Override
	public String serialize(NamedIterable<?, ?> item) {
		return item.toString();
	}

	@Override
	public NamedIterable<?, ?> deserialize(String object,
			StoreReference storeReference) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String serializeToString(NamedIterable<?, ?> item) {
		return serialize(item);
	}

	@Override
	public NamedIterable<?, ?> deserializeFromString(String str, StoreReference storeReference) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Serializer.Index<NamedIterable<?, ?>>> getIndices() {
		return this.indices;
	}

}
