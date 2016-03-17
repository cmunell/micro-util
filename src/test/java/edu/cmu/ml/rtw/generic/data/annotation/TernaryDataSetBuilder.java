package edu.cmu.ml.rtw.generic.data.annotation;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.StoredItemSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.DataSetBuilder;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPBSON;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class TernaryDataSetBuilder extends DataSetBuilder<TestDatum<TernaryLabel>, TernaryLabel> {
	private String storage;
	private String collection;
	private int datumId;
	
	public TernaryDataSetBuilder() {
		this(null);
	}
	
	public TernaryDataSetBuilder(DatumContext<TestDatum<TernaryLabel>, TernaryLabel> context) {
		this.context = context;
		this.datumId = 2;
	}
	
	@Override
	public String[] getParameterNames() {
		return new String[] { "storage", "collection" };
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("storage"))
			return Obj.stringValue(this.storage);
		else if (parameter.equals("collection"))
			return Obj.stringValue(this.collection);
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
	public DataSetBuilder<TestDatum<TernaryLabel>, TernaryLabel> makeInstance(
			DatumContext<TestDatum<TernaryLabel>, TernaryLabel> context) {
		return new TernaryDataSetBuilder(context);
	}

	@Override
	public DataSet<TestDatum<TernaryLabel>, TernaryLabel> build() {
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> storedDocuments = getDocuments();
		DataSet<TestDatum<TernaryLabel>, TernaryLabel> data = new DataSet<TestDatum<TernaryLabel>, TernaryLabel>(this.context.getDatumTools());
		for (DocumentNLP document : storedDocuments) {
			int id = Integer.valueOf(document.getName());
			TernaryLabel label = null;
			if (id % 3 == 0)
				label = TernaryLabel.FIRST;
			else if (id % 2 == 0)
				label = TernaryLabel.SECOND;
			else
				label = TernaryLabel.THIRD;
			
			data.add(new TestDatum<TernaryLabel>(id, new TokenSpan(document, 0, 0, 0), label));
		}
		
		return data;
	}
	
	private DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> getDocuments() {
		DataTools dataTools = this.context.getDataTools();

		SerializerDocumentNLPBSON serializer = new SerializerDocumentNLPBSON(new DocumentNLPInMemory(dataTools));
		StoredItemSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> items = dataTools.getStoredItemSetManager()
			.getItemSet(this.storage, this.collection, true, serializer);
		PipelineNLPStanford pipe = new PipelineNLPStanford();
		pipe.initialize(AnnotationTypeNLP.POS);
		for (int i = 2; i < 32; i++) {
			DocumentNLPMutable document = new DocumentNLPInMemory(dataTools, String.valueOf(this.datumId), "I ate " + ((this.datumId % 2) + 1) + " apples.");
			document = pipe.run(document);
			items.addItem(document);
			this.datumId++;
		}
		
		return new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(items);
	}

	@Override
	protected boolean fromParseInternal(
			AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		return null;
	}

	@Override
	public String getGenericName() {
		return "Ternary";
	}
}
