package edu.cmu.ml.rtw.generic.data;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import java_cup.runtime.ComplexSymbolFactory;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.data.feature.rule.RuleSet;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.evaluation.GridSearch;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.CtxScanner;
import edu.cmu.ml.rtw.generic.parse.CtxParser;
import edu.cmu.ml.rtw.generic.parse.CtxParsable;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * Context holds a set of named model, feature, 
 * evaluation metric, function, and other objects that have 
 * been deserialized from a script written in the
 * language defined in edu.cmu.ml.rtw.generic.parse.  
 * These objects can refer to named tools stored in 
 * edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools
 * and edu.cmu.ml.rtw.generic.data.DataTools.  The 
 * deserialized contexts are generally used in validations
 * in edu.cmu.ml.rtw.generic.model.evaluation.
 * 
 * The following gives an example of the sort of script
 * that can be deserialized into a Context object.
 * 
 * value randomSeed="1";
 * value maxThreads="33";
 * value trainOnDev="false";
 * array validLabels=("1", "2");
 *
 * evaluation accuracy=Accuracy();
 * evaluation accuracyBase=Accuracy(computeBaseline="true");
 * evaluation f.5=F(mode="MACRO_WEIGHTED", filterLabel="true", Beta="0.5");
 * evaluation f1=F(mode="MACRO_WEIGHTED", filterLabel="true", Beta="1");
 * evaluation prec=Precision(weighted="false", filterLabel="true");
 * evaluation recall=Recall(weighted="false", filterLabel="true");
 * 
 * ts_fn head=Head();
 * ts_fn ins1=NGramInside(n="1", noHead="true");
 * ts_fn ins2=NGramInside(n="2", noHead="true");
 * ts_fn ctxb1=NGramContext(n="1", type="BEFORE");
 * ts_fn ctxa1=NGramContext(n="1", type="AFTER");
 * ts_fn ctxb2=NGramContext(n="2", type="BEFORE");
 * ts_fn ctxa2=NGramContext(n="2", type="AFTER");
 * ts_fn sent1=NGramSentence(n="1", noSpan="true");
 * ts_fn sent2=NGramSentence(n="2", noSpan="true");
 * ts_fn doc1=NGramDocument(n="1", noSentence="true");
 * ts_fn doc2=NGramDocument(n="2", noSentence="true");
 * ts_str_fn pos=PoS();
 * ts_str_fn str=String(splitTokens="false");
 * str_fn pre=Affix(nMin="3", nMax="5", type="PREFIX"); 
 * str_fn suf=Affix(nMin="3", nMax="5", type="SUFFIX"); 
 * str_fn split1=Split(chunkSize="1");
 * str_fn split2=Split(chunkSize="2");
 * str_fn split3=Split(chunkSize="3");
 * str_fn stem=Clean(cleanFn="CatStemCleanFn");
 *
 * feature fdep=NGramDep(scale="INDICATOR", mode="ParentsAndChildren", useRelationTypes="true", minFeatureOccurrence="2", n="1", cleanFn="CatStemCleanFn", tokenExtractor="AllTokenSpans");
 * feature fner=Ner(useTypes="true", tokenExtractor="AllTokenSpans");
 * feature ftcnt=TokenCount(maxCount="5", tokenExtractor="AllTokenSpans");
 * feature fform=StringForm(stringExtractor="FirstTokenSpan", minFeatureOccurrence="2");
 *
 * feature fpos=TokenSpanFnDataVocab(scale="INDICATOR", minFeatureOccurrence="2", tokenExtractor="AllTokenSpans", fn=${pos});
 * feature fphrh1=TokenSpanFnDataVocab(scale="INDICATOR", minFeatureOccurrence="2", tokenExtractor="AllTokenSpans", fn=(${strDef} o ${head}));
 * feature fphr1=TokenSpanFnDataVocab(scale="INDICATOR", minFeatureOccurrence="2", tokenExtractor="AllTokenSpans", fn=(${strDef} o ${ins1}));
 * feature fphr2=TokenSpanFnDataVocab(scale="INDICATOR", minFeatureOccurrence="2", tokenExtractor="AllTokenSpans", fn=(${strDef} o ${ins2}));
 * feature fphr3=TokenSpanFnDataVocab(scale="INDICATOR", minFeatureOccurrence="2", tokenExtractor="AllTokenSpans", fn=(${strDef} o ${ins3}));
 *
 * model lr=Areg(l1="0", l2="0", convergenceEpsilon=".00001", maxTrainingExamples="520001", batchSize="100", evaluationIterations="200", maxEvaluationConstantIterations="500", weightedLabels="false", computeTestEvaluations="false")
 * {
 *  array validLabels=${validLabels};
 * };
 * 
 * gs g=GridSearch() {
 *  dimension l1=Dimension(name="l1", values=(.00000001,.0000001,.000001,.00001,.0001,.001,.01,.1,1,10), trainingDimension="true");
 *  dimension ct=Dimension(name="classificationThreshold", values=(.5,.6,.7,.8,.9), trainingDimension="false");
 *  model model=${lr};
 *  evaluation evaluation=${accuracy};
 * };
 * 
 * @author Bill McDowell
 *
 * @param <D>
 * @param <L>
 */
public class Context<D extends Datum<L>, L> extends CtxParsable {
	private enum ObjectType {
		MODEL,
		FEATURE,
		GRID_SEARCH,
		EVALUATION,
		RULE_SET,
		TOKEN_SPAN_FN,
		STR_FN,
		TOKEN_SPAN_STR_FN,
		ARRAY,
		VALUE
	}
	
	public static final String MODEL_STR = "model";
	public static final String FEATURE_STR = "feature";
	public static final String GRID_SEARCH_STR = "gs";
	public static final String EVALUATION_STR = "evaluation";
	public static final String RULE_SET_STR = "rs";
	public static final String TOKEN_SPAN_FN_STR = "ts_fn";
	public static final String STR_FN_STR = "str_fn";
	public static final String TOKEN_SPAN_STR_FN_STR = "ts_str_fn";
	public static final String ARRAY_STR = "array";
	public static final String VALUE_STR = "value";
	
	private Datum.Tools<D, L> datumTools;
	private List<Pair<ObjectType, String>> objNameOrdering;
	private Map<String, SupervisedModel<D, L>> models;
	private Map<String, Feature<D, L>> features;
	private Map<String, GridSearch<D, L>> gridSearches;
	private Map<String, SupervisedModelEvaluation<D, L>> evaluations;
	private Map<String, RuleSet<D, L>> ruleSets;
	private Map<String, Fn<TokenSpan, TokenSpan>> tokenSpanFns;
	private Map<String, Fn<String, String>> strFns;
	private Map<String, Fn<TokenSpan, String>> tokenSpanStrFns;
	private Map<String, List<String>> arrays;
	private Map<String, String> values;
	private int currentReferenceId;
	
	public Context(Datum.Tools<D, L> datumTools) {
		this.datumTools = datumTools;
		this.objNameOrdering = new ArrayList<Pair<ObjectType, String>>();
		this.models = new TreeMap<String, SupervisedModel<D, L>>();
		this.features = new TreeMap<String, Feature<D, L>>();
		this.gridSearches = new TreeMap<String, GridSearch<D, L>>();
		this.evaluations = new TreeMap<String, SupervisedModelEvaluation<D, L>>();
		this.ruleSets = new TreeMap<String, RuleSet<D, L>>();
		this.tokenSpanFns = new ConcurrentHashMap<String, Fn<TokenSpan, TokenSpan>>();
		this.strFns = new ConcurrentHashMap<String, Fn<String, String>>();
		this.tokenSpanStrFns = new ConcurrentHashMap<String, Fn<TokenSpan, String>>();
		this.arrays = new TreeMap<String, List<String>>();
		this.values = new TreeMap<String, String>();
		this.currentReferenceId = 0;
	}
	
	public Context(Datum.Tools<D, L> datumTools, List<Feature<D, L>> features) {
		this(datumTools);
		
		for (int i = 0; i < features.size(); i++) {
			this.objNameOrdering.add(new Pair<ObjectType, String>(ObjectType.FEATURE, features.get(i).getReferenceName()));
			this.features.put(features.get(i).getReferenceName(), features.get(i));
		}
	}
	
	@Override
	public List<String> getModifiers() {
		return new ArrayList<String>();
	}

	@Override
	public Obj toParse() {
		AssignmentList assignmentList = new AssignmentList();
		for (Pair<ObjectType, String> obj : this.objNameOrdering) {
			if (obj.getFirst() == ObjectType.MODEL) {
				assignmentList.add(Assignment.assignmentTyped(this.models.get(obj.getSecond()).getModifiers(), MODEL_STR, obj.getSecond(), this.models.get(obj.getSecond()).toParse()));
			} else if (obj.getFirst() == ObjectType.FEATURE) {
				assignmentList.add(Assignment.assignmentTyped(this.features.get(obj.getSecond()).getModifiers(), FEATURE_STR, obj.getSecond(), this.features.get(obj.getSecond()).toParse()));
			} else if (obj.getFirst() == ObjectType.GRID_SEARCH) {
				assignmentList.add(Assignment.assignmentTyped(this.gridSearches.get(obj.getSecond()).getModifiers(), GRID_SEARCH_STR, obj.getSecond(), this.gridSearches.get(obj.getSecond()).toParse()));				
			} else if (obj.getFirst() == ObjectType.EVALUATION) {
				assignmentList.add(Assignment.assignmentTyped(this.evaluations.get(obj.getSecond()).getModifiers(), EVALUATION_STR, obj.getSecond(), this.evaluations.get(obj.getSecond()).toParse()));
			} else if (obj.getFirst() == ObjectType.RULE_SET) {
				assignmentList.add(Assignment.assignmentTyped(this.ruleSets.get(obj.getSecond()).getModifiers(), RULE_SET_STR, obj.getSecond(), this.ruleSets.get(obj.getSecond()).toParse()));
			} else if (obj.getFirst() == ObjectType.TOKEN_SPAN_FN) {
				assignmentList.add(Assignment.assignmentTyped(this.tokenSpanFns.get(obj.getSecond()).getModifiers(), TOKEN_SPAN_FN_STR, obj.getSecond(), this.tokenSpanFns.get(obj.getSecond()).toParse()));
			} else if (obj.getFirst() == ObjectType.STR_FN) {
				assignmentList.add(Assignment.assignmentTyped(this.strFns.get(obj.getSecond()).getModifiers(), STR_FN_STR, obj.getSecond(), this.strFns.get(obj.getSecond()).toParse()));	
			} else if (obj.getFirst() == ObjectType.TOKEN_SPAN_STR_FN) {
				assignmentList.add(Assignment.assignmentTyped(this.tokenSpanStrFns.get(obj.getSecond()).getModifiers(), TOKEN_SPAN_STR_FN_STR, obj.getSecond(), this.tokenSpanStrFns.get(obj.getSecond()).toParse()));	
			} else if (obj.getFirst() == ObjectType.ARRAY) {
				assignmentList.add(Assignment.assignmentTyped(new ArrayList<String>(), ARRAY_STR, obj.getSecond(), Obj.array(this.arrays.get(obj.getSecond()))));
			} else if (obj.getFirst() == ObjectType.VALUE) {
				assignmentList.add(Assignment.assignmentTyped(new ArrayList<String>(), VALUE_STR, obj.getSecond(), Obj.stringValue(this.values.get(obj.getSecond()))));
			} else {
				return null;
			}
		}
		
		return assignmentList;
	}

	@Override
	protected boolean fromParseHelper(Obj obj) {
		AssignmentList assignmentList = (AssignmentList)obj;
		
		for (int i = 0; i < assignmentList.size(); i++) {
			Assignment.AssignmentTyped assignment = (Assignment.AssignmentTyped)assignmentList.get(i);
			if (assignment.getType().equals(MODEL_STR)) {
				if (constructFromParseModel(assignment.getName(), assignment.getValue(), assignment.getModifiers()) == null){
					this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to construct '" + assignment.getName() + "' from context parse."); 
					return false;
				}
				this.objNameOrdering.add(new Pair<ObjectType, String>(ObjectType.MODEL, assignment.getName()));
			} else if (assignment.getType().equals(FEATURE_STR)) {
				if (constructFromParseFeature(assignment.getName(), assignment.getValue(), assignment.getModifiers()) == null){
					this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to construct '" + assignment.getName() + "' from context parse."); 
					return false;
				}
				this.objNameOrdering.add(new Pair<ObjectType, String>(ObjectType.FEATURE, assignment.getName()));
			} else if (assignment.getType().equals(GRID_SEARCH_STR)) {
				if (constructFromParseGridSearch(assignment.getName(), assignment.getValue(), assignment.getModifiers()) == null){
					this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to construct '" + assignment.getName() + "' from context parse."); 
					return false;
				}
				this.objNameOrdering.add(new Pair<ObjectType, String>(ObjectType.GRID_SEARCH, assignment.getName()));
			} else if (assignment.getType().equals(EVALUATION_STR)) {
				if (constructFromParseEvaluation(assignment.getName(), assignment.getValue(), assignment.getModifiers()) == null){
					this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to construct '" + assignment.getName() + "' from context parse."); 
					return false;
				}
				this.objNameOrdering.add(new Pair<ObjectType, String>(ObjectType.EVALUATION, assignment.getName()));
			} else if (assignment.getType().equals(RULE_SET_STR)) {
				if (constructFromParseRuleSet(assignment.getName(), assignment.getValue(), assignment.getModifiers()) == null){
					this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to construct '" + assignment.getName() + "' from context parse."); 
					return false;
				}
				this.objNameOrdering.add(new Pair<ObjectType, String>(ObjectType.RULE_SET, assignment.getName()));
			} else if (assignment.getType().equals(TOKEN_SPAN_FN_STR)) {
				if (constructFromParseTokenSpanFn(assignment.getName(), assignment.getValue(), assignment.getModifiers()) == null){
					this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to construct '" + assignment.getName() + "' from context parse."); 
					return false;
				}
				this.objNameOrdering.add(new Pair<ObjectType, String>(ObjectType.TOKEN_SPAN_FN, assignment.getName()));
			} else if (assignment.getType().equals(STR_FN_STR)) {
				if (constructFromParseStrFn(assignment.getName(), assignment.getValue(), assignment.getModifiers()) == null){
					this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to construct '" + assignment.getName() + "' from context parse."); 
					return false;
				}
				this.objNameOrdering.add(new Pair<ObjectType, String>(ObjectType.STR_FN, assignment.getName()));
			} else if (assignment.getType().equals(TOKEN_SPAN_STR_FN_STR)) {
				if (constructFromParseTokenSpanStrFn(assignment.getName(), assignment.getValue(), assignment.getModifiers()) == null) {
					this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to construct '" + assignment.getName() + "' from context parse."); 
					return false;
				}
				this.objNameOrdering.add(new Pair<ObjectType, String>(ObjectType.TOKEN_SPAN_STR_FN, assignment.getName()));
			} else if (assignment.getType().equals(ARRAY_STR)) {
				if (constructFromParseArray(assignment.getName(), assignment.getValue()) == null) {
					this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to construct '" + assignment.getName() + "' from context parse."); 
					return false;
				}
				this.objNameOrdering.add(new Pair<ObjectType, String>(ObjectType.ARRAY, assignment.getName()));
			} else if (assignment.getType().equals(VALUE_STR)) {
				if (constructFromParseValue(assignment.getName(), assignment.getValue()) == null) {
					this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to construct '" + assignment.getName() + "' from context parse."); 
					return false;
				}
				this.objNameOrdering.add(new Pair<ObjectType, String>(ObjectType.VALUE, assignment.getName()));
			} else {
				this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Invalid object type in context '" + assignment.getType() + "'.");
				return false;
			}
		}

		return true;
	}
	
	/* Generic object matching and construction */
	
	private abstract static interface GenericFunctionRetriever<T extends CtxParsableFunction> {
		List<T> retrieve(String name);
	}
	
	private <T extends CtxParsableFunction> T constructFromParse(ObjectType objectType, List<String> modifiers, String referenceName, Obj obj, Map<String, T> storageMap, GenericFunctionRetriever<T> retriever) {
		if (obj.getObjType() != Obj.Type.FUNCTION)
			return null;
		
		Obj.Function fnObj = (Obj.Function)obj;
		
		List<T> possibleGenerics = retriever.retrieve(fnObj.getName());
		for (T possibleGeneric : possibleGenerics) {
			if (possibleGeneric.fromParse(modifiers, referenceName, fnObj)) {
				if (referenceName == null) {
					// FIXME This works for now, but probably should disallow user-declared names that start with numbers to avoid conflicts
					String currentReferenceIdStr = String.valueOf(this.currentReferenceId);
					storageMap.put(currentReferenceIdStr, possibleGeneric);
					this.objNameOrdering.add(new Pair<ObjectType, String>(objectType, currentReferenceIdStr));
					this.currentReferenceId++;
				} else {
					storageMap.put(referenceName, possibleGeneric);
				}
				return possibleGeneric;
			}
		}
	
		return null;
	}
	
	private <T extends CtxParsableFunction> List<T> getMatches(Obj obj, Map<String, T> storageMap) {
		List<T> matches = new ArrayList<T>();
		
		if (obj.getObjType() == Obj.Type.VALUE) {
			Obj.Value vObj = (Obj.Value)obj;
			if (vObj.getType() == Obj.Value.Type.CURLY_BRACED) {
				if (storageMap.containsKey(vObj.getStr())) {
					matches.add(storageMap.get(vObj.getStr()));
					return matches;
				} else {
					return null;
				}
			}
		}
		
		// FIXME For now this just works assuming we only need to resolve fn references...
		obj.resolveValues(((AssignmentList)(except(ObjectType.FEATURE).except(ObjectType.MODEL).toParse())).makeObjMap());
		
		for (T item : storageMap.values()) {
			Map<String, Obj> matchMap = item.match(obj);
			if (matchMap.size() > 0)
				matches.add(item);	
		}
		
		return matches;
	}
	
	private <T extends CtxParsableFunction> T getMatch(Obj obj, Map<String, T> storageMap) {
		List<T> matches = getMatches(obj, storageMap);
		
		if (matches.size() >= 1) {
			return matches.get(0);
		} else
			return null;
	}
	
	public <T extends CtxParsableFunction> T getMatchOrConstruct(ObjectType objectType, Obj obj, Map<String, T> storageMap, GenericFunctionRetriever<T> retriever) {
		return getMatchOrConstruct(objectType, null, null, obj, storageMap, retriever);
	}
	
	private <T extends CtxParsableFunction> T getMatchOrConstruct(ObjectType objectType, List<String> modifiers, String referenceName, Obj obj, Map<String, T> storageMap, GenericFunctionRetriever<T> retriever) {
		List<T> matches = getMatches(obj, storageMap);
		if (matches != null && matches.size() >= 1) {
			return matches.get(0);
		} else {
			return constructFromParse(objectType, modifiers, referenceName, obj, storageMap, retriever);
		}
	}
	
	/* Match and construct token span fns */
	
	private GenericFunctionRetriever<Fn<TokenSpan, TokenSpan>> tokenSpanFnRetriever = new GenericFunctionRetriever<Fn<TokenSpan, TokenSpan>>() {
		@Override
		public List<Fn<TokenSpan, TokenSpan>> retrieve(String name) {
			return Context.this.datumTools.makeTokenSpanFns(name, Context.this);
		}
	};
	
	public Fn<TokenSpan, TokenSpan> getMatchTokenSpanFn(Obj obj) {
		synchronized (this.tokenSpanFns) {
			return getMatch(obj, this.tokenSpanFns);
		}
	}
	
	public List<Fn<TokenSpan, TokenSpan>> getMatchesTokenSpanFn(Obj obj) {
		synchronized (this.tokenSpanFns) {
			return getMatches(obj, this.tokenSpanFns);
		}
	}
	
	public Fn<TokenSpan, TokenSpan> getMatchOrConstructTokenSpanFn(String referenceName, Obj obj) {
		synchronized (this.tokenSpanFns) {
			return getMatchOrConstruct(ObjectType.TOKEN_SPAN_FN, null, referenceName, obj, this.tokenSpanFns, this.tokenSpanFnRetriever);
		}
	}
	
	public Fn<TokenSpan, TokenSpan> getMatchOrConstructTokenSpanFn(Obj obj) {
		synchronized (this.tokenSpanFns) {
			return getMatchOrConstruct(ObjectType.TOKEN_SPAN_FN, obj, this.tokenSpanFns, this.tokenSpanFnRetriever);
		}
	}
	
	private Fn<TokenSpan, TokenSpan> constructFromParseTokenSpanFn(String referenceName, Obj obj, List<String> modifiers) {
		synchronized (this.tokenSpanFns) {
			return constructFromParse(ObjectType.TOKEN_SPAN_FN, modifiers, referenceName, obj, this.tokenSpanFns, this.tokenSpanFnRetriever);
		}
	}
	
	/* Match and construct str fns */
	
	private GenericFunctionRetriever<Fn<String, String>> strFnRetriever = new GenericFunctionRetriever<Fn<String, String>>() {
		@Override
		public List<Fn<String, String>> retrieve(String name) {
			return Context.this.datumTools.makeStrFns(name, Context.this);
		}
	};
	
	public Fn<String, String> getMatchStrFn(Obj obj) {
		synchronized (this.strFns) {
			return getMatch(obj, this.strFns);
		}
	}
	
	public List<Fn<String, String>> getMatchesStrFn(Obj obj) {
		synchronized (this.strFns) {
			return getMatches(obj, this.strFns);
		}
	}

	public Fn<String, String> getMatchOrConstructStrFn(String referenceName, Obj obj) {
		synchronized (this.strFns) {
			return getMatchOrConstruct(ObjectType.STR_FN, null, referenceName, obj, this.strFns, this.strFnRetriever);
		}
	}
	
	public Fn<String, String> getMatchOrConstructStrFn(Obj obj) {
		synchronized (this.strFns) {
			return getMatchOrConstruct(ObjectType.STR_FN, obj, this.strFns, this.strFnRetriever);
		}
	}
	
	private Fn<String, String> constructFromParseStrFn(String referenceName, Obj obj, List<String> modifiers) {
		synchronized (this.strFns) {
			return constructFromParse(ObjectType.STR_FN, modifiers, referenceName, obj, this.strFns, this.strFnRetriever);
		}
	}
	
	/* Match and construct token span str fns */
	
	private GenericFunctionRetriever<Fn<TokenSpan, String>> tokenSpanStrFnRetriever = new GenericFunctionRetriever<Fn<TokenSpan, String>>() {
		@Override
		public List<Fn<TokenSpan, String>> retrieve(String name) {
			return Context.this.datumTools.makeTokenSpanStrFns(name, Context.this);
		}
	};
	
	public Fn<TokenSpan, String> getMatchTokenSpanStrFn(Obj obj) {
		synchronized (this.tokenSpanStrFns) {
			return getMatch(obj, this.tokenSpanStrFns);
		}
	}
	
	public List<Fn<TokenSpan, String>> getMatchesTokenSpanStrFn(Obj obj) {
		synchronized (this.tokenSpanStrFns) {
			return getMatches(obj, this.tokenSpanStrFns);
		}
	}

	public Fn<TokenSpan, String> getMatchOrConstructTokenSpanStrFn(String referenceName, Obj obj) {
		synchronized (this.tokenSpanStrFns) {
			return getMatchOrConstruct(ObjectType.TOKEN_SPAN_STR_FN, null, referenceName, obj, this.tokenSpanStrFns, this.tokenSpanStrFnRetriever);
		}
	}
	
	public Fn<TokenSpan, String> getMatchOrConstructTokenSpanStrFn(Obj obj) {
		synchronized (this.tokenSpanStrFns) {
			return getMatchOrConstruct(ObjectType.TOKEN_SPAN_STR_FN, obj, this.tokenSpanStrFns, this.tokenSpanStrFnRetriever);
		}
	}
	
	private Fn<TokenSpan, String> constructFromParseTokenSpanStrFn(String referenceName, Obj obj, List<String> modifiers) {
		synchronized (this.tokenSpanStrFns) {
			return constructFromParse(ObjectType.TOKEN_SPAN_STR_FN, modifiers, referenceName, obj, this.tokenSpanStrFns, this.tokenSpanStrFnRetriever);
		}
	}
	
	/* Match and construct models */
	
	private GenericFunctionRetriever<SupervisedModel<D, L>> modelRetriever = new GenericFunctionRetriever<SupervisedModel<D, L>>() {
		@Override
		public List<SupervisedModel<D, L>> retrieve(String name) {
			SupervisedModel<D, L> model = Context.this.datumTools.makeModelInstance(name, Context.this);
			List<SupervisedModel<D, L>> models = new ArrayList<SupervisedModel<D, L>>();
			models.add(model);
			return models;
		}
	};

	public SupervisedModel<D, L> getMatchModel(Obj obj) {
		return getMatch(obj, this.models);
	}
	
	public List<SupervisedModel<D, L>> getMatchesModel(Obj obj) {
		return getMatches(obj, this.models);
	}
	
	private SupervisedModel<D, L> constructFromParseModel(String referenceName, Obj obj, List<String> modifiers) {
		return constructFromParse(ObjectType.MODEL, modifiers, referenceName, obj, this.models, this.modelRetriever);
	}
	
	/* Match and construct evaluations */
	
	private GenericFunctionRetriever<SupervisedModelEvaluation<D, L>> evaluationRetriever = new GenericFunctionRetriever<SupervisedModelEvaluation<D, L>>() {
		@Override
		public List<SupervisedModelEvaluation<D, L>> retrieve(String name) {
			SupervisedModelEvaluation<D, L> evaluation = Context.this.datumTools.makeEvaluationInstance(name, Context.this);
			List<SupervisedModelEvaluation<D, L>> evaluations = new ArrayList<SupervisedModelEvaluation<D, L>>();
			evaluations.add(evaluation);
			return evaluations;
		}
	};

	public SupervisedModelEvaluation<D, L> getMatchEvaluation(Obj obj) {
		return getMatch(obj, this.evaluations);
	}
	
	public List<SupervisedModelEvaluation<D, L>> getMatchesEvaluation(Obj obj) {
		return getMatches(obj, this.evaluations);
	}
	
	private SupervisedModelEvaluation<D, L> constructFromParseEvaluation(String referenceName, Obj obj, List<String> modifiers) {
		return constructFromParse(ObjectType.EVALUATION, modifiers, referenceName, obj, this.evaluations, this.evaluationRetriever);
	}
	
	/* Match and construct features */
	
	private GenericFunctionRetriever<Feature<D, L>> featureRetriever = new GenericFunctionRetriever<Feature<D, L>>() {
		@Override
		public List<Feature<D, L>> retrieve(String name) {
			Feature<D, L> feature = Context.this.datumTools.makeFeatureInstance(name, Context.this);
			List<Feature<D, L>> features = new ArrayList<Feature<D, L>>();
			features.add(feature);
			return features;
		}
	};
	
	private Feature<D, L> constructFromParseFeature(String referenceName, Obj obj, List<String> modifiers) {
		return constructFromParse(ObjectType.FEATURE, modifiers, referenceName, obj, this.features, this.featureRetriever);
	}
	
	public Feature<D, L> getMatchFeature(Obj obj) {
		return getMatch(obj, this.features);
	}
	
	public List<Feature<D, L>> getMatchesFeature(Obj obj) {
		return getMatches(obj, this.features);
	}
	
	public Feature<D, L> getMatchOrConstructFeature(String referenceName, Obj obj) {
		return getMatchOrConstruct(ObjectType.FEATURE, null, referenceName, obj, this.features, this.featureRetriever);
	}
	
	/* Match and construct rule sets */
	
	private GenericFunctionRetriever<RuleSet<D, L>> ruleSetRetriever = new GenericFunctionRetriever<RuleSet<D, L>>() {
		@Override
		public List<RuleSet<D, L>> retrieve(String name) {
			RuleSet<D, L> rules = new RuleSet<D, L>(Context.this);
			List<RuleSet<D, L>> rulesList = new ArrayList<RuleSet<D, L>>();
			rulesList.add(rules);
			return rulesList;
		}
	};
	
	public RuleSet<D, L> getMatchRuleSet(Obj obj) {
		return getMatch(obj, this.ruleSets);
	}
	
	private RuleSet<D, L> constructFromParseRuleSet(String referenceName, Obj obj, List<String> modifiers) {
		return constructFromParse(ObjectType.RULE_SET, modifiers, referenceName, obj, this.ruleSets, this.ruleSetRetriever);
	}
	
	/* Match and construct grid searches */
	
	private GenericFunctionRetriever<GridSearch<D, L>> gridSearchRetriever = new GenericFunctionRetriever<GridSearch<D, L>>() {
		@Override
		public List<GridSearch<D, L>> retrieve(String name) {
			GridSearch<D, L> gs = new GridSearch<D, L>(Context.this);
			List<GridSearch<D, L>> gsList = new ArrayList<GridSearch<D, L>>();
			gsList.add(gs);
			return gsList;
		}
	};

	public GridSearch<D, L> getMatchGridSearch(Obj obj) {
		return getMatch(obj, this.gridSearches);
	}
	
	private GridSearch<D, L> constructFromParseGridSearch(String referenceName, Obj obj, List<String> modifiers) {
		return constructFromParse(ObjectType.GRID_SEARCH, modifiers, referenceName, obj, this.gridSearches, this.gridSearchRetriever);
	}
	
	/* Match and construct arrays */
	
	public List<String> constructFromParseArray(Obj obj) {
		return constructFromParseArray(null, obj);
	}
	
	public List<String> getMatchArray(Obj obj) {
		if (obj.getObjType() == Obj.Type.VALUE) {
			Obj.Value vObj = (Obj.Value)obj;
			if (vObj.getType() == Obj.Value.Type.CURLY_BRACED) {
				if (this.arrays.containsKey(vObj.getStr())) {
					return this.arrays.get(vObj.getStr());
				} else {
					this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to resolve reference variable '" + vObj.getStr() + "'.");
					return null;
				}
			}
		} else if (obj.getObjType() == Obj.Type.ARRAY) {
			Obj.Array arrayObj = (Obj.Array)obj;
			return arrayObj.toList(this.values);
		} 
		
		return null;
	}
	
	private List<String> constructFromParseArray(String referenceName, Obj obj) {
		if (obj.getObjType() != Obj.Type.ARRAY) {
			this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Invalid object type for array construction (" + obj.getObjType() + ").");
			return null;
		}
		
		Obj.Array arrObj = (Obj.Array)obj;
		List<String> array = arrObj.toList(this.values);
		if (array == null) {
			this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to construct array '" + ((referenceName != null) ? referenceName : "") + "'");
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
		if (obj.getObjType() == Obj.Type.VALUE) {
			Obj.Value vObj = (Obj.Value)obj;
			if (vObj.getType() == Obj.Value.Type.CURLY_BRACED) {
				if (this.values.containsKey(vObj.getStr())) {
					return this.values.get(vObj.getStr());
				} else {
					this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to resolve reference variable '" + vObj.getStr() + "'.");
					return null;
				}
			} else if (vObj.getType() == Obj.Value.Type.STRING) {
				return vObj.getValueStr(this.values);
			}
		}
		
		return null;
	}
	
	public String constructFromParseValue(Obj obj) {
		return constructFromParseValue(null, obj);
	}
	
	private String constructFromParseValue(String referenceName, Obj obj) {
		if (obj.getObjType() != Obj.Type.VALUE) {
			this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Invalid object type for value construction (" + obj.getObjType() + ").");
			return null;
		}
		
		Obj.Value vObj = (Obj.Value)obj;
		String value = vObj.getValueStr(this.values);
		if (value == null) {
			this.datumTools.getDataTools().getOutputWriter().debugWriteln("ERROR: Failed to construct value '" + ((referenceName != null) ? referenceName : "") + "'");
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
	
	/* Add objects */
	
	public synchronized boolean addValue(String name, String value) {
		this.values.put(name, value);
		return true;
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
	
	public List<SupervisedModel<D, L>> getModels() {
		return getObjects(this.models);
	}

	public List<GridSearch<D, L>> getGridSearches() {
		return getObjects(this.gridSearches);
	}
	
	public List<RuleSet<D, L>> getRuleSets() {
		return getObjects(this.ruleSets);
	}
	
	public int getIntValue(String name) {
		return Integer.valueOf(this.values.get(name));
	}
	
	public boolean getBooleanValue(String name) {
		return Boolean.valueOf(this.values.get(name));
	}

	public double getDoubleValue(String name) {
		return Double.valueOf(this.values.get(name));
	}
	
	public String getStringValue(String name) {
		return this.values.get(name);
	}
	
	public List<String> getStringArray(String name) {
		return this.arrays.get(name);
	}
	
	/* Other stuff */
	
	public Datum.Tools<D, L> getDatumTools() {
		return this.datumTools;
	}
	 
	public <T extends Datum<Boolean>> Context<T, Boolean> makeBinary(Datum.Tools<T, Boolean> binaryTools, LabelIndicator<L> labelIndicator) {
		Context<T, Boolean> binaryContext = new Context<T, Boolean>(binaryTools);

		binaryContext.tokenSpanFns = this.tokenSpanFns;
		binaryContext.tokenSpanStrFns = this.tokenSpanStrFns;
		binaryContext.strFns = this.strFns;
		
		for (Pair<ObjectType, String> objName : this.objNameOrdering) {
			if (objName.getFirst() == ObjectType.MODEL) {
				binaryContext.models.put(objName.getSecond(), this.models.get(objName.getSecond()).makeBinary(binaryContext, labelIndicator));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst() == ObjectType.FEATURE) {
				binaryContext.features.put(objName.getSecond(), this.features.get(objName.getSecond()).makeBinary(binaryContext, labelIndicator));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst() == ObjectType.GRID_SEARCH) {
				binaryContext.gridSearches.put(objName.getSecond(), this.gridSearches.get(objName.getSecond()).makeBinary(binaryContext, labelIndicator));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst() == ObjectType.EVALUATION) {
				SupervisedModelEvaluation<T, Boolean> evaluation = this.evaluations.get(objName.getSecond()).makeBinary(binaryContext, labelIndicator);
				if (evaluation != null) { // FIXME: This is a hack to make composite evaluations work with GSTBinary validation
					binaryContext.evaluations.put(objName.getSecond(), evaluation);
					binaryContext.objNameOrdering.add(objName);
				} 
			} else if (objName.getFirst() == ObjectType.RULE_SET) {
				binaryContext.ruleSets.put(objName.getSecond(), this.ruleSets.get(objName.getSecond()).makeBinary(binaryContext, labelIndicator));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst() == ObjectType.TOKEN_SPAN_FN) {
				//binaryContext.tokenSpanFns.put(objName.getSecond(), this.tokenSpanFns.get(objName.getSecond()));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst() == ObjectType.STR_FN) {
				//binaryContext.strFns.put(objName.getSecond(), this.strFns.get(objName.getSecond()));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst() == ObjectType.TOKEN_SPAN_STR_FN) {
				//binaryContext.tokenSpanStrFns.put(objName.getSecond(), this.tokenSpanStrFns.get(objName.getSecond()));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst() == ObjectType.ARRAY) {
				binaryContext.arrays.put(objName.getSecond(), this.arrays.get(objName.getSecond()));
				binaryContext.objNameOrdering.add(objName);
			} else if (objName.getFirst() == ObjectType.VALUE) {
				binaryContext.values.put(objName.getSecond(), this.values.get(objName.getSecond()));
				binaryContext.objNameOrdering.add(objName);
			} else {
				return null;
			}
		}
		
		binaryContext.currentReferenceId = this.currentReferenceId;
		
		return binaryContext;
	}
	
	public Context<D, L> clone(boolean cloneFeatureInternal) {
		Context<D, L> clone = new Context<D, L>(this.datumTools);
		
		if (cloneFeatureInternal) {
			if (!clone.fromParse(getModifiers(), getReferenceName(), toParse()))
				return null;
		} else {
			if (!clone.fromParse(getModifiers(), getReferenceName(), this.except(ObjectType.FEATURE).toParse()))
				return null;
			
			for (Entry<String, Feature<D, L>> entry : this.features.entrySet()) {
				clone.features.put(entry.getKey(), entry.getValue().clone(false));
				clone.objNameOrdering.add(new Pair<ObjectType, String>(ObjectType.FEATURE, entry.getKey()));
			}
		
			/*clone.tokenSpanFns = this.tokenSpanFns;
			clone.tokenSpanStrFns = this.tokenSpanStrFns;
			clone.strFns = this.strFns; Not sure why I was inclined to do this initially 
			FIXME this was done before so that fn results could be cached internally across several model threads.
			But may slow things down because of thread contention when constructing features... */
		}
		
		
		return clone;
	}
	
	public Context<D, L> only(ObjectType objectType) {
		Context<D, L> only = new Context<D, L>(this.datumTools);
		
		for (Pair<ObjectType, String> objName : this.objNameOrdering)
			if (objName.getFirst() == objectType)
				only.objNameOrdering.add(objName);
		
		if (objectType == ObjectType.MODEL) {
			for (Entry<String, SupervisedModel<D, L>> entry : this.models.entrySet())
				only.models.put(entry.getKey(), entry.getValue());
		} else if (objectType == ObjectType.FEATURE) {
			for (Entry<String, Feature<D, L>> entry : this.features.entrySet())
				only.features.put(entry.getKey(), entry.getValue());
		} else if (objectType == ObjectType.GRID_SEARCH) {
			for (Entry<String, GridSearch<D, L>> entry : this.gridSearches.entrySet())
				only.gridSearches.put(entry.getKey(), entry.getValue());
		} else if (objectType == ObjectType.EVALUATION) {
			for (Entry<String, SupervisedModelEvaluation<D, L>> entry : this.evaluations.entrySet())
				only.evaluations.put(entry.getKey(), entry.getValue());
		} else if (objectType == ObjectType.RULE_SET) {
			for (Entry<String, RuleSet<D, L>> entry : this.ruleSets.entrySet())
				only.ruleSets.put(entry.getKey(), entry.getValue());
		} else if (objectType == ObjectType.TOKEN_SPAN_FN) {
			only.tokenSpanFns = this.tokenSpanFns;			
		} else if (objectType == ObjectType.STR_FN) {
			only.strFns = this.strFns;
		} else if (objectType == ObjectType.TOKEN_SPAN_STR_FN) {
			only.tokenSpanStrFns = this.tokenSpanStrFns;
		} else if (objectType == ObjectType.ARRAY) {
			for (Entry<String, List<String>> entry : this.arrays.entrySet())
				only.arrays.put(entry.getKey(), entry.getValue());	
		} else if (objectType == ObjectType.VALUE) {
			for (Entry<String, String> entry : this.values.entrySet())
				only.values.put(entry.getKey(), entry.getValue());	
		}
		
		return only;
	}

	public Context<D, L> except(ObjectType objectType) {
		Context<D, L> except = new Context<D, L>(this.datumTools);
		
		for (Pair<ObjectType, String> objName : this.objNameOrdering)
			if (objName.getFirst() != objectType)
				except.objNameOrdering.add(objName);
		
		if (objectType != ObjectType.MODEL) {
			for (Entry<String, SupervisedModel<D, L>> entry : this.models.entrySet())
				except.models.put(entry.getKey(), entry.getValue());
		} 
		
		if (objectType != ObjectType.FEATURE) {
			for (Entry<String, Feature<D, L>> entry : this.features.entrySet())
				except.features.put(entry.getKey(), entry.getValue());
		} 
		
		if (objectType != ObjectType.GRID_SEARCH) {
			for (Entry<String, GridSearch<D, L>> entry : this.gridSearches.entrySet())
				except.gridSearches.put(entry.getKey(), entry.getValue());
		} 
		
		if (objectType != ObjectType.EVALUATION) {
			for (Entry<String, SupervisedModelEvaluation<D, L>> entry : this.evaluations.entrySet())
				except.evaluations.put(entry.getKey(), entry.getValue());
		} 
		
		if (objectType != ObjectType.RULE_SET) {
			for (Entry<String, RuleSet<D, L>> entry : this.ruleSets.entrySet())
				except.ruleSets.put(entry.getKey(), entry.getValue());
		} 
		
		if (objectType != ObjectType.TOKEN_SPAN_FN) {
			except.tokenSpanFns = this.tokenSpanFns;
		} 
		
		if (objectType != ObjectType.STR_FN) {
			except.strFns = this.strFns;	
		} 
		
		if (objectType != ObjectType.TOKEN_SPAN_STR_FN) {
			except.tokenSpanStrFns = this.tokenSpanStrFns;
		} 
		
		if (objectType != ObjectType.ARRAY) {
			for (Entry<String, List<String>> entry : this.arrays.entrySet())
				except.arrays.put(entry.getKey(), entry.getValue());	
		}
		
		if (objectType != ObjectType.VALUE) {
			for (Entry<String, String> entry : this.values.entrySet())
				except.values.put(entry.getKey(), entry.getValue());	
		}
		
		return except;
	}
	
	public static <D extends Datum<L>, L> Context<D, L> deserialize(Datum.Tools<D, L> datumTools, String str) {
		return deserialize(datumTools, new StringReader(str));
	}
	
	public static <D extends Datum<L>, L> Context<D, L> deserialize(Datum.Tools<D, L> datumTools, Reader reader) {
		CtxScanner scanner = new CtxScanner(reader);
		CtxParser parser = new CtxParser(scanner, new ComplexSymbolFactory());
		AssignmentList parse = null;
		try {
			parse = (AssignmentList)parser.parse().value;
		} catch (Exception e) {
			return null;
		}
		
		Context<D, L> context = new Context<D, L>(datumTools);
		if (!context.fromParse(parse))
			return null;
		return context;
	}
}
