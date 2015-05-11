package edu.cmu.ml.rtw.generic.util;

import org.json.JSONObject;

public interface JSONSerializable {
	public JSONObject toJSON();
	public boolean fromJSON(JSONObject json);
}
