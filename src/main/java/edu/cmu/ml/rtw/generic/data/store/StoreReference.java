package edu.cmu.ml.rtw.generic.data.store;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.util.JSONSerializable;

public class StoreReference implements JSONSerializable {
	private String storageName;
	private String collectionName;
	private List<String> indexFields;
	private List<Object> indexValues;

	public StoreReference() {
		
	}
	
	public StoreReference(String storageName, String collectionName, List<String> indexFields, List<Object> indexValues) {
		this.storageName = storageName;
		this.collectionName = collectionName;
		this.indexFields = indexFields;
		this.indexValues = indexValues;
	}
	
	public StoreReference(String storageName, String collectionName, String indexField, Object indexValue) {
		this.storageName = storageName;
		this.collectionName = collectionName;
		this.indexFields = new ArrayList<String>();
		this.indexFields.add(indexField);
		this.indexValues = new ArrayList<Object>();
		this.indexValues.add(indexValue);
	}
	
	public String getStorageName() {
		return this.storageName;
	}
	
	public String getCollectionName() {
		return this.collectionName;
	}

	public List<String> getIndexFields() {
		return this.indexFields;
	}
	
	public List<Object> getIndexValues() {
		return this.indexValues;
	}
	
	public Object getIndexValue(int index) {
		return this.indexValues.get(index);
	}
	
	@Override
	public boolean fromJSON(JSONObject json) {
		try {
			this.storageName = json.getString("s");
			this.collectionName = json.getString("n");
			
			JSONArray indexFields = json.getJSONArray("f");
			this.indexFields = new ArrayList<String>();
			for (int i = 0; i < indexFields.length(); i++)
				this.indexFields.add(indexFields.getString(i));
			
			JSONArray indexValues = json.getJSONArray("v");
			this.indexValues = new ArrayList<Object>();
			for (int i = 0; i < indexValues.length(); i++)
				this.indexValues.add(indexValues.get(i));
			
		} catch (JSONException e) {
			return false;
		}

		return true;
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		try {
			json.put("s", this.storageName);
			json.put("n", this.collectionName);
			json.put("f", new JSONArray(this.indexFields));
			json.put("v", new JSONArray(this.indexValues));
		} catch (JSONException e) {
			return null;
		}
		
		return json;
	}
	
	public static StoreReference makeFromJSON(JSONObject json) {
		StoreReference ref = new StoreReference();
		if (!ref.fromJSON(json))
			return null;
		else
			return ref;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T resolve(DataTools dataTools, boolean keepInMemory) {
		return (T)dataTools.getStoredItemSetManager().resolveStoreReference(this, keepInMemory);
	}
}
