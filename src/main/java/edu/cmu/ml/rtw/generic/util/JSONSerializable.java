package edu.cmu.ml.rtw.generic.util;

import org.json.JSONObject;

/**
 * JSONSerializable represents an object that 
 * can be converted to or from JSON.
 * 
 * @author Bill McDowell
 *
 */
public interface JSONSerializable {
	public JSONObject toJSON();
	public boolean fromJSON(JSONObject json);
}
