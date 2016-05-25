package edu.cmu.ml.rtw.generic.data.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.platanios.learn.math.matrix.Vector;
import org.platanios.learn.math.matrix.Vector.VectorElement;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;

public class SerializerDataFeatureMatrixBSONString extends Serializer<DataFeatureMatrix<?, ?>, String> {
	private List<Serializer.Index<DataFeatureMatrix<?, ?>>> indices;
	
	public SerializerDataFeatureMatrixBSONString(DataTools dataTools) {
		this.indices = new ArrayList<Serializer.Index<DataFeatureMatrix<?, ?>>>();
		
		this.indices.add(new Serializer.Index<DataFeatureMatrix<?, ?>>() {
			@Override
			public String getField() {
				return "name";
			}

			@Override
			public Object getValue(DataFeatureMatrix<?, ?> item) {
				return item.getReferenceName();
			}
		});
	}

	@Override
	public String getName() {
		return "DataFeatureMatrixBSONString";
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public String serialize(DataFeatureMatrix item) {
		StringBuilder str = new StringBuilder();
		
		for (Object o : item.getData()) {
			Datum d = (Datum)o;
			Vector v = item.getFeatureVocabularyValues(d, false);
			List<Integer> indices = new ArrayList<>();
			List<Double> values = new ArrayList<>();
			for (VectorElement e : v) {
				indices.add(e.index());
				values.add(e.value());
			}
			
			Map names = item.getFeatures().getFeatureVocabularyNamesForIndices(indices);
			
			Document bsonVector = new Document();
			for (int i = 0; i < indices.size(); i++) {
				int index = indices.get(i);
				String name = (String)names.get(index);
				double value = values.get(i);
				bsonVector.append(name, value);
			}
		
			String labelStr = (d.getLabel() == null) ? "null" : d.getLabel().toString();
			str.append(d.getId()).append("\t").append(labelStr).append("\t").append(bsonVector.toJson()).append("\n");
		}
		
		return str.toString();
	}

	@Override
	public DataFeatureMatrix<?, ?> deserialize(String object, StoreReference storeReference) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String serializeToString(DataFeatureMatrix<?, ?>  item) {
		return serialize(item);
	}

	@Override
	public DataFeatureMatrix<?, ?>  deserializeFromString(String str, StoreReference storeReference) {
		return deserialize(str, storeReference);
	}

	@Override
	public List<Serializer.Index<DataFeatureMatrix<?, ?>>> getIndices() {
		return this.indices;
	}

}
