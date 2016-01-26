package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;


import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.Annotation;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.DocumentAnnotation;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.util.OutputWriter;

public class DocumentNLPTest {	
	@Test
	public void testNLPAnnotationAndSerialization() {
		/*testNLPAnnotationAndSerializationDisabledFrom(AnnotationTypeNLP.POS);
		testNLPAnnotationAndSerializationDisabledFrom(AnnotationTypeNLP.CONSTITUENCY_PARSE);
		testNLPAnnotationAndSerializationDisabledFrom(AnnotationTypeNLP.DEPENDENCY_PARSE);*/
		testNLPAnnotationAndSerializationDisabledFrom(AnnotationTypeNLP.NER, "");
		testNLPAnnotationAndSerializationDisabledFrom(AnnotationTypeNLP.NER, "He died of cancer in August 2014.");
		testNLPAnnotationAndSerializationDisabledFrom(AnnotationTypeNLP.NER, "I eat. Jim learned to read at school. It was horrible, but he had to do it anyway.");
		/*testNLPAnnotationAndSerializationDisabledFrom(AnnotationTypeNLP.COREF);
		testNLPAnnotationAndSerializationDisabledFrom(null);*/
	}
	
	private void testNLPAnnotationAndSerializationDisabledFrom(AnnotationTypeNLP<?> disabledFrom, String text) {
		PipelineNLPStanford stanfordPipe = new PipelineNLPStanford();//7);
		stanfordPipe = new PipelineNLPStanford(stanfordPipe);
		Assert.assertTrue(stanfordPipe.initialize(disabledFrom));
		DataTools dataTools = new DataTools(new OutputWriter());
		
		DocumentNLPMutable document = new DocumentNLPInMemory(dataTools, 
				"theDocument", 
				text);
		
		stanfordPipe.run(document);
		
		SerializerDocumentNLPMicro microSerial = new SerializerDocumentNLPMicro(document);
		SerializerDocumentNLPJSONLegacy jsonSerial = new SerializerDocumentNLPJSONLegacy(document);
		SerializerDocumentNLPBSON bsonSerial = new SerializerDocumentNLPBSON(document);
		SerializerDocumentNLPHTML htmlSerial = new SerializerDocumentNLPHTML(document);
		
		DocumentAnnotation micro = microSerial.serialize(document);
		List<Annotation> microAnno = micro.getAllAnnotations();
		
		DocumentAnnotation micro2 = 
				microSerial.serialize(
					bsonSerial.deserialize(bsonSerial.serialize(
						jsonSerial.deserialize(jsonSerial.serialize(
								microSerial.deserialize(micro))))));
		List<Annotation> microAnno2 = micro2.getAllAnnotations();
		
		Assert.assertNotEquals(microAnno.size(), 0);
		Assert.assertEquals(microAnno.size(), microAnno2.size());
		
		for (int i = 0; i < microAnno.size(); i++) {
			JSONObject json = microAnno.get(i).toJson();
			JSONObject json2 = microAnno2.get(i).toJson();
			
			
			try {
				json.put("annotationTime", "");
				json2.put("annotationTime", "");
			} catch (JSONException e) {}

			Assert.assertEquals(json.toString(), json2.toString());
		}
		
		System.out.println(htmlSerial.serializeToString(microSerial.deserialize(micro2)));
	}
}
