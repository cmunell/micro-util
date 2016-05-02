package edu.cmu.ml.rtw.generic.parse;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java_cup.runtime.ComplexSymbolFactory;
import edu.cmu.ml.rtw.generic.parse.Assignment.AssignmentTyped;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.StringSerializable;

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
public class AssignmentList extends Obj implements Iterable<Assignment>, StringSerializable {
	private class AssignmentIterator implements Iterator<Assignment> {
		private Iterator<Pair<String, Assignment>> iterator;
		
		public AssignmentIterator(Iterator<Pair<String, Assignment>> iterator) {
			this.iterator = iterator;
		}
		
		@Override
		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		@Override
		public Assignment next() {
			return this.iterator.next().getSecond();
		}
		
	}
	
	private List<Pair<String, Assignment>> assignments;
	
	public AssignmentList() {
		this.assignments = new ArrayList<Pair<String, Assignment>>();
	}
	
	public boolean push(Assignment assignment) {
		if ((assignment.getName() == null && hasNames())
				|| (assignment.getName() != null && !hasNames() && size() > 0)) {
			// Must be either all named or all not-named assignments
			return false;
		}
		
		this.assignments.add(0, new Pair<>(assignment.getName(), assignment));
		
		return true;
	}
	
	public boolean add(Assignment assignment) {
		this.assignments.add(new Pair<>(assignment.getName(), assignment));
		return true;
	}
	
	public Assignment get(int index) {
		return this.assignments.get(index).getSecond();
	}
	
	public Assignment get(String name) {
		for (Pair<String, Assignment> assignment : this.assignments)
			if (name.equals(assignment.getFirst()))
				return assignment.getSecond();
		return null;
	}
	
	public boolean contains(String name) {
		for (Pair<String, Assignment> assignment : this.assignments)
			if (name.equals(assignment.getFirst()))
				return true;
		return false;
	}
	
	public int size() {
		return this.assignments.size();
	}
	
	public boolean hasNames() {
		return this.assignments.size() > 0 && this.assignments.get(0).getFirst() != null;
	}
	
	@Override
	public boolean serialize(Writer writer) throws IOException {
		for (int i = 0; i < this.assignments.size(); i++) {
			Assignment assignment = this.assignments.get(i).getSecond();
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
		Map<String, Obj> matches = new TreeMap<String, Obj>();
		
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
					return new TreeMap<String, Obj>();
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
		List<Pair<String, Assignment>> oldAssignments = this.assignments;
		this.assignments = new ArrayList<>();
		
		for (Pair<String, Assignment> pair: oldAssignments) {
			Assignment assignment = pair.getSecond();
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
				add(AssignmentTyped.assignmentUntyped(assignment.getName(), context.get(valueObj.getStr())));
			}
		}
		
		return resolved;
	}
	
	public Map<String, Obj> makeObjMap() {
		Map<String, Obj> objMap = new HashMap<String, Obj>();
		
		for (Pair<String, Assignment> pair: this.assignments) {
			objMap.put(pair.getFirst(), pair.getSecond().getValue());
		}
		
		return objMap;
	}

	@Override
	public Obj clone() {
		AssignmentList clone = new AssignmentList();
		if (this.assignments.size() == 0)
			return clone;
		
		if (this.assignments.get(0).getSecond().isTyped()) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List<Pair<String, AssignmentTyped>> typedAssignments = (List<Pair<String, AssignmentTyped>>)(List)this.assignments;
			for (Pair<String, AssignmentTyped> pair : typedAssignments) {
				AssignmentTyped assignmentTyped = pair.getSecond();
				clone.add(Assignment.assignmentTyped(assignmentTyped.getModifiers(), assignmentTyped.getType(), assignmentTyped.getName(), assignmentTyped.getValue().clone()));
			}
		} else {
			for (Pair<String, Assignment> pair : this.assignments) {
				Assignment assignment = pair.getSecond();
				clone.add(Assignment.assignmentUntyped(assignment.getName(), assignment.getValue().clone()));
			}
		}
		
		return clone;
	}

	@Override
	protected Set<String> getCurlyBracedValueStrs(Set<String> strs) {
		for (Pair<String, Assignment> assignment : this.assignments)
			assignment.getSecond().getValue().getCurlyBracedValueStrs(strs);
		return strs;
	}

	@Override
	public Iterator<Assignment> iterator() {
		return new AssignmentIterator(this.assignments.iterator());
	}
	
	@Override
	public boolean fromString(String str) {
		CtxScanner scanner = new CtxScanner(new StringReader(str));
		CtxParser parser = new CtxParser(scanner, new ComplexSymbolFactory());
		AssignmentList parse = null;
		try {
			parse = (AssignmentList)parser.parse().value;
			for (int i = 0; i < parse.size(); i++)
				add(parse.get(i));
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
