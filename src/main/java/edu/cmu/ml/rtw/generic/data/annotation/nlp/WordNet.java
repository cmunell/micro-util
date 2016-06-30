package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.dictionary.Dictionary;

/**
 * WordNet represents various aspects of English WordNet
 * (http://wordnet.princeton.edu/) or other non-English 
 * WordNets (http://www.illc.uva.nl/EuroWordNet/).  The class
 * currently only works with the English WordNet, but might 
 * be extended further to work with the non-English one.
 *
 * @authors Bill McDowell
 */
public class WordNet {
	private Dictionary dictionary;
	
	public WordNet() {
		try {
			this.dictionary = Dictionary.getDefaultResourceInstance();
		} catch (JWNLException e) {
			e.printStackTrace();
		}
	}
	
	private POS convertPoSTag(PoSTag tag) {
		if (PoSTagClass.classContains(PoSTagClass.VB, tag))
			return POS.VERB;
		else if (PoSTagClass.classContains(PoSTagClass.JJ, tag))
			return POS.ADJECTIVE;
		else if (PoSTagClass.classContains(PoSTagClass.RB, tag))
			return POS.ADVERB;
		else if (PoSTagClass.classContains(PoSTagClass.NN, tag) || PoSTagClass.classContains(PoSTagClass.NNP, tag))
			return POS.NOUN;
		else
			return null;
	}
	
	private String getSynsetName(Synset synset) {
		return synset.getKey().toString().replaceAll("\\s+", "_");
	}
	
	private Map<String, Synset> getImmediateSynsets(String word, PoSTag tag) {
		try {
			Map<String, Synset> synsets = new HashMap<String, Synset>();
			POS pos = convertPoSTag(tag);
			if (pos == null)
				return synsets;
			IndexWord iword = this.dictionary.lookupIndexWord(pos, word);
			if (iword == null)
				return synsets;
			List<Synset> synsetArray = iword.getSenses();
			if (synsetArray == null)
				return synsets;
			
			for (Synset synset : synsetArray)
				synsets.put(getSynsetName(synset), synset);
			
			return synsets;
		} catch (Exception ex) { 
			return null;
		}
	}
	
	public synchronized boolean areSynonyms(String word1, PoSTag tag1, String word2, PoSTag tag2) {
		Map<String, Synset> synsets1 = getImmediateSynsets(word1, tag1);
		Map<String, Synset> synsets2 = getImmediateSynsets(word2, tag2);
		for (String synset : synsets1.keySet())
			if (synsets2.containsKey(synset))
				return true;
		return false;
	}
	
	public synchronized String getFirstImmediateSynsetName(String word, PoSTag tag) {
		try {
			POS pos = convertPoSTag(tag);
			if (pos == null)
				return null;
			
			IndexWord iword = this.dictionary.lookupIndexWord(pos, word);
			if (iword == null)
				return null;
			
			List<Synset> synsetArray = iword.getSenses();
			if (synsetArray == null)
				return null;
			
			Synset s = synsetArray.get(0);
			return getSynsetName(s);
		} catch (Exception ex) { 
			return null;
		}
	}
	
	public synchronized Set<String> getImmediateSynsetNames(String word, PoSTag tag) {
		return getImmediateSynsets(word, tag).keySet();
	}
	
	public synchronized String getLemma(String word, PoSTag tag) {
		if(word.indexOf('-') > -1 || word.indexOf('/') > -1)
			return null;
		
		try {
			POS pos = convertPoSTag(tag);
			if (pos == null)
				return null;
			IndexWord iword = this.dictionary.lookupIndexWord(pos, word);
			if (iword == null)
				return null;
			String lemma = iword.getLemma();
			lemma = lemma.trim().replace(' ', '_');
	        return lemma;
		} catch (JWNLException e) {
			return null;
		}
	}
}
