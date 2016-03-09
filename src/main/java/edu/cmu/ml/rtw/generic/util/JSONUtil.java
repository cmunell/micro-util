package edu.cmu.ml.rtw.generic.util;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtil {
	public static JSONObject convertBSONToJSON(Document bson) {
		try {
			return new JSONObject(bson.toJson());
		} catch (JSONException e) {
			throw new UnsupportedOperationException("Failed to convert BSON to JSON object during BSON deserialization");
		}
	}
	
	public static Document convertJSONToBSON(JSONObject json) {
		String[] names = JSONObject.getNames(json);
		Document document = new Document();
		try {
			for (String name : names) {
				if (json.optJSONObject(name) != null) {
					document.append(name, convertJSONToBSON(json.getJSONObject(name)));
				} else if (json.optJSONArray(name) != null) {
					document.append(name, convertJSONArrayToBSONList(json.getJSONArray(name)));
				} else {
					document.append(name, json.get(name));
				}
			}
		} catch (JSONException e) {
			return null;
		}
			
		return document;
	}
	
	private static List<Object> convertJSONArrayToBSONList(JSONArray json) throws JSONException {
		List<Object> bsonList = new ArrayList<Object>();
		for (int i = 0; i < json.length(); i++) {
			if (json.optJSONObject(i) != null) {
				bsonList.add(convertJSONToBSON(json.getJSONObject(i)));
			} else if (json.optJSONArray(i) != null) {
				bsonList.add(convertJSONArrayToBSONList(json.getJSONArray(i)));
			} else {
				bsonList.add(json.get(i));
			}
		}
		return bsonList;
	}
}
