/**
 * Copyright 2014 Bill McDowell 
 *
 * This file is part of ARKWater (https://github.com/forkunited/ARKWater)
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

package edu.cmu.ml.rtw.generic.util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * CounterTable represents a histogram.  It allows incrementing 
 * and decrementing counts for each item, and transforming the histogram
 * into various data-structures.
 * 
 * @author Lingpeng Kong, Bill McDowell
 * 
 */
public class CounterTable<T>{
	public HashMap<T, Integer> counts;
	
	public CounterTable(){
		this.counts= new HashMap<T,Integer>();
	}
	
	public synchronized void incrementCount(T w){
		if(this.counts.containsKey(w)){
			this.counts.put(w, this.counts.get(w) + 1);
		} else {
			this.counts.put(w, 1);
		}
	}
	
	public void removeCountsLessThan(int minCount) {
		List<T> valuesToRemove = new ArrayList<T>();
		for (Entry<T, Integer> entry : this.counts.entrySet()) {
			if (entry.getValue() < minCount)
				valuesToRemove.add(entry.getKey());
		}
		
		for (T valueToRemove : valuesToRemove)
			this.counts.remove(valueToRemove);
	}
	
	public Map<T, Integer> buildIndex() {
		HashMap<T, Integer> index = new HashMap<T, Integer>(this.counts.size());
		int i = 0;
		
		for (Entry<T, Integer> entry : this.counts.entrySet()) {
			index.put(entry.getKey(), i);
			i++;
		}
		
		return index;
	}
	
	public TreeMap<Integer, List<T>> getSortedCounts() {
		TreeMap<Integer, List<T>> sortedCounts = new TreeMap<Integer, List<T>>();
		
		for (Entry<T, Integer> entry : this.counts.entrySet()) {
			if (!sortedCounts.containsKey(entry.getValue()))
				sortedCounts.put(entry.getValue(), new ArrayList<T>());
			
			sortedCounts.get(entry.getValue()).add(entry.getKey());
		}
		
		return sortedCounts;
	}
	
	public Map<T, Integer> getCounts() {
		return this.counts;
	}
	
	public int getSize() {
		return this.counts.size();
	}
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		TreeMap<Integer, List<T>> sortedCounts = getSortedCounts();
		
		try {
			for (Entry<Integer, List<T>> entry : sortedCounts.entrySet()) {
				for (T item : entry.getValue()) {
					json.put(item.toString(), entry.getKey());
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return json;
	}
	
	@SuppressWarnings("unchecked")
	public boolean fromJSON(JSONObject json) {
		this.counts = new HashMap<T, Integer>(); 
		
		JSONArray keys = json.names();
		try {
			for (int i = 0; i < keys.length(); i++) {
				this.counts.put((T)(keys.getString(i)), json.getInt(keys.getString(i)));
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
		
	}
}