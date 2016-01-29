package edu.cmu.ml.rtw.generic.data.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.structure.DatumStructureCollection;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.data.feature.FeatureConjunction;
import edu.cmu.ml.rtw.generic.data.feature.FeatureConstituencyPath;
import edu.cmu.ml.rtw.generic.data.feature.FeatureGazetteerContains;
import edu.cmu.ml.rtw.generic.data.feature.FeatureGazetteerEditDistance;
import edu.cmu.ml.rtw.generic.data.feature.FeatureGazetteerInitialism;
import edu.cmu.ml.rtw.generic.data.feature.FeatureGazetteerPrefixTokens;
import edu.cmu.ml.rtw.generic.data.feature.FeatureGramCluster;
import edu.cmu.ml.rtw.generic.data.feature.FeatureGramContextPattern;
import edu.cmu.ml.rtw.generic.data.feature.FeatureIdentity;
import edu.cmu.ml.rtw.generic.data.feature.FeatureDependencyPath;
import edu.cmu.ml.rtw.generic.data.feature.FeatureNGramContext;
import edu.cmu.ml.rtw.generic.data.feature.FeatureNGramDep;
import edu.cmu.ml.rtw.generic.data.feature.FeatureNGramSentence;
import edu.cmu.ml.rtw.generic.data.feature.FeatureStringForm;
import edu.cmu.ml.rtw.generic.data.feature.FeatureTokenCount;
import edu.cmu.ml.rtw.generic.data.feature.FeatureTokenSpanFnDataVocab;
import edu.cmu.ml.rtw.generic.data.feature.FeatureTokenSpanFnDataVocabTrie;
import edu.cmu.ml.rtw.generic.data.feature.FeatureTokenSpanFnFilteredVocab;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnAffix;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnClean;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnComposite;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnCompositeAppend;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnCoref;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnDependencyRelation;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnFilter;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnGazetteer;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnGazetteerFilter;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnHead;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnIdentity;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnNGramContext;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnNGramDocument;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnNGramInside;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnNGramSentence;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnPoS;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnRelationStr;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnSplit;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnString;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnTokenSpanPathStr;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.SupervisedModelAreg;
import edu.cmu.ml.rtw.generic.model.SupervisedModelCreg;
import edu.cmu.ml.rtw.generic.model.SupervisedModelLGApproximation;
import edu.cmu.ml.rtw.generic.model.SupervisedModelLabelDistribution;
import edu.cmu.ml.rtw.generic.model.SupervisedModelLogistmarGramression;
import edu.cmu.ml.rtw.generic.model.SupervisedModelPartition;
import edu.cmu.ml.rtw.generic.model.SupervisedModelSVM;
import edu.cmu.ml.rtw.generic.model.SupervisedModelSVMStructured;
import edu.cmu.ml.rtw.generic.model.SupervisedModelYADLL;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluationAccuracy;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluationF;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluationPrecision;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluationRecall;
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
			if (this.label.equals(label))
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
		
		public static interface InverseLabelIndicator<L> {
			String toString();
			L label(Map<String, Double> indicatorWeights, List<String> positiveIndicators);
		}
		
		public static interface Clusterer<D extends Datum<L>, L, C> {
			String toString();
			C getCluster(D datum);
		}
		
		protected DataTools dataTools;
		
		private Map<String, TokenSpanExtractor<D, L>> tokenSpanExtractors;
		private Map<String, StringExtractor<D, L>> stringExtractors;
		private Map<String, DoubleExtractor<D, L>> doubleExtractors;
		private Map<String, LabelMapping<L>> labelMappings;
		private Map<String, LabelIndicator<L>> labelIndicators;
		private Map<String, InverseLabelIndicator<L>> inverseLabelIndicators;
		
		private Map<String, Feature<D, L>> genericFeatures;
		private Map<String, SupervisedModel<D, L>> genericModels;
		private Map<String, SupervisedModelEvaluation<D, L>> genericEvaluations;
		
		private Map<String, DatumStructureCollection<D, L>> genericDatumStructureCollections;
		

		private Map<String, List<Fn<TokenSpan, TokenSpan>>> genericTokenSpanFns;
		private Map<String, List<Fn<TokenSpan, String>>> genericTokenSpanStrFns;
		private Map<String, List<Fn<String, String>>> genericStrFns;
		
		public Tools(DataTools dataTools) {
			this.dataTools = dataTools;
			
			this.tokenSpanExtractors = new HashMap<String, TokenSpanExtractor<D, L>>();
			this.stringExtractors = new HashMap<String, StringExtractor<D, L>>();
			this.doubleExtractors = new HashMap<String, DoubleExtractor<D, L>>();
			this.labelMappings = new HashMap<String, LabelMapping<L>>();
			this.labelIndicators = new HashMap<String, LabelIndicator<L>>();
			this.inverseLabelIndicators = new HashMap<String, InverseLabelIndicator<L>>();
			this.genericFeatures = new HashMap<String, Feature<D, L>>();
			this.genericModels = new HashMap<String, SupervisedModel<D, L>>();
			this.genericEvaluations = new HashMap<String, SupervisedModelEvaluation<D, L>>();
			
			this.genericDatumStructureCollections = new HashMap<String, DatumStructureCollection<D, L>>();
			
			this.genericTokenSpanFns = new HashMap<String, List<Fn<TokenSpan, TokenSpan>>>();
			this.genericTokenSpanStrFns = new HashMap<String, List<Fn<TokenSpan, String>>>();
			this.genericStrFns = new HashMap<String, List<Fn<String, String>>>();
			
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
			addGenericFeature(new FeatureIdentity<D, L>());
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
			
			addGenericModel(new SupervisedModelCreg<D, L>());
			addGenericModel(new SupervisedModelLabelDistribution<D, L>());
			addGenericModel(new SupervisedModelSVM<D, L>());
			addGenericModel(new SupervisedModelSVMStructured<D, L>());
			addGenericModel(new SupervisedModelPartition<D, L>());
			addGenericModel(new SupervisedModelAreg<D, L>());
			addGenericModel(new SupervisedModelLogistmarGramression<D, L>());
			addGenericModel(new SupervisedModelLGApproximation<D, L>());
			addGenericModel(new SupervisedModelYADLL<D, L>());
			
			addGenericEvaluation(new SupervisedModelEvaluationAccuracy<D, L>());
			addGenericEvaluation(new SupervisedModelEvaluationPrecision<D, L>());
			addGenericEvaluation(new SupervisedModelEvaluationRecall<D, L>());
			addGenericEvaluation(new SupervisedModelEvaluationF<D, L>());
			
			addGenericTokenSpanFn(new FnComposite.FnCompositeTokenSpan());
			addGenericTokenSpanFn(new FnCompositeAppend.FnCompositeAppendTokenSpan());
			addGenericTokenSpanFn(new FnHead());
			addGenericTokenSpanFn(new FnNGramContext());
			addGenericTokenSpanFn(new FnNGramDocument());
			addGenericTokenSpanFn(new FnNGramInside());
			addGenericTokenSpanFn(new FnNGramSentence());
			addGenericTokenSpanFn(new FnIdentity<TokenSpan>());
			addGenericTokenSpanFn(new FnCoref());
			addGenericTokenSpanFn(new FnDependencyRelation());
			
			addGenericTokenSpanStrFn(new FnComposite.FnCompositeTokenSpanTokenSpanStr());
			addGenericTokenSpanStrFn(new FnComposite.FnCompositeTokenSpanStrStr());
			addGenericTokenSpanStrFn(new FnRelationStr.FnRelationStrTokenSpan());
			addGenericTokenSpanStrFn(new FnPoS());
			addGenericTokenSpanStrFn(new FnString());
			addGenericTokenSpanStrFn(new FnTokenSpanPathStr());

			addGenericStrFn(new FnComposite.FnCompositeStr());
			addGenericStrFn(new FnCompositeAppend.FnCompositeAppendStr());
			addGenericStrFn(new FnRelationStr.FnRelationStrStr());
			addGenericStrFn(new FnAffix());
			addGenericStrFn(new FnFilter());
			addGenericStrFn(new FnGazetteerFilter());
			addGenericStrFn(new FnGazetteer());
			addGenericStrFn(new FnSplit());
			addGenericStrFn(new FnIdentity<String>());
			addGenericStrFn(new FnClean());
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
		
		public InverseLabelIndicator<L> getInverseLabelIndicator(String name) {
			return this.inverseLabelIndicators.get(name);
		}
		
		public Feature<D, L> makeFeatureInstance(String genericFeatureName, Context<D, L> context) {
			return this.genericFeatures.get(genericFeatureName).makeInstance(context); 
		}
		
		public SupervisedModel<D, L> makeModelInstance(String genericModelName, Context<D, L> context) {
			return this.genericModels.get(genericModelName).makeInstance(context); 
		}
		
		public SupervisedModelEvaluation<D, L> makeEvaluationInstance(String genericEvaluationName, Context<D, L> context) {
			if (!this.genericEvaluations.containsKey(genericEvaluationName))
				return null;
			return this.genericEvaluations.get(genericEvaluationName).makeInstance(context); 
		}
		
		public DatumStructureCollection<D, L> makeDatumStructureCollection(String genericCollectionName, DataSet<D, L> data) {
			return this.genericDatumStructureCollections.get(genericCollectionName).makeInstance(data);
		}
		
		public List<Fn<String, String>> makeStrFns(String genericStrFnName, Context<D, L> context) {
			if (!this.genericStrFns.containsKey(genericStrFnName))
				return new ArrayList<Fn<String, String>>();
			List<Fn<String, String>> genericStrFns = this.genericStrFns.get(genericStrFnName);
			List<Fn<String, String>> strFns = new ArrayList<Fn<String, String>>(genericStrFns.size());
			
			for (Fn<String, String> genericStrFn : genericStrFns)
				strFns.add(genericStrFn.makeInstance(context));
			
			return strFns;
		}
		
		public List<Fn<TokenSpan, TokenSpan>> makeTokenSpanFns(String genericTokenSpanFnName, Context<D, L> context) {
			if (!this.genericTokenSpanFns.containsKey(genericTokenSpanFnName))
				return new ArrayList<Fn<TokenSpan, TokenSpan>>();
			
			List<Fn<TokenSpan, TokenSpan>> genericTokenSpanFns = this.genericTokenSpanFns.get(genericTokenSpanFnName);
			List<Fn<TokenSpan, TokenSpan>> tokenSpanFns = new ArrayList<Fn<TokenSpan, TokenSpan>>(genericTokenSpanFns.size());
			
			for (Fn<TokenSpan, TokenSpan> genericTokenSpanFn : genericTokenSpanFns)
				tokenSpanFns.add(genericTokenSpanFn.makeInstance(context));
			
			return tokenSpanFns;
		}
		
		public List<Fn<TokenSpan, String>> makeTokenSpanStrFns(String genericTokenSpanStrFnName, Context<D, L> context) {
			if (!this.genericTokenSpanStrFns.containsKey(genericTokenSpanStrFnName))
				return new ArrayList<Fn<TokenSpan, String>>();
			List<Fn<TokenSpan, String>> genericTokenSpanStrFns = this.genericTokenSpanStrFns.get(genericTokenSpanStrFnName);
			List<Fn<TokenSpan, String>> tokenSpanStrFns = new ArrayList<Fn<TokenSpan, String>>(genericTokenSpanStrFns.size());
			
			for (Fn<TokenSpan, String> genericTokenSpanStrFn : genericTokenSpanStrFns)
				tokenSpanStrFns.add(genericTokenSpanStrFn.makeInstance(context));
			
			return tokenSpanStrFns;
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
		
		public boolean addGenericFeature(Feature<D, L> feature) {
			this.genericFeatures.put(feature.getGenericName(), feature);
			return true;
		}
		
		public boolean addGenericModel(SupervisedModel<D, L> model) {
			this.genericModels.put(model.getGenericName(), model);
			return true;
		}
		
		public boolean addGenericEvaluation(SupervisedModelEvaluation<D, L> evaluation) {
			this.genericEvaluations.put(evaluation.getGenericName(), evaluation);
			return true;
		}
		
		public boolean addGenericDatumStructureCollection(DatumStructureCollection<D, L> datumStructureCollection) {
			this.genericDatumStructureCollections.put(datumStructureCollection.getGenericName(), datumStructureCollection);
			return true;
		}
		
		public boolean addGenericStrFn(Fn<String, String> strFn) {
			if (!this.genericStrFns.containsKey(strFn.getGenericName()))
				this.genericStrFns.put(strFn.getGenericName(), new ArrayList<Fn<String, String>>());
			this.genericStrFns.get(strFn.getGenericName()).add(strFn);
			return true;
		}
		
		public boolean addGenericTokenSpanFn(Fn<TokenSpan, TokenSpan> tokenSpanFn) {
			if (!this.genericTokenSpanFns.containsKey(tokenSpanFn.getGenericName()))
				this.genericTokenSpanFns.put(tokenSpanFn.getGenericName(), new ArrayList<Fn<TokenSpan, TokenSpan>>());
			this.genericTokenSpanFns.get(tokenSpanFn.getGenericName()).add(tokenSpanFn);
			return true;
		}
		
		public boolean addGenericTokenSpanStrFn(Fn<TokenSpan, String> tokenSpanStrFn) {
			if (!this.genericTokenSpanStrFns.containsKey(tokenSpanStrFn.getGenericName()))
				this.genericTokenSpanStrFns.put(tokenSpanStrFn.getGenericName(), new ArrayList<Fn<TokenSpan, String>>());
			this.genericTokenSpanStrFns.get(tokenSpanStrFn.getGenericName()).add(tokenSpanStrFn);
			return true;
		}
		
		public boolean addLabelIndicator(LabelIndicator<L> labelIndicator) {
			this.labelIndicators.put(labelIndicator.toString(), labelIndicator);
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
		
		public abstract L labelFromString(String str);
		public abstract JSONObject datumToJSON(D datum);
		public abstract D datumFromJSON(JSONObject json);
		public abstract <T extends Datum<Boolean>> T makeBinaryDatum(D datum, LabelIndicator<L> labelIndicator);
		public abstract <T extends Datum<Boolean>> Datum.Tools<T, Boolean> makeBinaryDatumTools(LabelIndicator<L> labelIndicator);
	}
}
