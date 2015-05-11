/**
 * Copyright 2014 Bill McDowell 
 *
 * This file is part of theMess (https://github.com/forkunited/theMess)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 */

package edu.cmu.ml.rtw.generic.data;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * Gazetteer represents a deserialized dictionary of strings
 * mapped to IDs.  The file in which the gazetteer is stored
 * should contain lines of the form:
 * 
 * [ID]	[string_1]:[weight_1]	[string_2]:[weight_2]	...	[string_n]:[weight_n]
 * 
 * Each ID should only occur on a single line, but a string
 * can occur across multiple lines, to be mapped to multiple 
 * IDs.  The strings are cleaned by a specified clean function
 * as they are loaded into memory.  
 * 
 * The weights typically should indicate some kind of confidence
 * that each string should be assigned to the given ID, but
 * these are optional.
 * 
 * @authors Lingpeng Kong, Bill McDowell
 *
 */
public class Gazetteer {
	private HashMap<String, List<Pair<String, Double>>> gazetteer;
	private String name;
	private DataTools.StringTransform cleanFn;
	
	public Gazetteer(String name, String sourceFilePath) {
		this(name, sourceFilePath, null);
	}
	
	public Gazetteer(String name, String sourceFilePath, DataTools.StringTransform cleanFn) {
		this(name, sourceFilePath, cleanFn, false);
	}
	
	public Gazetteer(String name, String sourceFilePath, DataTools.StringTransform cleanFn, boolean hasWeights) {
		this.cleanFn = cleanFn;
		this.gazetteer = new HashMap<String, List<Pair<String, Double>>>();
		this.name = name;
		
		try {
			BufferedReader br = FileUtil.getFileReader(sourceFilePath);
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] lineValues = line.trim().split("\\t");
				if (lineValues.length < 2) {
					continue;
				}
				
				String id = lineValues[0];
				for (int i = 1; i < lineValues.length; i++) {
					String value = lineValues[i];
					double weight = 1.0;
					
					if (hasWeights) {
						String[] valueParts = lineValues[i].split(":");
						StringBuilder valueStr = new StringBuilder();
						for (int j = 0; j < valueParts.length - 1; j++)
							valueStr.append(valueParts[j]).append(":");
						if (valueStr.length() > 0)
							valueStr.delete(valueStr.length() - 1, valueStr.length());
						value = valueStr.toString();
						weight = Double.valueOf(valueParts[valueParts.length - 1]);
					}
					
					String cleanValue = cleanString(value);
					if (cleanValue.length() == 0)
						continue;
					if (!this.gazetteer.containsKey(cleanValue))
						this.gazetteer.put(cleanValue, new ArrayList<Pair<String, Double>>(2));
					List<Pair<String, Double>> ids = this.gazetteer.get(cleanValue);
					boolean idExists = false;
					for (Pair<String, Double> existingId : ids) {
						if (existingId.getFirst().equals(id)) {
							idExists = true;
							break;
						}
					}
					
					if (!idExists)
						this.gazetteer.get(cleanValue).add(new Pair<String, Double>(id, weight));
				}
			}
			
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getName() {
		return this.name;
	}
	
	private String cleanString(String str) {	
		if (this.cleanFn == null)
			return str;
		else
			return this.cleanFn.transform(str);
	}
	
	public boolean contains(String str) {
		return this.gazetteer.containsKey(cleanString(str));
	}
	
	public List<String> getIds(String str) {
		String cleanStr = cleanString(str);
		if (this.gazetteer.containsKey(cleanStr)) {
			List<Pair<String, Double>> weightedIds = this.gazetteer.get(cleanStr);
			List<String> ids = new ArrayList<String>(weightedIds.size());
			
			for (Pair<String, Double> weightedId : weightedIds)
				ids.add(weightedId.getFirst());
			
			return ids;
		} else
			return null;
	}
	
	public List<Pair<String, Double>> getWeightedIds(String str) {
		String cleanStr = cleanString(str);
		if (this.gazetteer.containsKey(cleanStr)) {
			return this.gazetteer.get(cleanStr);
		} else
			return null;
	}
	
	public Pair<List<Pair<String,Double>>, Double> min(String str, DataTools.StringPairMeasure fn) {
		double min = Double.POSITIVE_INFINITY;
		List<Pair<String, Double>> minIds = null;
		String cleanStr = cleanString(str);
		for (Entry<String, List<Pair<String, Double>>> entry : this.gazetteer.entrySet()) {
			double curMin = fn.compute(cleanStr, entry.getKey());
			if (curMin < min) {
				min = curMin;
				minIds = entry.getValue();
			}
		}
		return new Pair<List<Pair<String,Double>>, Double>(minIds, min);
	}
	
	public Pair<List<Pair<String,Double>>, Double> max(String str, DataTools.StringPairMeasure fn) {
		double max = Double.NEGATIVE_INFINITY;
		List<Pair<String, Double>> maxIds = null;
		String cleanStr = cleanString(str);
		for (Entry<String, List<Pair<String, Double>>> entry : this.gazetteer.entrySet()) {
			double curMax = fn.compute(cleanStr, entry.getKey());
			if (curMax > max) {
				max = curMax;
				maxIds = entry.getValue();
			}
		}
		return new Pair<List<Pair<String,Double>>, Double>(maxIds, max);
	}
	
	/**
	 * 
	 * @param str
	 * @return str without tokens contained in the gazetteer.
	 */
	public String removeTerms(String str) {
		String[] strTokens = str.split("\\s+");
		StringBuilder termsRemoved = new StringBuilder();
		for (int i = 0; i < strTokens.length; i++) {
			if (!contains(strTokens[i]))
				termsRemoved.append(strTokens[i]).append(" ");
		}
		
		return termsRemoved.toString().trim();
	}
	
	public Set<String> getValues() {
		return this.gazetteer.keySet();
	}
}