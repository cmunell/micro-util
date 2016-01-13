package edu.cmu.ml.rtw.generic.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * WeightedStringList represents a (possibly weighted)
 * list of strings
 * 
 * @author Bill McDowell
 *
 */
public class WeightedStringList implements StringSerializable {
	private Map<String, Double> list;
	
	public WeightedStringList() {
		this.list = new TreeMap<String, Double>();
	}
	
	public WeightedStringList(Map<String, Double> weightMap) {
		this.list = new TreeMap<String, Double>();
		this.list.putAll(weightMap);
	}
	
	public WeightedStringList(List<Pair<String, Double>> weightedList) {
		this.list = new TreeMap<String, Double>();
		for (Pair<String, Double> weightedStr : weightedList)
			this.list.put(weightedStr.getFirst(), weightedStr.getSecond());
	}
	
	public WeightedStringList(String[] list, double[] weights, int startIndex) {
		this.list = new TreeMap<String, Double>();
		
		for (int i = startIndex; i < list.length; i++) {
			this.list.put(list[i], (weights != null) ? weights[i] : 1.0);
		}
		
	}
	
	public WeightedStringList(String[] list, int startIndex) {
		this(list, null, startIndex);
	}
	
	public WeightedStringList(String[] list) {
		this(list, 0);
	}
	
	public WeightedStringList(Collection<String> list, int startIndex) {
		this(list.toArray(new String[0]), null, startIndex);
	}
	
	public WeightedStringList(Set<String> list) {
		this(list, 0);
	}
	
	public WeightedStringList(WeightedStringList list1, WeightedStringList list2) {
		this.list = new TreeMap<String, Double>();
		for (Entry<String, Double> entry : list1.list.entrySet())
			this.list.put(entry.getKey(), entry.getValue());
		for (Entry<String, Double> entry : list2.list.entrySet())
			this.list.put(entry.getKey(), entry.getValue());
	}
	
	public WeightedStringList(Collection<WeightedStringList> lists) {
		this.list = new TreeMap<String, Double>();
	
		for (WeightedStringList list : lists) {
			for (Entry<String, Double> entry : list.list.entrySet())
				this.list.put(entry.getKey(), entry.getValue());
		}
	}
	
	public boolean contains(String str) {
		return this.list.containsKey(str);
	}
	
	public String[] getStrings() {
		return this.list.keySet().toArray(new String[0]);
	}
	
	public Map<String, Double> getWeightMap() {
		return this.list;
	}
	
	public int size() {
		return this.list.size();
	}
	
	public double getStringWeight(String str) {
		if (this.list.containsKey(str))
			return this.list.get(str);
		return 0.0;
	}
	
	public List<String> getStringsAboveWeight(double threshold) {
		List<String> retStrs = new ArrayList<String>();
		
		for (Entry<String, Double> entry : this.list.entrySet())
			if (entry.getValue() >= threshold)
				retStrs.add(entry.getKey());
		
		return retStrs;
	}
	
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		List<Entry<String, Double>> labelEntries = new ArrayList<Entry<String, Double>>(this.list.size());
		labelEntries.addAll(this.list.entrySet());
			
		Collections.sort(labelEntries, new Comparator<Entry<String, Double>>() {
			@Override
			public int compare(Entry<String, Double> l1,
					Entry<String, Double> l2) {
				if (l1.getValue() > l2.getValue())
					return -1;
				else if (l1.getValue() < l2.getValue())
					return 1;
				else
					return 0;
			}
		});
			
		for (Entry<String, Double> weightedLabel : labelEntries)
			str.append(weightedLabel.getKey())
				.append(":")
				.append(weightedLabel.getValue())
				.append(",");
		
		if (str.length() > 0)
			str.delete(str.length() - 1, str.length());
		
		return str.toString();
	}
	
	/**
	 * Note that this does not consider weights
	 */
	@Override
	public boolean equals(Object o) {
		WeightedStringList l = (WeightedStringList)o;
		
		if (l.list.size() != this.list.size())
			return false;
		
		return l.list.keySet().containsAll(this.list.keySet())
				&& this.list.keySet().containsAll(l.list.keySet());
	}

	/**
	 * Note that this does not consider weights
	 */
	@Override
	public int hashCode() {
		int h = 0;
		for (String str : this.list.keySet())
			h ^= str.hashCode();
		return h;
	}
	
	@Override
	public boolean fromString(String str) {
		String[] strParts = str.split(",");
		if (strParts.length == 0 || (strParts.length == 1 && strParts[0].length() == 0)) {
			this.list.clear();
			return true;
		}
		
		if (!strParts[0].contains(":")) {
			WeightedStringList l = new WeightedStringList(strParts, 0);	
			this.list = l.list;
			return true;
		}
		
		String[] categories = new String[strParts.length];
		double[] labelWeights = new double[strParts.length];
		for (int i = 0; i < strParts.length; i++) {
			String[] labelParts = strParts[i].split(":");
			categories[i] = labelParts[0];
			labelWeights[i] = Double.parseDouble(labelParts[1]);
		}
		
		WeightedStringList l = new WeightedStringList(categories, labelWeights, 0);
		this.list = l.list;
		return true;
	}
}

