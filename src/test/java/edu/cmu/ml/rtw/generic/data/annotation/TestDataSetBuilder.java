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

public class TestDataSetBuilder extends DataSetBuilder<TestDatum<Boolean>, Boolean> {
	private String storage;
	private String collection;
	private int datumId;
	
	public TestDataSetBuilder() {
		this(null);
	}
	
	public TestDataSetBuilder(DatumContext<TestDatum<Boolean>, Boolean> context) {
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
	public DataSetBuilder<TestDatum<Boolean>, Boolean> makeInstance(
			DatumContext<TestDatum<Boolean>, Boolean> context) {
		return new TestDataSetBuilder(context);
	}

	@Override
	public DataSet<TestDatum<Boolean>, Boolean> build() {
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> storedDocuments = getDocuments();
		DataSet<TestDatum<Boolean>, Boolean> data = new DataSet<TestDatum<Boolean>, Boolean>(this.context.getDatumTools());
		for (DocumentNLP document : storedDocuments) {
			int id = Integer.valueOf(document.getName());
			data.add(new TestDatum<Boolean>(id, new TokenSpan(document, 0, 0, 0), id % 2 == 0));
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
		for (int i = 2; i < 100; i++) {
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
		return "Test";
	}
}
