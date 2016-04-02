package edu.cmu.ml.rtw.generic.model.annotator.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.lth.cs.srl.corpus.Sentence;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Predicate;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.matetools.MateTools;
import edu.cmu.ml.rtw.generic.util.Properties;
import edu.cmu.ml.rtw.generic.util.Triple;

public class PipelineNLPMateTools extends PipelineNLP {
	private MateTools mateTools;
	private Properties properties;
	
	public PipelineNLPMateTools(Properties properties) {
		super();
		this.properties = properties;
		
		addAnnotator(AnnotationTypeNLP.PREDICATE,  new AnnotatorTokenSpan<Predicate>() {
			public String getName() { return "mate_tools_4.31"; }
			public AnnotationType<Predicate> produces() { return AnnotationTypeNLP.PREDICATE; };
			public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { AnnotationTypeNLP.SENTENCE }; }
			public boolean measuresConfidence() { return false; }
			public List<Triple<TokenSpan, Predicate, Double>> annotate(DocumentNLP document) {
				List<Triple<TokenSpan, Predicate, Double>> srlAnnotations = new ArrayList<Triple<TokenSpan, Predicate, Double>>();
				MateTools tools = PipelineNLPMateTools.this.mateTools;
				try {
					for (int i = 0; i < document.getSentenceCount(); i++) {
						Sentence sentence = null;
						synchronized (tools) {
							sentence = tools.parseSentence(document.getSentence(i));
						}
						
						List<se.lth.cs.srl.corpus.Predicate> predicates = sentence.getPredicates();
						for (se.lth.cs.srl.corpus.Predicate predicate : predicates) {
							String sense = predicate.getSense();
							// FIXME
							
						}
					}
				} catch (Exception e) {
					return null;
				}
				
				return srlAnnotations;
			}
		});
	}
	
	public boolean initialize() {
		this.mateTools = new MateTools(this.properties);
		if (!this.mateTools.init())
			return false;
			
		return true;
	}
	
	public DocumentNLPMutable run(DocumentNLPMutable document, Collection<AnnotationType<?>> skipAnnotators) {
		if (this.mateTools == null)
			if (!initialize())
				return null;
		
		return super.run(document, skipAnnotators);
	}
}
