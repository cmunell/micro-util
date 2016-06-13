package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.Annotation; 
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;

//import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;

/**
 * Utility class to generate {@link DocumentNLP} objects from Stanford-format XML
 * annotations.
 *
 * This is a "J" StanfordXMLLoader because that's our traditional designation for things based on
 * Justin Bettridge's regex-based Stanford XML reader, which we've found to be dramatically and
 * usefully faster than using a real XML parsing library at no apparent cost to correctnes.
 *
 * I'm not sure that going through a DocumentNLPInMemory object is the best or most efficient way to
 * do this, but it does reuse more existing codepaths and entail less refactoring/modification to
 * existing code than generating, say, one of our Annotation objects directly (which seems to be the
 * obvious alternative).
 *
 * This might better belong in DocumentNLPInMemory alongside the methods to load them from JSON or
 * micro annotations.  Or maybe it'd be better to refactor in such a way that all these methods can
 * live externally?  This is left for future consideration for the time being.
 */
public class JStanfordXMLLoader {
    // bkdb: add TODO about conveying other Stanford annotations
    /** 
     * Speedy alternative to java.util.regex.Pattern and Matcher specifically meant for the simple
     * things we have to do here for XML
     */ 
    public static class XMLPattern { 
        protected final String tag; 
        protected final String attribute; 
 
        protected final String openbStr;
        protected final int openbStrLen;
        protected final String closebStr;
        protected final int closebStrLen;
        protected final String attbStr;
        protected final int attbStrLen;

        public XMLPattern(String tag, String attribute) {
            this.tag = tag;
            this.attribute = attribute;

            openbStr = "<" + tag;
            openbStrLen = openbStr.length();
            closebStr = "</" + tag + ">";
            closebStrLen = closebStr.length();

            if (attribute != null) {
                attbStr = " " + attribute + "=\"";
                attbStrLen = attbStr.length();
            } else {
                attbStr = null;
                attbStrLen = 0;
            }
        }

        public XMLPattern(String tag) {
            this(tag, null);
        }

        public XMLPattern.Matcher matcher(String haystack) {
            return new Matcher(haystack);
        }

        public class Matcher {
            protected final String haystack; 
            protected final int haylen;
            protected int pos;
            protected String curContent; 
            protected String curAttribute;
            protected int curContentBegin;
            protected int curContentEnd;
            protected int curAttributeBegin;
            protected int curAttributeEnd;

            protected Matcher(String haystack) {
                this.haystack = haystack;
                this.haylen = haystack.length();
                pos = 0;
                curContent = null;
                curAttribute = null;
                curContentBegin = -1;
                curContentEnd = -1;
                curAttributeBegin = -1;
                curAttributeEnd = -1;
            }

            public boolean find() {
                while (true) {
                    if (pos >= haylen) return false;
                    final int openb = haystack.indexOf(openbStr, pos);
                    if (openb < 0) return false;
                    final int pre_opene = openb + openbStrLen;
                    final char c = haystack.charAt(pre_opene);
                    final int opene;
                    if (c == '>') {
                        opene = pre_opene;
                    } else if (c == ' ') {
                        opene = haystack.indexOf(">", pre_opene);
                        if (opene < 0)
                            throw new RuntimeException("No closing '>' in " + tag + " tag: "
                                    + haystack.substring(openb));
                    } else {
                        // Matched first part of tag name, but it's longer than the tag we're
                        // looking for.
                        pos = pre_opene;
                        continue;
                    }
                    // System.out.println("bkdb: opening tag is " + haystack.substring(openb, opene));

                    final int closeb = haystack.indexOf(closebStr, opene+1);
                    if (closeb < 0)
                        throw new RuntimeException("No closing " + tag + " tag: "
                                + haystack.substring(openb));
                    curContentBegin = opene+1;
                    curContentEnd = closeb;
                    curContent = haystack.substring(curContentBegin, curContentEnd);
                    //System.out.println("bkdb: content: " + curContent);
                    pos = closeb + 1;
            
                    if (attribute != null) {
                        //System.out.println("bkdb: looking for attribute starting at: " + haystack.substring(openb + openbStrLen, openb + openbStrLen + 20));
                        final int attb = haystack.indexOf(attbStr, openb + openbStrLen);
                        if (attb < 0) {
                            // Arguably shouldn't happen, but we'll preserve original behavior by
                            // considering this to be a nonmatch after all and looping back up
                        	//if (true) throw new RuntimeException("bkdb: this really shouldn't happen");
                            continue;
                        }
                        //System.out.println("bkdb: looking for closing quote starting at: " + haystack.substring(attb + attbStrLen, attb + attbStrLen + 20));
                        final int atte = haystack.indexOf("\"", attb + attbStrLen);
                        if (atte < 0) {
                            // Arguably shouldn't happen, but we'll preserve original behavior by
                            // considering this to be a nonmatch after all and looping back up
                            if (true) throw new RuntimeException("bkdb: this really shouldn't happen");
                            //continue;
                        }
                        //System.out.println("bkdb: attb=" + attb + ", attbStrLen=" + attbStrLen + ", atte=" + atte);
                        curAttributeBegin = attb + attbStrLen;
                        curAttributeEnd = atte;
                        curAttribute = haystack.substring(curAttributeBegin, curAttributeEnd);
                        //System.out.println("bkdb: attribute: " + curAttribute);
                    }

                    return true;
                }
            }

            public String getContent() {
                return curContent;
            }

            public Pair<Integer, Integer> getContentOffsets() {
                return new Pair<Integer, Integer>(curContentBegin, curContentEnd);
            }
            
            public String getAttribute() {
                return curAttribute;
            }

            public Pair<Integer, Integer> getAttributeOffsets() {
                return new Pair<Integer, Integer>(curAttributeBegin, curAttributeEnd);
            }
        }
    } 


    /**
     * The kind of {@link DocumentNLP} instance that we return
     *
     * DocumentNLPInMemory is the obvious sort of DocumentNLP instance to build, because its
     * internal members are exactly what we are reading out of the Stanford XML.  But
     * DocumentNLPInMemory effectively offers only read-only access to its members.  While that
     * doesn't technically apply here because this class happens to share the same package, it seems
     * unclean to take advantage of that happenstance.  Maybe we want some sort of general purpose
     * implementation that offers the ability to modify its members, but we'll eschew that for now
     * by just using our own implementation.  And we'll do that the quick and easy way by simply
     * making our own subclass of DocumentNLPInMemory, and then we can modify its members directly
     * with a clear concience.
     */
    protected static class OurDocumentNLP extends DocumentNLPInMemory {
        protected OurDocumentNLP(DataTools dataTools) {
            super(dataTools);
        }

        protected void setName(String name) {
            this.name = name;
        }
    }

    // bkdb: prune these
    protected static String greedyPatt = "<REPLACEME>(.*)<\\/REPLACEME>";
    protected static Pattern greedyCorefPatt = Pattern.compile(greedyPatt.replace("REPLACEME", "coreference"));

    protected static XMLPattern sentPatt2 = new XMLPattern("sentence", "id");
    protected static XMLPattern tokPatt2 = new XMLPattern("token", "id");
    protected static XMLPattern wordPatt2 = new XMLPattern("word");
    protected static XMLPattern lemmaPatt2 = new XMLPattern("lemma");
    protected static XMLPattern charBegPatt2 = new XMLPattern("CharacterOffsetBegin");
    protected static XMLPattern charEndPatt2 = new XMLPattern("CharacterOffsetEnd");
    protected static XMLPattern posPatt2 = new XMLPattern("POS");
    protected static XMLPattern nerPatt2 = new XMLPattern("NER");
    protected static XMLPattern timexPatt2 = new XMLPattern("Timex");
    protected static XMLPattern parsePatt2 = new XMLPattern("parse");
    protected static XMLPattern basicDepPatt2 = new XMLPattern("dependencies", "type");
    protected static XMLPattern depPatt2 = new XMLPattern("dep", "type");
    protected static XMLPattern govPatt2 = new XMLPattern("governor", "idx");
    protected static XMLPattern dependentPatt2 = new XMLPattern("dependent", "idx");
    protected static XMLPattern collDepPatt2 = new XMLPattern("collapsed-dependencies");
    protected static XMLPattern collCcProcDepPatt2 = new XMLPattern("collapsed-ccprocessed-dependencies");
    protected static XMLPattern corefPatt2 = new XMLPattern("coreference");
    protected static XMLPattern menRepPatt2 = new XMLPattern("mention", "representative");
    protected static XMLPattern menPatt2 = new XMLPattern("mention");
    protected static XMLPattern sentNoIDPatt2 = new XMLPattern("sentence");
    protected static XMLPattern startPatt2 = new XMLPattern("start");
    protected static XMLPattern endPatt2 = new XMLPattern("end");
    protected static XMLPattern headPatt2 = new XMLPattern("head");
    protected static XMLPattern docIdPatt2 = new XMLPattern("docId");

    protected static String getXmlVal(String src, XMLPattern patt, boolean wantAttribute) {
        XMLPattern.Matcher m = patt.matcher(src);
        if (m.find()) {
            if (wantAttribute) return m.getAttribute();
            else return m.getContent();
        }
        return null;
    }

    protected static String getXmlVal(String src, Pattern patt, int group) {
        Matcher m = patt.matcher(src);
        if (m.find()) {
            return m.group(group);
        }
        return null;
    }

    /**
     * Helper function for {@link fromSingleXML} for generating {@link TokenSpan} objects from
     * coreference XML.
     */
    protected static TokenSpan makeTokenSpan(DocumentNLP doc, String menStr) {
        try {
            int sentIdx = Integer.parseInt(getXmlVal(menStr, sentNoIDPatt2, false));
            int startIdx = Integer.parseInt(getXmlVal(menStr, startPatt2, false));
            int endIdx = Integer.parseInt(getXmlVal(menStr, endPatt2, false));
            return new TokenSpan(doc, sentIdx - 1, startIdx - 1, endIdx - 1);
        } catch (Exception e) {
            throw new RuntimeException("Ill-formed mention string \"" + menStr + "\"", e);

        }
    }

    /**
     * Translates a single document in Stanford's XML format into a {@link DocumentNLP}
     * object.
     *
     * The given string must be the XML for exactly one document.
     *
     * It will be interesting if it turns out to be desirable for this to be a {@link
     * DocumentNLPInMemory} -- see comments on {@link OurDocumentNLP}.  That could suggest a need to
     * refactor.
     *
     * This implementation is taken from edu.cmu.ml.rtw.micro.core.Document's fromStanfordXMLString
     * method, although of course it has been modified substantially.  Those hunting bugs are
     * encouraged to refer to the original as well, since it seemed to be bug-free up until 2015
     * when we stopped using it.
     */
    @SuppressWarnings("unchecked")
    public static DocumentNLPInMemory fromSingleXML(String docStr, DataTools dataTools) {
        try {
            OurDocumentNLP ret = new OurDocumentNLP(dataTools);

            // DocumentNLPInMemory uses arrays for many things, so build up those things in
            // ArrayLists first so that we can know how big to make the arrays, and then transfer
            // the content over.
            ArrayList<ArrayList<Token>> ourTokens = new ArrayList<ArrayList<Token>>();
            ArrayList<ArrayList<PoSTag>> ourPOSTags = new ArrayList<ArrayList<PoSTag>>();
            ArrayList<ArrayList<String>> ourNERTags = new ArrayList<ArrayList<String>>();
            ArrayList<ArrayList<String>> ourLemmas = new ArrayList<ArrayList<String>>();
            ArrayList<ConstituencyParse> ourConstituencyParses = new ArrayList<ConstituencyParse>();
            ArrayList<DependencyParse> ourDependencyParses = new ArrayList<DependencyParse>();
            Map<Integer, List<Triple<TokenSpan, TokenSpanCluster, Double>>> ourCoref =
                    new HashMap<Integer, List<Triple<TokenSpan, TokenSpanCluster, Double>>>();

            String docID = getXmlVal(docStr, docIdPatt2, false);
            if (docID != null) ret.setName(docID);

            XMLPattern.Matcher m = sentPatt2.matcher(docStr);
            while (m.find()) {
                int sentenceId = Integer.parseInt(m.getAttribute()) - 1;
                try {
                    ArrayList<Token> tokens = new ArrayList<Token>();
                    while (ourTokens.size() <= sentenceId) ourTokens.add(null);
                    ourTokens.set(sentenceId, tokens);

                    String sentStr = m.getContent();
                    XMLPattern.Matcher tm = tokPatt2.matcher(sentStr);
                    while (tm.find()) {
                        int tokId = Integer.parseInt(tm.getAttribute()) - 1;
                        String tokStr = tm.getContent();
                        String word = getXmlVal(tokStr, wordPatt2, false);
                        String s = getXmlVal(tokStr, charBegPatt2, false);
                        int tStart = s != null ? Integer.parseInt(s) : -1;
                        s = getXmlVal(tokStr, charEndPatt2, false);
                        int tEnd = s != null ? Integer.parseInt(s) : -1;
                        while (tokens.size() <= tokId) tokens.add(null);
                        tokens.set(tokId, new Token(ret, word, tStart-1, tEnd-1));
                    
                        String posStr = getXmlVal(tokStr, posPatt2, false);
                        if (posStr != null) {
                            while (ourPOSTags.size() <= sentenceId) ourPOSTags.add(null);
                            ArrayList<PoSTag> tags = ourPOSTags.get(sentenceId);
                            if (tags == null) {
                                tags = new ArrayList<PoSTag>();
                                ourPOSTags.set(sentenceId, tags);
                            }
                            while (tags.size() <= tokId) tags.add(null);
                            if (posStr.length() > 0 && !Character.isLetter(posStr.charAt(0)))
                                tags.set(tokId, PoSTag.SYM);
                            else
                                tags.set(tokId, PoSTag.valueOf(posStr));
                        }

                        String nerStr = getXmlVal(tokStr, nerPatt2, false);
                        if (nerStr != null) {
                            while (ourNERTags.size() <= sentenceId) ourNERTags.add(null);
                            ArrayList<String> tags = ourNERTags.get(sentenceId);
                            if (tags == null) {
                                tags = new ArrayList<String>();
                                ourNERTags.set(sentenceId, tags);
                            }
                            while (tags.size() <= tokId) tags.add(null);
                            tags.set(tokId, nerStr);
                        }

                        String lemmaStr = getXmlVal(tokStr, lemmaPatt2, false);
                        if (lemmaStr != null) {
                            while (ourLemmas.size() <= sentenceId) ourLemmas.add(null);
                            ArrayList<String> tags = ourLemmas.get(sentenceId);
                            if (tags == null) {
                                tags = new ArrayList<String>();
                                ourLemmas.set(sentenceId, tags);
                            }
                            while (tags.size() <= tokId) tags.add(null);
                            tags.set(tokId, lemmaStr);
                        }

                        /* TODO: do we need TIMEX?
                           if (t.ner.equals("DATE")) {
                           try {
                           t.timex = getXmlVal(tokStr, timexPatt2, false);
                           } catch (java.lang.RuntimeException e) {
                           // This is exception is caused by <TIMEX .../> tags. In such cases,
                           // we use the word itself as timex.
                           // System.out.println("Runtimexception: " + e.getMessage());
                           t.timex = t.word;
                           }
                           } else {
                           t.timex = t.word;
                           }
                        */
                    }

                    String constituencyParseStr = getXmlVal(sentStr, parsePatt2, false);
                    if (constituencyParseStr != null) {
                        ConstituencyParse constituencyParse = 
                                (ConstituencyParse)AnnotationTypeNLP.CONSTITUENCY_PARSE.deserialize(ret, sentenceId, constituencyParseStr);
                        while (ourConstituencyParses.size() <= sentenceId) ourConstituencyParses.add(null);
                        ourConstituencyParses.set(sentenceId, constituencyParse);
                    }
                
                    String basicDepStr = getXmlVal(sentStr, basicDepPatt2, false);
                    if (basicDepStr != null) {
                        Map<Integer, Pair<List<DependencyParse.Dependency>, List<DependencyParse.Dependency>>> nodesToDeps =
                                new HashMap<Integer, Pair<List<DependencyParse.Dependency>, List<DependencyParse.Dependency>>>();
                        DependencyParse dp = new DependencyParse(ret, sentenceId, null, null);
                        int maxIndex = -1;
                        XMLPattern.Matcher dm = depPatt2.matcher(basicDepStr);
                        while (dm.find()) {
                            String depType = dm.getAttribute();
                            String depStr = dm.getContent();

                            int govIndex = Integer.parseInt(getXmlVal(depStr, govPatt2, true)) - 1;
                            int depIndex = Integer.parseInt(getXmlVal(depStr, dependentPatt2, true)) - 1;
                            DependencyParse.Dependency dependency =
                                    dp.newDependency(govIndex, depIndex, depType);
                        
                            maxIndex = Math.max(depIndex, Math.max(govIndex, maxIndex));
			
                            if (!nodesToDeps.containsKey(govIndex))
                                nodesToDeps.put(govIndex, new Pair<List<DependencyParse.Dependency>, List<DependencyParse.Dependency>>(new ArrayList<DependencyParse.Dependency>(), new ArrayList<DependencyParse.Dependency>()));
                            if (!nodesToDeps.containsKey(depIndex))
                                nodesToDeps.put(depIndex, new Pair<List<DependencyParse.Dependency>, List<DependencyParse.Dependency>>(new ArrayList<DependencyParse.Dependency>(), new ArrayList<DependencyParse.Dependency>()));
			
                            nodesToDeps.get(govIndex).getSecond().add(dependency);
                            nodesToDeps.get(depIndex).getFirst().add(dependency);
                        }
		
                        DependencyParse.Node[] tokenNodes = new DependencyParse.Node[maxIndex+1];
                        for (int i = 0; i < tokenNodes.length; i++)
                            if (nodesToDeps.containsKey(i))
                                tokenNodes[i] = dp.newNode(i, nodesToDeps.get(i).getFirst().toArray(new DependencyParse.Dependency[0]), nodesToDeps.get(i).getSecond().toArray(new DependencyParse.Dependency[0]));

                        if (!nodesToDeps.containsKey(-1)) {
                            throw new IllegalArgumentException("Failed to get dependency parse root");
                        }

                        dp.setNodes(dp.newNode(-1, new DependencyParse.Dependency[0], nodesToDeps.get(-1).getSecond().toArray(new DependencyParse.Dependency[0])), tokenNodes);
                        while (ourDependencyParses.size() <= sentenceId) ourDependencyParses.add(null);
                        ourDependencyParses.set(sentenceId, dp);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("In stentence " + sentenceId, e);
                }
            }

            String corefStr = getXmlVal(docStr, greedyCorefPatt, 1);
            if (corefStr != null) {
                XMLPattern.Matcher cm = corefPatt2.matcher(corefStr);
                int corefId = 0;
                while (cm.find()) {
                    String crfStr = cm.getContent();
                    String menStr = getXmlVal(crfStr, menRepPatt2, false);
                    if (menStr == null)
                        throw new RuntimeException("No representative mention in coref synset \""
                                + crfStr + "\"");
                    TokenSpan representativeSpan = makeTokenSpan(ret, menStr);

                    List<TokenSpan> spans = new ArrayList<TokenSpan>();
                    XMLPattern.Matcher mm = menPatt2.matcher(crfStr);
                    while (mm.find()) {
                        menStr = mm.getContent();
                        spans.add(makeTokenSpan(ret, menStr));
                    }
						
                    TokenSpanCluster cluster = new TokenSpanCluster(corefId, representativeSpan, spans);
                    ArrayList<Integer> usedSentences = new ArrayList<Integer>();
                    for (TokenSpan span : spans) {
                        int sentenceID = span.getSentenceIndex();
                        if (usedSentences.contains(sentenceID)) continue;
                        usedSentences.add(sentenceID);
                        if (!ourCoref.containsKey(sentenceID))
                            ourCoref.put(sentenceID, new ArrayList<Triple<TokenSpan, TokenSpanCluster, Double>>());
                        ourCoref.get(sentenceID).add(new Triple<TokenSpan, TokenSpanCluster, Double>(span, cluster, null));
                    }

                    corefId++;
                }
            }

            // Now we can take the completed our* variables we've been building and put them into
            // the DocumentNLPInMemory document.
            if (!ourTokens.isEmpty()) {
                ret.tokens = new Token[ourTokens.size()][];
                for (int i = 0; i < ourTokens.size(); i++)
                    ret.tokens[i] = ourTokens.get(i).toArray(new Token[0]);
            }
            if (!ourPOSTags.isEmpty()) {
                ret.posTags = new PoSTag[ourPOSTags.size()][];
                for (int i = 0; i < ourPOSTags.size(); i++)
                    ret.posTags[i] = ourPOSTags.get(i).toArray(new PoSTag[0]);
            }
            if (!ourNERTags.isEmpty()) {
                // DocumentNLPInMemory has the NER tags organized differnently; in particular runs
                // of consecutive tokens with the same span are represented as a single annotated
                // TokenSpan object.
                ret.ner = new HashMap<Integer, List<Triple<TokenSpan, String, Double>>>();
                for (int sentenceId = 0; sentenceId < ourNERTags.size(); sentenceId++) {
                    if (ourNERTags.get(sentenceId) == null || ourNERTags.get(sentenceId).isEmpty())
                        continue;
                    ArrayList<String> nerList = ourNERTags.get(sentenceId);
                    ArrayList<Triple<TokenSpan, String, Double>> tripleList =
                            new ArrayList<Triple<TokenSpan, String, Double>>();
                    String prevNER = null;
                    int prevNERStart = 0;
                    for (int tokenId = 0; tokenId < nerList.size(); tokenId++) {
                        String ner = nerList.get(tokenId);
                        if (!ner.equals(prevNER)) {
                            if (prevNER != null) {
                                TokenSpan span = new TokenSpan(ret, sentenceId, prevNERStart, tokenId);
                                tripleList.add(new Triple<TokenSpan, String, Double>(span, prevNER, null));
                            }
                            prevNER = ner;
                            prevNERStart = tokenId;
                        }
                    }
                    TokenSpan span = new TokenSpan(ret, sentenceId, prevNERStart, nerList.size());
                    tripleList.add(new Triple<TokenSpan, String, Double>(span, prevNER, null));
                    
                    ret.ner.put(sentenceId, tripleList);
                }
            }
            if (!ourLemmas.isEmpty()) {
                Pair<String, Double>[][] lemmas = (Pair<String, Double>[][])new Pair[ourLemmas.size()][];
                for (int i = 0; i < ourLemmas.size(); i++) {
                    List<String> ourLemmaList = ourLemmas.get(i);
                    lemmas[i] = (Pair<String, Double>[])new Pair[ourLemmaList.size()];
                    for (int j = 0; j < ourLemmaList.size(); j++) {
                        lemmas[i][j] = new Pair<String, Double>(ourLemmaList.get(j), null);
                    }
                }
                if (ret.otherTokenAnnotations == null)
                    ret.otherTokenAnnotations = new HashMap<AnnotationTypeNLP<?>, Pair<?, Double>[][]>();
                ret.otherTokenAnnotations.put(AnnotationTypeNLP.LEMMA, lemmas);
            }
            if (!ourConstituencyParses.isEmpty()) {
                ret.constituencyParses = ourConstituencyParses.toArray(new ConstituencyParse[0]);
            }
            if (!ourDependencyParses.isEmpty()) {
                ret.dependencyParses = ourDependencyParses.toArray(new DependencyParse[0]);
            }
            if (!ourCoref.isEmpty()) {
                ret.coref = ourCoref;
            }

            return ret;
        } catch (Exception e) {
            throw new RuntimeException("fronSingleXML(\"" + docStr + "\")", e);
        }
    }

    public static void main(String[] args) { 
        try { 
            DataTools dataTools = new DataTools();
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            
            String line;
            while ((line = br.readLine()) != null) { 
                System.out.println("---");
                DocumentNLPMutable doc = fromSingleXML(line, dataTools);
                SerializerDocumentNLPMicro serializer = new SerializerDocumentNLPMicro(doc);
                List<Annotation> annotations = serializer.serialize(doc).getAllAnnotations(); 
                for (Annotation annotation : annotations) 
                    System.out.println(annotation.toJsonString()); 
            } 
            br.close();
        } catch (Exception e) { 
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.err.println(sw.toString());
            System.exit(2); 
        } 
    } 
}
