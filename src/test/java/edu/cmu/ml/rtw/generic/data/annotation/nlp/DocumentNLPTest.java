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
	public void testNLPAnnotationAndMicroSerialization() {
		/*testNLPAnnotationAndMicroSerializationDisabledFrom(AnnotationTypeNLP.POS);
		testNLPAnnotationAndMicroSerializationDisabledFrom(AnnotationTypeNLP.CONSTITUENCY_PARSE);
		testNLPAnnotationAndMicroSerializationDisabledFrom(AnnotationTypeNLP.DEPENDENCY_PARSE);*/
		testNLPAnnotationAndMicroSerializationDisabledFrom(AnnotationTypeNLP.NER);
		/*testNLPAnnotationAndMicroSerializationDisabledFrom(AnnotationTypeNLP.COREF);
		testNLPAnnotationAndMicroSerializationDisabledFrom(null);*/
	}
	
	private void testNLPAnnotationAndMicroSerializationDisabledFrom(AnnotationTypeNLP<?> disabledFrom) {
		PipelineNLPStanford stanfordPipe = new PipelineNLPStanford();//7);
		stanfordPipe = new PipelineNLPStanford(stanfordPipe);
		Assert.assertTrue(stanfordPipe.initialize(disabledFrom));
		DataTools dataTools = new DataTools(new OutputWriter());
		
		DocumentNLP document = new DocumentNLPInMemory(dataTools, 
				"theDocument", 
				"",//"He died of cancer in August 2014.", //"I eat. Jim learned to read at school. It was horrible, but he had to do it anyway.",
				Language.English, 
				stanfordPipe);
		
		DocumentAnnotation documentAnnotation = document.toMicroAnnotation();
		List<Annotation> annotations = documentAnnotation.getAllAnnotations();
		
		Assert.assertNotEquals(annotations.size(), 0);
		
		document = new DocumentNLPInMemory(dataTools, documentAnnotation);
		DocumentAnnotation documentAnnotationCopy = document.toMicroAnnotation();
		List<Annotation> annotationsCopy = documentAnnotationCopy.getAllAnnotations();
		
		document = new DocumentNLPInMemory(dataTools, documentAnnotationCopy);
		DocumentAnnotation documentAnnotationCopyCopy = document.toMicroAnnotation();
		List<Annotation> annotationsCopyCopy = documentAnnotationCopyCopy.getAllAnnotations();
		
		Assert.assertEquals(annotations.size(), annotationsCopy.size());
		Assert.assertEquals(annotations.size(), annotationsCopyCopy.size());
		
		for (int i = 0; i < annotations.size(); i++) {
			JSONObject jsonCopy = annotationsCopy.get(i).toJson();
			JSONObject jsonCopyCopy = annotationsCopyCopy.get(i).toJson();
			System.out.println(jsonCopyCopy);
			
			try {
				jsonCopy.put("annotationTime", "");
				jsonCopyCopy.put("annotationTime", "");
			} catch (JSONException e) {}

			Assert.assertEquals(jsonCopy.toString(), jsonCopyCopy.toString());
		}
	}
}
