package edu.cmu.ml.rtw.generic.data.annotation;

import edu.cmu.ml.rtw.generic.data.StoredItemSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.DataSetBuilder;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;

public class DataSetBuilderStored<D extends Datum<L>, L> extends DataSetBuilder<D, L> {
	private String storage;
	private String collection;
	private String[] parameterNames = { "storage", "collection" };
	
	public DataSetBuilderStored() {
		
	}
	
	public DataSetBuilderStored(DatumContext<D, L> context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("storage"))
			return Obj.stringValue(this.storage);
		else if (parameter.equals("collection"))
			return Obj.stringValue(this.collection);
		else
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("storage"))
			this.storage = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("collection"))
			this.collection = this.context.getMatchValue(parameterValue);
		else
			return false;
		return true;
	}

	@Override
	public DataSetBuilder<D, L> makeInstance(DatumContext<D, L> context) {
		return new DataSetBuilderStored<D, L>(context);
	}

	@Override
	public DataSet<D, L> build() {
		StoredItemSetInMemoryLazy<D, ?> storedData = this.context.getDataTools().getStoredItemSetManager()
				.getItemSet(this.storage, this.collection, false, new SerializerDatumBSON<D, L>(this.context.getDatumTools()));
		
		DataSet<D, L> data = new DataSet<D, L>(this.context.getDatumTools());
		
		storedData.map(new Fn<D, Boolean>() {
			@Override
			public Boolean apply(D datum) {
				synchronized (data) {
					data.add(datum);
				}
	
				return true;
			}
			
		}, this.context.getMaxThreads(), this.context.getDataTools().getGlobalRandom());
		
		return data;
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
		return "Stored";
	}

}
