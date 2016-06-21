package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import org.junit.Assert;
import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.DataTools;

public class WordNetTest {
	@Test
	public void testWordNet() {
		WordNet wn = (new DataTools()).getWordNet();
		String lemma = wn.getLemma("walking", PoSTag.VBG);
		Assert.assertEquals("walk", lemma);
	}
}
