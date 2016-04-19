package edu.cmu.ml.rtw.generic.data;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
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
import edu.cmu.ml.rtw.generic.rule.Rule;
import edu.cmu.ml.rtw.generic.rule.RuleSet;
import edu.cmu.ml.rtw.generic.structure.WeightedStructure;
import edu.cmu.ml.rtw.generic.task.classify.multi.EvaluationMultiClassification;
import edu.cmu.ml.rtw.generic.task.classify.multi.MethodMultiClassification;
import edu.cmu.ml.rtw.generic.task.classify.multi.TaskMultiClassification;
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
		STRUCTURE_FN("structure_fn"),
		RULE("rule"),
		RULE_SET("rule_set"),
		ARRAY("array"),
		VALUE("value"),
		SEARCH("search"),
		MULTI_CLASSIFY_TASK("multi_classify_task"),
		MULTI_CLASSIFY_METHOD("multi_classify_method"),
		MULTI_CLASSIFY_EVAL("multi_classify_eval");
		
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
	
	private boolean initOverrideByName = false;
	private String initScript;
	private boolean initOnce = true;
	
	protected DataTools dataTools;
	protected String genericName;
	protected Context parentContext;
	
	protected List<Pair<String, String>> objNameOrdering;
	protected List<Map<String, ?>> storageMaps;
	
	protected Map<String, Context> contexts;
	protected Map<String, Fn<TokenSpan, TokenSpan>> tokenSpanFns;
	protected Map<String, Fn<String, String>> strFns;
	protected Map<String, Fn<TokenSpan, String>> tokenSpanStrFns;
	protected Map<String, Fn<?, ?>> structureFns;
	protected Map<String, Rule> rules;
	protected Map<String, RuleSet> ruleSets;
	protected Map<String, List<String>> arrays;
	protected Map<String, String> values;
	protected Map<String, Search> searches;
	private Map<String, MethodMultiClassification> multiClassifyMethods;
	private Map<String, TaskMultiClassification> multiClassifyTasks;
	private Map<String, EvaluationMultiClassification<?>> multiClassifyEvals;
	
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
		this.structureFns = new ConcurrentHashMap<String, Fn<?, ?>>();
		this.rules = new ConcurrentHashMap<String, Rule>();
		this.ruleSets = new ConcurrentHashMap<String, RuleSet>();
		this.arrays = new TreeMap<String, List<String>>();
		this.values = new TreeMap<String, String>();
		this.searches = new ConcurrentHashMap<String, Search>();
		this.multiClassifyMethods = new ConcurrentHashMap<String, MethodMultiClassification>();
		this.multiClassifyTasks = new ConcurrentHashMap<String, TaskMultiClassification>();
		this.multiClassifyEvals = new ConcurrentHashMap<String, EvaluationMultiClassification<?>>();
		
		this.storageMaps.add(this.contexts);
		this.storageMaps.add(this.tokenSpanFns);
		this.storageMaps.add(this.strFns);
		this.storageMaps.add(this.tokenSpanStrFns);
		this.storageMaps.add(this.structureFns);
		this.storageMaps.add(this.rules);
		this.storageMaps.add(this.ruleSets);
		this.storageMaps.add(this.arrays);
		this.storageMaps.add(this.values);
		this.storageMaps.add(this.searches);
		this.storageMaps.add(this.multiClassifyEvals);
		this.storageMaps.add(this.multiClassifyMethods);
		this.storageMaps.add(this.multiClassifyTasks);
		
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
		} else if (type == ObjectType.STRUCTURE_FN) {
			return Assignment.assignmentTyped(this.structureFns.get(obj.getSecond()).getModifiers(), ObjectType.STRUCTURE_FN.toString(), obj.getSecond(), this.structureFns.get(obj.getSecond()).toParse());	
		} else if (type == ObjectType.RULE) {
			return Assignment.assignmentTyped(this.rules.get(obj.getSecond()).getModifiers(), ObjectType.RULE.toString(), obj.getSecond(), this.rules.get(obj.getSecond()).toParse());	
		} else if (type == ObjectType.RULE_SET) {
			return Assignment.assignmentTyped(this.ruleSets.get(obj.getSecond()).getModifiers(), ObjectType.RULE_SET.toString(), obj.getSecond(), this.ruleSets.get(obj.getSecond()).toParse());	
		} else if (type == ObjectType.ARRAY) {
			return Assignment.assignmentTyped(new ArrayList<String>(), ObjectType.ARRAY.toString(), obj.getSecond(), Obj.array(this.arrays.get(obj.getSecond())));
		} else if (type == ObjectType.VALUE) {
			return Assignment.assignmentTyped(new ArrayList<String>(), ObjectType.VALUE.toString(), obj.getSecond(), Obj.stringValue(this.values.get(obj.getSecond())));
		} else if (type == ObjectType.SEARCH) {
			return Assignment.assignmentTyped(new ArrayList<String>(), ObjectType.SEARCH.toString(), obj.getSecond(), this.searches.get(obj.getSecond()).toParse());
		} else if (type == ObjectType.MULTI_CLASSIFY_EVAL) {
			return Assignment.assignmentTyped(this.multiClassifyEvals.get(obj.getSecond()).getModifiers(), ObjectType.MULTI_CLASSIFY_EVAL.toString(), obj.getSecond(), this.multiClassifyEvals.get(obj.getSecond()).toParse());	
		} else if (type == ObjectType.MULTI_CLASSIFY_TASK) {
			return Assignment.assignmentTyped(this.multiClassifyTasks.get(obj.getSecond()).getModifiers(), ObjectType.MULTI_CLASSIFY_TASK.toString(), obj.getSecond(), this.multiClassifyTasks.get(obj.getSecond()).toParse());	
		} else if (type == ObjectType.MULTI_CLASSIFY_METHOD) {
			return Assignment.assignmentTyped(this.multiClassifyMethods.get(obj.getSecond()).getModifiers(), ObjectType.MULTI_CLASSIFY_METHOD.toString(), obj.getSecond(), this.multiClassifyMethods.get(obj.getSecond()).toParse());	
		} else {
			return null;
		}
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		if (internalAssignments == null && this.initScript != null) {
			CtxScanner scanner = new CtxScanner(
				FileUtil.getFileReader(
					new File(this.dataTools.getProperties().getContextDirectory(), this.initScript).getAbsolutePath()
				)
			);
			
			CtxParser parser = new CtxParser(scanner, new ComplexSymbolFactory());
			try {
				internalAssignments = (AssignmentList)parser.parse().value;
			} catch (Exception e) {
				return false;
			}
		}
		
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
		} else if (assignment.getType().equals(ObjectType.STRUCTURE_FN.toString())) {
			if (runAssignmentCommandStructureFn(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null) {
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.RULE.toString())) {
			if (runAssignmentCommandRule(assignment.getName(), (Obj.Rule)assignment.getValue(), assignment.getModifiers()) == null) {
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.RULE_SET.toString())) {
			if (runAssignmentCommandRuleSet(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null) {
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
		} else if (assignment.getType().equals(ObjectType.MULTI_CLASSIFY_EVAL.toString())) {
			if (runAssignmentCommandMultiClassifyEval(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null) {
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.MULTI_CLASSIFY_TASK.toString())) {
			if (runAssignmentCommandMultiClassifyTask(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null) {
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.MULTI_CLASSIFY_METHOD.toString())) {
			if (runAssignmentCommandMultiClassifyMethod(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null) {
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
	
	public <T> List<T> getAssignedMatches(Obj obj) {
		return getAssignedMatches(obj, null);
	}
	
	@SuppressWarnings("unchecked")
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
				} else if (storageMap != null && storageMap.containsKey(reference)) {
					assignedMatches.add(storageMap.get(reference));
				} else if (storageMap == null){
					for (Map<String, ?> map : this.storageMaps) {
						if (map.containsKey(reference))
							assignedMatches.add((T)map.get(reference));
					}
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
	
	/* Match and construct structure fns */
	
	public Fn<?, ?> getMatchStructureFn(Obj obj) {
		synchronized (this.structureFns) {
			return getFunctionMatch(obj, this.structureFns);
		}
	}
	
	public List<Fn<?, ?>> getMatchesStructureFn(Obj obj) {
		synchronized (this.structureFns) {
			return getFunctionMatches(obj, this.structureFns);
		}
	}

	public Fn<?, ?> getMatchOrRunCommandStructureFn(String referenceName, Obj obj) {
		synchronized (this.structureFns) {
			return getMatchOrRunCommand(ObjectType.STRUCTURE_FN.toString(), null, referenceName, obj, this.structureFns);
		}
	}
	
	public Fn<?, ?> getMatchOrConstructStructureFn(Obj obj) {
		synchronized (this.structureFns) {
			return getMatchOrRunCommand(ObjectType.STRUCTURE_FN.toString(), obj, this.structureFns);
		}
	}
	
	private Fn<?, ?> runAssignmentCommandStructureFn(String referenceName, Obj.Function obj, List<String> modifiers) {
		synchronized (this.structureFns) {
			return runAssignmentCommand(ObjectType.STRUCTURE_FN.toString(), modifiers, referenceName, obj, this.structureFns);
		}
	}
	
	/* Match and construct rules */
	
	public Rule getMatchRule(Obj obj) {
		synchronized (this.rules) {
			return getAssignedMatches(obj, this.rules).get(0);
		}
	}
	
	private Rule runAssignmentCommandRule(String referenceName, Obj.Rule obj, List<String> modifiers) {
		synchronized (this.rules) {
			Rule rule = new Rule();
			if (!rule.fromParse(obj))
				return null;
			this.rules.put(referenceName, rule);
			return rule;
		}
	}
	
	/* Match and construct rule sets */
	
	public RuleSet getMatchRuleSet(Obj obj) {
		synchronized (this.ruleSets) {
			return getFunctionMatch(obj, this.ruleSets);
		}
	}
	
	public List<RuleSet> getMatchesRuleSet(Obj obj) {
		synchronized (this.ruleSets) {
			return getFunctionMatches(obj, this.ruleSets);
		}
	}
	
	private RuleSet runAssignmentCommandRuleSet(String referenceName, Obj.Function obj, List<String> modifiers) {
		synchronized (this.ruleSets) {
			return runAssignmentCommand(ObjectType.RULE_SET.toString(), modifiers, referenceName, obj, this.ruleSets);
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
	
	/* Match and construct multi-classify methods */
	
	public MethodMultiClassification getMatchMultiClassifyMethod(Obj obj) {
		return getFunctionMatch(obj, this.multiClassifyMethods);
	}
	
	private MethodMultiClassification runAssignmentCommandMultiClassifyMethod(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.MULTI_CLASSIFY_METHOD.toString(), modifiers, referenceName, obj, this.multiClassifyMethods);
	}
	
	/* Match and construct classify tasks */
	
	public TaskMultiClassification getMatchMultiClassifyTask(Obj obj) {
		return getFunctionMatch(obj, this.multiClassifyTasks);
	}
	
	private TaskMultiClassification runAssignmentCommandMultiClassifyTask(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.MULTI_CLASSIFY_TASK.toString(), modifiers, referenceName, obj, this.multiClassifyTasks);
	}

	/* Match and construct classify eval */
	
	public EvaluationMultiClassification<?> getMatchMultiClassifyEval(Obj obj) {
		return getFunctionMatch(obj, this.multiClassifyEvals);
	}
	
	private EvaluationMultiClassification<?> runAssignmentCommandMultiClassifyEval(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.MULTI_CLASSIFY_EVAL.toString(), modifiers, referenceName, obj, this.multiClassifyEvals);
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
	
	public WeightedStructure constructMatchWeightedStructure(Obj obj) {
		Obj.Function f = (Obj.Function)obj;
		WeightedStructure s = this.dataTools.makeWeightedStructure(f.getName(), this);
		if (!s.fromParse(f))
			return null;
		return s;
	}
	
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
		} else if (objectTypeStr.equals(ObjectType.STRUCTURE_FN.toString())) {
			only.structureFns = this.structureFns;
		} else if (objectTypeStr.equals(ObjectType.ARRAY.toString())) {
			for (Entry<String, List<String>> entry : this.arrays.entrySet())
				only.arrays.put(entry.getKey(), entry.getValue());	
		} else if (objectTypeStr.equals(ObjectType.VALUE.toString())) {
			for (Entry<String, String> entry : this.values.entrySet())
				only.values.put(entry.getKey(), entry.getValue());	
		} else if (objectTypeStr.equals(ObjectType.SEARCH.toString())) {
			only.searches = this.searches;
		} else if (objectTypeStr.equals(ObjectType.MULTI_CLASSIFY_EVAL.toString())) {
			only.multiClassifyEvals = this.multiClassifyEvals;
		} else if (objectTypeStr.equals(ObjectType.MULTI_CLASSIFY_TASK.toString())) {
			only.multiClassifyTasks = this.multiClassifyTasks;
		} else if (objectTypeStr.equals(ObjectType.MULTI_CLASSIFY_METHOD.toString())) {
			only.multiClassifyMethods = this.multiClassifyMethods;
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
		
		if (!objectTypeStr.equals(ObjectType.STRUCTURE_FN.toString())) {
			except.structureFns = this.structureFns;
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
		
		if (!objectTypeStr.equals(ObjectType.MULTI_CLASSIFY_EVAL.toString())) {
			except.multiClassifyEvals = this.multiClassifyEvals;
		}
		
		if (!objectTypeStr.equals(ObjectType.MULTI_CLASSIFY_METHOD.toString())) {
			except.multiClassifyMethods = this.multiClassifyMethods;
		} 
		
		if (!objectTypeStr.equals(ObjectType.MULTI_CLASSIFY_TASK.toString())) {
			except.multiClassifyTasks = this.multiClassifyTasks;
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
		return new String[] { "initScript", "initOnce", "initOverrideByName" };
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("initScript"))
			return Obj.stringValue(this.initScript);
		else if (parameter.equals("initOnce"))
			return Obj.stringValue(String.valueOf(this.initOnce));
		else if (parameter.equals("initOverrideByName"))
			return Obj.stringValue(String.valueOf(this.initOverrideByName));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("initScript"))
			this.initScript = (this.parentContext == null || parameterValue == null) ? null : this.parentContext.getMatchValue(parameterValue);
		else if (parameter.equals("initOnce"))
			this.initOnce = (this.parentContext == null || parameterValue == null) ? true : Boolean.valueOf(this.parentContext.getMatchValue(parameterValue));
		else if (parameter.equals("initOverrideByName"))
			this.initOverrideByName = (this.parentContext == null || parameterValue == null) ? false :  Boolean.valueOf(this.parentContext.getMatchValue(parameterValue));
		else 
			return false;
		
		return true;
	}
	
	@Override
	public String getGenericName() {
		return this.genericName;
	}
	
	public Context makeInstance(Context parentContext) {
		return new Context(this.dataTools, this.genericName, parentContext);
	}
	
	public Context getInitOnceContextForScript(String initScript) {
		Set<Context> visited = new HashSet<Context>();
		Stack<Pair<Boolean, Context>> toVisit = new Stack<Pair<Boolean, Context>>();
		toVisit.push(new Pair<Boolean, Context>(true, this));
		
		while (!toVisit.isEmpty()) {
			Pair<Boolean, Context> cur = toVisit.pop();
			Context curContext = cur.getSecond();
			boolean curFromBelow = cur.getFirst();
			
			if (curContext.initOnce && curContext.initScript != null && curContext.initScript.equals(initScript)) {
				return curContext;
			}
			
			if (curContext.initOnce || curFromBelow) {
				for (Context child : curContext.contexts.values())
					if (!visited.contains(child))
						toVisit.push(new Pair<Boolean, Context>(false, child));
			}
			
			if (curContext.parentContext != null && !visited.contains(curContext.parentContext))
				toVisit.add(new Pair<Boolean, Context>(true, curContext.parentContext));
			
			visited.add(curContext);
		}
		
		return null;
	}
	
	public Context getInitOnceContextForName(String name) {
		Set<Context> visited = new HashSet<Context>();
		Stack<Pair<Boolean, Context>> toVisit = new Stack<Pair<Boolean, Context>>();
		toVisit.push(new Pair<Boolean, Context>(true, this));
		
		while (!toVisit.isEmpty()) {
			Pair<Boolean, Context> cur = toVisit.pop();
			Context curContext = cur.getSecond();
			boolean curFromBelow = cur.getFirst();
			
			if (curContext.initOnce && curContext.referenceName != null && curContext.referenceName.equals(name)) {
				return curContext;
			}
			
			if (curContext.initOnce || curFromBelow) {
				for (Context child : curContext.contexts.values())
					if (!visited.contains(child))
						toVisit.push(new Pair<Boolean, Context>(false, child));
			}
			
			if (curContext.parentContext != null && !visited.contains(curContext.parentContext))
				toVisit.add(new Pair<Boolean, Context>(true, curContext.parentContext));
			
			visited.add(curContext);
		}
		
		
		return null;
	}
}
