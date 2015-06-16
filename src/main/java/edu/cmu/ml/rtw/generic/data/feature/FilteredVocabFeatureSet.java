package edu.cmu.ml.rtw.generic.data.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.platanios.learn.math.matrix.SparseVector;
import org.platanios.learn.math.matrix.Vector;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;

public class FilteredVocabFeatureSet<D extends Datum<L>, L> {
	private List<FeatureTokenSpanFnFilteredVocab<D, L>> featureList;
	private TreeMap<Integer, FeatureTokenSpanFnFilteredVocab<D, L>> features; // Map start indices to features
	private int featureVocabularySize;
	
	public FilteredVocabFeatureSet() {
		this.features = new TreeMap<Integer, FeatureTokenSpanFnFilteredVocab<D, L>>();
		this.featureVocabularySize = 0;
		this.featureList = new ArrayList<FeatureTokenSpanFnFilteredVocab<D, L>>();
	}
	
	public int getFeatureVocabularySize() {
		return this.featureVocabularySize;
	}
	
	public boolean addFeature(FeatureTokenSpanFnFilteredVocab<D, L> feature) {
		for (Entry<Integer, FeatureTokenSpanFnFilteredVocab<D, L>> existingFeature : this.features.entrySet()) {
			if (existingFeature.getValue().merge(feature, this.featureVocabularySize - existingFeature.getKey())) {
				this.featureVocabularySize += feature.getVocabularySize();
				return false;
			}
		}
		
		this.features.put(this.featureVocabularySize, feature);
		this.featureList.add(feature);
		this.featureVocabularySize += feature.getVocabularySize();
		
		return true;
	}
	
	public Vector computeFeatureVocabularyRange(D datum, int startIndex, int endIndex) {
		Map<Integer, Double> values = new HashMap<Integer, Double>();
		
		for (Entry<Integer, FeatureTokenSpanFnFilteredVocab<D, L>> featureEntry : this.features.entrySet()) {
			// FIXME There's another possible range optimization here if 
			// can get access to feature max index
			if (featureEntry.getKey() >= endIndex)
				break;
			
			values = featureEntry.getValue().computeVector(datum, 
														   featureEntry.getKey(), 
														   startIndex - featureEntry.getKey(),
														   endIndex - featureEntry.getKey(), 
														   values);
		}
		
		return new SparseVector(endIndex - startIndex, values);
	}
	
	public FeatureTokenSpanFnFilteredVocab<D, L> getFeatureByVocabularyIndex(int index) {
		for (Entry<Integer, FeatureTokenSpanFnFilteredVocab<D, L>> featureEntry : this.features.entrySet()) {
			if (featureEntry.getKey() > index)
				break;
			
			if (featureEntry.getValue().vocabularyContainsIndex(index - featureEntry.getKey()))
				return featureEntry.getValue();
		}
		
		return null;
	}
	
	public int getFeatureStartVocabularyIndex(int index) {
		for (Entry<Integer, FeatureTokenSpanFnFilteredVocab<D, L>> featureEntry : this.features.entrySet()) {
			if (featureEntry.getKey() > index)
				break;
			
			if (featureEntry.getValue().vocabularyContainsIndex(index - featureEntry.getKey()))
				return featureEntry.getKey();
		}
		
		return -1;
	}
	
	public int getFeatureCount() {
		return this.featureList.size();
	}
	
	public FeatureTokenSpanFnFilteredVocab<D, L> getFeature(int index) {
		return this.featureList.get(index);
	}
}
