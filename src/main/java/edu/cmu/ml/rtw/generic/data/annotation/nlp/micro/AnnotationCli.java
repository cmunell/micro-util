package edu.cmu.ml.rtw.generic.data.annotation.nlp.micro;

import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class AnnotationCli {
  
  public static void main(String[] args) {
    OptionParser parser = new OptionParser();
    OptionSpec<String> annotationFilename = parser.accepts("annotationFilename")
        .withRequiredArg().ofType(String.class).required();
    OptionSpec<String> getSlot = parser.accepts("getSlot").withRequiredArg().ofType(String.class);
    OptionSet options = parser.parse(args);
    
    List<DocumentAnnotation> docAnnotations = DocumentAnnotation.fromFile(options.valueOf(annotationFilename));

    System.out.println("number of document annotations: " + docAnnotations.size());
    
    if (options.has(getSlot)) {
      for (DocumentAnnotation docAnnotation : docAnnotations) {
        List<Annotation> annotations = docAnnotation.getAnnotationsWithSlot(options.valueOf(getSlot));
        for (Annotation annotation : annotations) {
          System.out.println(annotation.getSpanStart() + " " + annotation.getSpanEnd() + " " + annotation.getValue());
        }
      }
    }
  }
}
