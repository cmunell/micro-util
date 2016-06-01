package edu.cmu.ml.rtw.generic.opt.search;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class Position implements Comparable<Position> {
	protected Map<Dimension, Obj> coordinates;
	protected Context context;
	
	public Position(Context context) {
		this.coordinates = new TreeMap<Dimension, Obj>(new Comparator<Dimension>() {
			@Override
			public int compare(Dimension d1, Dimension d2) {
				return d1.getReferenceName().compareTo(d2.getReferenceName());
			}
		});
		
		this.context = context;
	}
	
	public Position getSubPositionUpTo(int stageIndex) {
		Position subPosition = new Position(this.context);
		
		for (Entry<Dimension, Obj> entry : this.coordinates.entrySet()) {
			if (entry.getKey().getStageIndex() <= stageIndex)
				subPosition.coordinates.put(entry.getKey(), entry.getValue());
		}
		
		return subPosition;
	}
	
	public boolean isSubPositionOf(Position position) {
		for (Entry<Dimension, Obj> entry : this.coordinates.entrySet()) 
			if (!position.coordinates.containsKey(entry.getKey()) || !position.coordinates.get(entry.getKey()).equals(entry.getValue()))
				return false;
		return true;
	}
	
	public Obj getDimensionValue(Dimension dimension) {
		return this.coordinates.get(dimension);
	}
	
	public boolean setDimensionValue(Dimension dimension, Obj value) {
		this.coordinates.put(dimension, value);
		return true;
	}
	
	public Map<Dimension, Obj> getCoordinates() {
		return this.coordinates;
	}
	
	public Position clone() {
		Position clonePosition = new Position(this.context);
		for (Entry<Dimension, Obj> entry : this.coordinates.entrySet())
			clonePosition.setDimensionValue(entry.getKey(), entry.getValue());
		return clonePosition;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("(");
		for (Entry<Dimension, Obj> entry : this.coordinates.entrySet()) {
			str.append(entry.getKey()).append("=").append(entry.getValue().toString()).append(",");
		}
		str.delete(str.length() - 1, str.length());
		str.append(")");
		return str.toString();
	}
	
	public String toValueString(String separator) {
		StringBuilder str = new StringBuilder();
		for (Entry<Dimension, Obj> entry : this.coordinates.entrySet()) {
			str.append(entry.getValue().toString()).append(separator);
		}
		str.delete(str.length() - 1, str.length());
		return str.toString();
	}
	
	public String toKeyString(String separator) {
		StringBuilder str = new StringBuilder();
		for (Entry<Dimension, Obj> entry : this.coordinates.entrySet()) {
			str.append(entry.getKey()).append(separator);
		}
		str.delete(str.length() - 1, str.length());
		return str.toString();
	}
	
	public String toKeyValueString(String separator, String keyValueGlue) {
		StringBuilder str = new StringBuilder();
		
		for (Entry<Dimension, Obj> entry : this.coordinates.entrySet()) {
			str.append(entry.getKey())
			   .append(keyValueGlue)
			   .append(entry.getValue().toString())
			   .append(separator);
		}
		
		str.delete(str.length() - 1, str.length());
		
		return str.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		Position g = (Position)o;
		
		if (g.coordinates.size() != this.coordinates.size())
			return false;
		
		for (Entry<Dimension, Obj> entry : this.coordinates.entrySet())
			if (!g.coordinates.containsKey(entry.getKey()) || !g.coordinates.get(entry.getKey()).equals(entry.getValue()))
				return false;
		
		return true;
	}
	
	@Override
	public int hashCode() {
		int hashCode = 0;
		
		for (Entry<Dimension, Obj> entry : this.coordinates.entrySet())
			hashCode ^= entry.getKey().hashCode() ^ entry.getValue().hashCode();
		
		return hashCode;
	}

	@Override
	public int compareTo(Position o) {
		return this.toString().compareTo(o.toString());
	}
}
