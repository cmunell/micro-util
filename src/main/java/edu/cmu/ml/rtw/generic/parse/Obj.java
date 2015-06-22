package edu.cmu.ml.rtw.generic.parse;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Obj represents an object that can be
 * assigned a name within a ctx script. The 
 * CtxParse parses a ctx script into a list
 * of assignments (stored 
 * in an AssignmentList object) of names to 
 * Objs.  This AssignmentList can be converted to
 * an edu.cmu.ml.rtw.generic.data.Context object
 * containing features, models, rule sets, etc.  
 * The features,
 * models, rule sets etc are constructed from Objs.
 * 
 * @author Bill McDowell
 *
 */
public abstract class Obj extends Serializable {
	public enum Type {
		FUNCTION,
		VALUE,
		ARRAY,
		RULE,
		ASSIGNMENT_LIST
	}
	
	public abstract Type getObjType();
	public abstract Map<String, Obj> match(Obj obj);
	public abstract boolean resolveValues(Map<String, Obj> context);
	public abstract Obj clone();
	
	public static Function function(String name, AssignmentList parameters, AssignmentList internalAssignments) { return new Function(name, parameters, internalAssignments); }
	public static Function function(String name, AssignmentList parameters) { return new Function(name, parameters); }
	public static Function functionComposition(Function function1, Function function2) { return new Function(function1, function2, Function.Type.COMPOSITION); }
	public static Function functionComposition(Value value1, Value value2) { return new Function(value1, value2, Function.Type.COMPOSITION); }
	public static Function functionComposition(Function function, Value value) { return new Function(function, value, Function.Type.COMPOSITION); }
	public static class Function extends Obj {
		public enum Type {
			COMPOSITION
		}
		
		private String name;
		private AssignmentList parameters;
		private AssignmentList internalAssignments;
		
		public Function(Function function1, Function function2, Type type) {
			this.name = "Composite";
			this.parameters = new AssignmentList();
			this.parameters.add(Assignment.assignmentUntyped(function1));
			this.parameters.add(Assignment.assignmentUntyped(function2));
		}
		
		public Function(Function function, Value value, Type type) {
			this.name = "Composite";
			this.parameters = new AssignmentList();
			this.parameters.add(Assignment.assignmentUntyped(function));
			this.parameters.add(Assignment.assignmentUntyped(value));
		}
		
		public Function(Value value1, Value value2, Type type) {
			this.name = "Composite";
			this.parameters = new AssignmentList();
			this.parameters.add(Assignment.assignmentUntyped(value1));
			this.parameters.add(Assignment.assignmentUntyped(value2));
		}
		
		public Function(String name, AssignmentList parameters) {
			this.name = name;
			this.parameters = parameters;
			this.internalAssignments = null;
		}
		
		public Function(String name, AssignmentList parameters, AssignmentList internalAssignments) {
			this.name = name;
			this.parameters = parameters;
			this.internalAssignments = internalAssignments;
		}
		
		public String getName() {
			return this.name;
		}
		
		public AssignmentList getParameters() {
			return this.parameters;
		}
		
		public AssignmentList getInternalAssignments() {
			return this.internalAssignments;
		}

		@Override
		public boolean serialize(Writer writer) throws IOException {
			writer.write(this.name);
			writer.write("(");
			if (!this.parameters.serialize(writer))
				return false;
			writer.write(")");
			
			if (this.internalAssignments != null) {
				writer.write(" {\n");
				if (!this.internalAssignments.serialize(writer))
					return false;
				writer.write("}");
			};
			
			return true;
		}

		@Override
		public Obj.Type getObjType() {
			return Obj.Type.FUNCTION;
		}

		@Override
		public Map<String, Obj> match(Obj obj) {
			Map<String, Obj> matches = new HashMap<String, Obj>();
			
			if (obj.getObjType() == Obj.Type.VALUE) {
				Obj.Value vObj = (Obj.Value)obj;
				if (vObj.getType() == Value.Type.SQUARE_BRACKETED) {
					matches.put(vObj.getStr(), this);
				}
				return matches;
			} else if (obj.getObjType() != Obj.Type.FUNCTION)
				return matches;
			
			Obj.Function fnObj = (Obj.Function)obj;
			
			if (!this.name.equals(fnObj.name) 
					|| fnObj.internalAssignments != null && this.internalAssignments == null)
				return matches;
			
			Map<String, Obj> pMatch = this.parameters.match(fnObj.parameters);
			if (pMatch.size() == 0)
				return pMatch;
			matches.putAll(pMatch);
			
			if (fnObj.internalAssignments != null) {
				Map<String, Obj> aMatch = this.internalAssignments.match(fnObj.internalAssignments);
				if (aMatch.size() == 0)
					return aMatch;
				matches.putAll(aMatch);
			}
			
			matches.put("", this);
				
			return matches;
		}

		@Override
		public boolean resolveValues(Map<String, Obj> context) {
			boolean resolved = this.parameters.resolveValues(context);
			resolved = (this.internalAssignments == null || this.internalAssignments.resolveValues(context)) && resolved;
			return resolved;
		}

		@Override
		public Obj clone() {
			return Obj.function(this.name, (AssignmentList)this.parameters.clone(), (this.internalAssignments != null) ? (AssignmentList)this.internalAssignments.clone() : null);
		}
	}
	
	public static Value squareBracketedValue(String str) { return new Value(str, Value.Type.SQUARE_BRACKETED); }
	public static Value curlyBracedValue(String str) { return new Value(str, Value.Type.CURLY_BRACED); }
	public static Value stringValue(String str) { return new Value(str, Value.Type.STRING); }
	public static class Value extends Obj {
		public enum Type {
			SQUARE_BRACKETED,
			CURLY_BRACED,
			STRING
		}
		
		private String str;
		private Type type;
		
		public Value(String str, Type type) {
			this.str = str;
			this.type = type;
		}
		
		@Override
		public int hashCode() {
			return this.str.hashCode() ^ this.type.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			Value v = (Value)o;
			return v.type == this.type && v.str.equals(this.str);
		}
		
		public String getStr() {
			return this.str;
		}
		
		public String getValueStr(Map<String, String> valueContext) {
			if (getType() == Value.Type.SQUARE_BRACKETED)
				return null;
			else if (getType() == Value.Type.CURLY_BRACED) {
				if (!valueContext.containsKey(getStr()))
					return null;
				return valueContext.get(getStr());
			} else {
				String str = getStr();
				for (Entry<String, String> entry : valueContext.entrySet()) {
					str = str.replaceAll("\\$\\{" + entry.getKey() + "\\}", entry.getValue());
				}
				
				return str;
			}
		}
		
		public Type getType() {
			return this.type;
		}

		@Override
		public boolean serialize(Writer writer) throws IOException {
			if (this.type == Type.SQUARE_BRACKETED) {
				writer.write("[");
				writer.write(this.str);
				writer.write("]");
			} else if (this.type == Type.CURLY_BRACED) {
				writer.write("${");
				writer.write(this.str);
				writer.write("}");
			} else {
				writer.write("\"");
				writer.write(this.str.replace("\"", "\\\""));
				writer.write("\"");
			}

			return true;
		}
		
		@Override
		public Obj.Type getObjType() {
			return Obj.Type.VALUE;
		}
		
		@Override
		public Map<String, Obj> match(Obj obj) {
			Map<String, Obj> matches = new HashMap<String, Obj>();
			
			if (obj.getObjType() != Obj.Type.VALUE)
				return matches;
			
			Obj.Value vObj = (Obj.Value)obj;
			if (vObj.getType() == Value.Type.SQUARE_BRACKETED) {
				matches.put(vObj.getStr(), this);
			} else if (vObj.getType() == Value.Type.STRING && this.str.equals(vObj.str))
				matches.put("", this);
				
			return matches;
		}

		@Override
		public boolean resolveValues(Map<String, Obj> context) {
			if (this.type != Type.CURLY_BRACED)
				return true;
			
			if (!context.containsKey(this.str))
				return false;
			
			Obj.Value value = (Obj.Value)context.get(this.str);
			this.type = value.type;
			this.str = value.str;
			
			return true;
		}

		@Override
		public Obj clone() {
			return new Obj.Value(this.str, this.type);
		}
	}
	
	public static Array array() { return new Array(); }
	public static Array array(List<String> values) { return new Array(values); }
	public static Array array(String[] values) { return new Array(values); }
	public static class Array extends Obj {
		private List<Value> values;
		
		public Array() {
			this.values = new ArrayList<Value>();
		}
		
		public Array(List<String> values) {
			this.values = new ArrayList<Value>(values.size());
			for (String value : values)
				this.values.add(Obj.stringValue(value));
		}
		
		public Array(String[] values) {
			this.values = new ArrayList<Value>(values.length);
			for (String value : values)
				this.values.add(Obj.stringValue(value));
		}
		
		public void add(Value value) {
			this.values.add(value);
		}
		
		public Obj.Value get(int index) {
			return this.values.get(index);
		}
		
		public String getStr(int index) {
			return this.values.get(index).getStr();
		}
		
		public int size() {
			return this.values.size();
		}
		
		public List<String> toList(Map<String, String> valueContext) {
			List<String> list = new ArrayList<String>(this.values.size());
			for (Obj.Value value : this.values) {
				String valueStr = value.getValueStr(valueContext);
				if (valueStr == null)
					return null;
				list.add(valueStr);
			}
			
			return list;
		}
		
		@Override
		public boolean serialize(Writer writer) throws IOException {
			writer.write("(");
			for (int i = 0; i < this.values.size(); i++) {
				Value value = this.values.get(i);
				value.serialize(writer);
				if (i != this.values.size() - 1)
					writer.write(", ");
			}
			writer.write(")");
			
			return true;
		}
		
		@Override
		public Obj.Type getObjType() {
			return Obj.Type.ARRAY;
		}

		@Override
		public Map<String, Obj> match(Obj obj) {
			Map<String, Obj> matches = new HashMap<String, Obj>();
			
			if (obj.getObjType() == Obj.Type.VALUE) {
				Obj.Value vObj = (Obj.Value)obj;
				if (vObj.getType() == Value.Type.SQUARE_BRACKETED) {
					matches.put(vObj.getStr(), this);
				}
				return matches;
			} else if (obj.getObjType() != Obj.Type.ARRAY)
				return matches;
			
			Obj.Array aObj = (Obj.Array)obj;
			
			if (aObj.size() != this.size())
				return matches;
			
			for (int i = 0; i < aObj.size(); i++) {
				Map<String, Obj> aMatch = this.get(i).match(aObj.get(i));
				if (aMatch.size() == 0)
					return matches;
				matches.putAll(aMatch);
			}
			
			matches.put("", this);
			
			return matches;
		}

		@Override
		public boolean resolveValues(Map<String, Obj> context) {
			boolean resolved = true;
			
			for (Obj.Value value : this.values) {
				resolved = value.resolveValues(context) && resolved;
			}
			
			return resolved;
		}

		@Override
		public Obj clone() {
			Obj.Array array = Obj.array();
			for (Value value : this.values)
				array.add((Obj.Value)value.clone());
			return array;
		}
	}
	
	public static Rule rule(Function source, Function target) { return new Rule(source, target); }
	public static class Rule extends Obj {
		private Function source;
		private Function target;
		
		public Rule(Function source, Function target) {
			this.source = source;
			this.target = target;
		}
		
		public Function getSource() {
			return this.source;
		}
		
		public Function getTarget() {
			return this.target;
		}

		@Override
		public boolean serialize(Writer writer) throws IOException {
			writer.write("(");
			
			if (!this.source.serialize(writer))
				return false;
			
			writer.write(") -> (");
			
			if (!this.target.serialize(writer))
				return false;
			
			writer.write(")");
			
			return true;
		}
		
		@Override
		public Obj.Type getObjType() {
			return Obj.Type.RULE;
		}

		@Override
		public Map<String, Obj> match(Obj obj) {
			Map<String, Obj> matches = new HashMap<String, Obj>(); 
			if (obj.getObjType() != Obj.Type.RULE)
				return matches;
			
			matches.put("", this); // TODO: Eventually do something about matching rules... but this is good for now
			
			return matches;
		}

		@Override
		public boolean resolveValues(Map<String, Obj> context) {
			boolean resolved = this.source.resolveValues(context);
			resolved = this.target.resolveValues(context) && resolved;
			return resolved;
		}

		@Override
		public Obj clone() {
			return Obj.rule((Obj.Function)this.source.clone(), (Obj.Function)this.target.clone());
		}
	}
}
