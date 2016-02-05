package edu.cmu.ml.rtw.generic.parse;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.cmu.ml.rtw.generic.parse.Assignment.AssignmentTyped;
import edu.cmu.ml.rtw.generic.parse.Assignment.AssignmentUntyped;

/**
 * AssignmentList represents a list of assignments in 
 * a ctx script parsed using the CtxParser. A ctx
 * script is an assignment list at the highest level,
 * but assignment lists are also used to give the
 * parameter settings and internal assignments 
 * for function Objs.
 * 
 * @author Bill McDowell
 *
 */

public class AssignmentList extends Obj {
	private List<Assignment> assignments;
	private Map<String, Assignment> assignmentMap;
	
	public AssignmentList() {
		this.assignments = new ArrayList<Assignment>();
		this.assignmentMap = new HashMap<String, Assignment>();
	}
	
	public boolean push(Assignment assignment) {
		if ((assignment.getName() == null && hasNames())
				|| (assignment.getName() != null && !hasNames() && size() > 0)) {
			// Must be either all named or all not-named assignments
			return false;
		}
		
		this.assignments.add(0, assignment);
		if (assignment.getName() != null)
			this.assignmentMap.put(assignment.getName(), assignment);
		
		return true;
	}
	
	public boolean add(Assignment assignment) {
		if ((assignment.getName() == null && hasNames())
				|| (assignment.getName() != null && !hasNames() && size() > 0)) {
			// Must be either all named or all not-named assignments
			return false;
		}
		
		this.assignments.add(assignment);
		if (assignment.getName() != null)
			this.assignmentMap.put(assignment.getName(), assignment);
		
		return true;
	}
	
	public Assignment get(int index) {
		return this.assignments.get(index);
	}
	
	public Assignment get(String name) {
		return this.assignmentMap.get(name);
	}
	
	public boolean contains(String name) {
		return this.assignmentMap.containsKey(name);
	}
	
	public int size() {
		return this.assignments.size();
	}
	
	public boolean hasNames() {
		return this.assignmentMap.size() > 0;
	}
	
	@Override
	public boolean serialize(Writer writer) throws IOException {
		for (int i = 0; i < this.assignments.size(); i++) {
			Assignment assignment = this.assignments.get(i);
			if (!assignment.serialize(writer))
				return false;
			
			if (assignment.isTyped())
				writer.write(";\n");
			else if (i != this.assignments.size() - 1)
				writer.write(", ");
		}
		
		return true;
	}
	
	@Override
	public Obj.Type getObjType() {
		return Obj.Type.ASSIGNMENT_LIST;
	}

	@Override
	public Map<String, Obj> match(Obj obj) {
		Map<String, Obj> matches = new HashMap<String, Obj>();
		
		if (obj.getObjType() == Obj.Type.VALUE) {
			Obj.Value vObj = (Obj.Value)obj;
			if (vObj.getType() == Value.Type.SQUARE_BRACKETED)
				matches.put(vObj.getStr(), this);
			return matches;
		} else if (obj.getObjType() != Obj.Type.ASSIGNMENT_LIST)
			return matches;
		
		AssignmentList aList = (AssignmentList)obj;
		
		if (aList.size() > this.size() || (!hasNames() && aList.hasNames()))
			return matches;
		
		if (aList.hasNames()) {
			for (int i = 0; i < aList.size(); i++) {
				String name = aList.get(i).getName();
				if (!contains(name))
					return new HashMap<String, Obj>();
				Map<String, Obj> aMatches = get(name).getValue().match(aList.get(i).getValue());
				if (aMatches.size() == 0)
					return aMatches;
				matches.putAll(aMatches);
			}
		} else {
			for (int i = 0; i < aList.size(); i++) {
				Map<String, Obj> aMatches = get(i).getValue().match(aList.get(i).getValue());
				if (aMatches.size() == 0)
					return aMatches;
				matches.putAll(aMatches);
			}
		}
		
		matches.put("", this);
		
		return matches;
	}

	@Override
	public boolean resolveValues(Map<String, Obj> context) {
		boolean resolved = true;
		List<Assignment> oldAssignments = this.assignments;
		this.assignmentMap = new HashMap<String, Assignment>();
		this.assignments = new ArrayList<Assignment>();
		
		for (Assignment assignment : oldAssignments) {
			Obj obj = assignment.getValue();

			if (obj.getObjType() != Obj.Type.VALUE || ((Obj.Value)obj).getType() != Obj.Value.Type.CURLY_BRACED) {
				resolved = obj.resolveValues(context) && resolved;
				
				add(assignment);
				continue;
			}
			
			Obj.Value valueObj = (Obj.Value)obj;
			if (!context.containsKey(valueObj.getStr())) {
				add(assignment);
				resolved = false;
			} else if (assignment.isTyped()) {
				AssignmentTyped assignmentTyped = (AssignmentTyped)assignment;
				add(AssignmentTyped.assignmentTyped(assignmentTyped.getModifiers(), assignmentTyped.getType(), assignmentTyped.getName(), context.get(valueObj.getStr())));
			} else {
				AssignmentUntyped assignmentUntyped = (AssignmentUntyped)assignment;
				add(AssignmentTyped.assignmentUntyped(assignmentUntyped.getName(), context.get(valueObj.getStr())));
			}
		}
		
		return resolved;
	}
	
	public Map<String, Obj> makeObjMap() {
		Map<String, Obj> objMap = new HashMap<String, Obj>();
		
		for (Entry<String, Assignment> entry : this.assignmentMap.entrySet()) {
			objMap.put(entry.getKey(), entry.getValue().getValue());
		}
		
		return objMap;
	}

	@Override
	public Obj clone() {
		AssignmentList clone = new AssignmentList();
		for (Assignment assignment : this.assignments) {
			if (assignment.isTyped()) {
				AssignmentTyped assignmentTyped = (AssignmentTyped)assignment;
				clone.add(Assignment.assignmentTyped(assignmentTyped.getModifiers(), assignmentTyped.getType(), assignmentTyped.getName(), assignmentTyped.getValue().clone()));
			} else { 
				clone.add(Assignment.assignmentUntyped(assignment.getName(), assignment.getValue().clone()));
			}
		}
		
		return clone;
	}

	@Override
	protected Set<String> getCurlyBracedValueStrs(Set<String> strs) {
		for (Assignment assignment : this.assignments)
			assignment.getValue().getCurlyBracedValueStrs(strs);
		return strs;
	}
}
