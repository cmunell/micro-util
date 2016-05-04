package edu.cmu.ml.rtw.generic.data.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.DataTools.Command;
import edu.cmu.ml.rtw.generic.data.DataTools.MakeInstanceFn;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.structure.DatumStructureCollection;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.data.feature.FeatureClassificationMethod;
import edu.cmu.ml.rtw.generic.data.feature.FeatureConjunction;
import edu.cmu.ml.rtw.generic.data.feature.FeatureConstituencyParseRelation;
import edu.cmu.ml.rtw.generic.data.feature.FeatureConstituencyPath;
import edu.cmu.ml.rtw.generic.data.feature.FeatureDependencyPathType;
import edu.cmu.ml.rtw.generic.data.feature.FeatureGazetteerContains;
import edu.cmu.ml.rtw.generic.data.feature.FeatureGazetteerEditDistance;
import edu.cmu.ml.rtw.generic.data.feature.FeatureGazetteerInitialism;
import edu.cmu.ml.rtw.generic.data.feature.FeatureGazetteerPrefixTokens;
import edu.cmu.ml.rtw.generic.data.feature.FeatureGramCluster;
import edu.cmu.ml.rtw.generic.data.feature.FeatureGramContextPattern;
//import edu.cmu.ml.rtw.generic.data.feature.FeatureIdentity;
import edu.cmu.ml.rtw.generic.data.feature.FeatureDependencyPath;
import edu.cmu.ml.rtw.generic.data.feature.FeatureNGramContext;
import edu.cmu.ml.rtw.generic.data.feature.FeatureNGramDep;
import edu.cmu.ml.rtw.generic.data.feature.FeatureNGramSentence;
import edu.cmu.ml.rtw.generic.data.feature.FeaturePredicateArgumentPath;
import edu.cmu.ml.rtw.generic.data.feature.FeatureSet;
import edu.cmu.ml.rtw.generic.data.feature.FeatureStringForm;
import edu.cmu.ml.rtw.generic.data.feature.FeatureTokenCount;
import edu.cmu.ml.rtw.generic.data.feature.FeatureTokenSpanFnComparison;
import edu.cmu.ml.rtw.generic.data.feature.FeatureTokenSpanFnDataVocab;
import edu.cmu.ml.rtw.generic.data.feature.FeatureTokenSpanFnDataVocabTrie;
import edu.cmu.ml.rtw.generic.data.feature.FeatureTokenSpanFnFilteredVocab;
import edu.cmu.ml.rtw.generic.data.feature.FeatureWord2Vec;
import edu.cmu.ml.rtw.generic.data.feature.rule.RuleSet;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.SupervisedModelAreg;
import edu.cmu.ml.rtw.generic.model.SupervisedModelCreg;
import edu.cmu.ml.rtw.generic.model.SupervisedModelLGApproximation;
import edu.cmu.ml.rtw.generic.model.SupervisedModelLabelDistribution;
import edu.cmu.ml.rtw.generic.model.SupervisedModelLibSVM;
import edu.cmu.ml.rtw.generic.model.SupervisedModelLogistmarGramression;
import edu.cmu.ml.rtw.generic.model.SupervisedModelPartition;
import edu.cmu.ml.rtw.generic.model.SupervisedModelSVM;
import edu.cmu.ml.rtw.generic.model.SupervisedModelSVMStructured;
import edu.cmu.ml.rtw.generic.model.SupervisedModelWekaOneClass;
import edu.cmu.ml.rtw.generic.model.SupervisedModelWekaSVMOneClass;
import edu.cmu.ml.rtw.generic.model.SupervisedModelYADLL;
import edu.cmu.ml.rtw.generic.model.evaluation.GridSearch;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluationAccuracy;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluationF;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluationPrecision;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluationRecall;
import edu.cmu.ml.rtw.generic.opt.search.ParameterSearchable;
import edu.cmu.ml.rtw.generic.opt.search.Search;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.parse.Obj.Function;
import edu.cmu.ml.rtw.generic.structure.WeightedStructure;
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassification;
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassificationConfusionData;
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassificationConfusionMatrix;
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassificationMeasureAccuracy;
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassificationMeasureF;
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassificationMeasurePrecision;
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassificationMeasureRecall;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassificationConstant;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassificationFilterDatumIndicator;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassificationLabelMapping;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassificationSupervisedModel;
import edu.cmu.ml.rtw.generic.task.classify.TaskClassification;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * Datum represents a (possibly) labeled datum (training/evaluation
 * example).  A domain specific project should extend this class with
 * implementations of methods/tools that are particular to pieces of 
 * data in their domain.  See 
 * 
 * @author Bill McDowell
 *
 * @param <L> label type
 */
public abstract class Datum<L> {	
	protected int id;
	protected L label;
	protected List<Pair<L, Double>> labelDistribution;
	
	public int getId() {
		return this.id;
	}
	
	public L getLabel() {
		return this.label;
	}
	
	public boolean setLabel(L label) {
		this.label = label;
		return true;
	}
	
	public boolean setLabelWeight(L label, double weight) {
		if (this.labelDistribution == null)
			this.labelDistribution = new ArrayList<Pair<L, Double>>(2);
		
		Pair<L, Double> pair = getLabelWeightPair(label);
		if (pair == null)
			this.labelDistribution.add(new Pair<L, Double>(label, weight));
		else
			pair.setSecond(weight);
		
		return true;
	}
	
	public double getLabelWeight(L label) {
		if (this.labelDistribution == null) {
			if ((this.label == null && label == null) || (this.label != null && this.label.equals(label)))
				return 1.0;
			else
				return 0.0;
		}
		
		Pair<L, Double> pair = getLabelWeightPair(label);
		if (pair == null)
			return 0.0;
		else
			return pair.getSecond();
	}
	
	private Pair<L, Double> getLabelWeightPair(L label) {
		for (Pair<L, Double> pair : this.labelDistribution)
			if (pair.getFirst().equals(label))
				return pair;
		return null;
	}
	
	@Override
	public int hashCode() {
		// FIXME: Make better
		return this.id;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		Datum<L> datum = (Datum<L>)o;
		return datum.id == this.id;
	}
	
	/**
	 * Tools contains tools for working with a particular type
	 * of datum.  For example, each type of datum type has a 
	 * collection of "extractors" for retrieving associated strings or 
	 * token spans that are necessary for computing features, and 
	 * each datum type also has an associated set of models and
	 * features that can be used with them.
	 * 
	 * @author Bill McDowell
	 *
	 * @param <D> datum type
	 * @param <L> label type
	 * 
	 */
	public static abstract class Tools<D extends Datum<L>, L> {
		public static abstract class Structurizer<D extends Datum<L>, L, S extends WeightedStructure> extends CtxParsableFunction {
			protected DatumContext<D, L> context;
			
			public abstract boolean addToStructures(D datum, L label, double weight, Map<String, S> structures, Map<String, Collection<WeightedStructure>> changes);
			public abstract Map<String, S> makeStructures();
			public abstract Map<L, Double> getLabels(D datum, Map<String, S> structures);
			public abstract Structurizer<D, L, S> makeInstance(DatumContext<D, L> context);
			
			public boolean matchesData(DataSet<?, ?> data) {
				return data.getDatumTools().equals(this.context.getDatumTools());
			}
		}

		public static abstract class DataSetBuilder<D extends Datum<L>, L> extends CtxParsableFunction {
			protected DatumContext<D, L> context;
			
			public abstract DataSetBuilder<D, L> makeInstance(DatumContext<D, L> context);
			public abstract DataSet<D, L> build();
		}
		
		public static interface StringExtractor<D extends Datum<L>, L> {
			String toString();
			String[] extract(D datum);
		}
		
		public static interface TokenSpanExtractor<D extends Datum<L>, L> {
			String toString();
			TokenSpan[] extract(D datum);
		}
		
		public static interface DoubleExtractor<D extends Datum<L>, L> {
			String toString();
			double[] extract(D datum);
		}
		
		public static interface LabelMapping<L> {
			String toString();
			L map(L label);
		}
		
		public static interface LabelIndicator<L> {
			String toString();
			boolean indicator(L label);
			double weight(L label);
		}
		
		public static interface DatumIndicator<D> {
			String toString();
			boolean indicator(D datum);
		}
		
		public static interface InverseLabelIndicator<L> {
			String toString();
			L label(Map<String, Double> indicatorWeights, List<String> positiveIndicators);
		}
		
		public static interface Clusterer<D extends Datum<L>, L, C> {
			String toString();
			C getCluster(D datum);
		}
		
		protected DataTools dataTools;
		
		private Map<String, DataSetBuilder<D, L>> dataSetBuilders;
		private Map<String, TokenSpanExtractor<D, L>> tokenSpanExtractors;
		private Map<String, StringExtractor<D, L>> stringExtractors;
		private Map<String, DoubleExtractor<D, L>> doubleExtractors;
		private Map<String, LabelMapping<L>> labelMappings;
		private Map<String, LabelIndicator<L>> labelIndicators;
		private Map<String, DatumIndicator<D>> datumIndicators;
		private Map<String, InverseLabelIndicator<L>> inverseLabelIndicators;
		
		private Map<String, Feature<D, L>> genericFeatures;
		private Map<String, SupervisedModel<D, L>> genericModels;
		private Map<String, SupervisedModelEvaluation<D, L>> genericEvaluations;
		private Map<String, Structurizer<D, L, ?>> genericStructurizers;
		
		private Map<String, DatumStructureCollection<D, L>> genericDatumStructureCollections;
		
		private Map<String, MethodClassification<D, L>> genericClassifyMethods;
		private Map<String, EvaluationClassification<D, L, ?>> genericClassifyEvals;
		
		private Map<String, List<Command<?>>> commands;
		
		public Tools(DataTools dataTools) {
			this.dataTools = dataTools;
			
			this.dataSetBuilders = new HashMap<String, DataSetBuilder<D, L>>();
			
			this.tokenSpanExtractors = new HashMap<String, TokenSpanExtractor<D, L>>();
			this.stringExtractors = new HashMap<String, StringExtractor<D, L>>();
			this.doubleExtractors = new HashMap<String, DoubleExtractor<D, L>>();
			this.labelMappings = new HashMap<String, LabelMapping<L>>();
			this.labelIndicators = new HashMap<String, LabelIndicator<L>>();
			this.datumIndicators = new HashMap<String, DatumIndicator<D>>();
			this.inverseLabelIndicators = new HashMap<String, InverseLabelIndicator<L>>();
			
			this.genericFeatures = new HashMap<String, Feature<D, L>>();
			this.genericModels = new HashMap<String, SupervisedModel<D, L>>();
			this.genericEvaluations = new HashMap<String, SupervisedModelEvaluation<D, L>>();
			
			this.genericDatumStructureCollections = new HashMap<String, DatumStructureCollection<D, L>>();
			this.genericStructurizers = new HashMap<String, Structurizer<D, L, ?>>();
			
			this.genericClassifyMethods = new HashMap<String, MethodClassification<D, L>>();
			this.genericClassifyEvals = new HashMap<String, EvaluationClassification<D, L, ?>>();
			
			this.commands = new HashMap<String, List<Command<?>>>();
			
			addLabelMapping(new LabelMapping<L>() {
				public String toString() {
					return "Identity";
				}
				
				@Override
				public L map(L label) {
					return label;
				}
			});
			
			addGenericFeature(new FeatureGazetteerContains<D, L>());
			addGenericFeature(new FeatureGazetteerEditDistance<D, L>());
			addGenericFeature(new FeatureGazetteerInitialism<D, L>());
			addGenericFeature(new FeatureGazetteerPrefixTokens<D, L>());
			addGenericFeature(new FeatureNGramContext<D, L>());
			addGenericFeature(new FeatureNGramSentence<D, L>());
			addGenericFeature(new FeatureNGramDep<D, L>());
			//addGenericFeature(new FeatureIdentity<D, L>()); // FIXME The command for this conflicts with FnIdentity
			addGenericFeature(new FeatureDependencyPath<D, L>());
			addGenericFeature(new FeatureConstituencyPath<D, L>());
			addGenericFeature(new FeatureConjunction<D, L>());
			addGenericFeature(new FeatureGramContextPattern<D, L>());
			addGenericFeature(new FeatureTokenCount<D, L>());
			addGenericFeature(new FeatureStringForm<D, L>());
			addGenericFeature(new FeatureGramCluster<D, L>());
			addGenericFeature(new FeatureTokenSpanFnDataVocab<D, L>());
			addGenericFeature(new FeatureTokenSpanFnFilteredVocab<D, L>());
			addGenericFeature(new FeatureTokenSpanFnDataVocabTrie<D, L>());
			addGenericFeature(new FeatureDependencyPathType<D, L>());
			addGenericFeature(new FeatureConstituencyParseRelation<D, L>());
			addGenericFeature(new FeatureWord2Vec<D, L>());
			addGenericFeature(new FeatureTokenSpanFnComparison<D, L>());
			addGenericFeature(new FeaturePredicateArgumentPath<D, L>());
			addGenericFeature(new FeatureClassificationMethod<D, L>());
			
			addGenericModel(new SupervisedModelCreg<D, L>());
			addGenericModel(new SupervisedModelLabelDistribution<D, L>());
			addGenericModel(new SupervisedModelSVM<D, L>());
			addGenericModel(new SupervisedModelSVMStructured<D, L>());
			addGenericModel(new SupervisedModelPartition<D, L>());
			addGenericModel(new SupervisedModelAreg<D, L>());
			addGenericModel(new SupervisedModelLogistmarGramression<D, L>());
			addGenericModel(new SupervisedModelLGApproximation<D, L>());
			addGenericModel(new SupervisedModelYADLL<D, L>());
			addGenericModel(new SupervisedModelWekaOneClass<D, L>());
			addGenericModel(new SupervisedModelWekaSVMOneClass<D, L>());
			addGenericModel(new SupervisedModelLibSVM<D, L>());
			
			addGenericEvaluation(new SupervisedModelEvaluationAccuracy<D, L>());
			addGenericEvaluation(new SupervisedModelEvaluationPrecision<D, L>());
			addGenericEvaluation(new SupervisedModelEvaluationRecall<D, L>());
			addGenericEvaluation(new SupervisedModelEvaluationF<D, L>());
			
			addGenericClassifyMethod(new MethodClassificationSupervisedModel<D, L>());
			addGenericClassifyMethod(new MethodClassificationConstant<D, L>());
			addGenericClassifyMethod(new MethodClassificationLabelMapping<D, L>());
			addGenericClassifyMethod(new MethodClassificationFilterDatumIndicator<D, L>());
			
			addGenericClassifyEval(new EvaluationClassificationConfusionData<D, L>());
			addGenericClassifyEval(new EvaluationClassificationConfusionMatrix<D, L>());
			addGenericClassifyEval(new EvaluationClassificationMeasureAccuracy<D, L>());
			addGenericClassifyEval(new EvaluationClassificationMeasurePrecision<D, L>());
			addGenericClassifyEval(new EvaluationClassificationMeasureRecall<D, L>());
			addGenericClassifyEval(new EvaluationClassificationMeasureF<D, L>());
			
			addGenericDataSetBuilder(new DataSetBuilderStored<D, L>());
			
			addConstructionCommand(new RuleSet<D, L>(null), new MakeInstanceFn<RuleSet<D, L>>() {
					@SuppressWarnings("unchecked")
					public RuleSet<D, L> make(String name, Context parentContext) {
						return new RuleSet<D, L>((DatumContext<D, L>)parentContext); } });
	
			addConstructionCommand(new GridSearch<D, L>(null), new MakeInstanceFn<GridSearch<D, L>>() {
				@SuppressWarnings("unchecked")
				public GridSearch<D, L> make(String name, Context parentContext) {
					return new GridSearch<D, L>((DatumContext<D, L>)parentContext); } });
			
			addConstructionCommand(new FeatureSet<D, L>(null), new MakeInstanceFn<FeatureSet<D, L>>() {
				@SuppressWarnings("unchecked")
				public FeatureSet<D, L> make(String name, Context parentContext) {
					return new FeatureSet<D, L>((DatumContext<D, L>)parentContext); } });
			
			addConstructionCommand(new DataFeatureMatrix<D, L>(null), new MakeInstanceFn<DataFeatureMatrix<D, L>>() {
				@SuppressWarnings("unchecked")
				public DataFeatureMatrix<D, L> make(String name, Context parentContext) {
					return new DataFeatureMatrix<D, L>((DatumContext<D, L>)parentContext); } });
			
			addConstructionCommand(new TaskClassification<D, L>(null), new MakeInstanceFn<TaskClassification<D, L>>() {
				@SuppressWarnings("unchecked")
				public TaskClassification<D, L> make(String name, Context parentContext) {
					return new TaskClassification<D, L>((DatumContext<D, L>)parentContext); } });
			
			addCommand("CloneClassifyMethod", new Command<MethodClassification<D, L>>() {
				@SuppressWarnings("unchecked")
				@Override
				public MethodClassification<D, L> run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
					AssignmentList parameters = fnObj.getParameters();
					DatumContext<D, L> datumContext = (DatumContext<D, L>)context;
					MethodClassification<D, L> method = (MethodClassification<D, L>)datumContext.getMatchClassifyMethod(parameters.get("method").getValue()).clone(referenceName);
				
					for (Assignment assignment : parameters) {
						if (!assignment.getName().equals("method"))
							method.setParameterValue(assignment.getName(), assignment.getValue());
					}
					
					return method;
				}
			});
			
			addCommand("InitClassifyMethod", new Command<MethodClassification<D, L>>() {
				@SuppressWarnings("unchecked")
				@Override
				public MethodClassification<D, L> run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
					AssignmentList parameters = fnObj.getParameters();
					DatumContext<D, L> datumContext = (DatumContext<D, L>)context;
					DataSet<D, L> data = datumContext.getMatchDataSet(parameters.get("devData").getValue());
					MethodClassification<D, L> method = (MethodClassification<D, L>)datumContext.getMatchClassifyMethod(parameters.get("method").getValue()).clone(referenceName);
					if (!method.init(data))
						return null;
					else
						return method;
				}
			});
			
			addCommand("RunClassifyMethodSearch", new Command<MethodClassification<D, L>>() {
				@SuppressWarnings("unchecked")
				@Override
				public MethodClassification<D, L> run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
					AssignmentList parameters = fnObj.getParameters();
					ParameterSearchable fn = context.getMatchParameterSearchable(parameters.get("fn").getValue());
					Search search = context.getMatchSearch(parameters.get("search").getValue());

					if (!search.run(fn))
						return null;
					
					EvaluationClassification<D, L, ?> evaluation = (EvaluationClassification<D, L, ?>)search.getPositionFn(search.getBestPosition());
					return evaluation.getMethod().clone(referenceName);
				}
			});
			
			addCommand("StoreData", new Command<String>() {
				@SuppressWarnings("unchecked")
				@Override
				public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
					AssignmentList parameters = fnObj.getParameters();
					DatumContext<D, L> datumContext = (DatumContext<D, L>)context;
					DataSet<D, L> data = datumContext.getMatchDataSet(parameters.get("data").getValue());
					if (data.isBuildable() && !data.isBuilt() && !data.build())
						return String.valueOf(false);
					
					return String.valueOf(
							data.store(datumContext.getMatchValue(parameters.get("storage").getValue()), 
							   datumContext.getMatchValue(parameters.get("collection").getValue()), 
							   context.getMaxThreads()));
				}
			});
			
			addCommand("PartitionData", new Command<String>() {
				@SuppressWarnings("unchecked")
				@Override
				public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
					AssignmentList parameters = fnObj.getParameters();
					List<String> distributionStr = context.getMatchArray(parameters.get("distribution").getValue());
					DatumContext<D, L> datumContext = (DatumContext<D, L>)context;
					DataSet<D, L> data = datumContext.getMatchDataSet(parameters.get("data").getValue());
					if (data.isBuildable() && !data.isBuilt() && !data.build())
						return String.valueOf(false);
					
					double[] distribution = new double[distributionStr.size()];
					for (int i = 0; i < distribution.length; i++)
						distribution[i] = Double.valueOf(distributionStr.get(i));
					List<DataSet<D, L>> parts = data.makePartition(distribution, context.getDataTools().getGlobalRandom());
					
					for (int i = 0; i < parts.size(); i++) {
						datumContext.addDataSet(parts.get(i));
					}
					
					return String.valueOf(true);
				}
			});
			
			addCommand("FilterData", new Command<DataSet<D, L>>() {
				@SuppressWarnings("unchecked")
				@Override
				public DataSet<D, L> run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
					AssignmentList parameters = fnObj.getParameters();
					DatumContext<D, L> datumContext = (DatumContext<D, L>)context;
					DatumIndicator<D> datumIndicator = datumContext.getDatumTools().getDatumIndicator(
							datumContext.getMatchValue(parameters.get("datumIndicator").getValue()));
					DataSet<D, L> data = datumContext.getMatchDataSet(parameters.get("data").getValue());
					if (data.isBuildable() && !data.isBuilt() && !data.build())
						return null;
					return data.filter(referenceName, datumIndicator, context.getMaxThreads());
				}
			});
			
			addCommand("UnionData", new Command<DataSet<D, L>>() {
				@SuppressWarnings("unchecked")
				@Override
				public DataSet<D, L> run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
					AssignmentList parameters = fnObj.getParameters();
					Obj.Array dataSets = (Obj.Array)parameters.get("data").getValue();
					DatumContext<D, L> datumContext = (DatumContext<D, L>)context;
					DataSet<D, L> unionedData = new DataSet<D, L>(referenceName, Tools.this, null);
					for (int i = 0; i < dataSets.size(); i++) {
						DataSet<D, L> data = datumContext.getMatchDataSet(dataSets.get(i));
						if (data.isBuildable() && !data.isBuilt() && !data.build())
							return null;
						
						unionedData.addAll(data);
					}
					
					return unionedData;
				}
			});
			
			addCommand("SizeData", new Command<String>() {
				@SuppressWarnings("unchecked")
				@Override
				public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
					AssignmentList parameters = fnObj.getParameters();
					DatumContext<D, L> datumContext = (DatumContext<D, L>)context;
					DataSet<D, L> data = datumContext.getMatchDataSet(parameters.get("data").getValue());
					if (data.isBuildable() && !data.isBuilt() && !data.build())
						return String.valueOf("-1");
					return String.valueOf(data.size());
					
				}
			});
			
			addCommand("SizeFeatures", new Command<String>() {
				@SuppressWarnings("unchecked")
				@Override
				public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
					AssignmentList parameters = fnObj.getParameters();
					DatumContext<D, L> datumContext = (DatumContext<D, L>)context;
					FeatureSet<D, L> features = datumContext.getMatchFeatureSet(parameters.get("features").getValue());
					if (!features.isInitialized() && !features.init())
						return String.valueOf("-1");
					return String.valueOf(features.getFeatureVocabularySize());
					
				}
			});
			
			this.addCommand("ConjoinDatumIndicators", new Command<String>() {
				@SuppressWarnings("unchecked")
				@Override
				public String run(Context context, List<String> modifiers, String referenceName, Function fnObj) {
					String name = context.getMatchValue(fnObj.getParameters().get("name").getValue());
					List<String> fns = context.getMatchArray(fnObj.getParameters().get("fns").getValue());
					
					DatumContext<D, L> datumContext = (DatumContext<D, L>)context;
					return String.valueOf(datumContext.getDatumTools().addDatumIndicator(new DatumIndicator<D>() {
						@Override
						public String toString() {
							return name;
						}
						
						@Override
						public boolean indicator(D datum) {
							for (String fn : fns) {
								if (!datumContext.getDatumTools().getDatumIndicator(fn).indicator(datum))
									return false;
							}
							return true;
						}
					}));
				}
			});
		}
		
		public DataTools getDataTools() {
			return this.dataTools;
		}
		
		public TokenSpanExtractor<D, L> getTokenSpanExtractor(String name) {
			return this.tokenSpanExtractors.get(name);
		}
		
		public StringExtractor<D, L> getStringExtractor(String name) {
			return this.stringExtractors.get(name);
		}
		
		public DoubleExtractor<D, L> getDoubleExtractor(String name) {
			return this.doubleExtractors.get(name);
		}
		
		public LabelMapping<L> getLabelMapping(String name) {
			return this.labelMappings.get(name);
		}
		
		public LabelIndicator<L> getLabelIndicator(String name) {
			return this.labelIndicators.get(name);
		}
		
		public DatumIndicator<D> getDatumIndicator(String name) {
			if (!this.datumIndicators.containsKey(name))
				throw new IllegalArgumentException("Missing datum indicator " + name);
			
			return this.datumIndicators.get(name);
		}
		
		public InverseLabelIndicator<L> getInverseLabelIndicator(String name) {
			return this.inverseLabelIndicators.get(name);
		}
		
		public DataSetBuilder<D, L> makeDataSetBuilder(String name, DatumContext<D, L> context) {
			return this.dataSetBuilders.get(name).makeInstance(context);
		}
		
		public DataSet<D, L> makeUnbuiltDataSet(String name, DatumContext<D, L> context) {
			return new DataSet<D, L>(this.dataSetBuilders.get(name).makeInstance(context), this);
		}
		
		public Feature<D, L> makeFeatureInstance(String genericFeatureName, DatumContext<D, L> context) {
			return this.genericFeatures.get(genericFeatureName).makeInstance(context); 
		}
		
		public SupervisedModel<D, L> makeModelInstance(String genericModelName, DatumContext<D, L> context) {
			return this.genericModels.get(genericModelName).makeInstance(context); 
		}
		
		public SupervisedModelEvaluation<D, L> makeEvaluationInstance(String genericEvaluationName, DatumContext<D, L> context) {
			if (!this.genericEvaluations.containsKey(genericEvaluationName))
				return null;
			return this.genericEvaluations.get(genericEvaluationName).makeInstance(context); 
		}
		
		public EvaluationClassification<D, L, ?> makeClassifyEvalInstance(String genericClassifyEvalName, DatumContext<D, L> context) {
			return this.genericClassifyEvals.get(genericClassifyEvalName).makeInstance(context); 
		}
		
		public MethodClassification<D, L> makeClassifyMethodInstance(String genericClassifyMethodName, DatumContext<D, L> context) {
			return this.genericClassifyMethods.get(genericClassifyMethodName).makeInstance(context); 
		}
		
		public DatumStructureCollection<D, L> makeDatumStructureCollection(String genericCollectionName, DataSet<D, L> data) {
			return this.genericDatumStructureCollections.get(genericCollectionName).makeInstance(data);
		}
		
		public Structurizer<D, L, ?> makeStructurizerInstance(String name, DatumContext<D, L> context) {
			return this.genericStructurizers.get(name).makeInstance(context);
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
		
		public boolean addTokenSpanExtractor(TokenSpanExtractor<D, L> tokenSpanExtractor) {
			this.tokenSpanExtractors.put(tokenSpanExtractor.toString(), tokenSpanExtractor);
			return true;
		}
		
		public boolean addStringExtractor(StringExtractor<D, L> stringExtractor) {
			this.stringExtractors.put(stringExtractor.toString(), stringExtractor);
			return true;
		}
		
		public boolean addDoubleExtractor(DoubleExtractor<D, L> doubleExtractor) {
			this.doubleExtractors.put(doubleExtractor.toString(), doubleExtractor);
			return true;
		}
		
		public boolean addLabelMapping(LabelMapping<L> labelMapping) {
			this.labelMappings.put(labelMapping.toString(), labelMapping);
			return true;
		}
		
		public boolean addGenericDataSetBuilder(DataSetBuilder<D, L> builder) {
			this.dataSetBuilders.put(builder.getGenericName(), builder);

			return addConstructionCommand(builder, new MakeInstanceFn<DataSet<D, L>>() {
				@SuppressWarnings("unchecked")
				public DataSet<D, L> make(String name, Context parentContext) {
					return makeUnbuiltDataSet(name, (DatumContext<D, L>)parentContext); } }
			);
		}
		
		public boolean addGenericFeature(Feature<D, L> feature) {
			this.genericFeatures.put(feature.getGenericName(), feature);
			
			return addConstructionCommand(feature, new MakeInstanceFn<Feature<D, L>>() {
				@SuppressWarnings("unchecked")
				public Feature<D, L> make(String name, Context parentContext) {
					return makeFeatureInstance(name, (DatumContext<D, L>)parentContext); } }
			);
		}
		
		public boolean addGenericModel(SupervisedModel<D, L> model) {
			this.genericModels.put(model.getGenericName(), model);
			
			return addConstructionCommand(model, new MakeInstanceFn<SupervisedModel<D, L>>() {
				@SuppressWarnings("unchecked")
				public SupervisedModel<D, L> make(String name, Context parentContext) {
					return makeModelInstance(name, (DatumContext<D, L>)parentContext); } }
			);
		}
		
		public boolean addGenericEvaluation(SupervisedModelEvaluation<D, L> evaluation) {
			this.genericEvaluations.put(evaluation.getGenericName(), evaluation);
			
			return addConstructionCommand(evaluation, new MakeInstanceFn<SupervisedModelEvaluation<D, L>>() {
				@SuppressWarnings("unchecked")
				public SupervisedModelEvaluation<D, L> make(String name, Context parentContext) {
					return makeEvaluationInstance(name, (DatumContext<D, L>)parentContext); } }
			);
		}
		
		public boolean addGenericClassifyEval(EvaluationClassification<D, L, ?> classifyEval) {
			this.genericClassifyEvals.put(classifyEval.getGenericName(), classifyEval);
			
			return addConstructionCommand(classifyEval, new MakeInstanceFn<EvaluationClassification<D, L, ?>>() {
				@SuppressWarnings("unchecked")
				public EvaluationClassification<D, L, ?> make(String name, Context parentContext) {
					return makeClassifyEvalInstance(name, (DatumContext<D, L>)parentContext); } }
			);
		}
		
		public boolean addGenericClassifyMethod(MethodClassification<D, L> classifyMethod) {
			this.genericClassifyMethods.put(classifyMethod.getGenericName(), classifyMethod);
			
			return addConstructionCommand(classifyMethod, new MakeInstanceFn<MethodClassification<D, L>>() {
				@SuppressWarnings("unchecked")
				public MethodClassification<D, L> make(String name, Context parentContext) {
					return makeClassifyMethodInstance(name, (DatumContext<D, L>)parentContext); } }
			);
		}
		
		public boolean addGenericStructurizer(Structurizer<D, L, ?> structurizer) {
			this.genericStructurizers.put(structurizer.getGenericName(), structurizer);
			
			return addConstructionCommand(structurizer, new MakeInstanceFn<Structurizer<D, L, ?>>() {
				@SuppressWarnings("unchecked")
				public Structurizer<D, L, ?> make(String name, Context parentContext) {
					return makeStructurizerInstance(name, (DatumContext<D, L>)parentContext); } }
			);
		}
		
		public boolean addGenericDatumStructureCollection(DatumStructureCollection<D, L> datumStructureCollection) {
			this.genericDatumStructureCollections.put(datumStructureCollection.getGenericName(), datumStructureCollection);
			return true;
		}
		
		public boolean addLabelIndicator(LabelIndicator<L> labelIndicator) {
			this.labelIndicators.put(labelIndicator.toString(), labelIndicator);
			return true;
		}
		
		public boolean addDatumIndicator(DatumIndicator<D> datumIndicator) {
			this.datumIndicators.put(datumIndicator.toString(), datumIndicator);
			return true;
		}
		
		public boolean addInverseLabelIndicator(InverseLabelIndicator<L> inverseLabelIndicator) {
			this.inverseLabelIndicators.put(inverseLabelIndicator.toString(), inverseLabelIndicator);
			return true;
		}
		
		public List<LabelIndicator<L>> getLabelIndicators() {
			return new ArrayList<LabelIndicator<L>>(this.labelIndicators.values());
		}
		
		public <T extends Datum<Boolean>> T makeBinaryDatum(D datum, String labelIndicator) {
			return makeBinaryDatum(datum, this.getLabelIndicator(labelIndicator));
		}
		
		@SuppressWarnings("unchecked")
		public <T> T runCommand(Context context, List<String> modifiers, String referenceName, Obj.Function fnObj) {
			if (this.commands.containsKey(fnObj.getName())) {
				for (Command<?> command : this.commands.get(fnObj.getName())) {
					Object obj = command.run(context, modifiers, referenceName, fnObj);
					if (obj != null) {
						return (T)obj;
					}
				}
			}
			
			return this.dataTools.runCommand(context, modifiers, referenceName, fnObj);
		}
		
		public abstract L labelFromString(String str);
		public abstract JSONObject datumToJSON(D datum);
		public abstract D datumFromJSON(JSONObject json);
		public abstract <T extends Datum<Boolean>> T makeBinaryDatum(D datum, LabelIndicator<L> labelIndicator);
		public abstract <T extends Datum<Boolean>> Datum.Tools<T, Boolean> makeBinaryDatumTools(LabelIndicator<L> labelIndicator);
	}
}
