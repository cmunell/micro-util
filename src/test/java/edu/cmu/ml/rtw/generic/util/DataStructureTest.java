package edu.cmu.ml.rtw.generic.util;

import java.util.SortedMap;

import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.ardverk.collection.Trie;
import org.junit.Test;
import org.junit.Assert;
import org.platanios.learn.math.matrix.SparseVector;
import org.platanios.learn.math.matrix.Vector;

public class DataStructureTest {
	@Test
	public void testTrie() {
		Trie<String, String> trie = new PatriciaTrie<String, String>(StringKeyAnalyzer.CHAR);
		trie.put("cat", "cat");
		trie.put("dog", "dog");
		trie.put("mouse", "mouse");
		trie.put("mole", "mole");
		
		SortedMap<String, String> prefixed = trie.prefixMap("mo");
		Assert.assertEquals(2, prefixed.size());
		Assert.assertTrue(prefixed.containsKey("mouse"));
		Assert.assertTrue(prefixed.containsKey("mole"));
		
		prefixed = trie.prefixMap("d");
		Assert.assertEquals(1, prefixed.size());
		Assert.assertTrue(prefixed.containsKey("dog"));
	}
	
	@Test
	public void testVectorSetRange1() {
		Vector overwritten = new SparseVector(20, 
				new int[] { 5, 8, 11, 12, 13, 17 }, 
				new double[] { 5.0, 8.0, 11.0, 12.0, 13.0, 17.0 });
		
		Vector overwriting = new SparseVector(20, 
				new int[] { 0, 1, 3 }, 
				new double[] { 0.0, 1.0, 3.0 });
		
		overwritten.set(12, 17, overwriting);
		
		Assert.assertEquals(0.0, overwritten.get(0), .01);
		Assert.assertEquals(0.0, overwritten.get(1), .01);
		Assert.assertEquals(0.0, overwritten.get(2), .01);
		Assert.assertEquals(0.0, overwritten.get(3), .01);
		Assert.assertEquals(0.0, overwritten.get(4), .01);
		Assert.assertEquals(5.0, overwritten.get(5), .01);
		Assert.assertEquals(0.0, overwritten.get(6), .01);
		Assert.assertEquals(0.0, overwritten.get(7), .01);
		Assert.assertEquals(8.0, overwritten.get(8), .01);
		Assert.assertEquals(0.0, overwritten.get(9), .01);
		Assert.assertEquals(0.0, overwritten.get(10), .01);
		Assert.assertEquals(11.0, overwritten.get(11), .01);
		Assert.assertEquals(0.0, overwritten.get(12), .01);
		Assert.assertEquals(1.0, overwritten.get(13), .01);
		Assert.assertEquals(0.0, overwritten.get(14), .01);
		Assert.assertEquals(3.0, overwritten.get(15), .01);
		Assert.assertEquals(0.0, overwritten.get(16), .01);
		Assert.assertEquals(0.0, overwritten.get(17), .01);
		Assert.assertEquals(0.0, overwritten.get(18), .01);
		Assert.assertEquals(0.0, overwritten.get(19), .01);
		
		overwritten.set(0, 1, overwriting);
		
		Assert.assertEquals(0.0, overwritten.get(0), .01);
		Assert.assertEquals(1.0, overwritten.get(1), .01);
		Assert.assertEquals(0.0, overwritten.get(2), .01);
		Assert.assertEquals(0.0, overwritten.get(3), .01);
		Assert.assertEquals(0.0, overwritten.get(4), .01);
		Assert.assertEquals(5.0, overwritten.get(5), .01);
		Assert.assertEquals(0.0, overwritten.get(6), .01);
		Assert.assertEquals(0.0, overwritten.get(7), .01);
		Assert.assertEquals(8.0, overwritten.get(8), .01);
		Assert.assertEquals(0.0, overwritten.get(9), .01);
		Assert.assertEquals(0.0, overwritten.get(10), .01);
		Assert.assertEquals(11.0, overwritten.get(11), .01);
		Assert.assertEquals(0.0, overwritten.get(12), .01);
		Assert.assertEquals(1.0, overwritten.get(13), .01);
		Assert.assertEquals(0.0, overwritten.get(14), .01);
		Assert.assertEquals(3.0, overwritten.get(15), .01);
		Assert.assertEquals(0.0, overwritten.get(16), .01);
		Assert.assertEquals(0.0, overwritten.get(17), .01);
		Assert.assertEquals(0.0, overwritten.get(18), .01);
		Assert.assertEquals(0.0, overwritten.get(19), .01);
		
		overwritten.set(18, 19, overwriting);
		
		Assert.assertEquals(0.0, overwritten.get(0), .01);
		Assert.assertEquals(1.0, overwritten.get(1), .01);
		Assert.assertEquals(0.0, overwritten.get(2), .01);
		Assert.assertEquals(0.0, overwritten.get(3), .01);
		Assert.assertEquals(0.0, overwritten.get(4), .01);
		Assert.assertEquals(5.0, overwritten.get(5), .01);
		Assert.assertEquals(0.0, overwritten.get(6), .01);
		Assert.assertEquals(0.0, overwritten.get(7), .01);
		Assert.assertEquals(8.0, overwritten.get(8), .01);
		Assert.assertEquals(0.0, overwritten.get(9), .01);
		Assert.assertEquals(0.0, overwritten.get(10), .01);
		Assert.assertEquals(11.0, overwritten.get(11), .01);
		Assert.assertEquals(0.0, overwritten.get(12), .01);
		Assert.assertEquals(1.0, overwritten.get(13), .01);
		Assert.assertEquals(0.0, overwritten.get(14), .01);
		Assert.assertEquals(3.0, overwritten.get(15), .01);
		Assert.assertEquals(0.0, overwritten.get(16), .01);
		Assert.assertEquals(0.0, overwritten.get(17), .01);
		Assert.assertEquals(0.0, overwritten.get(18), .01);
		Assert.assertEquals(1.0, overwritten.get(19), .01);
		
		overwritten.set(5, 8, overwriting);
		
		Assert.assertEquals(0.0, overwritten.get(0), .01);
		Assert.assertEquals(1.0, overwritten.get(1), .01);
		Assert.assertEquals(0.0, overwritten.get(2), .01);
		Assert.assertEquals(0.0, overwritten.get(3), .01);
		Assert.assertEquals(0.0, overwritten.get(4), .01);
		Assert.assertEquals(0.0, overwritten.get(5), .01);
		Assert.assertEquals(1.0, overwritten.get(6), .01);
		Assert.assertEquals(0.0, overwritten.get(7), .01);
		Assert.assertEquals(3.0, overwritten.get(8), .01);
		Assert.assertEquals(0.0, overwritten.get(9), .01);
		Assert.assertEquals(0.0, overwritten.get(10), .01);
		Assert.assertEquals(11.0, overwritten.get(11), .01);
		Assert.assertEquals(0.0, overwritten.get(12), .01);
		Assert.assertEquals(1.0, overwritten.get(13), .01);
		Assert.assertEquals(0.0, overwritten.get(14), .01);
		Assert.assertEquals(3.0, overwritten.get(15), .01);
		Assert.assertEquals(0.0, overwritten.get(16), .01);
		Assert.assertEquals(0.0, overwritten.get(17), .01);
		Assert.assertEquals(0.0, overwritten.get(18), .01);
		Assert.assertEquals(1.0, overwritten.get(19), .01);
		
		overwritten.set(4, 19, overwriting);
		Assert.assertEquals(0.0, overwritten.get(0), .01);
		Assert.assertEquals(1.0, overwritten.get(1), .01);
		Assert.assertEquals(0.0, overwritten.get(2), .01);
		Assert.assertEquals(0.0, overwritten.get(3), .01);
		Assert.assertEquals(0.0, overwritten.get(4), .01);
		Assert.assertEquals(1.0, overwritten.get(5), .01);
		Assert.assertEquals(0.0, overwritten.get(6), .01);
		Assert.assertEquals(3.0, overwritten.get(7), .01);
		Assert.assertEquals(0.0, overwritten.get(8), .01);
		Assert.assertEquals(0.0, overwritten.get(9), .01);
		Assert.assertEquals(0.0, overwritten.get(10), .01);
		Assert.assertEquals(0.0, overwritten.get(11), .01);
		Assert.assertEquals(0.0, overwritten.get(12), .01);
		Assert.assertEquals(0.0, overwritten.get(13), .01);
		Assert.assertEquals(0.0, overwritten.get(14), .01);
		Assert.assertEquals(0.0, overwritten.get(15), .01);
		Assert.assertEquals(0.0, overwritten.get(16), .01);
		Assert.assertEquals(0.0, overwritten.get(17), .01);
		Assert.assertEquals(0.0, overwritten.get(18), .01);
		Assert.assertEquals(0.0, overwritten.get(19), .01);
		
		overwriting = new SparseVector(20, 
				new int[] { 1  }, 
				new double[] { 1.0 });
		
		overwritten.set(1, 2, overwriting);
		Assert.assertEquals(0.0, overwritten.get(0), .01);
		Assert.assertEquals(0.0, overwritten.get(1), .01);
		Assert.assertEquals(1.0, overwritten.get(2), .01);
		Assert.assertEquals(0.0, overwritten.get(3), .01);
		Assert.assertEquals(0.0, overwritten.get(4), .01);
		Assert.assertEquals(1.0, overwritten.get(5), .01);
		Assert.assertEquals(0.0, overwritten.get(6), .01);
		Assert.assertEquals(3.0, overwritten.get(7), .01);
		Assert.assertEquals(0.0, overwritten.get(8), .01);
		Assert.assertEquals(0.0, overwritten.get(9), .01);
		Assert.assertEquals(0.0, overwritten.get(10), .01);
		Assert.assertEquals(0.0, overwritten.get(11), .01);
		Assert.assertEquals(0.0, overwritten.get(12), .01);
		Assert.assertEquals(0.0, overwritten.get(13), .01);
		Assert.assertEquals(0.0, overwritten.get(14), .01);
		Assert.assertEquals(0.0, overwritten.get(15), .01);
		Assert.assertEquals(0.0, overwritten.get(16), .01);
		Assert.assertEquals(0.0, overwritten.get(17), .01);
		Assert.assertEquals(0.0, overwritten.get(18), .01);
		Assert.assertEquals(0.0, overwritten.get(19), .01);
		
		overwriting = new SparseVector(20, 
				new int[] {  }, 
				new double[] { });
		
		overwritten.set(1, 2, overwriting);
		Assert.assertEquals(0.0, overwritten.get(0), .01);
		Assert.assertEquals(0.0, overwritten.get(1), .01);
		Assert.assertEquals(0.0, overwritten.get(2), .01);
		Assert.assertEquals(0.0, overwritten.get(3), .01);
		Assert.assertEquals(0.0, overwritten.get(4), .01);
		Assert.assertEquals(1.0, overwritten.get(5), .01);
		Assert.assertEquals(0.0, overwritten.get(6), .01);
		Assert.assertEquals(3.0, overwritten.get(7), .01);
		Assert.assertEquals(0.0, overwritten.get(8), .01);
		Assert.assertEquals(0.0, overwritten.get(9), .01);
		Assert.assertEquals(0.0, overwritten.get(10), .01);
		Assert.assertEquals(0.0, overwritten.get(11), .01);
		Assert.assertEquals(0.0, overwritten.get(12), .01);
		Assert.assertEquals(0.0, overwritten.get(13), .01);
		Assert.assertEquals(0.0, overwritten.get(14), .01);
		Assert.assertEquals(0.0, overwritten.get(15), .01);
		Assert.assertEquals(0.0, overwritten.get(16), .01);
		Assert.assertEquals(0.0, overwritten.get(17), .01);
		Assert.assertEquals(0.0, overwritten.get(18), .01);
		Assert.assertEquals(0.0, overwritten.get(19), .01);
	}
} 
