package edu.cmu.ml.rtw.generic.parse;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Assignment represents an assignment in a ctx script 
 * parsed using the CtxParser.  An assignment
 * gives a name to some object value (where an object is
 * represented as an Obj object).
 * 
 * @author Bill McDowell
 *
 */
public abstract class Assignment extends Serializable {
	protected String name;
	protected Obj value;
	
	public String getName() {
		return this.name;
	}
	
	public Obj getValue() {
		return this.value;
	}
	
	public abstract boolean isTyped();
	
	public static AssignmentUntyped assignmentUntyped(String name, Obj value) { return new AssignmentUntyped(name, value); }
	public static AssignmentUntyped assignmentUntyped(Obj value) { return new AssignmentUntyped(null, value); }
	public static class AssignmentUntyped extends Assignment {
		public AssignmentUntyped(String name, Obj value) {
			this.name = name;
			this.value = value;
		}
		
		@Override
		public boolean serialize(Writer writer) throws IOException {
			if (this.name != null) {	
				writer.write(this.name);
				writer.write("=");
			}
			
			if (!this.value.serialize(writer))
				return false;
			
			return true;
		}

		@Override
		public boolean isTyped() {
			return false;
		}
	}
	
	public static AssignmentTyped assignmentTyped(List<String> modifiers, String type, String name, Obj value) { return new AssignmentTyped(modifiers, type, name, value); }
	public static class AssignmentTyped extends Assignment {
		private String type;
		private List<String> modifiers;
		
		public AssignmentTyped(List<String> modifiers, String type, String name, Obj value) {
			this.name = name;
			this.value = value;
			this.type = type;
			this.modifiers = modifiers;
		}
		
		public String getType() {
			return this.type;
		}
		
		public List<String> getModifiers() {
			return this.modifiers;
		}
	
		@Override
		public boolean serialize(Writer writer) throws IOException {
			if (this.modifiers != null && this.modifiers.size() > 0) {
				writer.write("(");
				for (int i = 0; i < this.modifiers.size(); i++) {
					writer.write(this.modifiers.get(i));
					if (i < this.modifiers.size() - 1)
						writer.write(", ");
				}
				writer.write(") ");
			}
			
			writer.write(this.type);
			writer.write(" ");
			writer.write(this.name);
			writer.write("=");
			
			if (!this.value.serialize(writer))
				return false;
			
			return true;
		}

		@Override
		public boolean isTyped() {
			return true;
		}
	}
}
