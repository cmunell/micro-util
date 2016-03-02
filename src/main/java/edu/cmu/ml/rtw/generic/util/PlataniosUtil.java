package edu.cmu.ml.rtw.generic.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.platanios.learn.data.DataSetInMemory;
import org.platanios.learn.data.PredictedDataInstance;
import org.platanios.learn.math.matrix.SparseVector;
import org.platanios.learn.math.matrix.Vector;
import org.platanios.learn.math.matrix.Vector.VectorElement;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;

public class PlataniosUtil {
	public static Map<Integer, Double> vectorToMap(Vector vector) {
		Map<Integer, Double> map = new HashMap<Integer, Double>();
		for (VectorElement vectorElement : vector)
			map.put(vectorElement.index(), vectorElement.value());
		return map;
	}
	
	/**
	 * Converts a DataFeatureMatrix into a 'learn' library 
	 * data set for use in SupervisedModelAreg, SupervisedModelLogistmarGramression,
	 * and other models that use Platanios 'learn' library internally.
	 * 
	 * @param dataFeatureMatrix the data to convert
	 * 
	 * @param weightedLabels indicates whether data instance labels should be 
	 * weighted (this is for use in EM)
	 * 
	 * @param minPositiveSampleRate gives the minimum fraction of positive labels
	 * in the returned data set.  If there are less than this fraction of positive
	 * examples in the source data set, then only a subset of negative examples will
	 * be in the returned data to match the minimum fraction.  This is useful when
	 * training logistic regression classifiers on data where some labels occur
	 * very infrequently 
	 * 
	 * @param onlyLabeled indicates whether only labeled data should be returned
	 * 
	 * @param infiniteVectorsWithBias indicates whether the returned vectors should
	 * have infinite dimensions with a bias element (always 1) for the first dimension.
	 * This is used by SupervisedModelLogistmarGramression.
	 * 
	 * @return a 'learn' library data set
	 */
	public static <T extends Datum<Boolean>> DataSetInMemory<PredictedDataInstance<Vector, Double>> makePlataniosDataSet(DataFeatureMatrix<T, Boolean> data, boolean weightedLabels, double minPositiveSampleRate, boolean onlyLabeled, boolean infiniteVectorsWithBias) {
		
		
		double pos = (data.getData().getDataSizeForLabel(true) > 0) ? data.getData().getDataSizeForLabel(true) : 1.0;
		double neg = (data.getData().getDataSizeForLabel(false) > 0) ? data.getData().getDataSizeForLabel(false) : 0.0;
		double posFrac = pos/(pos+neg);
		double negFrac = 1.0;
		if (posFrac < minPositiveSampleRate) {
			double targetNeg = (pos-minPositiveSampleRate*pos)/minPositiveSampleRate;
			negFrac = targetNeg/neg;
		}
		
		final double finalNegFrac = negFrac;
		final Random r = data.getData().getDatumTools().getDataTools().makeLocalRandom();
		
		List<PredictedDataInstance<Vector, Double>> dataInstances = data.getData().map(new ThreadMapper.Fn<T, PredictedDataInstance<Vector, Double>>() {
				@Override
				public PredictedDataInstance<Vector, Double> apply(T datum) {
					Vector vector = null;
					if (infiniteVectorsWithBias) {
						Vector features = data.getFeatureVocabularyValues(datum, false);
						Map<Integer, Double> vectorMap = new HashMap<Integer, Double>();
						vectorMap.put(0, 1.0);
						for (VectorElement featureElement : features)
							vectorMap.put(featureElement.index() + 1, featureElement.value());
						vector = new SparseVector(Integer.MAX_VALUE, vectorMap);
					} else {
						vector = data.getFeatureVocabularyValues(datum, false);
					}
					
					Double label = null;
					if (datum.getLabel() != null) {
						if (!(Boolean)datum.getLabel() && r.nextDouble() > finalNegFrac)
							return null;					
						if (weightedLabels) {
							label = datum.getLabelWeight(new Boolean(true));
						} else {
							label = (Boolean)datum.getLabel() ? 1.0 : 0.0;
						}
					}
				
					return new PredictedDataInstance<Vector, Double>(String.valueOf(datum.getId()), vector, label, null, 1);
				}
			}, data.getContext().getMaxThreads());
		
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
