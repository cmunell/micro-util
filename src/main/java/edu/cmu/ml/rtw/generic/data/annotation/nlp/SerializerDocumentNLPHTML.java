package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.SerializerDocument;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.Triple;

public class SerializerDocumentNLPHTML extends SerializerDocument<DocumentNLPMutable, String> {		
	public SerializerDocumentNLPHTML() {
		this(new DocumentNLPInMemory(new DataTools()), null);
	}
	
	public SerializerDocumentNLPHTML(DataTools dataTools) {
		this(new DocumentNLPInMemory(dataTools), null);
	}
	
	public SerializerDocumentNLPHTML(DocumentNLPMutable genericDocument) {
		this(genericDocument, null);
	}
	
	public SerializerDocumentNLPHTML(DocumentNLPMutable genericDocument, Collection<AnnotationType<?>> annotationTypes) {
		super(genericDocument, annotationTypes);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public String serialize(DocumentNLPMutable document) {
		StringBuilder htmlBuilder = new StringBuilder();
		beginHTML(htmlBuilder);
		if (!document.hasAnnotationType(AnnotationTypeNLP.ORIGINAL_TEXT)) {
			throw new RuntimeException("You really need to have the original text to output html...");
		}
		
                htmlBuilder.append("<div style=\"position:absolute; top:0; bottom:0; overflow:hidden; width:99%\">");
                htmlBuilder.append("<div class=\"document\" style=\"width:69%; overflow-y:scroll; margin-left:31%; float:right; height:100%; position: absolute\">");
                htmlBuilder.append("<h3>Annotations</h3>");
                htmlBuilder.append("<ul>");

		Collection<AnnotationType<?>> docAnnotationTypes = document.getAnnotationTypes();
		
		Collection<AnnotationType<?>> annotationTypes = null;
		if (this.annotationTypes == null) {
			annotationTypes = docAnnotationTypes;
		} else {
			annotationTypes = this.annotationTypes;
		}
		
		for (AnnotationType<?> annotationType : annotationTypes) {
			AnnotationTypeNLP<?> annotationTypeNLP = (AnnotationTypeNLP<?>)annotationType;
			if (annotationTypeNLP.getTarget() == Target.DOCUMENT && !annotationTypeNLP.equals(AnnotationTypeNLP.ORIGINAL_TEXT)) {
				Object annotation = document.getDocumentAnnotation(annotationTypeNLP);	
				int sentenceStart = 0;
				int sentenceEnd = document.getSentenceCount() == 0 ? 1 : document.getToken(document.getSentenceCount() - 1, 
						document.getSentenceTokenCount(document.getSentenceCount() - 1) - 1).getCharSpanEnd();
				
				htmlBuilder.append("<li><div class=\"annotation\"");
				htmlBuilder.append(" spanStart=\"" + sentenceStart + "\"");
				htmlBuilder.append(" spanEnd=\"" + sentenceEnd + "\"");
				htmlBuilder.append(">\n");
				htmlBuilder.append("Type: " + annotationTypeNLP.toString());
				if (document.hasConfidence(annotationTypeNLP))
					htmlBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Score: " + String.format("%.04f", document.getDocumentAnnotationConfidence(annotationTypeNLP)));
				htmlBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Value: " + annotationTypeNLP.toHTML(annotation));
				htmlBuilder.append("</div></li>\n");	

			}		
		}

		if (!document.hasAnnotationType(AnnotationTypeNLP.TOKEN)) {
			endHTML(htmlBuilder);
			return htmlBuilder.toString();
		}

                // Being a human-friendly output format, we'll ensure that the sub-document
                // annotations are grouped and sorted.
                //
                // Sentence and token annotations are already ordered correctly by the way we loop.
                //
                // For free-form annotations, we enforce ordering by increasing start token and
                // subordering by end token.  Then within that, decreasing score for each set of
                // matching spans.

		for (AnnotationType<?> annotationType : annotationTypes) {
			AnnotationTypeNLP<?> annotationTypeNLP = (AnnotationTypeNLP<?>)annotationType;
			if (annotationTypeNLP.getTarget() == Target.SENTENCE) {
				for (int i = 0; i < document.getSentenceCount(); i++) {
					Object annotation = document.getSentenceAnnotation(annotationTypeNLP, i);
					if (annotation != null) {
						int sentenceStart = document.getToken(i, 0).getCharSpanStart();
						int sentenceEnd = document.getToken(i, document.getSentenceTokenCount(i) - 1).getCharSpanEnd();
						
						htmlBuilder.append("<li><div class=\"annotation\"");
						htmlBuilder.append(" spanStart=\"" + sentenceStart + "\"");
						htmlBuilder.append(" spanEnd=\"" + sentenceEnd + "\"");
						htmlBuilder.append(">\n");
						htmlBuilder.append("Type: " + annotationTypeNLP.toString());
						if (document.hasConfidence(annotationTypeNLP))
							htmlBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Score: " + String.format("%.04f", document.getSentenceAnnotationConfidence(annotationTypeNLP, i)));
						htmlBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Value: " + annotationTypeNLP.toHTML(annotation));
						htmlBuilder.append("</div></li>\n");	
					}
				}
			} else if (annotationTypeNLP.getTarget() == Target.TOKEN_SPAN) {
                                // Here, we accumulate the annotations in a TreeMap.  Each TreeMap entry
                                // consists of a key constructed such that iterating over them results
                                // in correct ordering of the annotations, and a value that is the HTML
                                // string for that annotation.
                                TreeMap<String, String> treeMap = new TreeMap<String, String>();

				List<?> annotationObjs = document.getTokenSpanAnnotationConfidences(annotationTypeNLP);
				for (Object annotationObj : annotationObjs) {
					Triple<TokenSpan, ?, Double> annotation = (Triple<TokenSpan, ?, Double>)annotationObj;
					
					int sentenceIndex = annotation.getFirst().getSentenceIndex();
					int spanStart = document.getToken(sentenceIndex, annotation.getFirst().getStartTokenIndex()).getCharSpanStart();
					int spanEnd = document.getToken(sentenceIndex, annotation.getFirst().getEndTokenIndex()-1).getCharSpanEnd(); 
			
                                        StringBuilder htmlBuilder2 = new StringBuilder();
					htmlBuilder2.append("<li><div class=\"annotation\"");
					htmlBuilder2.append(" spanStart=\"" + spanStart + "\"");
					htmlBuilder2.append(" spanEnd=\"" + spanEnd + "\"");
					htmlBuilder2.append(">\n");
					htmlBuilder2.append("Type: " + annotationTypeNLP.toString());
					if (document.hasConfidence(annotationTypeNLP))
						htmlBuilder2.append("&nbsp;&nbsp;&nbsp;&nbsp;Score: " + String.format("%.04f", annotation.getThird()));
					htmlBuilder2.append("&nbsp;&nbsp;&nbsp;&nbsp;Value: " + annotationTypeNLP.toHTML(annotation.getSecond()));
					htmlBuilder2.append("</div></li>\n");

                                        String key = String.format("%04d\t%06d\t%06d", sentenceIndex, spanStart, spanEnd);
                                        if (document.hasConfidence(annotationTypeNLP)) key = String.format("%s\t%.08f", key, (1.0 - annotation.getThird()));
                                        treeMap.put(key, htmlBuilder2.toString());
				}

                                // Now we can iterate through our TreeMap and spit out the annotations
                                if (treeMap.size() > 0) {
                                    for (String key = treeMap.firstKey(); key != null; key = treeMap.higherKey(key)) {
                                        htmlBuilder.append(treeMap.get(key));
                                    }
                                }
			
			} else if (annotationTypeNLP.getTarget() == Target.TOKEN) {
				for (int i = 0; i < document.getSentenceCount(); i++) {
					for (int j = 0; j < document.getSentenceTokenCount(i); j++) {
						int spanStart = document.getToken(i, j).getCharSpanStart();
						int spanEnd = document.getToken(i, j).getCharSpanEnd();
						
						htmlBuilder.append("<li><div class=\"annotation\"");
						htmlBuilder.append(" spanStart=\"" + spanStart + "\"");
						htmlBuilder.append(" spanEnd=\"" + spanEnd + "\"");
						htmlBuilder.append(">\n");
						htmlBuilder.append("Type: " + annotationTypeNLP.toString());
						if (document.hasConfidence(annotationTypeNLP))
							htmlBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Score: " + String.format("%.04f", document.getTokenAnnotationConfidence(annotationTypeNLP, i, j)));
						htmlBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Value: " + annotationTypeNLP.toHTML(document.getTokenAnnotation(annotationTypeNLP, i, j)));
						htmlBuilder.append("</div></li>\n");
					}
				}
			}
		}

		htmlBuilder.append("</ul></div>\n");

                htmlBuilder.append("<div class=\"document\" style=\"width:29%; float:left; height:100%;\">"); 
                htmlBuilder.append("<div class=\"textLabel\"><h3>Document Text</h3></div>");
                htmlBuilder.append("<div class=\"text\">");
		htmlBuilder.append(AnnotationTypeNLP.ORIGINAL_TEXT.toHTML(document.getOriginalText()));
		htmlBuilder.append("</div>");
		htmlBuilder.append("</div>");
		htmlBuilder.append("</div>");

		endHTML(htmlBuilder);

		return htmlBuilder.toString();
	}

	@Override
	public DocumentNLPMutable deserialize(String object, StoreReference storeReference) {
		throw new UnsupportedOperationException();
	}
	
	private void beginHTML(StringBuilder htmlBuilder) {
		htmlBuilder.append("<html>");
		htmlBuilder.append("<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js\"></script>");
		htmlBuilder.append("<body>");
	}

	private void endHTML(StringBuilder htmlBuilder) {
		// Add some javascript at the end of the body.  Pretty ugly to be developing javascript here,
		// but it's either this or making an html page that isn't stand-alone...
		htmlBuilder.append("<script type=\"text/javascript\">\n");
		htmlBuilder.append("//# sourceURL=annotations.js\n");
		htmlBuilder.append("var text = $(\".text\").html()\n");
		htmlBuilder.append("$(document).ready(function() {\n");
		htmlBuilder.append("  $(\".annotation\").hover(function() {\n");
		htmlBuilder.append("    var spanStart = $(this).attr(\"spanStart\");\n");
		htmlBuilder.append("    var spanEnd = $(this).attr(\"spanEnd\");\n");
		htmlBuilder.append("    highlightSpan([spanStart, spanEnd]);\n");
		htmlBuilder.append("  },\n");
		htmlBuilder.append("  removeHighlight);\n");
		htmlBuilder.append("});\n");
		htmlBuilder.append("function highlightSpan(span) {\n");
		htmlBuilder.append("  var before = text.slice(0, span[0]);\n");
		htmlBuilder.append("  var after = text.slice(span[1]);\n");
		htmlBuilder.append("  var to_highlight = text.slice(span[0], span[1]);\n");
		htmlBuilder.append("  var highlighted = before + '<span class=\"highlight\">' + to_highlight + '</span>' + after;\n");
		htmlBuilder.append("  $(\".text\").html(highlighted);\n");
		htmlBuilder.append("}\n");
		htmlBuilder.append("function removeHighlight() {\n");
		htmlBuilder.append("  $(\".text\").html(text);\n");
		htmlBuilder.append("}\n");
		htmlBuilder.append("</script>\n");
		// And some simple CSS so that the highlight shows up.
		htmlBuilder.append("<style>.highlight { background-color: red }</style>\n");
		// And then close the html document.
		htmlBuilder.append("</body></html>");
	}

	@Override
	public String getName() {
		return "DocumentNLPHTML";
	}

	@Override
	public String serializeToString(DocumentNLPMutable item) {
		return serialize(item);
	}

	@Override
	public DocumentNLPMutable deserializeFromString(String str, StoreReference storeReference) {
		return deserialize(str, storeReference);
	}

	@Override
	public SerializerDocument<DocumentNLPMutable, String> makeInstance(
			DocumentNLPMutable genericDocument,
			Collection<AnnotationType<?>> annotationTypes) {
		return new SerializerDocumentNLPHTML(genericDocument, annotationTypes);
	}
}
