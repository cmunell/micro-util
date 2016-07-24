package edu.cmu.ml.rtw.generic.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.platanios.learn.math.matrix.Vector;
import org.platanios.learn.math.matrix.Vector.VectorElement;

import edu.stanford.nlp/*.legacy*/.classify.GeneralDataset;
import edu.stanford.nlp/*.legacy*/.classify.LinearClassifier;
import edu.stanford.nlp/*.legacy*/.classify.LinearClassifierFactory;
import edu.stanford.nlp/*.legacy*/.classify.RVFDataset;
import edu.stanford.nlp/*.legacy*/.ling.RVFDatum;
import edu.stanford.nlp/*.legacy*/.stats.ClassicCounter;
import edu.stanford.nlp/*.legacy*/.stats.Counter;
import edu.stanford.nlp/*.legacy*/.stats.Counters;
import edu.stanford.nlp/*.legacy*/.util.Triple;
import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet.DataFilter;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.MathUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.StringUtil;

public class SupervisedModelStanfordLinear<D extends Datum<L>, L> extends SupervisedModel<D, L> {
	private double classificationThreshold = -1.0;
	private String defaultLabel = null;
	private boolean searchThreshold = false;
	private double searchThresholdFB = 1.0;
	private int minFeatureOccurrence = 1;
	private String[] hyperParameterNames = { "classificationThreshold", "defaultLabel", "searchThreshold", "searchThresholdFB", "minFeatureOccurrence" };
	
	private LinearClassifier<String, String> classifier;
	private List<String> featureNames = null;
	
	public SupervisedModelStanfordLinear() {
		
	}
	
	public SupervisedModelStanfordLinear(DatumContext<D, L> context) {
		this.context = context;
	}
	
	@Override
	public boolean train(DataFeatureMatrix<D, L> data, DataFeatureMatrix<D, L> testData, List<SupervisedModelEvaluation<D, L>> evaluations) {
		GeneralDataset<String,String> rvfData = makeData(data, true);

		LinearClassifierFactory<String,String> linearFactory = new LinearClassifierFactory<String,String>();
	    this.classifier = linearFactory.trainClassifier(rvfData);
	    
	    if (this.searchThreshold && this.defaultLabel != null)
	    	this.classificationThreshold = searchFBThreshold(testData);
	    
	    
        List<Triple<String, String, Double>> fw = this.classifier.getTopFeatures(0.0, true, 200);
        for (Triple<String, String, Double> featureWeight : fw) {
            System.out.println(featureWeight.first() + " " + featureWeight.second() + " " + featureWeight.third());
        }

		return true; 
	}
	
	private double searchFBThreshold(DataFeatureMatrix<D, L> testData) {
		Map<D, Map<L, Double>> p = posterior(testData);
		List<Pair<D, Double>> sortedData = new ArrayList<>();
		double totalPositive = 0.0;
		L defaultLabel = this.context.getDatumTools().labelFromString(this.defaultLabel);
		for (Entry<D, Map<L, Double>> entry : p.entrySet()) {
			if (entry.getValue().containsKey(defaultLabel)) {
				sortedData.add(new Pair<>(entry.getKey(), entry.getValue().get(defaultLabel)));
			}
			
			if (entry.getKey().getLabel().equals(defaultLabel))
				totalPositive++;
		}
		
		
		sortedData.sort(new Comparator<Pair<D, Double>>() {
			@Override
			public int compare(Pair<D, Double> o1, Pair<D, Double> o2) {
				return Double.compare(o2.getSecond(), o1.getSecond());
			}
		});
		
		double tp = 0;
		double fp = 0;
		double maxF1 = Double.NEGATIVE_INFINITY;
		double threshold = -1.0;
		double B = this.searchThresholdFB;
		for (Pair<D, Double> entry : sortedData) {
			if (entry.getFirst().getLabel().equals(defaultLabel))
				tp++;
			else
				fp++;
			
			double fn = totalPositive - tp;
			double prec = Double.compare(tp + fp, 0.0) == 0 ? 1.0 : tp/(tp + fp);
			double recall = Double.compare(tp + fn, 0.0) == 0 ? 1.0 : tp/(tp + fn);
			double fB = (1 + B*B) * prec * recall / (B*B*prec + recall);
			
			if (Double.compare(fB, maxF1) >= 0) {
				maxF1 = fB;
				threshold = entry.getSecond();
			}
		}
		
		return threshold;
	}

	private GeneralDataset<String, String> makeData(DataFeatureMatrix<D, L> data, boolean onlyLabeled) {
		Iterator<D> iter = onlyLabeled ? data.getData().iterator(DataFilter.OnlyLabeled) : data.getData().iterator();
		GeneralDataset<String, String> rvfData = /*new Dataset<String, String>();*/new RVFDataset<String,String>();
		synchronized (this) {
			if (this.featureNames == null)
				this.featureNames = data.getFeatures().getFeatureVocabularyNames();
		}
		
		while (iter.hasNext()) {
			D datum = iter.next();
			Vector f = data.getFeatureVocabularyValues(datum, false);
			Counter<String> feats = new ClassicCounter<String>();
			
			for (VectorElement e : f) {
				feats.incrementCount(String.valueOf(e.index() + "_" + this.featureNames.get(e.index())), e.value());
			}
			
			rvfData.add(	
				new RVFDatum<String,String>(feats,  datum.getLabel() != null ? datum.getLabel().toString() : "null")
			);
		}
	
		if (this.minFeatureOccurrence > 1 && onlyLabeled) {
			rvfData.applyFeatureCountThreshold(this.minFeatureOccurrence);
		}
			
		return rvfData;
	}
	
	@Override
	public Map<D, Map<L, Double>> posterior(DataFeatureMatrix<D, L> data) {
		GeneralDataset<String,String> rvfData = makeData(data, false);
		Map<D, Map<L, Double>> posteriors = new HashMap<D, Map<L, Double>>();
		int i = 0; 
		for (D datum : data.getData()) {
			Counter<String> scores = this.classifier.scoresOf(rvfData.getDatum(i));
			Map<L, Double> posterior = new HashMap<L, Double>();
			double sum = 0.0;
			for (Entry<String, Double> entry : scores.entrySet()) {
				double value = Math.exp(entry.getValue());
				L label = this.context.getDatumTools().labelFromString(entry.getKey());
				posterior.put(label, value);
				sum += value;
			}
			
			posterior = MathUtil.normalize(posterior, sum);
			
			/*if (Double.compare(this.classificationThreshold, 0.0) > 0) {
				List<L> toRemove = new ArrayList<L>();
				for (Entry<L, Double> entry : posterior.entrySet())
					if (Double.compare(entry.getValue(), this.classificationThreshold) < 0)
						toRemove.add(entry.getKey());
				for (L l : toRemove)
					posterior.remove(l);
			}*/
					
			posteriors.put(datum, posterior);
			
			i++;
		}

		return posteriors;
	}
	
	@Override
	public Map<D, L> classify(DataFeatureMatrix<D, L> data) {
		Map<D, L> classifications = new HashMap<D, L>();
		if (Double.compare(this.classificationThreshold, 0.0) > 0) {
			Map<D, Map<L, Double>> p = posterior(data);
			L defaultLabel = null;
			if (this.defaultLabel != null)
				defaultLabel = this.context.getDatumTools().labelFromString(this.defaultLabel);
			for (Entry<D, Map<L, Double>> entry : p.entrySet()) {
				double max = Double.NEGATIVE_INFINITY;
				L maxLabel = null;
				
				if (defaultLabel != null) {
					if (entry.getValue().containsKey(defaultLabel) 
							&& Double.compare(entry.getValue().get(defaultLabel), this.classificationThreshold) >= 0) {
						maxLabel = defaultLabel;
						max = entry.getValue().get(maxLabel);
					}
				} 
				
				if (maxLabel == null) {
					for (Entry<L, Double> entry2 : entry.getValue().entrySet()) {
						if (!entry2.getKey().equals(defaultLabel) && Double.compare(max, entry2.getValue()) <= 0) {
							max = entry2.getValue();
							maxLabel = entry2.getKey();
						}
					}
				}
				
				if (maxLabel != null)
					classifications.put(entry.getKey(), maxLabel);
			}
		} else {
			GeneralDataset<String,String> rvfData = makeData(data, false);
			int i = 0; 
			for (D datum : data.getData()) {
				Counter<String> scores = this.classifier.scoresOf(rvfData.getDatum(i));
				Counters.logNormalizeInPlace(scores);
				for (String label : scores.keySet()) {
					scores.setCount(label, Math.exp(scores.getCount(label)));
				}
				
				classifications.put(datum, 
					this.context.getDatumTools().labelFromString(Counters.argmax(scores)));
				i++;
			}
		}
		
		return classifications;
	}

	@Override
	public String getGenericName() {
		return "StanfordLinear";
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("searchThresholdFB"))
			return Obj.stringValue(String.valueOf(this.searchThresholdFB));
		else if (parameter.equals("searchThreshold")) 
			return Obj.stringValue(String.valueOf(this.searchThreshold));
		else if (parameter.equals("classificationThreshold"))
			return Obj.stringValue(String.valueOf(this.classificationThreshold));
		else if (parameter.equals("defaultLabel"))
			return (this.defaultLabel != null) ? Obj.stringValue(this.defaultLabel.toString()) : null;
		else if (parameter.equals("minFeatureOccurrence"))
			return Obj.stringValue(String.valueOf(this.minFeatureOccurrence));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("searchThresholdFB"))
			this.searchThresholdFB = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("searchThreshold"))
			this.searchThreshold = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("classificationThreshold"))
			this.classificationThreshold = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("defaultLabel"))
			this.defaultLabel = (parameterValue != null) ? this.context.getMatchValue(parameterValue) : null;
		else if (parameter.equals("minFeatureOccurrence"))
			this.minFeatureOccurrence = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.hyperParameterNames;
	}

	@Override
	public SupervisedModel<D, L> makeInstance(DatumContext<D, L> context) {
		return new SupervisedModelStanfordLinear<D, L>(context);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		if (internalAssignments == null || !internalAssignments.contains("classifier"))
			return true;
		
		try {
			this.classifier = (LinearClassifier<String, String>)StringUtil.deserializeFromBase64String(((Obj.Value)internalAssignments.get("classifier").getValue()).getStr());
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(AssignmentList internalAssignments) {
		if (this.classifier == null) 
			return internalAssignments;
		
		try {
			String classifier = StringUtil.serializeToBase64String(this.classifier);
			internalAssignments.add(
					Assignment.assignmentTyped(null, 
					Context.ObjectType.VALUE.toString(), "classifier", Obj.stringValue(classifier)));
			
			List<Triple<String, String, Double>> fw = this.classifier.getTopFeatures(0.0, true, 200);
			int i = 0;
			for (Triple<String, String, Double> featureWeight : fw) {
				Obj.Array arr = Obj.array(new String[] { featureWeight.first(), featureWeight.second(), String.valueOf(featureWeight.third()) });
				internalAssignments.add(
						Assignment.assignmentTyped(null, 
						Context.ObjectType.ARRAY.toString(), "w_" + i, arr));
				i++;
			}
		} catch (IOException e) {
			return null;
		}
		
		return internalAssignments;
	}
	
	@Override
	protected <T extends Datum<Boolean>> SupervisedModel<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			SupervisedModel<T, Boolean> binaryModel) {
		return binaryModel;
	}

	@Override
	public boolean iterateTraining(DataFeatureMatrix<D, L> data,
			DataFeatureMatrix<D, L> testData,
			List<SupervisedModelEvaluation<D, L>> evaluations,
			Map<D, L> constrainedData) {
		throw new UnsupportedOperationException();
	}
}
