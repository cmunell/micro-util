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

/**
 * FilteredVocabFeatureSet represents a set of FeatureTokenSpanFnFilteredVocab
 * features with interleaving vocabularies.  The vocabulary indices of features
 * in the vocabulary interleave as a result of merging new feature objects with
 * existing features in the vocabulary (see the addFeature method below).  This
 * merging improves efficiency when thousands of new features with small (i.e. 
 * single element) vocabularies are constructed, and they all need to be computed
 * for many examples (e.g. in SupervisedModelLogistmarGramression).
 * 
 * (This is currently just used by SupervisedModelLogistmarGrammression. If you
 * aren't working with that, then you can just ignore this class.)
 * 
 * @author Bill McDowell
 *
 * @param <D>
 * @param <L>
 */
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
