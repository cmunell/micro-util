package edu.cmu.ml.rtw.generic.data.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.platanios.learn.math.matrix.SparseVector;
import org.platanios.learn.math.matrix.Vector;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.Assignment.AssignmentTyped;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FeatureSet<D extends Datum<L>, L> extends CtxParsableFunction {
	private DatumContext<D, L> context;
	
	private boolean initialized = false;
	private List<Feature<D, L>> featureList;
	private TreeMap<Integer, Feature<D, L>> features; // Maps from the feature's starting vocabulary index to the feature
	private Map<Integer, String> featureVocabularyNames; // Sparse map from indices to names
	private int featureVocabularySize;
	
	private Obj.Array featuresObj;
	private Obj.Array initDataObj;
	private String[] parameterNames = { "features", "initData" };
	
	public FeatureSet(DatumContext<D, L> context) {
		this.context = context;	
	}

	public FeatureSet(DatumContext<D, L> context, List<Feature<D, L>> features) {
		this.context = context;	
		clear();
		for (Feature<D, L> feature : features)
			addFeatureHelper(feature);
	}
	
	private boolean clear() {
		this.featureList = new ArrayList<Feature<D, L>>();
		this.features = new TreeMap<Integer, Feature<D, L>>();
		this.featureVocabularySize = 0;
		this.featureVocabularyNames = new ConcurrentHashMap<Integer, String>();
		this.initialized = false;
		return true;
	}
	
	public boolean isInitialized() {
		return this.initialized;
	}
	
	public boolean init() {
		if (!clear())
			return false;
		
		DataSet<D, L> initData = new DataSet<D, L>(this.context.getDatumTools());
		
		for (int i = 0; i < this.initDataObj.size(); i++) {
			DataSet<D, L> data = this.context.getMatchDataSet(this.initDataObj.get(i));
			if (data.isBuildable() && !data.isBuilt())
				if (!data.build())
					return false;
			
			initData.addAll(data);
		}
		
		for (int i = 0; i < this.featuresObj.size(); i++) {
			Feature<D, L> feature = this.context.getMatchFeature(this.featuresObj.get(i)).clone(false);
			if (!feature.init(initData))
				return false;
			
			if (!addFeatureHelper(feature))
				return false;
		}
		
		this.initialized =  true;
		
		return true;
	}
	
	private boolean addFeatureHelper(Feature<D, L> feature) {
		this.featureList.add(feature);
		
		if (!feature.isIgnored()) {
			this.features.put(this.featureVocabularySize, feature);
			this.featureVocabularySize += feature.getVocabularySize();
		}
		
		return true;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("features"))
			return this.featuresObj;
		else if (parameter.equals("initData"))
			return this.initDataObj;
		else 
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("features"))
			this.featuresObj = (Obj.Array)parameterValue;
		else if (parameter.equals("initData"))
			this.initDataObj = (Obj.Array)parameterValue;
		else 
			return false;
		return true;
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		if (internalAssignments == null)
			return true;

		if (!clear())
			return false;
		
		for (int i = 0; i < internalAssignments.size(); i++) {
			AssignmentTyped assignment = (AssignmentTyped)internalAssignments.get(i);
			
			if (assignment.getType().equals(Context.ObjectType.VALUE.toString()) && assignment.getName().equals("initialized")) {
				this.initialized = Boolean.valueOf(this.context.getMatchValue(assignment.getValue()));
				continue;
			}
			
			if (!assignment.getType().equals(DatumContext.ObjectType.FEATURE.toString()))
				continue;
			
			Obj.Function fnObj = (Obj.Function)assignment.getValue();
			Feature<D, L> feature = this.context.getDatumTools().makeFeatureInstance(fnObj.getName(), this.context);
			if (!feature.fromParse(assignment.getModifiers(), assignment.getName(), fnObj))
				return false;
			
			if (!addFeatureHelper(feature))
				return false;
		}

		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		AssignmentList assignments = new AssignmentList();
		
		assignments.add(Assignment.assignmentTyped(null, Context.ObjectType.VALUE.toString(), "vocabularySize", Obj.stringValue(String.valueOf(this.featureVocabularySize))));
		assignments.add(Assignment.assignmentTyped(null, Context.ObjectType.VALUE.toString(), "initialized", Obj.stringValue(String.valueOf(this.initialized))));
	
		for (Feature<D, L> feature : this.featureList) {
			assignments.add(Assignment.assignmentTyped(feature.getModifiers(), DatumContext.ObjectType.FEATURE.toString(), feature.getReferenceName(), feature.toParse(true)));
		}
		return assignments;
	}

	@Override
	public String getGenericName() {
		return "FeatureSet";
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
	
	public Vector getFeatureVocabularyValues(D datum) {
		Map<Integer, Double> values = new HashMap<Integer, Double>();
		for (Entry<Integer, Feature<D, L>> featureEntry : this.features.entrySet()) {
			values = featureEntry.getValue().computeVector(datum, featureEntry.getKey(), values);
		}
		
		return new SparseVector(getFeatureVocabularySize(), values);
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
	
	public <T extends Datum<Boolean>> FeatureSet<T, Boolean> makeBinary(LabelIndicator<L> labelIndicator, DatumContext<T, Boolean> context) {
		FeatureSet<T, Boolean> featureSet = new FeatureSet<T, Boolean>(context);
		featureSet.clear();
		
		for (Feature<D, L> feature : this.featureList) {
			featureSet.addFeatureHelper(feature.makeBinary(context, labelIndicator));
		}

		featureSet.featureVocabularyNames = this.featureVocabularyNames;
		featureSet.featureVocabularySize = this.featureVocabularySize;
		featureSet.featuresObj = this.featuresObj;
		featureSet.initDataObj = this.initDataObj;
				
		return featureSet;
	}
	
	public FeatureSet<D, L> clone(boolean cloneInternal) {
		FeatureSet<D, L> clone = new FeatureSet<D, L>(this.context);
		
		if (!clone.fromParse(getModifiers(), getReferenceName(), toParse(cloneInternal)))
			return null;
		
		return clone;
	}
	
	public DatumContext<D, L> getContext() {
		return this.context;
	}
}
