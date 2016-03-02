package edu.cmu.ml.rtw.generic.data.annotation;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import java_cup.runtime.ComplexSymbolFactory;
import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.data.feature.FeatureSet;
import edu.cmu.ml.rtw.generic.data.feature.rule.RuleSet;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.evaluation.GridSearch;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.opt.search.ParameterSearchable;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.CtxParser;
import edu.cmu.ml.rtw.generic.parse.CtxScanner;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassification;
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassificationMeasure;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.cmu.ml.rtw.generic.task.classify.TaskClassification;
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassification.Type;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;

public class DatumContext<D extends Datum<L>, L> extends Context {	
	public enum ObjectType {
		DATA("data"),
		MODEL("model"),
		FEATURE("feature"),
		GRID_SEARCH("gs"),
		EVALUATION("evaluation"),
		RULE_SET("rs"),
		FEATURE_SET("feature_set"),
		DATA_FEATURES("data_features"),
		CLASSIFY_TASK("classify_task"),
		CLASSIFY_METHOD("classify_method"),
		CLASSIFY_EVAL("classify_eval");
		
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
	
	private Datum.Tools<D, L> datumTools;
	
	private Map<String, DataSet<D, L>> data;
	private Map<String, SupervisedModel<D, L>> models;
	private Map<String, Feature<D, L>> features;
	private Map<String, GridSearch<D, L>> gridSearches;
	private Map<String, SupervisedModelEvaluation<D, L>> evaluations;
	private Map<String, RuleSet<D, L>> ruleSets;
	private Map<String, FeatureSet<D, L>> featureSets;
	private Map<String, DataFeatureMatrix<D, L>> dataFeatures;
	private Map<String, MethodClassification<D, L>> classifyMethods;
	private Map<String, TaskClassification<D, L>> classifyTasks;
	private Map<String, EvaluationClassification<D, L, ?>> classifyEvals;
	
	public DatumContext(Datum.Tools<D, L> datumTools) {
		this(datumTools, "DatumContext");
	}
	
	public DatumContext(Datum.Tools<D, L> datumTools, String genericName) {
		this(datumTools, genericName, null);
	}
	
	public DatumContext(Datum.Tools<D, L> datumTools, String genericName, Context parentContext) {
		super(datumTools.getDataTools(), genericName, parentContext);
		
		this.datumTools = datumTools;
		
		this.data = new TreeMap<String, DataSet<D, L>>();
		this.models = new TreeMap<String, SupervisedModel<D, L>>();
		this.features = new TreeMap<String, Feature<D, L>>();
		this.gridSearches = new TreeMap<String, GridSearch<D, L>>();
		this.evaluations = new TreeMap<String, SupervisedModelEvaluation<D, L>>();
		this.ruleSets = new TreeMap<String, RuleSet<D, L>>();
		this.featureSets = new TreeMap<String, FeatureSet<D, L>>();
		this.dataFeatures = new TreeMap<String, DataFeatureMatrix<D, L>>();
		this.classifyMethods = new TreeMap<String, MethodClassification<D, L>>();
		this.classifyTasks = new TreeMap<String, TaskClassification<D, L>>();
		this.classifyEvals = new TreeMap<String, EvaluationClassification<D, L, ?>>();
		
		this.storageMaps.add(this.data);
		this.storageMaps.add(this.models);
		this.storageMaps.add(this.features);
		this.storageMaps.add(this.gridSearches);
		this.storageMaps.add(this.evaluations);
		this.storageMaps.add(this.ruleSets);
		this.storageMaps.add(this.featureSets);
		this.storageMaps.add(this.dataFeatures);
		this.storageMaps.add(this.classifyMethods);
		this.storageMaps.add(this.classifyTasks);
		this.storageMaps.add(this.classifyEvals);
	}
	
	public DatumContext(Datum.Tools<D, L> datumTools, List<Feature<D, L>> features) {
		this(datumTools);
		
		for (int i = 0; i < features.size(); i++) {
			this.objNameOrdering.add(new Pair<String, String>(ObjectType.FEATURE.toString(), features.get(i).getReferenceName()));
			this.features.put(features.get(i).getReferenceName(), features.get(i));
		}
	}
	
	@Override
	public List<String> getModifiers() {
		return new ArrayList<String>();
	}

	@Override
	protected Assignment toAssignment(Pair<String, String> obj) {
		Assignment assignment = super.toAssignment(obj);
		if (assignment != null)
			return assignment;
		
		ObjectType type = ObjectType.fromString(obj.getFirst());
		if (type == ObjectType.DATA) { 
			return Assignment.assignmentTyped(this.data.get(obj.getSecond()).getModifiers(), ObjectType.DATA.toString(), obj.getSecond(), this.data.get(obj.getSecond()).toParse());
		} else if (type == ObjectType.MODEL) {
			return Assignment.assignmentTyped(this.models.get(obj.getSecond()).getModifiers(), ObjectType.MODEL.toString(), obj.getSecond(), this.models.get(obj.getSecond()).toParse());
		} else if (type == ObjectType.FEATURE) {
			return Assignment.assignmentTyped(this.features.get(obj.getSecond()).getModifiers(), ObjectType.FEATURE.toString(), obj.getSecond(), this.features.get(obj.getSecond()).toParse());
		} else if (type == ObjectType.GRID_SEARCH) {
			return Assignment.assignmentTyped(this.gridSearches.get(obj.getSecond()).getModifiers(), ObjectType.GRID_SEARCH.toString(), obj.getSecond(), this.gridSearches.get(obj.getSecond()).toParse());				
		} else if (type == ObjectType.EVALUATION) {
			return Assignment.assignmentTyped(this.evaluations.get(obj.getSecond()).getModifiers(), ObjectType.EVALUATION.toString(), obj.getSecond(), this.evaluations.get(obj.getSecond()).toParse());
		} else if (type == ObjectType.RULE_SET) {
			return Assignment.assignmentTyped(this.ruleSets.get(obj.getSecond()).getModifiers(), ObjectType.RULE_SET.toString(), obj.getSecond(), this.ruleSets.get(obj.getSecond()).toParse());
		} else if (type == ObjectType.FEATURE_SET) {
			return Assignment.assignmentTyped(this.featureSets.get(obj.getSecond()).getModifiers(), ObjectType.FEATURE_SET.toString(), obj.getSecond(), this.featureSets.get(obj.getSecond()).toParse());
		} else if (type == ObjectType.DATA_FEATURES) {
			return Assignment.assignmentTyped(this.dataFeatures.get(obj.getSecond()).getModifiers(), ObjectType.DATA_FEATURES.toString(), obj.getSecond(), this.dataFeatures.get(obj.getSecond()).toParse());
		} else if (type == ObjectType.CLASSIFY_METHOD) {
			return Assignment.assignmentTyped(this.classifyMethods.get(obj.getSecond()).getModifiers(), ObjectType.CLASSIFY_METHOD.toString(), obj.getSecond(), this.classifyMethods.get(obj.getSecond()).toParse());
		} else if (type == ObjectType.CLASSIFY_TASK) {
			return Assignment.assignmentTyped(this.classifyTasks.get(obj.getSecond()).getModifiers(), ObjectType.CLASSIFY_TASK.toString(), obj.getSecond(), this.classifyTasks.get(obj.getSecond()).toParse());
		} else if (type == ObjectType.CLASSIFY_EVAL) {
			return Assignment.assignmentTyped(this.classifyEvals.get(obj.getSecond()).getModifiers(), ObjectType.CLASSIFY_EVAL.toString(), obj.getSecond(), this.classifyEvals.get(obj.getSecond()).toParse());
		} else {
			return null;
		}
	}
	
	@Override
	protected boolean fromParseAssignment(Assignment.AssignmentTyped assignment) {
		if (super.fromParseAssignment(assignment))
			return true;
		
		if (assignment.getType().equals(ObjectType.DATA.toString())) {
			if (runAssignmentCommandData(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null) {
				return false;					
			}
		} else if (assignment.getType().equals(ObjectType.MODEL.toString())) {
			if (runAssignmentCommandModel(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null){
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.FEATURE.toString())) {
			if (runAssignmentCommandFeature(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null){
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.GRID_SEARCH.toString())) {
			if (runAssignmentCommandGridSearch(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null){
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.EVALUATION.toString())) {
			if (runAssignmentCommandEvaluation(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null){
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.RULE_SET.toString())) {
			if (runAssignmentCommandRuleSet(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null){
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.FEATURE_SET.toString())) {
			if (runAssignmentCommandFeatureSet(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null){
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.DATA_FEATURES.toString())) {
			if (runAssignmentCommandDataFeatures(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null){
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.CLASSIFY_METHOD.toString())) {
			if (runAssignmentCommandClassifyMethod(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null){
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.CLASSIFY_TASK.toString())) {
			if (runAssignmentCommandClassifyTask(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null){
				return false;
			}
		} else if (assignment.getType().equals(ObjectType.CLASSIFY_EVAL.toString())) {
			if (runAssignmentCommandClassifyEval(assignment.getName(), (Obj.Function)assignment.getValue(), assignment.getModifiers()) == null){
				return false;
			}
		} else {
			return false;
		}
		
		return true;
	}
	
	/* Generic object matching and construction */
	
	@Override
	protected <T> T runCommand(List<String> modifiers, String referenceName, Obj.Function fnObj) {
		return this.datumTools.runCommand(this, modifiers, referenceName, fnObj);
	}
	
	@Override
	protected <T extends CtxParsableFunction> List<T> getFunctionMatches(Obj obj, Map<String, T> storageMap) {
		List<T> matches = getAssignedMatches(obj, storageMap);
		if (matches.size() >= 1)
			return matches;
		
		Map<String, Obj> ctx = ((Obj.Function)(except(Context.ObjectType.CONTEXT.toString())
												.except(ObjectType.FEATURE.toString())
												.except(ObjectType.MODEL.toString())
												.except(ObjectType.FEATURE_SET.toString())
												.except(ObjectType.DATA_FEATURES.toString())
												.except(ObjectType.CLASSIFY_EVAL.toString())
												.except(ObjectType.CLASSIFY_METHOD.toString())
												.except(ObjectType.CLASSIFY_TASK.toString()).toParse()))
													.getInternalAssignments().makeObjMap();
		
		// FIXME For now this just works assuming we only need to resolve fn references...
		obj.resolveValues(ctx);
		
		for (T item : storageMap.values()) {
			Map<String, Obj> matchMap = item.match(obj);
			if (matchMap.size() > 0)
				matches.add(item);	
		}
		
		return matches;
	}
	
	/* Match and construct data */
	
	public DataSet<D, L> getMatchDataSet(Obj obj) {
		return getFunctionMatch(obj, this.data);
	}
	
	public List<DataSet<D, L>> getMatchesDataSet(Obj obj) {
		return getFunctionMatches(obj, this.data);
	}
	
	private DataSet<D, L> runAssignmentCommandData(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.DATA.toString(), modifiers, referenceName, obj, this.data);
	}
	
	/* Match and construct models */

	public SupervisedModel<D, L> getMatchModel(Obj obj) {
		return getFunctionMatch(obj, this.models);
	}
	
	public List<SupervisedModel<D, L>> getMatchesModel(Obj obj) {
		return getFunctionMatches(obj, this.models);
	}
	
	private SupervisedModel<D, L> runAssignmentCommandModel(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.MODEL.toString(), modifiers, referenceName, obj, this.models);
	}
	
	/* Match and construct evaluations */

	public SupervisedModelEvaluation<D, L> getMatchEvaluation(Obj obj) {
		return getFunctionMatch(obj, this.evaluations);
	}
	
	public List<SupervisedModelEvaluation<D, L>> getMatchesEvaluation(Obj obj) {
		return getFunctionMatches(obj, this.evaluations);
	}
	
	private SupervisedModelEvaluation<D, L> runAssignmentCommandEvaluation(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.EVALUATION.toString(), modifiers, referenceName, obj, this.evaluations);
	}
	
	/* Match and construct features */
	
	private Feature<D, L> runAssignmentCommandFeature(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.FEATURE.toString(), modifiers, referenceName, obj, this.features);
	}
	
	public Feature<D, L> getMatchFeature(Obj obj) {
		return getFunctionMatch(obj, this.features);
	}
	
	public List<Feature<D, L>> getMatchesFeature(Obj obj) {
		return getFunctionMatches(obj, this.features);
	}
	
	public Feature<D, L> getMatchOrRunCommandFeature(String referenceName, Obj obj) {
		return getMatchOrRunCommand(ObjectType.FEATURE.toString(), null, referenceName, obj, this.features);
	}
	
	/* Match and construct rule sets */
	
	public RuleSet<D, L> getMatchRuleSet(Obj obj) {
		return getFunctionMatch(obj, this.ruleSets);
	}
	
	private RuleSet<D, L> runAssignmentCommandRuleSet(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.RULE_SET.toString(), modifiers, referenceName, obj, this.ruleSets);
	}
	
	/* Match and construct grid searches */
	
	public GridSearch<D, L> getMatchGridSearch(Obj obj) {
		return getFunctionMatch(obj, this.gridSearches);
	}
	
	private GridSearch<D, L> runAssignmentCommandGridSearch(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.GRID_SEARCH.toString(), modifiers, referenceName, obj, this.gridSearches);
	}
	
	/* Match and construct feature sets */
	
	public FeatureSet<D, L> getMatchFeatureSet(Obj obj) {
		return getFunctionMatch(obj, this.featureSets);
	}
	
	private FeatureSet<D, L> runAssignmentCommandFeatureSet(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.FEATURE_SET.toString(), modifiers, referenceName, obj, this.featureSets);
	}
	
	/* Match and construct data features */
	
	public DataFeatureMatrix<D, L> getMatchDataFeatures(Obj obj) {
		return getFunctionMatch(obj, this.dataFeatures);
	}
	
	private DataFeatureMatrix<D, L> runAssignmentCommandDataFeatures(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.DATA_FEATURES.toString(), modifiers, referenceName, obj, this.dataFeatures);
	}
	
	/* Match and construct classify methods */
	
	public MethodClassification<D, L> getMatchClassifyMethod(Obj obj) {
		return getFunctionMatch(obj, this.classifyMethods);
	}
	
	private MethodClassification<D, L> runAssignmentCommandClassifyMethod(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.CLASSIFY_METHOD.toString(), modifiers, referenceName, obj, this.classifyMethods);
	}
	
	/* Match and construct classify tasks */
	
	public TaskClassification<D, L> getMatchClassifyTask(Obj obj) {
		return getFunctionMatch(obj, this.classifyTasks);
	}
	
	private TaskClassification<D, L> runAssignmentCommandClassifyTask(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.CLASSIFY_TASK.toString(), modifiers, referenceName, obj, this.classifyTasks);
	}

	/* Match and construct classify eval */
	
	public EvaluationClassification<D, L, ?> getMatchClassifyEval(Obj obj) {
		return getFunctionMatch(obj, this.classifyEvals);
	}
	
	private EvaluationClassification<D, L, ?> runAssignmentCommandClassifyEval(String referenceName, Obj.Function obj, List<String> modifiers) {
		return runAssignmentCommand(ObjectType.CLASSIFY_EVAL.toString(), modifiers, referenceName, obj, this.classifyEvals);
	}
	
	/* Match parameter searches */
	@SuppressWarnings("unchecked")
	public ParameterSearchable getMatchParameterSearchable(Obj obj) {
		EvaluationClassification<D, L, ?> eval = this.getMatchClassifyEval(obj);
		if (eval.getType() != Type.MEASURE)
			return null;
		return (EvaluationClassificationMeasure<D, L>)eval;
	}
	
	/* Get objects */
	
	private <T> List<T> getObjects(Map<String, T> objectMap) {
		return new ArrayList<T>(objectMap.values());
	}
	
	public List<Feature<D, L>> getFeatures() {
		return getObjects(this.features);
	}
	
	public List<SupervisedModelEvaluation<D, L>> getEvaluations() {
		return getObjects(this.evaluations);
	}
	
	public List<SupervisedModelEvaluation<D, L>> getEvaluationsWithModifier(String modifier) {
		List<SupervisedModelEvaluation<D, L>> evaluations = getEvaluations();
		List<SupervisedModelEvaluation<D, L>> withModifier = new ArrayList<SupervisedModelEvaluation<D, L>>(evaluations.size());
		
		for (SupervisedModelEvaluation<D, L> evaluation : evaluations) {
			if (evaluation.getModifiers().contains(modifier))
				withModifier.add(evaluation);
		}
		
		return withModifier;
	}
	
	public List<SupervisedModelEvaluation<D, L>> getEvaluationsWithoutModifier(String modifier) {
		List<SupervisedModelEvaluation<D, L>> evaluations = getEvaluations();
		List<SupervisedModelEvaluation<D, L>> withoutModifier = new ArrayList<SupervisedModelEvaluation<D, L>>(evaluations.size());
		
		for (SupervisedModelEvaluation<D, L> evaluation : evaluations) {
			if (!evaluation.getModifiers().contains(modifier))
				withoutModifier.add(evaluation);
		}
		
		return withoutModifier;
	}
	
	public List<DataSet<D, L>> getDataSets() {
		return getObjects(this.data);
	}
	
	public List<SupervisedModel<D, L>> getModels() {
		return getObjects(this.models);
	}

	public List<GridSearch<D, L>> getGridSearches() {
		return getObjects(this.gridSearches);
	}
	
	public List<RuleSet<D, L>> getRuleSets() {
		return getObjects(this.ruleSets);
	}
	
	/* Other stuff */
	
	public Datum.Tools<D, L> getDatumTools() {
		return this.datumTools;
	}
	
	// FIXME Remove this eventually (after micro-cat stops relying on it).
	// Also, note that it hasn't been updated to include searches, featuresets, etc
	public <T extends Datum<Boolean>> DatumContext<T, Boolean> makeBinary(Datum.Tools<T, Boolean> binaryTools, LabelIndicator<L> labelIndicator) {
		DatumContext<T, Boolean> binaryContext = new DatumContext<T, Boolean>(binaryTools, this.genericName, this.parentContext);

		binaryContext.tokenSpanFns = this.tokenSpanFns;
		binaryContext.tokenSpanStrFns = this.tokenSpanStrFns;
		binaryContext.strFns = this.strFns;
		
		for (Pair<String, String> objName : this.objNameOrdering) {
			if (objName.getFirst().equals(ObjectType.DATA.toString())) {
				binaryContext.data.put(objName.getSecond(), this.data.get(objName.getSecond()).makeBinary(labelIndicator, binaryContext));
				binaryContext.objNameOrdering.add(objName); 
			} else if (objName.getFirst().equals(ObjectType.MODEL.toString())) {
				binaryContext.models.put(objName.getSecond(), this.models.get(objName.getSecond()).makeBinary(binaryContext, labelIndicator));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst().equals(ObjectType.FEATURE.toString())) {
				binaryContext.features.put(objName.getSecond(), this.features.get(objName.getSecond()).makeBinary(binaryContext, labelIndicator));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst().equals(ObjectType.GRID_SEARCH.toString())) {
				binaryContext.gridSearches.put(objName.getSecond(), this.gridSearches.get(objName.getSecond()).makeBinary(binaryContext, labelIndicator));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst().equals(ObjectType.EVALUATION.toString())) {
				SupervisedModelEvaluation<T, Boolean> evaluation = this.evaluations.get(objName.getSecond()).makeBinary(binaryContext, labelIndicator);
				if (evaluation != null) { // FIXME: This is a hack to make composite evaluations work with GSTBinary validation
					binaryContext.evaluations.put(objName.getSecond(), evaluation);
					binaryContext.objNameOrdering.add(objName);
				} 
			} else if (objName.getFirst().equals(ObjectType.RULE_SET.toString())) {
				binaryContext.ruleSets.put(objName.getSecond(), this.ruleSets.get(objName.getSecond()).makeBinary(binaryContext, labelIndicator));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst().equals(Context.ObjectType.TOKEN_SPAN_FN.toString())) {
				//binaryContext.tokenSpanFns.put(objName.getSecond(), this.tokenSpanFns.get(objName.getSecond()));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst().equals(Context.ObjectType.STR_FN.toString())) {
				//binaryContext.strFns.put(objName.getSecond(), this.strFns.get(objName.getSecond()));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst().equals(Context.ObjectType.TOKEN_SPAN_STR_FN.toString())) {
				//binaryContext.tokenSpanStrFns.put(objName.getSecond(), this.tokenSpanStrFns.get(objName.getSecond()));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst().equals(Context.ObjectType.ARRAY.toString())) {
				binaryContext.arrays.put(objName.getSecond(), this.arrays.get(objName.getSecond()));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst().equals(Context.ObjectType.VALUE.toString())) {
				binaryContext.values.put(objName.getSecond(), this.values.get(objName.getSecond()));
				binaryContext.objNameOrdering.add(objName);
			} else {
				return null;
			}
		}
		
		binaryContext.currentReferenceId = this.currentReferenceId;
		
		return binaryContext;
	}
	
	@Override
	public synchronized Context clone(boolean cloneBigInternals) {
		DatumContext<D, L> clone = new DatumContext<D, L>(this.datumTools, this.genericName, this.parentContext);
		
		if (cloneBigInternals) {
			if (!clone.fromParse(getModifiers(), getReferenceName(), toParse()))
				return null;
		} else {
			if (!clone.fromParse(getModifiers(), getReferenceName(), this.except(ObjectType.FEATURE.toString()).toParse()))
				return null;
			
			for (Entry<String, Feature<D, L>> entry : this.features.entrySet()) {
				clone.features.put(entry.getKey(), entry.getValue().clone(false));
				clone.objNameOrdering.add(new Pair<String, String>(ObjectType.FEATURE.toString(), entry.getKey()));
			}
			
			for (Entry<String, FeatureSet<D, L>> entry : this.featureSets.entrySet()) {
				clone.featureSets.put(entry.getKey(), entry.getValue().clone(false));
				clone.objNameOrdering.add(new Pair<String, String>(ObjectType.FEATURE_SET.toString(), entry.getKey()));
			}
			/*clone.tokenSpanFns = this.tokenSpanFns;
			clone.tokenSpanStrFns = this.tokenSpanStrFns;
			clone.strFns = this.strFns; Not sure why I was inclined to do this initially 
			FIXME this was done before so that fn results could be cached internally across several model threads.
			But may slow things down because of thread contention when constructing features... */
		}
		
		
		return clone;
	}
	
	@Override
	public Context only(String objectTypeStr) {
		DatumContext<D, L> only = new DatumContext<D, L>(this.datumTools, this.genericName, this.parentContext);
		
		for (Pair<String, String> objName : this.objNameOrdering)
			if (objName.getFirst().equals(objectTypeStr))
				only.objNameOrdering.add(objName);
		
		return onlyHelper(only, objectTypeStr);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Context onlyHelper(Context only, String objectTypeStr) {
		DatumContext<D, L> onlyDatum = (DatumContext<D, L>)super.onlyHelper(only, objectTypeStr);
		
		if (objectTypeStr.equals(ObjectType.DATA.toString())) {
			for (Entry<String, DataSet<D, L>> entry : this.data.entrySet())
				onlyDatum.data.put(entry.getKey(), entry.getValue());
		} else if (objectTypeStr.equals(ObjectType.MODEL.toString())) {
			for (Entry<String, SupervisedModel<D, L>> entry : this.models.entrySet())
				onlyDatum.models.put(entry.getKey(), entry.getValue());
		} else if (objectTypeStr.equals(ObjectType.FEATURE.toString())) {
			for (Entry<String, Feature<D, L>> entry : this.features.entrySet())
				onlyDatum.features.put(entry.getKey(), entry.getValue());
		} else if (objectTypeStr.equals(ObjectType.GRID_SEARCH.toString())) {
			for (Entry<String, GridSearch<D, L>> entry : this.gridSearches.entrySet())
				onlyDatum.gridSearches.put(entry.getKey(), entry.getValue());
		} else if (objectTypeStr.equals(ObjectType.EVALUATION.toString())) {
			for (Entry<String, SupervisedModelEvaluation<D, L>> entry : this.evaluations.entrySet())
				onlyDatum.evaluations.put(entry.getKey(), entry.getValue());
		} else if (objectTypeStr.equals(ObjectType.RULE_SET.toString())) {
			for (Entry<String, RuleSet<D, L>> entry : this.ruleSets.entrySet())
				onlyDatum.ruleSets.put(entry.getKey(), entry.getValue());
		} else if (objectTypeStr.equals(ObjectType.FEATURE_SET.toString())) {
			for (Entry<String, FeatureSet<D, L>> entry : this.featureSets.entrySet())
				onlyDatum.featureSets.put(entry.getKey(), entry.getValue());
		} else if (objectTypeStr.equals(ObjectType.DATA_FEATURES.toString())) {
			for (Entry<String, DataFeatureMatrix<D, L>> entry : this.dataFeatures.entrySet())
				onlyDatum.dataFeatures.put(entry.getKey(), entry.getValue());
		} else if (objectTypeStr.equals(ObjectType.CLASSIFY_METHOD.toString())) {
			for (Entry<String, MethodClassification<D, L>> entry : this.classifyMethods.entrySet())
				onlyDatum.classifyMethods.put(entry.getKey(), entry.getValue());
		} else if (objectTypeStr.equals(ObjectType.CLASSIFY_TASK.toString())) {
			for (Entry<String, TaskClassification<D, L>> entry : this.classifyTasks.entrySet())
				onlyDatum.classifyTasks.put(entry.getKey(), entry.getValue());
		} else if (objectTypeStr.equals(ObjectType.CLASSIFY_EVAL.toString())) {
			for (Entry<String, EvaluationClassification<D, L, ?>> entry : this.classifyEvals.entrySet())
				onlyDatum.classifyEvals.put(entry.getKey(), entry.getValue());
		}
		
		return onlyDatum;
	}

	@Override
	public Context except(String objectTypeStr) {
		DatumContext<D, L> except = new DatumContext<D, L>(this.datumTools, this.genericName, this.parentContext);
		
		for (Pair<String, String> objName : this.objNameOrdering) {
			if (!objectTypeStr.equals(objName.getFirst()))
				except.objNameOrdering.add(objName);
		}
		
		return exceptHelper(except, objectTypeStr);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Context exceptHelper(Context except, String objectTypeStr) {
		DatumContext<D, L> exceptDatum = (DatumContext<D, L>)super.exceptHelper(except, objectTypeStr);
		
		if (!objectTypeStr.equals(ObjectType.DATA.toString())) {
			for (Entry<String, DataSet<D, L>> entry : this.data.entrySet())
				exceptDatum.data.put(entry.getKey(), entry.getValue());
		} 
		
		if (!objectTypeStr.equals(ObjectType.MODEL.toString())) {
			for (Entry<String, SupervisedModel<D, L>> entry : this.models.entrySet())
				exceptDatum.models.put(entry.getKey(), entry.getValue());
		} 
		
		if (!objectTypeStr.equals(ObjectType.FEATURE.toString())) {
			for (Entry<String, Feature<D, L>> entry : this.features.entrySet())
				exceptDatum.features.put(entry.getKey(), entry.getValue());
		} 
		
		if (!objectTypeStr.equals(ObjectType.GRID_SEARCH.toString())) {
			for (Entry<String, GridSearch<D, L>> entry : this.gridSearches.entrySet())
				exceptDatum.gridSearches.put(entry.getKey(), entry.getValue());
		} 
		
		if (!objectTypeStr.equals(ObjectType.EVALUATION.toString())) {
			for (Entry<String, SupervisedModelEvaluation<D, L>> entry : this.evaluations.entrySet())
				exceptDatum.evaluations.put(entry.getKey(), entry.getValue());
		} 
		
		if (!objectTypeStr.equals(ObjectType.RULE_SET.toString())) {
			for (Entry<String, RuleSet<D, L>> entry : this.ruleSets.entrySet())
				exceptDatum.ruleSets.put(entry.getKey(), entry.getValue());
		} 
		
		if (!objectTypeStr.equals(ObjectType.FEATURE_SET.toString())) {
			for (Entry<String, FeatureSet<D, L>> entry : this.featureSets.entrySet())
				exceptDatum.featureSets.put(entry.getKey(), entry.getValue());
		} 
		
		if (!objectTypeStr.equals(ObjectType.DATA_FEATURES.toString())) {
			for (Entry<String, DataFeatureMatrix<D, L>> entry : this.dataFeatures.entrySet())
				exceptDatum.dataFeatures.put(entry.getKey(), entry.getValue());
		} 
		
		if (!objectTypeStr.equals(ObjectType.CLASSIFY_TASK.toString())) {
			for (Entry<String, TaskClassification<D, L>> entry : this.classifyTasks.entrySet())
				exceptDatum.classifyTasks.put(entry.getKey(), entry.getValue());
		} 
		
		if (!objectTypeStr.equals(ObjectType.CLASSIFY_METHOD.toString())) {
			for (Entry<String, MethodClassification<D, L>> entry : this.classifyMethods.entrySet())
				exceptDatum.classifyMethods.put(entry.getKey(), entry.getValue());
		} 
		
		if (!objectTypeStr.equals(ObjectType.CLASSIFY_EVAL.toString())) {
			for (Entry<String, EvaluationClassification<D, L, ?>> entry : this.classifyEvals.entrySet())
				exceptDatum.classifyEvals.put(entry.getKey(), entry.getValue());
		} 
		
		return exceptDatum;
	}
	
	@Override
	public Context makeInstance(Context parentContext) {
		return new DatumContext<D, L>(this.datumTools, this.genericName, parentContext);
	}
	
	public static <D extends Datum<L>, L> DatumContext<D, L> run(Datum.Tools<D, L> datumTools, File file) {
		return run(datumTools, FileUtil.getFileReader(file.getAbsolutePath()));
	}
	
	public static <D extends Datum<L>, L> DatumContext<D, L> run(Datum.Tools<D, L> datumTools, String str) {
		return run(datumTools, new StringReader(str));
	}
	
	public static <D extends Datum<L>, L> DatumContext<D, L> run(Datum.Tools<D, L> datumTools, Reader reader) {
		CtxScanner scanner = new CtxScanner(reader);
		CtxParser parser = new CtxParser(scanner, new ComplexSymbolFactory());
		AssignmentList parse = null;
		try {
			parse = (AssignmentList)parser.parse().value;
		} catch (Exception e) {
			return null;
		}
		
		DatumContext<D, L> context = new DatumContext<D, L>(datumTools);
		if (!context.fromParse(Obj.function(null, new AssignmentList(), parse)))
			return null;
		return context;
	}
}