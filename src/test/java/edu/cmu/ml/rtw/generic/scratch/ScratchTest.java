package edu.cmu.ml.rtw.generic.scratch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.platanios.learn.math.matrix.SparseVector;
import org.platanios.learn.math.matrix.Vector.VectorElement;

public class ScratchTest {
	@Test
	public void testSparseVector() {
		Map<Integer, Double> v1Map = new HashMap<Integer, Double>();
		v1Map.put(1, 1.0);
		v1Map.put(20000, 3.0);
		SparseVector v1 = new SparseVector(Integer.MAX_VALUE, v1Map);
		
		Map<Integer, Double> v2Map = new HashMap<Integer, Double>();
		v2Map.put(1, 1.0);
		v2Map.put(20000, 3.0);
		v2Map.put(8, 1.0);
		v2Map.put(3232, 3.0);
		SparseVector v2 = new SparseVector(Integer.MAX_VALUE, v2Map);
	
		v1 = v1.add(v2);
		
		for (VectorElement e : v1) {
			System.out.println(e.index() + ", " + e.value());
			Assert.assertEquals(e.value(), v1.get(e.index()), .0001);
		}
	}
}
