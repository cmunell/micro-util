package edu.cmu.ml.rtw.generic.model.annotator.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.ConstituencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Token;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpanCluster;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.ConstituencyParse.Constituent;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse.Dependency;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse.Node;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;

public class PipelineNLPStanford extends PipelineNLP {
	private StanfordCoreNLP nlpPipeline;
	private Annotation annotatedText;
	private int maxSentenceLength;
	
	private int validSentenceCount;
	private int[] originalToValidSentenceIndices;
	
	public PipelineNLPStanford() {
		super();
		this.maxSentenceLength = 0;
	}
	
	public PipelineNLPStanford(int maxSentenceLength) {
		super();
		this.maxSentenceLength = maxSentenceLength;
	}
	
	public PipelineNLPStanford(PipelineNLPStanford pipeline) {
		super();
		
		this.document = pipeline.document;
		this.nlpPipeline = pipeline.nlpPipeline;
		this.annotatedText = pipeline.annotatedText;
		this.maxSentenceLength = pipeline.maxSentenceLength;
		this.validSentenceCount = pipeline.validSentenceCount;
		this.originalToValidSentenceIndices = pipeline.originalToValidSentenceIndices;
		
		for (AnnotationType<?> annotationType : pipeline.annotationOrder)
			addAnnotator(annotationType);
	}
	
	public boolean initialize() {
		return initialize(null, null);
	}
	
	public boolean initialize(AnnotationTypeNLP<?> disableFrom) {
		return initialize(disableFrom, null);
	}
	
	public boolean initialize(AnnotationTypeNLP<?> disableFrom, Annotator tokenizer) {
		Properties props = new Properties();
		
		if (tokenizer != null) {
			if (!tokenizer.requirementsSatisfied().containsAll(Annotator.TOKENIZE_AND_SSPLIT) || tokenizer.requirementsSatisfied().size() != 2)
				return false;
			
			String tokenizerClass = tokenizer.getClass().getName();
		    props.put("customAnnotatorClass.tokenize", tokenizerClass);
		    props.put("customAnnotatorClass.ssplit", tokenizerClass);
		}
		
		String propsStr = "";
		if (disableFrom == null) {
			propsStr = "tokenize, ssplit, pos, lemma, parse, ner, dcoref";
		} else if (disableFrom.equals(AnnotationTypeNLP.TOKEN)) {
			throw new IllegalArgumentException("Can't disable tokenization");
		} else if (disableFrom.equals(AnnotationTypeNLP.POS)) {
			propsStr = "tokenize, ssplit";
		} else if (disableFrom.equals(AnnotationTypeNLP.LEMMA)) {
			propsStr = "tokenize, ssplit, pos";
		} else if (disableFrom.equals(AnnotationTypeNLP.CONSTITUENCY_PARSE)) {
			propsStr = "tokenize, ssplit, pos, lemma";
		} else if (disableFrom.equals(AnnotationTypeNLP.DEPENDENCY_PARSE)) {
			propsStr = "tokenize, ssplit, pos, lemma, parse";
		} else if (disableFrom.equals(AnnotationTypeNLP.NER)) {
			propsStr = "tokenize, ssplit, pos, lemma, parse";
		} else if (disableFrom.equals(AnnotationTypeNLP.COREF)) {
			propsStr = "tokenize, ssplit, pos, lemma, parse, ner";
		}

		props.put("pos.maxlen", String.valueOf(this.maxSentenceLength));
		props.put("parse.maxlen", String.valueOf(this.maxSentenceLength));
		
		props.put("annotators", propsStr);
		this.nlpPipeline = new StanfordCoreNLP(props);
		
		clearAnnotators();
		
		if (!addAnnotator(AnnotationTypeNLP.TOKEN))
			return false;
		
		if (disableFrom != null && disableFrom.equals(AnnotationTypeNLP.POS))
			return true;
		
		if (!addAnnotator(AnnotationTypeNLP.POS))
			return false;
		
		if (disableFrom != null && disableFrom.equals(AnnotationTypeNLP.LEMMA))
			return true;

		if (!addAnnotator(AnnotationTypeNLP.LEMMA))
			return false;
		
		if (disableFrom != null && disableFrom.equals(AnnotationTypeNLP.CONSTITUENCY_PARSE))
			return true;
		
		if (!addAnnotator(AnnotationTypeNLP.CONSTITUENCY_PARSE))
			return false;
		
		if (disableFrom != null && disableFrom.equals(AnnotationTypeNLP.DEPENDENCY_PARSE))
			return true;
		
		if (!addAnnotator(AnnotationTypeNLP.DEPENDENCY_PARSE))
			return false;
	
		if (disableFrom != null && disableFrom.equals(AnnotationTypeNLP.NER))
			return true;
		
		if (!addAnnotator(AnnotationTypeNLP.NER))
			return false;
		
		if (disableFrom != null && disableFrom.equals(AnnotationTypeNLP.COREF))
			return true;
		
		if (!addAnnotator(AnnotationTypeNLP.COREF))
			return false;
		
		return true;
	}
	
	private int getValidSentenceCount() {
		return this.validSentenceCount;
	}
	
	private int getValidSentenceIndex(int sentenceIndex) {
		if (this.originalToValidSentenceIndices == null)
			return sentenceIndex;
		else
			return this.originalToValidSentenceIndices[sentenceIndex];
	}
	
	
	private boolean addAnnotator(AnnotationType<?> annotationType) {
		if (annotationType.equals(AnnotationTypeNLP.TOKEN)) {
			addAnnotator(AnnotationTypeNLP.TOKEN,  new AnnotatorToken<Token>() {
				public String getName() { return "stanford"; }
				public AnnotationType<Token> produces() { return AnnotationTypeNLP.TOKEN; };
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { AnnotationTypeNLP.ORIGINAL_TEXT }; }
				public boolean measuresConfidence() { return false; }
				@SuppressWarnings("unchecked")
				public Pair<Token, Double>[][] annotate(DocumentNLP document) {
					List<CoreMap> sentences = annotatedText.get(SentencesAnnotation.class);
					Pair<Token, Double>[][] tokens = (Pair<Token, Double>[][])(new Pair[getValidSentenceCount()][]);
					for(int i = 0; i < sentences.size(); i++) {
						List<CoreLabel> sentenceTokens = sentences.get(i).get(TokensAnnotation.class);
						int validSentenceIndex = getValidSentenceIndex(i);
						if (validSentenceIndex < 0)
							continue;
						
						tokens[validSentenceIndex] = (Pair<Token, Double>[])new Pair[sentenceTokens.size()];
						for (int j = 0; j < sentenceTokens.size(); j++) {
							String word = sentenceTokens.get(j).get(TextAnnotation.class); 
							int charSpanStart = sentenceTokens.get(j).beginPosition();
							int charSpanEnd = sentenceTokens.get(j).endPosition();
							tokens[validSentenceIndex][j] = new Pair<Token, Double>(new Token(document, word, charSpanStart, charSpanEnd), null);
						}
					}
					
					return tokens;
				}
			});
			
			return true;
		} else if (annotationType.equals(AnnotationTypeNLP.POS)) {
			addAnnotator(AnnotationTypeNLP.POS,  new AnnotatorToken<PoSTag>() {
				public String getName() { return "stanford"; }
				public AnnotationType<PoSTag> produces() { return AnnotationTypeNLP.POS; };
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { AnnotationTypeNLP.TOKEN }; }
				public boolean measuresConfidence() { return false; }
				@SuppressWarnings("unchecked")
				public Pair<PoSTag, Double>[][] annotate(DocumentNLP document) {
					List<CoreMap> sentences = annotatedText.get(SentencesAnnotation.class);
					Pair<PoSTag, Double>[][] posTags = (Pair<PoSTag, Double>[][])new Pair[getValidSentenceCount()][];
					
					for (int i = 0; i < sentences.size(); i++) {
						List<CoreLabel> sentenceTokens = sentences.get(i).get(TokensAnnotation.class);
						int validSentenceIndex = getValidSentenceIndex(i);
						if (validSentenceIndex < 0)
							continue;
						
						posTags[validSentenceIndex] = (Pair<PoSTag, Double>[])new Pair[sentenceTokens.size()];
						for (int j = 0; j < sentenceTokens.size(); j++) {
							String pos = sentenceTokens.get(j).get(PartOfSpeechAnnotation.class);  
							
							if (pos.length() > 0 && !Character.isLetter(pos.toCharArray()[0]))
								posTags[validSentenceIndex][j] = new Pair<PoSTag, Double>(PoSTag.SYM, null);
							else
								posTags[validSentenceIndex][j] = new Pair<PoSTag, Double>(PoSTag.valueOf(pos), null);
						}
					}
					
					return posTags;
				}
			});
			
			return true;
		} else if (annotationType.equals(AnnotationTypeNLP.LEMMA)) {
			addAnnotator(AnnotationTypeNLP.LEMMA,  new AnnotatorToken<String>() {
				public String getName() { return "stanford"; }
				public AnnotationType<String> produces() { return AnnotationTypeNLP.LEMMA; };
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { AnnotationTypeNLP.TOKEN, AnnotationTypeNLP.POS }; }
				public boolean measuresConfidence() { return false; }
				@SuppressWarnings("unchecked")
				public Pair<String, Double>[][] annotate(DocumentNLP document) {
					List<CoreMap> sentences = annotatedText.get(SentencesAnnotation.class);
					Pair<String, Double>[][] lemmas = (Pair<String, Double>[][])new Pair[getValidSentenceCount()][];
					
					for (int i = 0; i < sentences.size(); i++) {
						List<CoreLabel> sentenceTokens = sentences.get(i).get(TokensAnnotation.class);
						int validSentenceIndex = getValidSentenceIndex(i);
						if (validSentenceIndex < 0)
							continue;
						
						lemmas[validSentenceIndex] = (Pair<String, Double>[])new Pair[sentenceTokens.size()];
						for (int j = 0; j < sentenceTokens.size(); j++) {
							String lemma = sentenceTokens.get(j).get(LemmaAnnotation.class);  
							lemmas[validSentenceIndex][j] = new Pair<String, Double>(lemma, null);
						}
					}
					
					return lemmas;
				}
			});
		
			return true;
		} else if (annotationType.equals(AnnotationTypeNLP.CONSTITUENCY_PARSE)) {
			addAnnotator(AnnotationTypeNLP.CONSTITUENCY_PARSE,  new AnnotatorSentence<ConstituencyParse>() {
				public String getName() { return "stanford"; }
				public AnnotationType<ConstituencyParse> produces() { return AnnotationTypeNLP.CONSTITUENCY_PARSE; };
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { AnnotationTypeNLP.TOKEN, AnnotationTypeNLP.POS }; }
				public boolean measuresConfidence() { return false; }
				public Map<Integer, Pair<ConstituencyParse, Double>> annotate(DocumentNLP document) {
					List<CoreMap> sentences = annotatedText.get(SentencesAnnotation.class);
					Map<Integer, Pair<ConstituencyParse, Double>> parses = new HashMap<Integer, Pair<ConstituencyParse, Double>>();

					for(int i = 0; i < sentences.size(); i++) {
						int validSentenceIndex = getValidSentenceIndex(i);
						if (validSentenceIndex < 0)
							continue;
						
						Tree tree = sentences.get(i).get(TreeAnnotation.class);
	
						Constituent root = null;
						parses.put(validSentenceIndex, new Pair<ConstituencyParse, Double>(new ConstituencyParse(document, validSentenceIndex, null), null));
						
						if (tree == null)
							continue;
						
						Stack<Pair<Tree, List<Constituent>>> constituents = new Stack<Pair<Tree, List<Constituent>>>();
						Stack<Tree> toVisit = new Stack<Tree>();
						toVisit.push(tree);
						int tokenIndex = 0;
						while (!toVisit.isEmpty()) {
							Tree currentTree = toVisit.pop();
							
							if (!constituents.isEmpty()) {
								while (!isStanfordTreeParent(currentTree, constituents.peek().getFirst())) {
									Pair<Tree, List<Constituent>> currentNeighbor = constituents.pop();
									ConstituencyParse.Constituent constituent = parses.get(validSentenceIndex).getFirst().new Constituent(currentNeighbor.getFirst().label().value(), currentNeighbor.getSecond().toArray(new ConstituencyParse.Constituent[0]));
									constituents.peek().getSecond().add(constituent);
								}
							}
							
							if (currentTree.isPreTerminal()) {
								String label = currentTree.label().value();
								ConstituencyParse.Constituent constituent = parses.get(validSentenceIndex).getFirst().new Constituent(label, new TokenSpan(document, validSentenceIndex, tokenIndex, tokenIndex + 1));
								tokenIndex++;
								if (!constituents.isEmpty())
									constituents.peek().getSecond().add(constituent);
								else
									root = constituent;
							} else {
								constituents.push(new Pair<Tree, List<Constituent>>(currentTree, new ArrayList<Constituent>()));
								for (int j = currentTree.numChildren() - 1; j >= 0; j--)
									toVisit.push(currentTree.getChild(j));
							}
						}
						
						while (!constituents.isEmpty()) {
							Pair<Tree, List<Constituent>> possibleRoot = constituents.pop();
							root = parses.get(validSentenceIndex).getFirst().new Constituent(possibleRoot.getFirst().label().value(), possibleRoot.getSecond().toArray(new ConstituencyParse.Constituent[0]));
							if (!constituents.isEmpty())
								constituents.peek().getSecond().add(root);
						}
						
						parses.put(validSentenceIndex, new Pair<ConstituencyParse, Double>(new ConstituencyParse(document, validSentenceIndex, root), null));
					}
					
					return parses;
				}
				
				private boolean isStanfordTreeParent(Tree tree, Tree possibleParent) {
					for (int j = 0; j < possibleParent.numChildren(); j++) {
						if (possibleParent.getChild(j).equals(tree)) {
							return true;
						}
					}
					return false;
				}
			});
		
			return true;
		} else if (annotationType.equals(AnnotationTypeNLP.DEPENDENCY_PARSE)) {
			addAnnotator(AnnotationTypeNLP.DEPENDENCY_PARSE,  new AnnotatorSentence<DependencyParse>() {
				public String getName() { return "stanford"; }
				public AnnotationType<DependencyParse> produces() { return AnnotationTypeNLP.DEPENDENCY_PARSE; };
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { AnnotationTypeNLP.TOKEN, AnnotationTypeNLP.POS, AnnotationTypeNLP.CONSTITUENCY_PARSE }; }
				public boolean measuresConfidence() { return false; }
				public Map<Integer, Pair<DependencyParse, Double>> annotate(DocumentNLP document) {
					List<CoreMap> sentences = annotatedText.get(SentencesAnnotation.class);
					Map<Integer, Pair<DependencyParse, Double>> parses = new HashMap<Integer, Pair<DependencyParse, Double>>();
					for(int i = 0; i < sentences.size(); i++) {
						int validSentenceIndex = getValidSentenceIndex(i);
						if (validSentenceIndex < 0)
							continue;
						
						SemanticGraph sentenceDependencyGraph = sentences.get(i).get(CollapsedCCProcessedDependenciesAnnotation.class);
						
						Set<IndexedWord> sentenceWords = sentenceDependencyGraph.vertexSet();
						
						Map<Integer, Pair<List<DependencyParse.Dependency>, List<DependencyParse.Dependency>>> nodesToDeps = new HashMap<Integer, Pair<List<DependencyParse.Dependency>, List<DependencyParse.Dependency>>>();
						parses.put(validSentenceIndex, new Pair<DependencyParse, Double>(new DependencyParse(document, validSentenceIndex, null, null), null));
						int maxIndex = -1;
						for (IndexedWord sentenceWord1 : sentenceWords) {
							for (IndexedWord sentenceWord2 : sentenceWords) {
								if (sentenceWord1.equals(sentenceWord2))
									continue;
								GrammaticalRelation relation = sentenceDependencyGraph.reln(sentenceWord1, sentenceWord2);
								if (relation == null)
									continue;
							
								int govIndex = sentenceWord1.index() - 1;
								int depIndex = sentenceWord2.index() - 1;
								
								maxIndex = Math.max(depIndex, Math.max(govIndex, maxIndex));
								
								DependencyParse.Dependency dependency = parses.get(validSentenceIndex).getFirst().new Dependency(govIndex, depIndex, relation.getShortName());
								
								if (!nodesToDeps.containsKey(govIndex))
									nodesToDeps.put(govIndex, new Pair<List<Dependency>, List<Dependency>>(new ArrayList<Dependency>(), new ArrayList<Dependency>()));
								if (!nodesToDeps.containsKey(depIndex))
									nodesToDeps.put(depIndex, new Pair<List<Dependency>, List<Dependency>>(new ArrayList<Dependency>(), new ArrayList<Dependency>()));
								
								nodesToDeps.get(govIndex).getSecond().add(dependency);
								nodesToDeps.get(depIndex).getFirst().add(dependency);
							}
						}
						
						if (!nodesToDeps.containsKey(-1))
							nodesToDeps.put(-1, new Pair<List<Dependency>, List<Dependency>>(new ArrayList<Dependency>(), new ArrayList<Dependency>()));
						
						
						Collection<IndexedWord> rootDeps = sentenceDependencyGraph.getRoots();
						for (IndexedWord rootDep : rootDeps) {
							int depIndex = rootDep.index() - 1;
							DependencyParse.Dependency dependency = parses.get(validSentenceIndex).getFirst().new Dependency(-1, depIndex, "root");
							
							if (!nodesToDeps.containsKey(depIndex))
								nodesToDeps.put(depIndex, new Pair<List<Dependency>, List<Dependency>>(new ArrayList<Dependency>(), new ArrayList<Dependency>()));
							
							nodesToDeps.get(-1).getSecond().add(dependency);
							nodesToDeps.get(depIndex).getFirst().add(dependency);
						}
						
						Node[] tokenNodes = new Node[maxIndex+1];
						for (int j = 0; j < tokenNodes.length; j++)
							if (nodesToDeps.containsKey(j))
								tokenNodes[j] = parses.get(validSentenceIndex).getFirst().new Node(j, nodesToDeps.get(j).getFirst().toArray(new Dependency[0]), nodesToDeps.get(j).getSecond().toArray(new Dependency[0]));
						
						Node rootNode = parses.get(validSentenceIndex).getFirst().new Node(-1, new Dependency[0], nodesToDeps.get(-1).getSecond().toArray(new Dependency[0]));
						parses.put(validSentenceIndex, new Pair<DependencyParse, Double>(new DependencyParse(document, validSentenceIndex, rootNode, tokenNodes), null));
					}
					
					return parses;
				}
			});
			
			return true;
		} else if (annotationType.equals(AnnotationTypeNLP.NER)) {
			addAnnotator(AnnotationTypeNLP.NER,  new AnnotatorTokenSpan<String>() {
				public String getName() { return "stanford"; }
				public AnnotationType<String> produces() { return AnnotationTypeNLP.NER; };
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { AnnotationTypeNLP.TOKEN, AnnotationTypeNLP.POS, AnnotationTypeNLP.CONSTITUENCY_PARSE, AnnotationTypeNLP.DEPENDENCY_PARSE }; }
				public boolean measuresConfidence() { return false; }
				public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
					// FIXME Don't need to do this in a two step process where construct
					// array and then convert it into token span list.  This was just refactored
					// from old code in a rush, but can be done more efficiently
					
					List<CoreMap> sentences = annotatedText.get(SentencesAnnotation.class);
					String[][] ner = new String[sentences.size()][];
					for(int i = 0; i < sentences.size(); i++) {
						int validSentenceIndex = getValidSentenceIndex(i);
						if (validSentenceIndex < 0)
							continue;
						
						List<CoreLabel> sentenceTokens = sentences.get(i).get(TokensAnnotation.class);
						
						ner[i] = new String[sentenceTokens.size()];
						for (int j = 0; j < sentenceTokens.size(); j++) {
							ner[i][j] = sentenceTokens.get(j).get(NamedEntityTagAnnotation.class); 
						}
					}
					
					List<Triple<TokenSpan, String, Double>> nerAnnotations = new ArrayList<Triple<TokenSpan, String, Double>>();
					for (int i = 0; i < ner.length; i++) {
						int validSentenceIndex = getValidSentenceIndex(i);
						if (validSentenceIndex < 0)
							continue;
						
						for (int j = 0; j < ner[i].length; j++) {
							if (ner[i][j] != null) {
								int endTokenIndex = j + 1;
								for (int k = j + 1; k < ner[i].length; k++) {
									if (ner[i][k] == null || !ner[i][k].equals(ner[i][j])) {
										endTokenIndex = k;
										break;
									}
									ner[i][k] = null;
								}
								
								nerAnnotations.add(new Triple<TokenSpan, String, Double>(new TokenSpan(document, validSentenceIndex, j, endTokenIndex), ner[i][j], null));
							}
						}
					}
					
					return nerAnnotations;
				}
			});
			
			return true;
		} else if (annotationType.equals(AnnotationTypeNLP.COREF)) {
			addAnnotator(AnnotationTypeNLP.COREF,  new AnnotatorTokenSpan<TokenSpanCluster>() {
				public String getName() { return "stanford"; }
				public AnnotationType<TokenSpanCluster> produces() { return AnnotationTypeNLP.COREF; };
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { AnnotationTypeNLP.TOKEN, AnnotationTypeNLP.POS, AnnotationTypeNLP.CONSTITUENCY_PARSE, AnnotationTypeNLP.DEPENDENCY_PARSE, AnnotationTypeNLP.NER }; }
				public boolean measuresConfidence() { return false; }
				public List<Triple<TokenSpan, TokenSpanCluster, Double>> annotate(DocumentNLP document) {
					Map<Integer, CorefChain> corefGraph = annotatedText.get(CorefChainAnnotation.class);
					List<Triple<TokenSpan, TokenSpanCluster, Double>> annotations = new ArrayList<Triple<TokenSpan, TokenSpanCluster, Double>>();
					
					for (Entry<Integer, CorefChain> entry : corefGraph.entrySet()) {
						CorefChain corefChain = entry.getValue();
						CorefMention representativeMention = corefChain.getRepresentativeMention();
						int representativeSentIndex = getValidSentenceIndex(representativeMention.sentNum - 1);
						if (representativeSentIndex < 0)
							continue;
						
						TokenSpan representativeSpan = new TokenSpan(document, 
																	 representativeSentIndex,
																	 representativeMention.startIndex - 1,
																	 representativeMention.endIndex - 1);
						
						List<TokenSpan> spans = new ArrayList<TokenSpan>();
						Map<IntPair, Set<CorefMention>> mentionMap = corefChain.getMentionMap();
						for (Entry<IntPair, Set<CorefMention>> spanEntry : mentionMap.entrySet()) {
							for (CorefMention mention : spanEntry.getValue()) {
								int validSentenceIndex = getValidSentenceIndex(mention.sentNum - 1);
								if (validSentenceIndex < 0)
									continue;
								
								spans.add(new TokenSpan(document,
															validSentenceIndex,
															mention.startIndex - 1,
															mention.endIndex - 1));
							}
						}
						
						TokenSpanCluster cluster = new TokenSpanCluster(entry.getKey(), representativeSpan, spans);
						for (TokenSpan span : spans)
							annotations.add(new Triple<TokenSpan, TokenSpanCluster, Double>(span, cluster, null));
					}
					
					return annotations;
				}
			});
			
			return true;
		}
		
		return false;
	}
	
	public boolean setDocument(DocumentNLP document) {
		if (!super.setDocument(document))
			return false;
		
		if (this.nlpPipeline == null)
			if (!initialize())
				return false;
		
		this.annotatedText = new Annotation(document.getOriginalText());
		this.nlpPipeline.annotate(this.annotatedText);
		
		List<CoreMap> sentences = this.annotatedText.get(SentencesAnnotation.class);
		if (this.maxSentenceLength == 0) {
			this.validSentenceCount = sentences.size();
			this.originalToValidSentenceIndices = null;
		} else {
			this.validSentenceCount = 0;
			this.originalToValidSentenceIndices = new int[sentences.size()];
			for(int i = 0; i < sentences.size(); i++) {
				List<CoreLabel> sentenceTokens = sentences.get(i).get(TokensAnnotation.class);
				if (sentenceTokens.size() <= this.maxSentenceLength) {
					this.originalToValidSentenceIndices[i] = this.validSentenceCount;
					this.validSentenceCount++;
				} else {
					this.originalToValidSentenceIndices[i] = -1;
				}
			}
		}
		
		
		return true;
	}
}
