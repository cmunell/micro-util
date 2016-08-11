package edu.cmu.ml.rtw.generic.data.feature.meta;

import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.parse.Assignment.AssignmentTyped;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.meta.PredictionClassificationDatum;

public class FeatureMetaClassificationIdentity<L> extends Feature<PredictionClassificationDatum<L>, L> {
	private Feature<?, L> feature;
	private String[] parameterNames = { "feature" };
	private Obj featureObj;
	
	public FeatureMetaClassificationIdentity() {
		
	}
	
	public FeatureMetaClassificationIdentity(DatumContext<PredictionClassificationDatum<L>, L> context) {
		this.context = context;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean init(DataSet<PredictionClassificationDatum<L>, L> dataSet) {
		DataSet internalData = new DataSet(this.feature.getContext().getDatumTools());
		
		for (PredictionClassificationDatum<L> datum : dataSet) {
			internalData.add(datum.getPrediction().getDatum());
		}
		
		this.feature = this.context.getMatchFeature(this.featureObj).clone(false);
		
		if (!this.feature.init(internalData))
			return false;
		
		return true;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Map<Integer, Double> computeVector(PredictionClassificationDatum<L> datum, int offset, Map<Integer, Double> vector) {
		return ((Feature)this.feature).computeVector((Datum)datum.getPrediction().getDatum(), offset, vector);
	}

	@Override
	public String getGenericName() {
		return "MetaClassificationIdentity";
	}

	@Override
	public int getVocabularySize() {
		return this.feature.getVocabularySize();
	}

	@Override
	public String getVocabularyTerm(int index) {
		return this.feature.getVocabularyTerm(index);
	}

	@Override
	protected boolean setVocabularyTerm(int index, String term) {
		return true;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("feature")) {
			return this.featureObj;	
		}

		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("feature")) {
			this.featureObj = parameterValue;
		} else {
			return false;
		}
		return true;
	}

	@Override
	public Feature<PredictionClassificationDatum<L>, L> makeInstance(DatumContext<PredictionClassificationDatum<L>, L> context) {
		return new FeatureMetaClassificationIdentity<L>(context);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureMetaClassificationIdentity<Boolean> binaryFeatureMeta = (FeatureMetaClassificationIdentity<Boolean>)binaryFeature;

		if (this.feature != null) {
			binaryFeatureMeta.feature = this.feature.makeBinary(context, labelIndicator);
		}
		
		return (Feature<T, Boolean>)binaryFeatureMeta;
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {	
		for (int i = 0; i < internalAssignments.size(); i++) {
			AssignmentTyped assignment = (AssignmentTyped)internalAssignments.get(i);
			if (!assignment.getType().equals(DatumContext.ObjectType.FEATURE.name()))
				continue;
			
			Obj.Function featureObj = (Obj.Function)assignment.getValue();
			Obj.Function featureObjNoInternal = Obj.function(featureObj.getName(), featureObj.getParameters());
			this.feature = this.context.getMatchFeature(featureObjNoInternal);
			if (!feature.fromParse(assignment.getModifiers(), this.referenceName, featureObj));
				return false;
		}
		
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(AssignmentList internalAssignments) {
		if (this.feature == null)
			return internalAssignments;
		
		internalAssignments.add(
			AssignmentTyped.assignmentTyped(null, DatumContext.ObjectType.FEATURE.name(), this.feature.getReferenceName(), feature.toParse(true))
		);
		
		return internalAssignments;
	}

	@Override
	protected boolean cloneHelper(Feature<PredictionClassificationDatum<L>, L> clone) {
		FeatureMetaClassificationIdentity<L> cloneMeta = (FeatureMetaClassificationIdentity<L>)clone;
		if (this.feature != null)
			cloneMeta.feature = this.feature.clone(true);
		return true;
	}
}
