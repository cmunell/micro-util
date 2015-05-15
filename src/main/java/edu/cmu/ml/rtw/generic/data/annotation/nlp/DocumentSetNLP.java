package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.DocumentSet;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.Annotation;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.DocumentAnnotation;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;

public class DocumentSetNLP<D extends DocumentNLP> extends DocumentSet<D> {
	public DocumentSetNLP(String name) {
		super(name);
	}

	@Override 
	public DocumentSet<D> makeInstance() {
		return new DocumentSetNLP<D>(getName());
	}
	
	public boolean saveToMicroDirectory(String directoryPath, Collection<AnnotationTypeNLP<?>> annotationTypes) {
		for (String name : getDocumentNames()) {
			D document = getDocumentByName(name);
			DocumentAnnotation documentAnnotation = document.toMicroAnnotation(annotationTypes);
			documentAnnotation.writeToFile(new File(directoryPath, document.getName()).getAbsolutePath());
		}
		
		return true;
	}
	
	public boolean saveToMicroFile(String filePath, Collection<AnnotationTypeNLP<?>> annotationTypes) {
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(filePath));
		
			for (String name : getDocumentNames()) {
				D document = getDocumentByName(name);
				DocumentAnnotation documentAnnotation = document.toMicroAnnotation(annotationTypes);
				List<Annotation> annotations = documentAnnotation.getAllAnnotations();
				for (Annotation annotation : annotations) {
					w.write(annotation.toJsonString() + "\n");
				}
			}
			
			w.close();
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}

	public static <D extends DocumentNLP> DocumentSetNLP<D> loadFromMicroPathThroughPipeline(String name, String path, D genericDocument) {
		return loadFromMicroPathThroughPipeline(name, path, genericDocument, null, null);
	}
	
	public static <D extends DocumentNLP> DocumentSetNLP<D> loadFromMicroPathThroughPipeline(String name, String path, D genericDocument, PipelineNLP pipeline) {
		return loadFromMicroPathThroughPipeline(name, path, genericDocument, pipeline, null);
	}
	
	public static <D extends DocumentNLP> DocumentSetNLP<D> loadFromMicroPathThroughPipeline(String name, String path, D genericDocument, PipelineNLP pipeline, Collection<AnnotationTypeNLP<?>> skipAnnotators) {
		File filePath = new File(path);
		File[] files = null;
		if (filePath.isDirectory()) {
			files = filePath.listFiles();
		} else {
			files = new File[] { filePath };
		}
		
		// FIXME: This assumes that documents are not split across files
		// Might want to remove this assumption...
		DocumentSetNLP<D> documentSet = new DocumentSetNLP<D>(name);
		for (File file : files) {
			documentSet.addAll(loadFromMicroFileThroughPipeline(file, genericDocument, pipeline, skipAnnotators));
		}
	
		return documentSet;
	}

	@SuppressWarnings("unchecked")
	private static <D extends DocumentNLP> DocumentSetNLP<D> loadFromMicroFileThroughPipeline(File file, D genericDocument, PipelineNLP pipeline, Collection<AnnotationTypeNLP<?>> skipAnnotators) {
		List<DocumentAnnotation> documentAnnotations = DocumentAnnotation.fromFile(file.getAbsolutePath());
		DocumentSetNLP<D> documentSet = new DocumentSetNLP<D>("");
		for (DocumentAnnotation documentAnnotation : documentAnnotations) {
			documentSet.add((D)genericDocument.makeInstanceFromMicroAnnotation(documentAnnotation, pipeline, skipAnnotators));
		}
	
		return documentSet;
	}
}
