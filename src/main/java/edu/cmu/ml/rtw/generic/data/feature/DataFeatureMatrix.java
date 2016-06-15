package edu.cmu.ml.rtw.generic.data.feature;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.platanios.learn.math.matrix.Vector;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet.DataFilter;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;

public class DataFeatureMatrix<D extends Datum<L>, L> extends CtxParsableFunction implements Iterable<Vector> {
	public class VectorIterator implements Iterator<Vector> {
		private Iterator<D> iterator;
		private boolean cacheVectors;
		
		public VectorIterator(Iterator<D> iterator, boolean cacheVectors) {
			this.iterator = iterator;
			this.cacheVectors = cacheVectors;
		}
		
		@Override
		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		@Override
		public Vector next() {
			return getFeatureVocabularyValues(this.iterator.next(), this.cacheVectors);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	private DatumContext<D, L> context;
	
	private Obj featuresRef;
	private FeatureSet<D, L> features;
	private Obj dataRef;
	private DataSet<D, L> data;
	private String[] parameterNames = { "features", "data" };
	
	private Map<Integer, Vector> featureVocabularyValues; // Map from datum ids to feature vectors
	private boolean precomputed;
	
	public DataFeatureMatrix(DatumContext<D, L> context) {
		this.context = context;
		clear();
	}

	public DataFeatureMatrix(DatumContext<D, L> context, String referenceName, DataSet<D, L> data, FeatureSet<D, L> features) {
		this.context = context;
		this.referenceName = referenceName;
		this.data = data;
		this.features = features;
		clear();
	}
	
	public synchronized boolean init() {
		return (!this.data.isBuildable() || this.data.isBuilt() || this.data.build()) 
				&& (this.features.isInitialized() || this.features.init());
	}
	
	public boolean isInitialized() {
		return (!this.data.isBuildable() || this.data.isBuilt()) && this.features.isInitialized();
	}
	
	private boolean clear() {
		this.featureVocabularyValues = new ConcurrentHashMap<Integer, Vector>();
		return true;
	}
	
	public DatumContext<D, L> getContext() {
		return this.context;
	}
	
	public boolean isPrecomputed() {
		return this.precomputed;
	}
	
	public synchronized boolean precompute() {
		if (this.precomputed)
			return true;
		
		List<Boolean> threadResults = this.data.map(new ThreadMapper.Fn<D, Boolean>() {
			@Override
			public Boolean apply(D datum) {
				Vector featureVector = getFeatureVocabularyValues(datum);
					if (featureVector == null)
						return false;
				
				return true;
			}
		}, this.context.getMaxThreads());
		

		for (boolean result : threadResults)
			if (!result)
				return false;
		
		this.precomputed = true;
		return true;
	}
	
	public FeatureSet<D, L> getFeatures() {
		return this.features;
	}
	
	public DataSet<D, L> getData() {
		return this.data;
	}
	
	public Vector getFeatureVocabularyValues(D datum) {
		return getFeatureVocabularyValues(datum, true);
	}
	
	public Vector getFeatureVocabularyValues(D datum, boolean cacheValues) {
		if (!this.data.contains(datum))
			return null;
		if (this.featureVocabularyValues.containsKey(datum.getId()))
			return this.featureVocabularyValues.get(datum.getId());

		Vector vector = this.features.getFeatureVocabularyValues(datum);
		
		if (cacheValues)
			this.featureVocabularyValues.put(datum.getId(), vector);
		
		return vector;
	}
	
	public <T extends Datum<Boolean>> DataFeatureMatrix<T, Boolean> makeBinary(LabelIndicator<L> labelIndicator, DatumContext<T, Boolean> context) {
		DataFeatureMatrix<T, Boolean> matrix = new DataFeatureMatrix<T, Boolean>(context);
		if (!matrix.fromParse(getModifiers(), getReferenceName(), toParse()))
			return null;
		if (matrix.data == null)
			matrix.data = this.data.makeBinary(labelIndicator, context);
		if (matrix.features == null)
			matrix.features = this.features.makeBinary(labelIndicator, context);
		
		return matrix;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("data"))
			return this.dataRef;
		else if (parameter.equals("features"))
			return this.featuresRef;
		else
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("data")) {
			this.dataRef = parameterValue;
			this.data = parameterValue == null ? null : this.context.getMatchDataSet(parameterValue);
		} else if (parameter.equals("features")) {
			this.featuresRef = parameterValue;
			this.features = parameterValue == null ? null : this.context.getMatchFeatureSet(parameterValue);
		} else
			return false;
		return true;
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		return null;
	}

	@Override
	public String getGenericName() {
		return "DataFeatureMatrix";
	}
	
	@Override
	public Iterator<Vector> iterator() {
		return iterator(false);
	}

	public Iterator<Vector> iterator(boolean cacheVectors) {
		return iterator(cacheVectors, DataFilter.All);
	}
	
	public Iterator<Vector> iterator(boolean cacheVectors, DataFilter filter) {
		return new VectorIterator(this.data.iterator(filter), cacheVectors);
	}
}
