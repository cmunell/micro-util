package edu.cmu.ml.rtw.generic.data;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import java_cup.runtime.ComplexSymbolFactory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.opt.search.ParameterSearchable;
import edu.cmu.ml.rtw.generic.opt.search.Search;
import edu.cmu.ml.rtw.generic.parse.CtxScanner;
import edu.cmu.ml.rtw.generic.parse.CtxParser;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.parse.Obj.Type;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * Context holds a set of named objects that have 
 * been constructed through a script written in the
 * language defined in edu.cmu.ml.rtw.generic.parse.  
 * These objects can refer to named tools stored in 
 * edu.cmu.ml.rtw.generic.data.DataTools. 
 * 
 * @author Bill McDowell
 *
 */
public class Context extends CtxParsableFunction {
	public enum ObjectType {
		CONTEXT("context"),
		TOKEN_SPAN_FN("ts_fn"),
		STR_FN("str_fn"),
		TOKEN_SPAN_STR_FN("ts_str_fn"),
		ARRAY("array"),
		VALUE("value"),
		SEARCH("search");
		
		private String str;
		
		ObjectType(String str) {
			this.str = str;
		}
		
		public String toString() {
			return this.str;
		}
		
		public static ObjectType fromString(String str) {
			ObjectType[] types = ObjectType.values();
			for (ObjectType type : types) {
				if (str.equals(type.toString()))
					return type;
			}
			return null;
		}
	}
	
	protected DataTools dataTools;
	protected String genericName;
	protected Context parentContext;
	
	protected List<Pair<String, String>> objNameOrdering;
	protected List<Map<String, ?>> storageMaps;
	
	protected Map<String, Context> contexts;
	protected Map<String, Fn<TokenSpan, TokenSpan>> tokenSpanFns;
	protected Map<String, Fn<String, String>> strFns;
	protected Map<String, Fn<TokenSpan, String>> tokenSpanStrFns;
	protected Map<String, List<String>> arrays;
	protected Map<String, String> values;
	protected Map<String, Search> searches;
	
	protected int currentReferenceId;
	
	public Context(DataTools dataTools) {
		this(dataTools, "Context", null);
	}
	
	public Context(DataTools dataTools, String genericName, Context parentContext) {
		this.dataTools = dataTools;
		this.objNameOrdering = new ArrayList<Pair<String, String>>();
		this.storageMaps = new ArrayList<Map<String, ?>>();
		
		this.contexts = new ConcurrentHashMap<String, Context>();
		this.tokenSpanFns = new ConcurrentHashMap<String, Fn<TokenSpan, TokenSpan>>();
		this.strFns = new ConcurrentHashMap<String, Fn<String, String>>();
		this.tokenSpanStrFns = new ConcurrentHashMap<String, Fn<TokenSpan, String>>();
		this.arrays = new TreeMap<String, List<String>>();
		this.values = new TreeMap<String, String>();
		this.searches = new ConcurrentHashMap<String, Search>();
		
		this.storageMaps.add(this.contexts);
		this.storageMaps.add(this.tokenSpanFns);
		this.storageMaps.add(this.strFns);
		this.storageMaps.add(this.tokenSpanStrFns);
		this.storageMaps.add(this.arrays);
		this.storageMaps.add(this.values);
		this.storageMaps.add(this.searches);
		
		this.currentReferenceId = 0;
		
		this.genericName = genericName;
		this.parentContext = parentContext;
	}
	
	@Override
	public List<String> getModifiers() {
		return new ArrayList<String>();
	}

	@Override
	protected AssignmentList toParseInternal() {
		AssignmentList assignmentList = new AssignmentList();
		for (Pair<String, String> obj : this.objNameOrdering) {
			Assignment assignment = toAssignment(obj);
			if (assignment == null)
				return null;
			assignmentList.add(assignment);
		}
		
		return assignmentList;
	}
	
	protected Assignment toAssignment(Pair<String, String> obj) {
		ObjectType type = ObjectType.fromString(obj.getFirst());
		if (type == ObjectType.CONTEXT) {
			return Assignment.assignmentTyped(this.contexts.get(obj.getSecond()).getModifiers(), ObjectType.CONTEXT.toString(), obj.getSecond(), this.contexts.get(obj.getSecond()).toParse());
		} else if (type == ObjectType.TOKEN_SPAN_FN) {
			return Assignment.assignmentTyped(this.tokenSpanFns.get(obj.getSecond()).getModifiers(), ObjectType.TOKEN_SPAN_FN.toString(), obj.getSecond(), this.tokenSpanFns.get(obj.getSecond()).toParse());
		} else if (type == ObjectType.STR_FN) {
			return Assignment.assignmentTyped(this.strFns.get(obj.getSecond()).getModifiers(), ObjectType.STR_FN.toString(), obj.getSecond(), this.strFns.get(obj.getSecond()).toParse());	
		} else if (type == ObjectType.TOKEN_SPAN_STR_FN) {
			return Assignment.assignmentTyped(this.tokenSpanStrFns.get(obj.getSecond()).getModifiers(), ObjectType.TOKEN_SPAN_STR_FN.toString(), obj.getSecond(), this.tokenSpanStrFns.get(obj.getSecond()).toParse());	
		} else if (type == ObjectType.ARRAY) {
			return Assignment.assignmentTyped(new ArrayList<String>(), ObjectType.ARRAY.toString(), obj.getSecond(), Obj.array(this.arrays.get(obj.getSecond())));
		} else if (type == ObjectType.VALUE) {
			return Assignment.assignmentTyped(new ArrayList<String>(), ObjectType.VALUE.toString(), obj.getSecond(), Obj.stringValue(this.values.get(obj.getSecond())));
		} else if (type == ObjectType.SEARCH) {
			return Assignment.assignmentTyped(new ArrayList<String>(), ObjectType.SEARCH.toString(), obj.getSecond(), this.searches.get(obj.getSecond()).toParse());
		} else {
			return null;
		}
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {		
		for (int i = 0; i < internalAssignments.size(); i++) {
			Assignment.AssignmentTyped assignment = (Assignment.AssignmentTyped)internalAssignments.get(i);
			if (!fromParseAssignment(assignment)) {
				this.dataTools.getOutputWriter().debugWriteln("ERROR: Failed to construct '" + assignment.getName() + "' from context parse."); 
				return false;
			}
		}

		return true;
	}
	
	protected boolean fromParseAssignment(Assignment.AssignmentTyped assignment) {
		if (assignment.getType().equals(ObjectType.CONTEXT.toString())) {
			if (runAssignmentCommandContext(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null) {
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.TOKEN_SPAN_FN.toString())) {
			if (runAssignmentCommandTokenSpanFn(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null) {
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.STR_FN.toString())) {
			if (runAssignmentCommandStrFn(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null){
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.TOKEN_SPAN_STR_FN.toString())) {
			if (runAssignmentCommandTokenSpanStrFn(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null) {
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.ARRAY.toString())) {
			if (constructOrRunCommandArray(assignment.getModifiers(), assignment.getName(), assignment.getValue()) == null) {
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.VALUE.toString())) {
			if (constructOrRunCommandValue(assignment.getModifiers(), assignment.getName(), assignment.getValue()) == null) {
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.SEARCH.toString())) {
			if (runAssignmentCommandSearch(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null) {
				return false;
			}
		} else {
			return false;
		}
		
		return true;
	}
	
	/* Generic object matching and commands */
	
	protected <T> T runCommand(List<String> modifiers, String referenceName, Obj.Function fnObj) {
		return this.dataTools.runCommand(this, modifiers, referenceName, fnObj);
	}
	
	protected <T> T runAssignmentCommand(String objectTypeStr, List<String> modifiers, String referenceName, Obj.Function fnObj, Map<String, T> storageMap) {
		T result = runCommand(modifiers, referenceName, fnObj);
		if (result == null)
			return null;
		
		if (referenceName == null) {
			// FIXME This works for now, but probably should disallow user-declared names that start with numbers to avoid conflicts
			String currentReferenceIdStr = String.valueOf(this.currentReferenceId);
			storageMap.put(currentReferenceIdStr, result);
			this.objNameOrdering.add(new Pair<String, String>(objectTypeStr, currentReferenceIdStr));
			this.currentReferenceId++;
		} else {
			storageMap.put(referenceName, result);
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<T> getAssignedMatches(Obj obj) {
		List<T> assignedMatches = new ArrayList<T>();
		if (obj.getObjType() == Obj.Type.VALUE) {
			for (Map<String, ?> map : this.storageMaps) {
				List<?> mapMatches = getAssignedMatches(obj, map);
				if (mapMatches.size() > 0)
					assignedMatches.addAll((List<T>)mapMatches);
			}
		}
		return assignedMatches;
	}
	
	protected <T> List<T> getAssignedMatches(Obj obj, Map<String, T> storageMap) {
		List<T> assignedMatches = new ArrayList<T>();
		if (obj.getObjType() == Obj.Type.VALUE) {
			Obj.Value vObj = (Obj.Value)obj;
			if (vObj.getType() == Obj.Value.Type.CURLY_BRACED) {
				String reference = vObj.getStr();
				if (reference.contains(".")) {
					String[] referenceParts = reference.split("\\.");
					String restOfReference = reference.substring(referenceParts[0].length() + 1);
					if (this.contexts.containsKey(referenceParts[0]))
						assignedMatches.addAll(this.contexts.get(referenceParts[0]).getAssignedMatches(Obj.curlyBracedValue(restOfReference)));
				} else if (storageMap.containsKey(reference)) {
					assignedMatches.add(storageMap.get(reference));
				} 
				
				if (assignedMatches.size() == 0 && this.parentContext != null)
					assignedMatches.addAll(this.parentContext.getAssignedMatches(vObj));
			}
		}
		
		return assignedMatches;
	}
	
	protected <T extends CtxParsableFunction> List<T> getFunctionMatches(Obj obj, Map<String, T> storageMap) {
		List<T> matches = getAssignedMatches(obj, storageMap);
		if (matches.size() >= 1)
			return matches;
		
		// FIXME For now this just works assuming we only need to resolve non-context references...
		
		Map<String, Obj> ctx = ((Obj.Function)(except(ObjectType.CONTEXT.toString()).toParse()))
									.getInternalAssignments().makeObjMap();
		obj.resolveValues(ctx);
		
		for (T item : storageMap.values()) {
			Map<String, Obj> matchMap = item.match(obj);
			if (matchMap.size() > 0)
				matches.add(item);	
		}
		
		return matches;
	}
	
	protected <T extends CtxParsableFunction> T getFunctionMatch(Obj obj, Map<String, T> storageMap) {
		List<T> matches = getFunctionMatches(obj, storageMap);
		
		if (matches.size() >= 1) {
			return matches.get(0);
		} else
			return null;
	}
	
	protected <T extends CtxParsableFunction> T getMatchOrRunCommand(String objectTypeStr, Obj obj, Map<String, T> storageMap) {
		return getMatchOrRunCommand(objectTypeStr, null, null, obj, storageMap);
	}
	
	protected <T extends CtxParsableFunction> T getMatchOrRunCommand(String objectTypeStr, List<String> modifiers, String referenceName, Obj obj, Map<String, T> storageMap) {
		List<T> matches = getFunctionMatches(obj, storageMap);
		if (matches.size() >= 1) {
			return matches.get(0);
		} else if (obj.getObjType() == Type.FUNCTION) {
			return runAssignmentCommand(objectTypeStr, modifiers, referenceName, (Obj.Function)obj, storageMap);
		} else {
			return null;
		}
	}
	
	/* Match and construct contexts */
	
	public Context getMatchContext(Obj obj) {
		synchronized (this.contexts) {
			return getFunctionMatch(obj, this.contexts);
		}
	}
	
	public List<Context> getMatchesContext(Obj obj) {
		synchronized (this.contexts) {
			return getFunctionMatches(obj, this.contexts);
		}
	}
	
	public Context getMatchOrRunCommandContext(String referenceName, Obj obj) {
		synchronized (this.contexts) {
			return getMatchOrRunCommand(ObjectType.CONTEXT.toString(), null, referenceName, obj, this.contexts);
		}
	}
	
	public Context getMatchOrRunCommandContext(Obj obj) {
		synchronized (this.contexts) {
			return getMatchOrRunCommand(ObjectType.CONTEXT.toString(), obj, this.contexts);
		}
	}
	
	private Context runAssignmentCommandContext(String referenceName, Obj.Function obj, List<String> modifiers) {
		synchronized (this.contexts) {
			return runAssignmentCommand(ObjectType.CONTEXT.toString(), modifiers, referenceName, obj, this.contexts);
		}
	}
	
	/* Match and construct token span fns */
	
	public Fn<TokenSpan, TokenSpan> getMatchTokenSpanFn(Obj obj) {
		synchronized (this.tokenSpanFns) {
			return getFunctionMatch(obj, this.tokenSpanFns);
		}
	}
	
	public List<Fn<TokenSpan, TokenSpan>> getMatchesTokenSpanFn(Obj obj) {
		synchronized (this.tokenSpanFns) {
			return getFunctionMatches(obj, this.tokenSpanFns);
		}
	}
	
	public Fn<TokenSpan, TokenSpan> getMatchOrRunCommandTokenSpanFn(String referenceName, Obj obj) {
		synchronized (this.tokenSpanFns) {
			return getMatchOrRunCommand(ObjectType.TOKEN_SPAN_FN.toString(), null, referenceName, obj, this.tokenSpanFns);
		}
	}
	
	public Fn<TokenSpan, TokenSpan> getMatchOrRunCommandTokenSpanFn(Obj obj) {
		synchronized (this.tokenSpanFns) {
			return getMatchOrRunCommand(ObjectType.TOKEN_SPAN_FN.toString(), obj, this.tokenSpanFns);
		}
	}
	
	private Fn<TokenSpan, TokenSpan> runAssignmentCommandTokenSpanFn(String referenceName, Obj.Function obj, List<String> modifiers) {
		synchronized (this.tokenSpanFns) {
			return runAssignmentCommand(ObjectType.TOKEN_SPAN_FN.toString(), modifiers, referenceName, obj, this.tokenSpanFns);
		}
	}
	
	/* Match and construct str fns */
	
	public Fn<String, String> getMatchStrFn(Obj obj) {
		synchronized (this.strFns) {
			return getFunctionMatch(obj, this.strFns);
		}
	}
	
	public List<Fn<String, String>> getMatchesStrFn(Obj obj) {
		synchronized (this.strFns) {
			return getFunctionMatches(obj, this.strFns);
		}
	}

	public Fn<String, String> getMatchOrRunCommandStrFn(String referenceName, Obj obj) {
		synchronized (this.strFns) {
			return getMatchOrRunCommand(ObjectType.STR_FN.toString(), null, referenceName, obj, this.strFns);
		}
	}
	
	public Fn<String, String> getMatchOrRunCommandStrFn(Obj obj) {
		synchronized (this.strFns) {
			return getMatchOrRunCommand(ObjectType.STR_FN.toString(), obj, this.strFns);
		}
	}
	
	private Fn<String, String> runAssignmentCommandStrFn(String referenceName, Obj.Function obj, List<String> modifiers) {
		synchronized (this.strFns) {
			return runAssignmentCommand(ObjectType.STR_FN.toString(), modifiers, referenceName, obj, this.strFns);
		}
	}
	
	/* Match and construct token span str fns */
	
	public Fn<TokenSpan, String> getMatchTokenSpanStrFn(Obj obj) {
		synchronized (this.tokenSpanStrFns) {
			return getFunctionMatch(obj, this.tokenSpanStrFns);
		}
	}
	
	public List<Fn<TokenSpan, String>> getMatchesTokenSpanStrFn(Obj obj) {
		synchronized (this.tokenSpanStrFns) {
			return getFunctionMatches(obj, this.tokenSpanStrFns);
		}
	}

	public Fn<TokenSpan, String> getMatchOrRunCommandTokenSpanStrFn(String referenceName, Obj obj) {
		synchronized (this.tokenSpanStrFns) {
			return getMatchOrRunCommand(ObjectType.TOKEN_SPAN_STR_FN.toString(), null, referenceName, obj, this.tokenSpanStrFns);
		}
	}
	
	public Fn<TokenSpan, String> getMatchOrConstructTokenSpanStrFn(Obj obj) {
		synchronized (this.tokenSpanStrFns) {
			return getMatchOrRunCommand(ObjectType.TOKEN_SPAN_STR_FN.toString(), obj, this.tokenSpanStrFns);
		}
	}
	
	private Fn<TokenSpan, String> runAssignmentCommandTokenSpanStrFn(String referenceName, Obj.Function obj, List<String> modifiers) {
		synchronized (this.tokenSpanStrFns) {
			return runAssignmentCommand(ObjectType.TOKEN_SPAN_STR_FN.toString(), modifiers, referenceName, obj, this.tokenSpanStrFns);
		}
	}
	
	/* Match and construct arrays */
	
	public List<String> constructOrRunCommandArray(Obj obj) {
		return constructOrRunCommandArray(null, null, obj);
	}
	
	public List<String> getMatchArray(Obj obj) {
		List<List<String>> matches = this.getAssignedMatches(obj, this.arrays);
		if (matches.size() > 0)
			return matches.get(0);
		else if (obj.getObjType() == Obj.Type.ARRAY) {
			Obj.Array arrayObj = (Obj.Array)obj;
			return arrayObj.toList(this.values);
		}
		
		return null;
	}
	
	private List<String> constructOrRunCommandArray(List<String> modifiers, String referenceName, Obj obj) {
		if (obj.getObjType() == Obj.Type.FUNCTION) {
			return runAssignmentCommand(ObjectType.ARRAY.toString(), modifiers, referenceName, (Obj.Function)obj, this.arrays);
		}	
		
		if (obj.getObjType() != Obj.Type.ARRAY) {
			this.dataTools.getOutputWriter().debugWriteln("ERROR: Invalid object type for array construction (" + obj.getObjType() + ").");
			return null;
		}
		
		Obj.Array arrObj = (Obj.Array)obj;
		List<String> array = arrObj.toList(this.values);
		if (array == null) {
			this.dataTools.getOutputWriter().debugWriteln("ERROR: Failed to construct array '" + ((referenceName != null) ? referenceName : "") + "'");
			return null;
		}
		
		if (referenceName == null) {
			this.arrays.put(String.valueOf(this.currentReferenceId), array);
			this.currentReferenceId++;
		} else {
			this.arrays.put(referenceName, array);
		}

		return array;
	}
	
	/* Match and construct values */
	
	public String getMatchValue(Obj obj) {
		List<String> matches = this.getAssignedMatches(obj, this.values);
		if (matches.size() > 0)
			return matches.get(0);
		
		if (obj.getObjType() == Obj.Type.VALUE) {
			Obj.Value vObj = (Obj.Value)obj;
			if (vObj.getType() == Obj.Value.Type.STRING) {
				return vObj.getValueStr(this.values);
			}
		}
		
		return null;
	}
	
	public String constructOrRunCommandValue(Obj obj) {
		return constructOrRunCommandValue(null, null, obj);
	}
	
	private String constructOrRunCommandValue(List<String> modifiers, String referenceName, Obj obj) {
		if (obj.getObjType() == Obj.Type.FUNCTION) {
			return runAssignmentCommand(ObjectType.VALUE.toString(), modifiers, referenceName, (Obj.Function)obj, this.values);
		}
		
		if (obj.getObjType() != Obj.Type.VALUE) {
			this.dataTools.getOutputWriter().debugWriteln("ERROR: Invalid object type for value construction (" + obj.getObjType() + ").");
			return null;
		}
		
		Obj.Value vObj = (Obj.Value)obj;
		String value = vObj.getValueStr(this.values);
		if (value == null) {
			this.dataTools.getOutputWriter().debugWriteln("ERROR: Failed to construct value '" + ((referenceName != null) ? referenceName : "") + "'");
			return null;
		}
		
		if (referenceName == null) {
			this.values.put(String.valueOf(this.currentReferenceId), value);
			this.currentReferenceId++;
		} else {
			this.values.put(referenceName, value);
		}

		return value;
	}
	
	/* Match and construct searches */
	
	public Search getMatchSearch(Obj obj) {
		synchronized (this.searches) {
			return getFunctionMatch(obj, this.searches);
		}
	}
	
	public List<Search> getMatchesSearch(Obj obj) {
		synchronized (this.searches) {
			return getFunctionMatches(obj, this.searches);
		}
	}
	
	private Search runAssignmentCommandSearch(String referenceName, Obj.Function obj, List<String> modifiers) {
		synchronized (this.searches) {
			return runAssignmentCommand(ObjectType.SEARCH.toString(), modifiers, referenceName, obj, this.searches);
		}
	}
	
	/* Match parameter searchable */
	
	public ParameterSearchable getMatchParameterSearchable(Obj obj) {
		return null;
	}
	
	/* Add objects */
	
	public synchronized boolean addValue(String name, String value) {
		this.values.put(name, value);
		return true;
	}
	
	/* Get objects */
	
	public int getMaxThreads() {
		String maxThreadsStr = getMatchValue(Obj.curlyBracedValue("maxThreads"));
		if (maxThreadsStr != null)
			return Integer.valueOf(maxThreadsStr);
		
		if (this.dataTools.getProperties() != null && this.dataTools.getProperties().getMaxThreads() != null)
			return this.dataTools.getProperties().getMaxThreads();
		return 1;	
	}
	
	public int getRandomSeed() {
		String randomSeedStr = getMatchValue(Obj.curlyBracedValue("randomSeed"));
		if (randomSeedStr != null)
			return Integer.valueOf(randomSeedStr);
		
		if (this.dataTools.getProperties() != null && this.dataTools.getProperties().getRandomSeed() != null)
			return this.dataTools.getProperties().getRandomSeed();
		return 1;	
	}
	
	public int getIntValue(String name) {
		return Integer.valueOf(getMatchValue(Obj.curlyBracedValue(name)));
	}
	
	public boolean getBooleanValue(String name) {
		return Boolean.valueOf(getMatchValue(Obj.curlyBracedValue(name)));
	}

	public double getDoubleValue(String name) {
		return Double.valueOf(getMatchValue(Obj.curlyBracedValue(name)));
	}
	
	public String getStringValue(String name) {
		return getMatchValue(Obj.curlyBracedValue(name));
	}
	
	public List<String> getStringArray(String name) {
		return getMatchArray(Obj.curlyBracedValue(name));
	}
	
	/* Other stuff */
	
	public DataTools getDataTools() {
		return this.dataTools;
	}
	
	public synchronized Context clone(boolean cloneBigInternals) {
		Context clone = new Context(this.dataTools, this.genericName, this.parentContext);
		
		if (cloneBigInternals) {
			if (!clone.fromParse(getModifiers(), getReferenceName(), toParse()))
				return null;
		} else {
			if (!clone.fromParse(getModifiers(), getReferenceName(), this.except(ObjectType.CONTEXT.toString()).toParse()))
				return null;
			
			for (Entry<String, Context> entry : this.contexts.entrySet()) {
				clone.contexts.put(entry.getKey(), entry.getValue().clone(false));
				clone.objNameOrdering.add(new Pair<String, String>(ObjectType.CONTEXT.toString(), entry.getKey()));
			}
		}
		
		return clone;
	}
	
	public Context only(String objectTypeStr) {
		Context only = new Context(this.dataTools, this.genericName, this.parentContext);
		
		for (Pair<String, String> objName : this.objNameOrdering)
			if (objName.getFirst().equals(objectTypeStr))
				only.objNameOrdering.add(objName);
		
		return onlyHelper(only, objectTypeStr);
	}
	
	protected Context onlyHelper(Context only, String objectTypeStr) {
		if (objectTypeStr.equals(ObjectType.CONTEXT.toString())) {
			only.contexts = this.contexts;
		} else if (objectTypeStr.equals(ObjectType.TOKEN_SPAN_FN.toString())) {
			only.tokenSpanFns = this.tokenSpanFns;			
		} else if (objectTypeStr.equals(ObjectType.STR_FN.toString())) {
			only.strFns = this.strFns;
		} else if (objectTypeStr.equals(ObjectType.TOKEN_SPAN_STR_FN.toString())) {
			only.tokenSpanStrFns = this.tokenSpanStrFns;
		} else if (objectTypeStr.equals(ObjectType.ARRAY.toString())) {
			for (Entry<String, List<String>> entry : this.arrays.entrySet())
				only.arrays.put(entry.getKey(), entry.getValue());	
		} else if (objectTypeStr.equals(ObjectType.VALUE.toString())) {
			for (Entry<String, String> entry : this.values.entrySet())
				only.values.put(entry.getKey(), entry.getValue());	
		} else if (objectTypeStr.equals(ObjectType.SEARCH.toString())) {
			only.searches = this.searches;
		}
		
		return only;
	}

	public Context except(String objectTypeStr) {
		Context except = new Context(this.dataTools, this.genericName, this.parentContext);
		
		for (Pair<String, String> objName : this.objNameOrdering) {
			if (!objectTypeStr.equals(objName.getFirst()))
				except.objNameOrdering.add(objName);
		}
		
		return exceptHelper(except, objectTypeStr);
	}
	
	protected Context exceptHelper(Context except, String objectTypeStr) {
		if (!objectTypeStr.equals(ObjectType.CONTEXT.toString())) {
			except.contexts = this.contexts;
		}
		
		if (!objectTypeStr.equals(ObjectType.TOKEN_SPAN_FN.toString())) {
			except.tokenSpanFns = this.tokenSpanFns;
		} 
		
		if (!objectTypeStr.equals(ObjectType.STR_FN.toString())) {
			except.strFns = this.strFns;	
		} 
		
		if (!objectTypeStr.equals(ObjectType.TOKEN_SPAN_STR_FN.toString())) {
			except.tokenSpanStrFns = this.tokenSpanStrFns;
		} 
		
		if (!objectTypeStr.equals(ObjectType.ARRAY.toString())) {
			for (Entry<String, List<String>> entry : this.arrays.entrySet())
				except.arrays.put(entry.getKey(), entry.getValue());	
		}
		
		if (!objectTypeStr.equals(ObjectType.VALUE.toString())) {
			for (Entry<String, String> entry : this.values.entrySet())
				except.values.put(entry.getKey(), entry.getValue());	
		}
		
		if (!objectTypeStr.equals(ObjectType.SEARCH.toString())) {
			for (Entry<String, Search> entry : this.searches.entrySet())
				except.searches.put(entry.getKey(), entry.getValue());
		}
		
		return except;
	}
	
	public static Context run(DataTools dataTools, File file) {
		return run(file.getName(), dataTools, FileUtil.getFileReader(file.getAbsolutePath()));
	}
	
	public static Context run(String referenceName, DataTools dataTools, String str) {
		return run(referenceName, dataTools, new StringReader(str));
	}
	
	public static Context run(String referenceName, DataTools dataTools, Reader reader) {
		CtxScanner scanner = new CtxScanner(reader);
		CtxParser parser = new CtxParser(scanner, new ComplexSymbolFactory());
		AssignmentList parse = null;
		try {
			parse = (AssignmentList)parser.parse().value;
		} catch (Exception e) {
			return null;
		}
		
		Context context = new Context(dataTools);
		if (!context.fromParse(Obj.function(referenceName, new AssignmentList(), parse)))
			return null;
		return context;
	}

	@Override
	public String[] getParameterNames() {
		return new String[0];
	}

	@Override
	public Obj getParameterValue(String parameter) {
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		return true;
	}
	
	@Override
	public String getGenericName() {
		return this.genericName;
	}
	
	public Context makeInstance(Context parentContext) {
		return new Context(this.dataTools, this.genericName, parentContext);
	}
}
