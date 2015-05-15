package edu.cmu.ml.rtw.generic.data.annotation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.JSONSerializable;

public abstract class Document implements JSONSerializable {
	protected DataTools dataTools;
	protected String name;
	
	public Document(DataTools dataTools) {
		this.dataTools = dataTools;
	}
	
	public Document(DataTools dataTools, JSONObject json) {
		this(dataTools);
		fromJSON(json);
	}
	
	public Document(DataTools dataTools, String jsonPath) {
		this(dataTools);
		BufferedReader r = FileUtil.getFileReader(jsonPath);
		String line = null;
		StringBuffer lines = new StringBuffer();
		try {
			while ((line = r.readLine()) != null) {
				lines.append(line).append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			fromJSON(new JSONObject(lines.toString()));
		} catch (JSONException e) {
			
		}
	}
	
	public String getName() {
		return this.name;
	}
	
	public boolean saveToJSONFile(String path) {
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(path));
			
			w.write(toJSON().toString());
			
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean meetsAnnotatorRequirements(AnnotationType<?>[] requirements) {
		for (AnnotationType<?> type : requirements)
			if (!hasAnnotationType(type))
				return false;
		return true;
	}
	
	public abstract Document makeInstanceFromJSONFile(String path);
	public abstract String getAnnotatorName(AnnotationType<?> annotationType);
	public abstract boolean hasAnnotationType(AnnotationType<?> annotationType);
	public abstract boolean hasConfidence(AnnotationType<?> annotationType);
	public abstract Collection<AnnotationType<?>> getAnnotationTypes();
}
