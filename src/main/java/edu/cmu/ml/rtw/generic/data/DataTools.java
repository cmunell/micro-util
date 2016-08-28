package edu.cmu.ml.rtw.generic.data;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import edu.cmu.ml.rtw.generic.opt.search.Search;
import edu.cmu.ml.rtw.generic.opt.search.SearchGrid;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.parse.Obj.Function;
import edu.cmu.ml.rtw.generic.rule.RuleSet;
import edu.cmu.ml.rtw.generic.str.StringTransform;
import edu.cmu.ml.rtw.generic.str.StringTransformRemoveLongTokens;
import edu.cmu.ml.rtw.generic.str.StringTransformRemoveSymbols;
import edu.cmu.ml.rtw.generic.str.StringTransformReplaceNumbers;
import edu.cmu.ml.rtw.generic.str.StringTransformSpaceToUnderscore;
import edu.cmu.ml.rtw.generic.str.StringTransformStem;
import edu.cmu.ml.rtw.generic.str.StringTransformToLowerCase;
import edu.cmu.ml.rtw.generic.str.StringTransformTrim;
import edu.cmu.ml.rtw.generic.str.StringTransformUnderscoreToSpace;
import edu.cmu.ml.rtw.generic.structure.FnGraphOpenTriangles;
import edu.cmu.ml.rtw.generic.structure.FnGraphPaths;
import edu.cmu.ml.rtw.generic.structure.FnGreedyStructureRules;
import edu.cmu.ml.rtw.generic.structure.WeightedStructure;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureGraph;
import edu.cmu.ml.rtw.generic.structure.WeightedStructureSequence;
import edu.cmu.ml.rtw.generic.task.classify.multi.EvaluationMultiClassification;
import edu.cmu.ml.rtw.generic.task.classify.multi.EvaluationMultiClassificationMeasureAccuracy;
import edu.cmu.ml.rtw.generic.task.classify.multi.EvaluationMultiClassificationMeasureF;
import edu.cmu.ml.rtw.generic.task.classify.multi.EvaluationMultiClassificationMeasurePrecision;
import edu.cmu.ml.rtw.generic.task.classify.multi.EvaluationMultiClassificationMeasureRecall;
import edu.cmu.ml.rtw.generic.task.classify.multi.MethodMultiClassification;
import edu.cmu.ml.rtw.generic.task.classify.multi.MethodMultiClassificationPrecedenceScore;
import edu.cmu.ml.rtw.generic.task.classify.multi.MethodMultiClassificationPrecedenceScoreByModel;
import edu.cmu.ml.rtw.generic.task.classify.multi.MethodMultiClassificationRandomSieve;
import edu.cmu.ml.rtw.generic.task.classify.multi.MethodMultiClassificationSelfTrain;
import edu.cmu.ml.rtw.generic.task.classify.multi.MethodMultiClassificationSieve;
import edu.cmu.ml.rtw.generic.task.classify.multi.TaskMultiClassification;
import edu.cmu.ml.rtw.generic.util.NamedIterable;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Properties;
import edu.cmu.ml.rtw.generic.util.StringUtil;
import edu.cmu.ml.rtw.generic.util.Timer;
import edu.cmu.ml.rtw.generic.util.WeightedStringList;
import edu.cmu.ml.rtw.generic.cluster.Clusterer;
import edu.cmu.ml.rtw.generic.cluster.ClustererString;
import edu.cmu.ml.rtw.generic.cluster.ClustererTokenSpanPoSTag;
import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Document;
import edu.cmu.ml.rtw.generic.data.annotation.SerializerDocument;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPDatum;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPBSON;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPHTML;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPJSONLegacy;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPMicro;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpansDatum;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Word2Vec;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.WordNet;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.time.NormalizedTimeValue;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.time.TimeExpression;
import edu.cmu.ml.rtw.generic.data.feature.SerializerDataFeatureMatrixBSONString;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnAffix;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnCat;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnClean;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnComposite;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnCompositeAppend;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnCoref;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnDependencyRelation;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnFilter;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnFilterPoSTagClass;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnGazetteer;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnGazetteerFilter;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnHead;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnIdentity;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnNGramContext;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnNGramDocument;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnNGramInside;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnNGramSentence;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnPoS;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnPoSUniversal;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnPredicateArgument;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnPredicateSense;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnPrepositionOfClause;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnRelationStr;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnRemoveLongStringParts;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnSentencePosition;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnSplit;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnStemStringParts;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnString;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnStringCase;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnStringListReplace;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnStringReplace;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnTail;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnTokenAnnotation;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnTokenSpanLengthFilter;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnTokenSpanPathStr;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnTrim;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnWordNetLemma;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnWordNetSynset;

/**
 * 
 * DataTools loads gazetteers, brown clusterers, string cleaning 
 * functions, and other tools used in various 
 * models and experiments.  
 * 
 * A DataSet (in edu.cmu.ml.rtw.generic.data.annotation) has
 * access to a Tools object (defined in edu.cmu.ml.rtw.generic.data.annotation.Datum),
 * and that object contains a pointer to a DataTools object, so
 * any place in the code that has access to a DataSet also has access
 * to DataTools.  The difference between DataTools and Datum.Tools is
 * that Datum.Tools contains tools specific to a particular kind of
 * datum (e.g. a document datum in text classification or a tlink datum
 * in temporal ordering), whereas DataTools contains generic tools
 * that can be useful when working with many kinds of Datums.  This
 * split between generic tools and datum-specific tools allows the generic
 * tools to be loaded into memory only once even if you're working with
 * many kinds of datums at the same time.
 * 
 * Currently, for convenience, DataTools just loads everything into 
 * memory upon construction.  If memory conservation becomes particularly
 * important, then possibly this class should be rewritten to only keep 
 * things in memory when they are needed.
 * 
 * @author Bill McDowell
 *
 */
public class DataTools {	
	/**
	 * Interface for a function that maps a pair of strings to an indicator
	 *
	 */
	public interface StringPairIndicator {
		boolean compute(String str1, String str2);
	}
	
	/**
	 * Interface for a function that maps a pair of strings to a real number--
	 * for example, as a measure of their similarity.
	 *
	 */
	public interface StringPairMeasure {
		double compute(String str1, String str2);
	}
	
	/**
	 * Interface for a function that maps a string to a collection of strings--
	 * for example, to compute a collection of prefixes or suffixes for a string.
	 *
	 */
	public interface StringCollectionTransform {
		Collection<String> transform(String str);
		String toString();
	}
	
	/**
	 * Represents a named file path.  It's useful for file paths to have
	 * names so that they can be referenced in experiment configuration files
	 * without machine specific file locations.
	 *
	 */
	public class Path {
		private String name;
		private String value;
		
		public Path(String name, String value) {
			this.name = name;
			this.value = value;
		}
		
		public String getValue() {
			return this.value;
		}
		
		public String getName() {
			return this.name;
		}
	}
	
	public static interface Command<T> {
		T run(Context context, List<String> modifiers, String referenceName, Obj.Function fnObj);
	}
	
	public static interface MakeInstanceFn<T> {
		T make(String name, Context context);
	}
	
	protected Map<String, Gazetteer> gazetteers;
	protected Map<String, StringTransform> cleanFns;
	protected Map<String, StringPairIndicator> stringPairIndicatorsFns;
	protected Map<String, DataTools.StringCollectionTransform> collectionFns;
	protected Map<String, Clusterer<String>> stringClusterers;
	protected Map<String, Clusterer<TokenSpan>> tokenSpanClusterers;
	protected Map<String, Path> paths;

	protected Map<String, AnnotationTypeNLP<?>> annotationTypesNLP;
	protected Map<String, SerializerDocument<?, ?>> documentSerializers;
	
	protected Map<String, List<Fn<TokenSpan, TokenSpan>>> genericTokenSpanFns;
	protected Map<String, List<Fn<TokenSpan, String>>> genericTokenSpanStrFns;
	protected Map<String, List<Fn<String, String>>> genericStrFns;
	protected Map<String, List<Fn<?, ?>>> genericStructureFns;
	
	protected Map<String, Search> genericSearches;
	protected Map<String, WeightedStructure> genericStructures;
	
	protected Map<String, MethodMultiClassification> genericMultiClassifyMethods;
	protected Map<String, EvaluationMultiClassification<?>> genericMultiClassifyEvals;
	
	protected Map<String, List<Command<?>>> commands;
	
	protected Map<String, Context> genericContexts;
	
	protected Properties properties;
	protected StoredItemSetManager storedItemSetManager;
	
	protected long randomSeed = 1;
	protected Random globalRandom;
	protected OutputWriter outputWriter;
	protected Timer timer;
	protected WordNet wordNet;
	protected Word2Vec word2Vec;
	
	protected int incrementId = 0;
	
	public DataTools() {
		this(new OutputWriter());
	}
	
	public DataTools(OutputWriter outputWriter) {
		this(outputWriter, null);
	}
	
	public DataTools(OutputWriter outputWriter, Properties properties) {
		this.gazetteers = new HashMap<String, Gazetteer>();
		this.cleanFns = new HashMap<String, StringTransform>();
		this.stringPairIndicatorsFns = new HashMap<String, StringPairIndicator>();
		this.collectionFns = new HashMap<String, DataTools.StringCollectionTransform>();
		this.stringClusterers = new HashMap<String, Clusterer<String>>();
		this.tokenSpanClusterers = new HashMap<String, Clusterer<TokenSpan>>();
		this.paths = new HashMap<String, Path>();
		this.annotationTypesNLP = new HashMap<String, AnnotationTypeNLP<?>>();
		this.documentSerializers = new HashMap<String, SerializerDocument<?, ?>>();
		
		this.genericTokenSpanFns = new HashMap<String, List<Fn<TokenSpan, TokenSpan>>>();
		this.genericTokenSpanStrFns = new HashMap<String, List<Fn<TokenSpan, String>>>();
		this.genericStrFns = new HashMap<String, List<Fn<String, String>>>();
		this.genericStructureFns = new HashMap<String, List<Fn<?, ?>>>();
		
		this.genericSearches = new HashMap<String, Search>();
		this.genericStructures = new HashMap<String, WeightedStructure>();
		
		this.genericMultiClassifyMethods = new HashMap<String, MethodMultiClassification>();
		this.genericMultiClassifyEvals = new HashMap<String, EvaluationMultiClassification<?>>();
		
		this.commands = new HashMap<String, List<Command<?>>>();
		
		this.genericContexts = new HashMap<String, Context>();
		
		this.properties = properties;
		
		this.outputWriter = outputWriter;
		
		this.addCleanFn(new StringTransform() {
			public String toString() {
				return "DefaultCleanFn";
			}
			
			public String transform(String str) {
				return StringUtil.clean(str);
			}
		});
		
		this.addCleanFn(new StringTransformRemoveLongTokens());
		this.addCleanFn(new StringTransformRemoveSymbols());
		this.addCleanFn(new StringTransformReplaceNumbers());
		this.addCleanFn(new StringTransformSpaceToUnderscore());
		this.addCleanFn(new StringTransformStem());
		this.addCleanFn(new StringTransformToLowerCase());
		this.addCleanFn(new StringTransformTrim());
		this.addCleanFn(new StringTransformUnderscoreToSpace());
		
		this.collectionFns.put("Prefixes", new DataTools.StringCollectionTransform() {
			public String toString() {
				return "Prefixes";
			}
			
			public Collection<String> transform(String str) {
				return StringUtil.prefixes(str);
			}
		});
		
		this.addTokenSpanClusterer(new ClustererTokenSpanPoSTag());
		
		this.collectionFns.put("None", null);
		this.stringClusterers.put("None", null);
		this.tokenSpanClusterers.put("None", null);
		this.globalRandom = new Random(this.randomSeed);
		this.timer = new Timer();
		
		this.addAnnotationTypeNLP(AnnotationTypeNLP.ORIGINAL_TEXT);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.LANGUAGE);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.TOKEN);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.SENTENCE);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.POS);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.LEMMA);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.NER);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.COREF);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.DEPENDENCY_PARSE);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.CONSTITUENCY_PARSE);
		this.addAnnotationTypeNLP(AnnotationTypeNLP.PREDICATE);
		
		this.addDocumentSerializer(new SerializerDocumentNLPBSON(this));
		this.addDocumentSerializer(new SerializerDocumentNLPHTML(this));
		this.addDocumentSerializer(new SerializerDocumentNLPJSONLegacy(this));
		this.addDocumentSerializer(new SerializerDocumentNLPMicro(this));
		
		this.addGenericTokenSpanFn(new FnComposite.FnCompositeTokenSpan());
		this.addGenericTokenSpanFn(new FnCompositeAppend.FnCompositeAppendTokenSpan());
		this.addGenericTokenSpanFn(new FnHead());
		this.addGenericTokenSpanFn(new FnTail());
		this.addGenericTokenSpanFn(new FnNGramContext());
		this.addGenericTokenSpanFn(new FnNGramDocument());
		this.addGenericTokenSpanFn(new FnNGramInside());
		this.addGenericTokenSpanFn(new FnNGramSentence());
		this.addGenericTokenSpanFn(new FnIdentity<TokenSpan>());
		this.addGenericTokenSpanFn(new FnCoref());
		this.addGenericTokenSpanFn(new FnDependencyRelation());
		this.addGenericTokenSpanFn(new FnFilterPoSTagClass());
		this.addGenericTokenSpanFn(new FnPredicateArgument());
		this.addGenericTokenSpanFn(new FnPrepositionOfClause());
		this.addGenericTokenSpanFn(new FnTokenSpanLengthFilter());
		
		this.addGenericTokenSpanStrFn(new FnComposite.FnCompositeTokenSpanTokenSpanStr());
		this.addGenericTokenSpanStrFn(new FnComposite.FnCompositeTokenSpanStrStr());
		this.addGenericTokenSpanStrFn(new FnRelationStr.FnRelationStrTokenSpan());
		this.addGenericTokenSpanStrFn(new FnPoS());
		this.addGenericTokenSpanStrFn(new FnPoSUniversal());
		this.addGenericTokenSpanStrFn(new FnString());
		this.addGenericTokenSpanStrFn(new FnTokenSpanPathStr());
		this.addGenericTokenSpanStrFn(new FnTokenAnnotation());
		this.addGenericTokenSpanStrFn(new FnSentencePosition());
		this.addGenericTokenSpanStrFn(new FnWordNetLemma());
		this.addGenericTokenSpanStrFn(new FnWordNetSynset());
		this.addGenericTokenSpanStrFn(new FnPredicateSense());
		
		this.addGenericStrFn(new FnComposite.FnCompositeStr());
		this.addGenericStrFn(new FnCompositeAppend.FnCompositeAppendStr());
		this.addGenericStrFn(new FnRelationStr.FnRelationStrStr());
		this.addGenericStrFn(new FnAffix());
		this.addGenericStrFn(new FnFilter());
		this.addGenericStrFn(new FnGazetteerFilter());
		this.addGenericStrFn(new FnGazetteer());
		this.addGenericStrFn(new FnSplit());
		this.addGenericStrFn(new FnIdentity<String>());
		this.addGenericStrFn(new FnClean());
		this.addGenericStrFn(new FnCat());
		this.addGenericStrFn(new FnRemoveLongStringParts());
		this.addGenericStrFn(new FnStemStringParts());
		this.addGenericStrFn(new FnStringCase());
		this.addGenericStrFn(new FnStringReplace());
		this.addGenericStrFn(new FnTrim());
		this.addGenericStrFn(new FnStringListReplace());
		
		this.addGenericStructureFn(new FnGreedyStructureRules<WeightedStructureGraph>());
		this.addGenericStructureFn(new FnGraphPaths());
		this.addGenericStructureFn(new FnGraphOpenTriangles());
		
		this.addGenericSearch(new SearchGrid());
		
		this.addGenericWeightedStructure(new WeightedStructureGraph());
		this.addGenericWeightedStructure(new WeightedStructureSequence());
		
		this.addGenericMultiClassifyEval(new EvaluationMultiClassificationMeasureAccuracy());
		this.addGenericMultiClassifyEval(new EvaluationMultiClassificationMeasureF());
		this.addGenericMultiClassifyEval(new EvaluationMultiClassificationMeasurePrecision());
		this.addGenericMultiClassifyEval(new EvaluationMultiClassificationMeasureRecall());
		
		this.addGenericMultiClassifyMethod(new MethodMultiClassificationSieve());
		this.addGenericMultiClassifyMethod(new MethodMultiClassificationRandomSieve());
		this.addGenericMultiClassifyMethod(new MethodMultiClassificationPrecedenceScore());
		this.addGenericMultiClassifyMethod(new MethodMultiClassificationPrecedenceScoreByModel());
		this.addGenericMultiClassifyMethod(new MethodMultiClassificationSelfTrain());
		
		this.addGenericContext(new DatumContext<DocumentNLPDatum<Boolean>, Boolean>(DocumentNLPDatum.getBooleanTools(this), "DocumentNLPBoolean"));
		this.addGenericContext(new DatumContext<DocumentNLPDatum<String>, String>(DocumentNLPDatum.getStringTools(this), "DocumentNLPString"));
		this.addGenericContext(new DatumContext<DocumentNLPDatum<WeightedStringList>, WeightedStringList>(DocumentNLPDatum.getWeightedStringListTools(this), "DocumentNLPWeightedStringList"));
		this.addGenericContext(new DatumContext<TokenSpansDatum<Boolean>, Boolean>(TokenSpansDatum.getBooleanTools(this), "TokenSpansBoolean"));
		this.addGenericContext(new DatumContext<TokenSpansDatum<String>, String>(TokenSpansDatum.getStringTools(this), "TokenSpansString"));
		this.addGenericContext(new DatumContext<TokenSpansDatum<WeightedStringList>, WeightedStringList>(TokenSpansDatum.getWeightedStringListTools(this), "TokenSpansWeightedStringList"));
		
		addConstructionCommand(new TaskMultiClassification(null), new MakeInstanceFn<TaskMultiClassification>() {
			public TaskMultiClassification make(String name, Context parentContext) {
				return new TaskMultiClassification(parentContext); } });
		
		this.addConstructionCommand(new RuleSet(null), new MakeInstanceFn<RuleSet>() {
			public RuleSet make(String name, Context parentContext) {
				return new RuleSet(parentContext); } });
		
		this.addCommand("MultiplyValues", new Command<String>() {
			@Override
			public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				AssignmentList parameters = fnObj.getParameters();
				double value1 = Double.valueOf(context.getMatchValue(parameters.get("value1").getValue()));
				double value2 = Double.valueOf(context.getMatchValue(parameters.get("value2").getValue()));
				boolean round = !parameters.contains("round") ? false : Boolean.valueOf(context.getMatchValue(parameters.get("round").getValue()));
				
				return (round) ? String.valueOf((int)Math.floor(value1*value2)) : String.valueOf(value1*value2);
			}
		});
		
		addCommand("InitMultiClassifyMethod", new Command<MethodMultiClassification>() {
			@Override
			public MethodMultiClassification run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				AssignmentList parameters = fnObj.getParameters();

				TaskMultiClassification task = context.getMatchMultiClassifyTask(parameters.get("devTask").getValue());
				MethodMultiClassification method = (MethodMultiClassification)context.getMatchMultiClassifyMethod(parameters.get("method").getValue());
				if (!method.init(task.getData()))
					return null;
				else
					return method;
			}
		});
		
		addCommand("CloneSearch", new Command<Search>() {
			@Override
			public Search run(Context context, List<String> modifiers,
					String referenceName, Function fnObj) {
				AssignmentList parameters = fnObj.getParameters();
				Search search = context.getMatchSearch(parameters.get("search").getValue());
				return search.clone(referenceName);
			}
		});
		
		addCommand("ConcatenateArrays", new Command<List<String>>() {
			@Override
			public List<String> run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				AssignmentList parameters = fnObj.getParameters();
				List<String> arr1 = context.getMatchArray(parameters.get("a1").getValue());
				List<String> arr2 = context.getMatchArray(parameters.get("a2").getValue());
				List<String> ret = new ArrayList<String>();
				ret.addAll(arr1);
				ret.addAll(arr2);
				return ret;
			}
		});
		
		
		addCommand("SizeArray", new Command<String>() {
			@Override
			public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				AssignmentList parameters = fnObj.getParameters();
				return String.valueOf(context.getMatchArray(parameters.get("array").getValue()).size());
			}
		});
		
		this.addCommand("BuildCleanFn", new Command<String>() {
			@Override
			public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				String name = context.getMatchValue(fnObj.getParameters().get("name").getValue());
				List<String> fns = context.getMatchArray(fnObj.getParameters().get("fns").getValue());
				
				return String.valueOf(context.getDataTools().addCleanFn(new StringTransform() {
					@Override
					public String toString() {
						return name;
					}
					
					@Override
					public String transform(String str) {
						for (String fn : fns) {
							str = context.getDataTools().getCleanFn(fn).transform(str);
						}
						
						return str;
					}
				}));
			}
		});
		
		this.addCommand("LoadGazetteer", new Command<String>() {
			@Override
			public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				String name = context.getMatchValue(fnObj.getParameters().get("name").getValue());
				String storageName = context.getMatchValue(fnObj.getParameters().get("storageName").getValue());
				String collectionName = context.getMatchValue(fnObj.getParameters().get("collectionName").getValue());
				
				BufferedReader reader = 
					context.getDataTools().getStoredItemSetManager().getItemSet(storageName, collectionName)
					.getStoredItems()
					.getFirstReaderByIndex(SerializerGazetteerString.NAME_INDEX_FIELD, name);
				
				StringTransform cleanFn = context.getDataTools().getCleanFn(context.getMatchValue(fnObj.getParameters().get("cleanFn").getValue()));
				boolean hasWeights = Boolean.valueOf(context.getMatchValue(fnObj.getParameters().get("hasWeights").getValue()));
				return String.valueOf(context.getDataTools().addGazetteer(new Gazetteer(name, reader, cleanFn, hasWeights)));
			}
		});
		
		this.addCommand("SetRandomSeed", new Command<String>() {
			@Override
			public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				int seed = 0;
				if (fnObj.getParameters().contains("seed"))
					seed = Integer.valueOf(context.getMatchValue(fnObj.getParameters().get("seed").getValue()));
				else
					seed = context.getRandomSeed();
					
				if (!context.getDataTools().setRandomSeed(seed))
					return null;
				else
					return String.valueOf(seed);
			}
		});
		
		this.addCommand("Debug", new Command<String>() {
			@Override
			public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				File outputFile = null;
				if (fnObj.getParameters().contains("file"))
					outputFile = new File(DataTools.this.properties.getDebugDirectory(),
						context.getMatchValue(fnObj.getParameters().get("file").getValue()));
				
				return String.valueOf( 
					DataTools.this.outputWriter.setDebugFile(outputFile, false));
			}
		});
		
		this.addCommand("OutputContext", new Command<String>() {
			@Override
			public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				AssignmentList parameters = fnObj.getParameters();
				String storageName = context.getMatchValue(parameters.get("storage").getValue());
				String collectionName = context.getMatchValue(parameters.get("collection").getValue());
				String id = context.getMatchValue(parameters.get("id").getValue());
				SerializerAssignmentListString serializer = new SerializerAssignmentListString(DataTools.this);
				
				return String.valueOf(
					DataTools.this.getStoredItemSetManager()
						.getItemSet(storageName, collectionName, true, serializer)
						.addItem(new NamedIterable<AssignmentList, Assignment>(id, ((Obj.Function)context.getRootContext().toParse(true)).getInternalAssignments())
					));
			}
		});
		
		this.addCommand("OutputParses", new Command<String>() {
			@Override
			public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				AssignmentList parameters = fnObj.getParameters();
				String storageName = context.getMatchValue(parameters.get("storage").getValue());
				String collectionName = context.getMatchValue(parameters.get("collection").getValue());
				String id = context.getMatchValue(parameters.get("id").getValue());
				
				Obj.Array objRefs = (Obj.Array)parameters.get("fns").getValue();
				List<String> objTypes = context.getMatchArray(parameters.get("types").getValue());
				List<String> objParams = (parameters.contains("params")) ?
											context.getMatchArray(parameters.get("params").getValue())
										  : null;
											
				List<String> objNames= (parameters.contains("names")) ?
											context.getMatchArray(parameters.get("names").getValue())
										  : null;
				
				SerializerAssignmentListString serializer = new SerializerAssignmentListString(DataTools.this);
				AssignmentList list = new AssignmentList();
				for (int i = 0; i < objRefs.size(); i++) {
					List<?> objs = context.getAssignedMatches(objRefs.get(i));
					CtxParsableFunction ctxObj = (CtxParsableFunction)objs.get(0);
					Obj obj = null;
					if (objParams != null && objParams.get(i).length() > 0) {
						obj = ctxObj.getParameterValue(objParams.get(i));
					} else {
						obj = ctxObj.toParse();
					}
					
					list.add(Assignment.assignmentTyped(null, objTypes.get(i), (objNames != null) ? objNames.get(i) : objRefs.getStr(i), obj));
				}
				
				return String.valueOf(
					DataTools.this.getStoredItemSetManager()
						.getItemSet(storageName, collectionName, true, serializer)
						.addItem(new NamedIterable<AssignmentList, Assignment>(id, list)
					));
			}
		});
		
		this.addCommand("OutputStrings", new Command<String>() {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				AssignmentList parameters = fnObj.getParameters();
				String storageName = context.getMatchValue(parameters.get("storage").getValue());
				String collectionName = context.getMatchValue(parameters.get("collection").getValue());
				
				Obj.Array objRefs = (Obj.Array)parameters.get("refs").getValue();

				List<Object> list = new ArrayList<Object>();
				for (int i = 0; i < objRefs.size(); i++) {
					List<?> objs = context.getAssignedMatches(objRefs.get(i));
					String name = objRefs.get(i).getStr();
					if (name.contains("."))
						name = name.substring(0, name.lastIndexOf('.'));
					list.add(new Pair<String, Object>(name, objs.get(0)));
				}
				
				if (!parameters.contains("serializer")) {
					String id = context.getMatchValue(parameters.get("id").getValue());
					SerializerNamedIterableToString serializer = new SerializerNamedIterableToString();
					return String.valueOf(
						DataTools.this.getStoredItemSetManager()
							.getItemSet(storageName, collectionName, true, serializer)
							.addItem(new NamedIterable<List<Object>, Object>(id, list)
						));
				} else {
					if (objRefs.size() > 1)
						return "false";
					
					String ser = context.getMatchValue(parameters.get("serializer").getValue());
					Serializer serializer = DataTools.this.getSerializers().get(ser);
					return String.valueOf(
							DataTools.this.getStoredItemSetManager()
								.getItemSet(storageName, collectionName, true, serializer)
								.addItem(((Pair<String, Object>)list.get(0)).getSecond()
							));
				}
			}
		});
		
		this.addCommand("OutputDebug", new Command<String>() {
			@Override
			public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				AssignmentList parameters = fnObj.getParameters();
				
				if (parameters.contains("refs")) {
					Obj.Array objRefs = (Obj.Array)parameters.get("refs").getValue();
	
					for (int i = 0; i < objRefs.size(); i++) {
						List<?> objs = context.getAssignedMatches(objRefs.get(i));
						context.getDataTools().getOutputWriter().debugWriteln(objRefs.getStr(i) + ": " + objs.get(0).toString());
					}
				} else if (parameters.contains("str")) {
					context.getDataTools().getOutputWriter().debugWriteln(
							context.getMatchValue(parameters.get("str").getValue()));
				} else {
					return String.valueOf("false");
				}
				
				return String.valueOf("true");
			}
		});
	}
	
	public synchronized int getIncrementId() {
		this.incrementId += 1;
		return this.incrementId;
	}
	
	// Inclusive start, Non-inclusive end
	public synchronized Pair<Integer, Integer> getIncrementIdRange(int range) {
		int start = this.incrementId + 1;
		int end = start + range;
		this.incrementId = end - 1;
		return new Pair<Integer, Integer>(start, end);
	}
	
	public StoredItemSetManager getStoredItemSetManager() {
		if (this.storedItemSetManager == null) {	
			Map<String, Serializer<?, ?>> serializers = getSerializers();
			this.storedItemSetManager = new StoredItemSetManager(this.properties, serializers);
		}
		
		return this.storedItemSetManager;
	}
	
	public Gazetteer getGazetteer(String name) {
		return this.gazetteers.get(name);
	}
	
	public StringTransform getCleanFn(String name) {
		return this.cleanFns.get(name);
	}
	
	public StringPairIndicator getStringPairIndicatorFn(String name) {
		return this.stringPairIndicatorsFns.get(name);
	}
	
	public DataTools.StringCollectionTransform getCollectionFn(String name) {
		return this.collectionFns.get(name);
	}
	
	public Clusterer<String> getStringClusterer(String name) {
		return this.stringClusterers.get(name);
	}
	
	public Clusterer<TokenSpan> getTokenSpanClusterer(String name) {
		return this.tokenSpanClusterers.get(name);
	}
	
	public Path getPath(String name) {
		return this.paths.get(name);
	}

	public AnnotationTypeNLP<?> getAnnotationTypeNLP(String type) {
		return this.annotationTypesNLP.get(type);
	}
	
	public Collection<AnnotationTypeNLP<?>> getAnnotationTypesNLP() {
		return this.annotationTypesNLP.values();
	}
	
	@SuppressWarnings("unchecked")
	public <D extends Document> SerializerDocument<D, ?> getDocumentSerializer(String name, D genericDocument, Collection<AnnotationType<?>> annotationTypes) {
		return ((SerializerDocument<D, ?>)this.documentSerializers.get(name)).makeInstance(genericDocument, annotationTypes);
	}
	
	public <D extends Document> Map<String, Serializer<?, ?>> getDocumentSerializers(D genericDocument, Collection<AnnotationType<?>> annotationTypes) {
		Map<String, Serializer<?, ?>> map = new HashMap<String, Serializer<?, ?>>();
		for (String key : this.documentSerializers.keySet())
			map.put(key, getDocumentSerializer(key, genericDocument, annotationTypes));
		return map;
	}

	public Map<String, Serializer<?, ?>> getSerializers() {
		Map<String, Serializer<?, ?>> serializers = new HashMap<String, Serializer<?, ?>>();
		serializers.putAll(this.documentSerializers);
		
		SerializerAssignmentListString aListSerializer = new SerializerAssignmentListString(this);
		serializers.put(aListSerializer.getName(), aListSerializer);
		SerializerNamedIterableToString nIterSerializer = new SerializerNamedIterableToString();
		serializers.put(nIterSerializer.getName(), nIterSerializer);
		SerializerGazetteerString gSerializer = new SerializerGazetteerString();
		serializers.put(gSerializer.getName(), gSerializer);
		SerializerDataFeatureMatrixBSONString dSerializer = new SerializerDataFeatureMatrixBSONString(this);
		serializers.put(dSerializer.getName(), dSerializer);
		
		SerializerJSONBSON<TimeExpression> timeExpressionSerializer = new SerializerJSONBSON<TimeExpression>("TimeExpression", new TimeExpression(this));
		serializers.put(timeExpressionSerializer.getName(), timeExpressionSerializer);
		SerializerJSONBSON<NormalizedTimeValue> timeValueSerializer = new SerializerJSONBSON<NormalizedTimeValue>("NormalizedTimeValue", new NormalizedTimeValue(this));
		serializers.put(timeValueSerializer.getName(), timeValueSerializer);
		
		return serializers;
	}
	
	public OutputWriter getOutputWriter() {
		return this.outputWriter;
	}
	
	public Random getGlobalRandom() {
		return getGlobalRandom(this.randomSeed);
	}
	
	/**
	 * @return a Random object instance that was instantiated when the DataTools
	 * object was instantiated. 
	 */
	public Random getGlobalRandom(long seed) {
		if (this.randomSeed != seed) {
			this.randomSeed = seed;
			this.globalRandom = new Random(seed);
		}
		return this.globalRandom;
	}
	
	/**
	 * @return a Random object instance that is instantiated when makeLocalRandom 
	 * is called.  This is useful when there are multiple threads that require
	 * their own Random instances in order to preserve determinism with respect
	 * to a single Random seed.  Otherwise, if threads share the same Random 
	 * instance, then the order in which they interleave execution will determine 
	 * the behavior of the program.
	 * 
	 */
	public Random makeLocalRandom(long seed) {
		return new Random(seed);
	}
	
	public Random makeLocalRandom() {
		return new Random(this.randomSeed); 
	}
	
	public long getRandomSeed() {
		return this.randomSeed;
	}
	
	public Timer getTimer() {
		return this.timer;
	}
	
	public List<Fn<String, String>> makeStrFns(String genericStrFnName, Context context) {
		if (!this.genericStrFns.containsKey(genericStrFnName))
			return new ArrayList<Fn<String, String>>();
		List<Fn<String, String>> genericStrFns = this.genericStrFns.get(genericStrFnName);
		List<Fn<String, String>> strFns = new ArrayList<Fn<String, String>>(genericStrFns.size());
		
		for (Fn<String, String> genericStrFn : genericStrFns)
			strFns.add(genericStrFn.makeInstance(context));
		
		return strFns;
	}
	
	public List<Fn<TokenSpan, TokenSpan>> makeTokenSpanFns(String genericTokenSpanFnName, Context context) {
		if (!this.genericTokenSpanFns.containsKey(genericTokenSpanFnName))
			return new ArrayList<Fn<TokenSpan, TokenSpan>>();
		
		List<Fn<TokenSpan, TokenSpan>> genericTokenSpanFns = this.genericTokenSpanFns.get(genericTokenSpanFnName);
		List<Fn<TokenSpan, TokenSpan>> tokenSpanFns = new ArrayList<Fn<TokenSpan, TokenSpan>>(genericTokenSpanFns.size());
		
		for (Fn<TokenSpan, TokenSpan> genericTokenSpanFn : genericTokenSpanFns)
			tokenSpanFns.add(genericTokenSpanFn.makeInstance(context));
		
		return tokenSpanFns;
	}
	
	public List<Fn<TokenSpan, String>> makeTokenSpanStrFns(String genericTokenSpanStrFnName, Context context) {
		if (!this.genericTokenSpanStrFns.containsKey(genericTokenSpanStrFnName))
			return new ArrayList<Fn<TokenSpan, String>>();
		List<Fn<TokenSpan, String>> genericTokenSpanStrFns = this.genericTokenSpanStrFns.get(genericTokenSpanStrFnName);
		List<Fn<TokenSpan, String>> tokenSpanStrFns = new ArrayList<Fn<TokenSpan, String>>(genericTokenSpanStrFns.size());
		
		for (Fn<TokenSpan, String> genericTokenSpanStrFn : genericTokenSpanStrFns)
			tokenSpanStrFns.add(genericTokenSpanStrFn.makeInstance(context));
		
		return tokenSpanStrFns;
	}
	
	public List<Fn<?, ?>> makeStructureFns(String genericStructureFnName, Context context) {
		if (!this.genericStructureFns.containsKey(genericStructureFnName))
			return new ArrayList<Fn<?, ?>>();
		List<Fn<?, ?>> genericStructureFns = this.genericStructureFns.get(genericStructureFnName);
		List<Fn<?, ?>> structureFns = new ArrayList<Fn<?, ?>>(genericStructureFns.size());
		
		for (Fn<?, ?> genericTokenSpanStrFn : genericStructureFns)
			structureFns.add(genericTokenSpanStrFn.makeInstance(context));
		
		return structureFns;
	}
	
	public Context makeContext(String genericContextName, Context parentContext) {
		if (!this.genericContexts.containsKey(genericContextName))
			return null;
		return this.genericContexts.get(genericContextName).makeInstance(parentContext);
	}
	
	public Search makeSearch(String genericSearchName, Context context) {
		if (!this.genericSearches.containsKey(genericSearchName))
			return null;
		return this.genericSearches.get(genericSearchName).makeInstance(context);
	}
	
	public WeightedStructure makeWeightedStructure(String genericStructureName, Context context) {
		if (!this.genericStructures.containsKey(genericStructureName))
			return null;
		return this.genericStructures.get(genericStructureName).makeInstance(context);
	}
	
	public EvaluationMultiClassification<?> makeMultiClassifyEvalInstance(String genericMultiClassifyEvalName, Context context) {
		return this.genericMultiClassifyEvals.get(genericMultiClassifyEvalName).makeInstance(context); 
	}
	
	public MethodMultiClassification makeMultiClassifyMethodInstance(String genericMultiClassifyMethodName, Context context) {
		return this.genericMultiClassifyMethods.get(genericMultiClassifyMethodName).makeInstance(context); 
	}
	
	public boolean addGazetteer(Gazetteer gazetteer) {
		this.gazetteers.put(gazetteer.getName(), gazetteer);
		return true;
	}
	
	public boolean addCleanFn(StringTransform cleanFn) {
		this.cleanFns.put(cleanFn.toString(), cleanFn);
		return true;
	}
	
	public boolean addStringPairIndicatorFn(StringPairIndicator indicatorFn) {
		this.stringPairIndicatorsFns.put(indicatorFn.toString(), indicatorFn);
		return true;
	}
	
	public boolean addStopWordsCleanFn(final Gazetteer stopWords) {
		this.cleanFns.put("StopWordsCleanFn_" + stopWords.getName(), 
			new StringTransform() {
				public String toString() {
					return "StopWordsCleanFn_" + stopWords.getName();
				}
				
				public String transform(String str) {
					str = StringUtil.clean(str);
					String stoppedStr = stopWords.removeTerms(str);
					if (stoppedStr.length() > 0)
						return stoppedStr;
					else 
						return str;
				}
			}
		);
		
		return true;
	}
	
	public boolean addCollectionFn(DataTools.StringCollectionTransform collectionFn) {
		this.collectionFns.put(collectionFn.toString(), collectionFn);
		return true;
	}
	
	public boolean addStringClusterer(ClustererString clusterer) {
		this.stringClusterers.put(clusterer.getName(), clusterer);
		return true;
	}
	
	public boolean addTokenSpanClusterer(Clusterer<TokenSpan> clusterer) {
		this.tokenSpanClusterers.put(clusterer.getName(), clusterer);
		return true;
	}
	
	public boolean addPath(String name, Path path) {
		this.paths.put(name, path);
		return true;
	}

	public boolean addAnnotationTypeNLP(AnnotationTypeNLP<?> type) {
		this.annotationTypesNLP.put(type.getType(), type);
		return true;
	}
	
	public boolean addDocumentSerializer(SerializerDocument<?,?> documentSerializer) {
		this.documentSerializers.put(documentSerializer.getName(), documentSerializer);
		return true;
	}
	
	public boolean addGenericStrFn(Fn<String, String> strFn) {
		if (!this.genericStrFns.containsKey(strFn.getGenericName()))
			this.genericStrFns.put(strFn.getGenericName(), new ArrayList<Fn<String, String>>());
		this.genericStrFns.get(strFn.getGenericName()).add(strFn);
		
		return addCommand(strFn.getGenericName(), new Command<Fn<String, String>>() {
			@Override
			public Fn<String, String> run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				List<Fn<String, String>> strFns = makeStrFns(strFn.getGenericName(), context);
				for (Fn<String, String> strFn : strFns) {
					if (strFn.fromParse(modifiers, referenceName, fnObj)) {
						return strFn;
					}
				}
				
				return null;
			}
		});
	}
	
	public boolean addGenericTokenSpanFn(Fn<TokenSpan, TokenSpan> tokenSpanFn) {
		if (!this.genericTokenSpanFns.containsKey(tokenSpanFn.getGenericName()))
			this.genericTokenSpanFns.put(tokenSpanFn.getGenericName(), new ArrayList<Fn<TokenSpan, TokenSpan>>());
		this.genericTokenSpanFns.get(tokenSpanFn.getGenericName()).add(tokenSpanFn);
		
		return addCommand(tokenSpanFn.getGenericName(), new Command<Fn<TokenSpan, TokenSpan>>() {
			@Override
			public Fn<TokenSpan, TokenSpan> run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				List<Fn<TokenSpan, TokenSpan>> tokenSpanFns = makeTokenSpanFns(tokenSpanFn.getGenericName(), context);
				for (Fn<TokenSpan, TokenSpan> tokenSpanFn : tokenSpanFns) {
					if (tokenSpanFn.fromParse(modifiers, referenceName, fnObj)) {
						return tokenSpanFn;
					}
				}
				
				return null;
			}
		});
	}
	
	public boolean addGenericTokenSpanStrFn(Fn<TokenSpan, String> tokenSpanStrFn) {
		if (!this.genericTokenSpanStrFns.containsKey(tokenSpanStrFn.getGenericName()))
			this.genericTokenSpanStrFns.put(tokenSpanStrFn.getGenericName(), new ArrayList<Fn<TokenSpan, String>>());
		this.genericTokenSpanStrFns.get(tokenSpanStrFn.getGenericName()).add(tokenSpanStrFn);
		
		return addCommand(tokenSpanStrFn.getGenericName(), new Command<Fn<TokenSpan, String>>() {
			@Override
			public Fn<TokenSpan, String> run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				List<Fn<TokenSpan, String>> tokenSpanStrFns = makeTokenSpanStrFns(tokenSpanStrFn.getGenericName(), context);
				for (Fn<TokenSpan, String> tokenSpanStrFn : tokenSpanStrFns) {
					if (tokenSpanStrFn.fromParse(modifiers, referenceName, fnObj)) {
						return tokenSpanStrFn;
					}
				}
				return null;
			}
		});
	}
	
	public boolean addGenericStructureFn(Fn<?, ?> structureFn) {
		if (!this.genericStructureFns.containsKey(structureFn.getGenericName()))
			this.genericStructureFns.put(structureFn.getGenericName(), new ArrayList<Fn<?, ?>>());
		this.genericStructureFns.get(structureFn.getGenericName()).add(structureFn);
		
		return addCommand(structureFn.getGenericName(), new Command<Fn<?, ?>>() {
			@Override
			public Fn<?, ?> run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				List<Fn<?, ?>> structureFns = makeStructureFns(structureFn.getGenericName(), context);
				for (Fn<?, ?> structureFn : structureFns) {
					if (structureFn.fromParse(modifiers, referenceName, fnObj)) {
						return structureFn;
					}
				}
				return null;
			}
		});
	}
	
	public boolean addGenericContext(Context context) {
		this.genericContexts.put(context.getGenericName(), context);
		
		return addContextConstructionCommand(context, new MakeInstanceFn<Context>() {
			public Context make(String name, Context parentContext) {
				return makeContext(name, parentContext); } }
		);
	}
	
	public boolean addGenericSearch(Search search) {
		this.genericSearches.put(search.getGenericName(), search);
		
		return addConstructionCommand(search, new MakeInstanceFn<Search>() {
			public Search make(String name, Context context) {
				return makeSearch(name, context); } }
		);
	}
	
	public boolean addGenericMultiClassifyEval(EvaluationMultiClassification<?> multiClassifyEval) {
		this.genericMultiClassifyEvals.put(multiClassifyEval.getGenericName(), multiClassifyEval);
		
		return addConstructionCommand(multiClassifyEval, new MakeInstanceFn<EvaluationMultiClassification<?>>() {
			public EvaluationMultiClassification<?> make(String name, Context context) {
				return makeMultiClassifyEvalInstance(name, context); } }
		);
	}
	
	public boolean addGenericMultiClassifyMethod(MethodMultiClassification multiClassifyMethod) {
		this.genericMultiClassifyMethods.put(multiClassifyMethod.getGenericName(), multiClassifyMethod);
		
		return addConstructionCommand(multiClassifyMethod, new MakeInstanceFn<MethodMultiClassification>() {
			public MethodMultiClassification make(String name, Context context) {
				return makeMultiClassifyMethodInstance(name, context); } }
		);
	}
	
	public boolean addGenericWeightedStructure(WeightedStructure structure) {
		if (this.genericStructures.containsKey(structure.getGenericName()))
			return true;
		
		this.genericStructures.put(structure.getGenericName(), structure);
		
		return addConstructionCommand(structure, new MakeInstanceFn<WeightedStructure>() {
			public WeightedStructure make(String name, Context context) {
				return makeWeightedStructure(name, context); } }
		);
	}
	
	public boolean addCommand(String name, Command<?> command) {
		if (!this.commands.containsKey(name))
			this.commands.put(name, new ArrayList<Command<?>>());
		this.commands.get(name).add(command);
		return true;
	}
	
	public <T extends CtxParsableFunction> boolean addConstructionCommand(CtxParsableFunction obj, MakeInstanceFn<T> makeInstanceFn) {
		return addCommand(obj.getGenericName(), new Command<T>() {
			@Override
			public T run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
				T instance = makeInstanceFn.make(obj.getGenericName(), context);
				if (instance.fromParse(modifiers, referenceName, fnObj))
					return instance;
				return null;
			}
		});
	}
	
	public boolean addContextConstructionCommand(CtxParsableFunction obj, MakeInstanceFn<Context> makeInstanceFn) {
		return addCommand(obj.getGenericName(), new Command<Context>() {
			@Override
			public Context run(Context context, List<String> modifiers, String referenceName, Function fnObj) {				
				if (fnObj.getParameters().contains("initScript")) {
					boolean initOnce = (fnObj.getParameters().contains("initOnce")) ? Boolean.valueOf(context.getMatchValue(fnObj.getParameters().get("initOnce").getValue())) : true;
					boolean initOverrideByName = (fnObj.getParameters().contains("initOverrideByName")) ? Boolean.valueOf(context.getMatchValue(fnObj.getParameters().get("initOnce").getValue())) : false;
					String initScript = context.getMatchValue(fnObj.getParameters().get("initScript").getValue());
					if (initOnce) {
						Context existingContext = null;
						if (initOverrideByName)
							existingContext = context.getInitOnceContextForName(referenceName);
						if (existingContext == null)
							existingContext = context.getInitOnceContextForScript(initScript);
						if (existingContext != null)
							return existingContext;
					}
				}
				
				Context instance = makeInstanceFn.make(obj.getGenericName(), context);
				if (instance.fromParse(modifiers, referenceName, fnObj))
					return instance;
				return null;
			}
		});
	}
	
	public boolean setRandomSeed(long seed) {
		this.randomSeed = seed;
		this.globalRandom.setSeed(this.randomSeed);
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T runCommand(Context context, List<String> modifiers, String referenceName, Obj.Function fnObj) {
		List<Command<?>> commands = this.commands.get(fnObj.getName());
		if (commands == null) {
			this.outputWriter.debugWriteln("ERROR: Command not found: " + fnObj.getName());
			throw new NullPointerException();
		}
			
		for (Command<?> command : commands) {
			Object obj = command.run(context, modifiers, referenceName, fnObj);
			if (obj != null) {
				return (T)obj;
			}
		}
		
		return null;
	}
	
	public Properties getProperties() {
		return this.properties;
	}
	
	public synchronized WordNet getWordNet() {
		if (this.wordNet == null)
			this.wordNet = new WordNet();
		return this.wordNet;
	}
	
	public synchronized Word2Vec getWord2Vec() {
		if (this.word2Vec == null)
			this.word2Vec = new Word2Vec(this.properties);
		return this.word2Vec;
	}
	
	public DataTools makeInstance(OutputWriter outputWriter) {
		DataTools instance = makeInstance();
		instance.outputWriter = outputWriter;
		instance.setRandomSeed(getGlobalRandom(this.randomSeed).nextLong());
		return instance;
	}
	
	public DataTools makeInstance(OutputWriter outputWriter, long randomSeed) {
		DataTools clone = makeInstance();
		clone.outputWriter = outputWriter;
		clone.setRandomSeed(randomSeed);
		return clone;
	}
	
	/**
	 * @return a new DataTools instance object.  Classes that extend DataTools
	 * should override this method to make a new instance of their type.
	 */
	public DataTools makeInstance() {
		return new DataTools();
	}
}
