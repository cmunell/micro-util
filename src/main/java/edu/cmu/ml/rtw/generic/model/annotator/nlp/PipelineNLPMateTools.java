package edu.cmu.ml.rtw.generic.model.annotator.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.corpus.Yield;
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
						
						int maxTokenIndex = document.getSentenceTokenCount(i) - 1;
						if (sentence.getPOSArray().length - 1 != document.getSentenceTokenCount(i)) {
							synchronized (tools) {
								System.out.println("WARNING: Matetools gives different sentence length for " + document.getName() + " sentence " + 
													i + " (" + sentence.getPOSArray().length + " " + document.getSentenceTokenCount(i) + 
													").  Annotations may be misaligned.");
								System.out.println("Matetools tokenization: ");
								for (int j = 0; j < sentence.getPOSArray().length; j++)
									System.out.println(sentence.getFormArray()[j]);
							}
						}
						
						List<se.lth.cs.srl.corpus.Predicate> predicates = sentence.getPredicates();
						for (se.lth.cs.srl.corpus.Predicate predicate : predicates) {
							Map<Word, String> mateArguments = predicate.getArgMap();
							String sense = predicate.getSense();
							int predicateStartIndex = Math.min(predicate.getIdx() - 1, maxTokenIndex);
							int predicateEndIndex = Math.min(predicate.getIdx(), maxTokenIndex + 1);
							TokenSpan span = new TokenSpan(document, i, predicateStartIndex, predicateEndIndex);
							Map<String, Integer> argTagCounts = new TreeMap<String, Integer>();
							Map<String, TokenSpan[]> arguments = new TreeMap<String, TokenSpan[]>();

							for (Entry<Word, String> entry : mateArguments.entrySet()) {
								if (!argTagCounts.containsKey(entry.getValue()))
									argTagCounts.put(entry.getValue(), 0);
								argTagCounts.put(entry.getValue(), argTagCounts.get(entry.getValue()) + 1);
							}
							
							for (Entry<Word, String> entry : mateArguments.entrySet()) {
								String tag = entry.getValue();
								if (!arguments.containsKey(tag)) {
									arguments.put(tag, new TokenSpan[argTagCounts.get(tag)]);
									argTagCounts.put(tag, 0);
								}
							
								Word word = entry.getKey();
								Yield yield = word.getYield(predicate, tag, mateArguments.keySet());
								int startIndex = Math.min(yield.first().getIdx() - 1, maxTokenIndex);
								int endIndex = Math.min(yield.last().getIdx(), maxTokenIndex + 1);
								
								arguments.get(tag)[argTagCounts.get(tag)] = new TokenSpan(document, i, startIndex, endIndex);
								argTagCounts.put(tag, argTagCounts.get(tag) + 1);		
							}

							srlAnnotations.add(new Triple<TokenSpan, Predicate, Double>(
									span,
									new Predicate(sense, span, arguments),
									null
									));		
						}
					}
				} catch (Exception e) {
					return null;
				}
				
				return srlAnnotations;
			}
		});
	}
	
	public synchronized boolean initialize() {
		if (this.mateTools != null)
			return true;
		this.mateTools = new MateTools(this.properties);		
		return true;
	}
	
	public DocumentNLPMutable run(DocumentNLPMutable document, Collection<AnnotationType<?>> skipAnnotators) {
		if (!initialize())
			return null;
		
		return super.run(document, skipAnnotators);
	}
}
