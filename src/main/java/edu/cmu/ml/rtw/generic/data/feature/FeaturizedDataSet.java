package edu.cmu.ml.rtw.generic.data.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.TreeMap;

import org.platanios.learn.data.DataSetInMemory;
import org.platanios.learn.data.PredictedDataInstance;
import org.platanios.learn.math.matrix.SparseVector;
import org.platanios.learn.math.matrix.Vector;
import org.platanios.learn.math.matrix.Vector.VectorElement;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;

/**
 * DataSet represents a collection of labeled and/or unlabeled 'datums'
 * that have been featurized by a provided set of features to train and 
 * evaluate models.  
 * 
 * The current implementation computes the features on demand as their
 * values are requested, and permanently caches their values in memory.
 * In the future, this might be improved so that some values can be
 * evicted from the cache and possibly serialized/deserialized from
 * disk.
 * 
 * @author Bill McDowell
 *
 * @param <D> Datum type
 * @param <L> Datum label type
 */
public class FeaturizedDataSet<D extends Datum<L>, L> extends DataSet<D, L> {
	private String name;
	private int maxThreads;
	
	private List<Feature<D, L>> featureList; // Just to keep all of the features referenced in one place for when cloning the dataset
	private Map<String, Feature<D, L>> referencedFeatures; // Maps from reference names to features
	private TreeMap<Integer, Feature<D, L>> features; // Maps from the feature's starting vocabulary index to the feature
	private Map<Integer, String> featureVocabularyNames; // Sparse map from indices to names
	private Map<Integer, Vector> featureVocabularyValues; // Map from datum ids to indices to values
	private int featureVocabularySize;
	private boolean precomputedFeatures;
	
	public FeaturizedDataSet(String name, Datum.Tools<D, L> datumTools, Datum.Tools.LabelMapping<L> labelMapping) {
		this(name, 1, datumTools, labelMapping);
	}
	
	public FeaturizedDataSet(String name, int maxThreads, Datum.Tools<D, L> datumTools, Datum.Tools.LabelMapping<L> labelMapping) {
		this(name, new ArrayList<Feature<D, L>>(), maxThreads, datumTools, labelMapping);
	}
	
	public FeaturizedDataSet(String name, List<Feature<D, L>> features, int maxThreads, Datum.Tools<D, L> datumTools, Datum.Tools.LabelMapping<L> labelMapping) {
		super(datumTools, labelMapping);
		this.name = name;
		this.referencedFeatures = new HashMap<String, Feature<D, L>>();
		this.features = new TreeMap<Integer, Feature<D, L>>();
		this.maxThreads = maxThreads;
		 
		this.featureList = new ArrayList<Feature<D, L>>();
		this.featureVocabularySize = 0;
		for (Feature<D, L> feature : features)
			addFeature(feature);
		
		this.featureVocabularyNames = new ConcurrentHashMap<Integer, String>();
		this.featureVocabularyValues = new ConcurrentHashMap<Integer, Vector>();
		this.precomputedFeatures = false;
	}
	
	public String getName() {
		return this.name;
	}
	
	public boolean getPrecomputedFeatures() {
		return this.precomputedFeatures;
	}
	
	public int getMaxThreads() {
		return this.maxThreads;
	}
	
	public boolean setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
		return true;
	}
	
	public <T> List<T> map(final ThreadMapper.Fn<D, T> fn) {
		return map(fn, this.maxThreads);
	}
	
	/**
	 * @param feature
	 * @param initFeature
	 * @return true if the feature has been added.  This does *not* call
	 * the feature's initialization method, so it must be called outside
	 * the FeaturizedDataSet before the feature is added to the set (this
	 * is necessary so that the feature can sometimes be initialized using
	 * a different data set from the one to which it is added)
	 * 
	 * If the feature is ignored (feature.isIgnored returns true), then
	 * the feature's values will not be included in vectors returned by requests
	 * to this FeaturizedDataSet, and so it will not be used by models that
	 * used this FeaturizedDataSet.
	 * 
	 * Features can be retrieved from this by their 'reference names' (even
	 * if they are ignored features).  This is useful when one feature is computed
	 * using the values of another.  If one feature requires another for its
	 * computation, but the required feature should not be included in a model that
	 * refers to FeaturizedDataSet, then the required feature should be 
	 * set to be 'ignored'.
	 * 
	 */
	public boolean addFeature(Feature<D, L> feature, boolean initFeature) {
		if (initFeature)
			if (!feature.init(this))
				return false;
		return addFeatureHelper(feature);
	}
	
	public boolean addFeature(Feature<D, L> feature) {
		return addFeature(feature, false);
	}
	
	public boolean addFeatures(List<Feature<D, L>> features, boolean initFeatures) {
		if (initFeatures) {
			final FeaturizedDataSet<D, L> data = this;
			ThreadMapper<Feature<D, L>, Boolean> threads = new ThreadMapper<Feature<D, L>, Boolean>(new Fn<Feature<D, L>, Boolean>() {
				public Boolean apply(Feature<D, L> feature) {
					return feature.init(data);
				}
			});
			
			List<Boolean> threadResults = threads.run(features, this.maxThreads);
			for (boolean threadResult : threadResults)
				if (!threadResult)
					return false;
		}
		
		for (Feature<D, L> feature : features)
			if (!addFeatureHelper(feature))
				return false;
		
		return true;
	}
	
	public boolean addFeatures(List<Feature<D, L>> features) {
		return addFeatures(features, false);
	}
	
	
	private boolean addFeatureHelper(Feature<D, L> feature) {
		if (!feature.isIgnored()) {
			this.features.put(this.featureVocabularySize, feature);
			this.featureVocabularySize += feature.getVocabularySize();
		}
		if (feature.getReferenceName() != null)
			this.referencedFeatures.put(feature.getReferenceName(), feature);
		this.featureList.add(feature);
		
		return true;
	}
	
	public Feature<D, L> getFeature(int index) {
		return this.featureList.get(index);
	}
	
	public List<Feature<D, L>> getFeatures() {
		return this.featureList;
	}
	
	public Feature<D, L> getFeatureByVocabularyIndex(int index) {
		return this.features.get(this.features.floorKey(index));
	}
	
	public int getFeatureStartVocabularyIndex(int index) {
		return this.features.floorKey(index);
	}
	
	public Feature<D, L> getFeatureByReferenceName(String referenceName) {
		return this.referencedFeatures.get(referenceName);
	}
	
	public int getFeatureCount() {
		return this.features.size();
	}
	
	public int getFeatureVocabularySize() {
		return this.featureVocabularySize;
	}
	
	public Map<Integer, String> getFeatureVocabularyNamesForIndices(Iterable<Integer> indices) {
		Map<Integer, String> names = new HashMap<Integer, String>();
		Map<Integer, List<Integer>> featuresToIndices = new HashMap<Integer, List<Integer>>();
		for (Integer index : indices) {
			if (this.featureVocabularyNames.containsKey(index)) {
				names.put(index, this.featureVocabularyNames.get(index));
			} else {
				Integer featureIndex = this.features.floorKey(index);
				if (!featuresToIndices.containsKey(featureIndex))
					featuresToIndices.put(featureIndex, new ArrayList<Integer>());
				featuresToIndices.get(featureIndex).add(index - featureIndex);
			}
		}
		
		for (Entry<Integer, List<Integer>> featureToIndices : featuresToIndices.entrySet()) {
			Feature<D, L> feature = this.features.get(featureToIndices.getKey());
			names = feature.getSpecificShortNamesForIndices(featureToIndices.getValue(), featureToIndices.getKey(), names);
		}
		
		this.featureVocabularyNames.putAll(names);
		
		return names;
	}
	
	public List<String> getFeatureVocabularyNames() {
		List<String> featureVocabularyNames = new ArrayList<String>(this.featureVocabularySize);
		
		for (Feature<D, L> feature : this.features.values()) {
			featureVocabularyNames = feature.getSpecificShortNames(featureVocabularyNames); 
		}
		
		return featureVocabularyNames;
	}
	
	public Map<Integer, Double> getFeatureVocabularyValuesAsMap(D datum) {
		return getFeatureVocabularyValuesAsMap(datum, true);
	}
	
	public Map<Integer, Double> getFeatureVocabularyValuesAsMap(D datum, boolean cacheValues) {
		Map<Integer, Double> map = new HashMap<Integer, Double>();
		Vector vector = getFeatureVocabularyValues(datum, cacheValues);
		for (VectorElement vectorElement : vector)
			map.put(vectorElement.index(), vectorElement.value());
		return map;
	}
	
	public Vector getFeatureVocabularyValues(D datum) {
		return getFeatureVocabularyValues(datum, true);
	}
	
	public Vector getFeatureVocabularyValues(D datum, boolean cacheValues) {
		if (!this.data.containsKey(datum.getId()))
			return null;
		if (this.featureVocabularyValues.containsKey(datum.getId()))
			return this.featureVocabularyValues.get(datum.getId());
		
		Map<Integer, Double> values = new HashMap<Integer, Double>();
		for (Entry<Integer, Feature<D, L>> featureEntry : this.features.entrySet()) {
			values = featureEntry.getValue().computeVector(datum, featureEntry.getKey(), values);
		}
		
		Vector vector = new SparseVector(getFeatureVocabularySize(), values);
		
		if (cacheValues)
			this.featureVocabularyValues.put(datum.getId(), vector);
		
		return vector;
	}
	
	public Vector computeFeatureVocabularyRange(D datum, int startIndex, int endIndex) {
		Map<Integer, Double> values = new HashMap<Integer, Double>();
		for (Entry<Integer, Feature<D, L>> featureEntry : this.features.entrySet()) {
			if (featureEntry.getKey() + featureEntry.getValue().getVocabularySize() <= startIndex)
				continue;
			if (featureEntry.getKey() >= endIndex)
				break;
			
			if (startIndex <= featureEntry.getKey() && endIndex >= featureEntry.getKey() + featureEntry.getValue().getVocabularySize()) {
				values = featureEntry.getValue().computeVector(datum, featureEntry.getKey(), values);
			} else {
				Map<Integer, Double> featureValues = featureEntry.getValue().computeVector(datum);
			
				for (Entry<Integer, Double> featureValueEntry : featureValues.entrySet()) {
					int index = featureValueEntry.getKey() + featureEntry.getKey();
					if (index < startIndex || index >= endIndex)
						continue;
					values.put(index - startIndex, featureValueEntry.getValue());
				}
			}
		}
		
		return new SparseVector(endIndex - startIndex, values);
	}
	
	public boolean precomputeFeatures() {
		if (this.precomputedFeatures)
			return true;
		
		List<Boolean> threadResults = map(new ThreadMapper.Fn<D, Boolean>() {
			@Override
			public Boolean apply(D datum) {
				Vector featureVector = getFeatureVocabularyValues(datum);
					if (featureVector == null)
						return false;
				
				return true;
			}
		}, this.maxThreads);
		

		for (boolean result : threadResults)
			if (!result)
				return false;
		
		this.precomputedFeatures = true;
		return true;
	}
	
	@Override
	public DataSet<D, L> getSubset(DataFilter dataFilter) {
		FeaturizedDataSet<D, L> subset = new FeaturizedDataSet<D, L>(this.name + " " + dataFilter, this.maxThreads, getDatumTools(), getLabelMapping());
		Iterator<D> iterator = iterator(dataFilter);
		
		if (!subset.addFeatures(this.featureList, false))
			return null;
		
		subset.featureVocabularyNames = this.featureVocabularyNames;
		subset.precomputedFeatures = this.precomputedFeatures;
		
		while (iterator.hasNext()) {
			D datum = iterator.next();
			subset.add(datum);
			subset.featureVocabularyValues.put(datum.getId(), this.featureVocabularyValues.get(datum.getId()));
		}
		
		return subset;
	}
	
	@Override
	public <T extends Datum<Boolean>> DataSet<T, Boolean> makeBinary(LabelIndicator<L> labelIndicator, Context<T, Boolean> context) {
		List<Feature<T, Boolean>> features = new ArrayList<Feature<T, Boolean>>();
		for (Feature<D, L> feature : this.featureList) {
			features.add(feature.makeBinary(context, labelIndicator));
		}
		
		FeaturizedDataSet<T, Boolean> dataSet = new FeaturizedDataSet<T, Boolean>(this.name + ((labelIndicator == null) ? "" : "/" + labelIndicator.toString()), features, 1, context.getDatumTools(), null);
		
		for (D datum : this) {
			dataSet.add(getDatumTools().<T>makeBinaryDatum(datum, labelIndicator));
		}
		
		dataSet.featureVocabularySize = this.featureVocabularySize;
		dataSet.featureVocabularyNames = this.featureVocabularyNames;
		dataSet.featureVocabularyValues = this.featureVocabularyValues;
		
		return dataSet;
	}
	
	public DataSetInMemory<PredictedDataInstance<Vector, Double>> makePlataniosDataSet(boolean weightedLabels, double minPositiveSampleRate, boolean onlyLabeled, boolean infiniteVectorsWithBias) {
		double pos = (this.labeledData.get(true) != null) ? this.labeledData.get(true).size() : 1.0;
		double neg = (this.labeledData.get(false) != null) ? this.labeledData.get(false).size() : 0.0;
		double posFrac = pos/(pos+neg);
		double negFrac = 1.0;
		if (posFrac < minPositiveSampleRate) {
			double targetNeg = (pos-minPositiveSampleRate*pos)/minPositiveSampleRate;
			negFrac = targetNeg/neg;
		}
		
		final double finalNegFrac = negFrac;
		final Random r = getDatumTools().getDataTools().makeLocalRandom();
		
		List<PredictedDataInstance<Vector, Double>> dataInstances = this.map(new ThreadMapper.Fn<D, PredictedDataInstance<Vector, Double>>() {
				@SuppressWarnings("unchecked")
				@Override
				public PredictedDataInstance<Vector, Double> apply(D datum) {
					Vector vector = null;
					if (infiniteVectorsWithBias) {
						Vector features = getFeatureVocabularyValues(datum, false);
						Map<Integer, Double> vectorMap = new HashMap<Integer, Double>();
						vectorMap.put(0, 1.0);
						for (VectorElement featureElement : features)
							vectorMap.put(featureElement.index() + 1, featureElement.value());
						vector = new SparseVector(Integer.MAX_VALUE, vectorMap);
					} else {
						vector = getFeatureVocabularyValues(datum, false);
					}
					
					Double label = null;
					if (datum.getLabel() != null) {
						if (!(Boolean)datum.getLabel() && r.nextDouble() > finalNegFrac)
							return null;					
						if (weightedLabels) {
							label = datum.getLabelWeight((L)(new Boolean(true)));
						} else {
							label = (Boolean)datum.getLabel() ? 1.0 : 0.0;
						}
					}
				
					return new PredictedDataInstance<Vector, Double>(String.valueOf(datum.getId()), vector, label, null, 1);
				}
			});
		
		// FIXME: Could do this faster by adding to list in thread mapper... but this is good enough for now
		List<PredictedDataInstance<Vector, Double>> retDataInstances = new ArrayList<PredictedDataInstance<Vector, Double>>();
		for (PredictedDataInstance<Vector, Double> dataInstance : dataInstances) {
			if (dataInstance == null) 
				continue;
			if (!onlyLabeled || dataInstance.label() != null)
				retDataInstances.add(dataInstance);
		}
		
		return new DataSetInMemory<PredictedDataInstance<Vector, Double>>(retDataInstances);
	}
}
