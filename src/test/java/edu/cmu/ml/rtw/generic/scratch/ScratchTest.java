package edu.cmu.ml.rtw.generic.scratch;

/*
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;

import org.junit.Test;

import edu.cmu.ml.rtw.generic.util.StringUtil;
import weka.classifiers.Classifier;
import weka.classifiers.meta.OneClassClassifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.SerializationHelper;
import weka.core.SparseInstance;
import weka.classifiers.functions.LibSVM;*/

public class ScratchTest {
	/*@Test
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
	}*/
	/*
	@Test
	public void wekaTest() {
		// Declare two numeric attributes
		Attribute f1 = new Attribute("f1");
		Attribute f2 = new Attribute("f2");

		// Declare the class attribute along with its values
		ArrayList<String> outputClassValues = new ArrayList<String>();
		outputClassValues.add("target");
		//outputClassValues.add("outlier");
		Attribute outputClass = new Attribute("output", outputClassValues);
		
		ArrayList<Attribute> attrs = new ArrayList<Attribute>();
		attrs.add(f1);
		attrs.add(f2);
		attrs.add(outputClass);
		
		Instances dataSet = new Instances("data", attrs, 10);
		dataSet.setClassIndex(2);
		
		Random r = new Random();
		for (int i = 0; i < 100; i++) {
			Instance instance = new SparseInstance(3);
			instance.setValue(attrs.get(0), 1.0 + (r.nextDouble() - .5)*.05);
			instance.setValue(attrs.get(1), 0.5 + (r.nextDouble() - .5)*.05);
			instance.setValue(attrs.get(2), "target");
			dataSet.add(instance);
		}
		 
		Instances testSet = new Instances("data", attrs, 10);
		testSet.setClassIndex(2);
		
		Instance testInstance = new SparseInstance(3);
		testInstance.setValue(attrs.get(0), 1.0);
		testInstance.setValue(attrs.get(1), 0.5);
		//testInstance.setValue(attrs.get(2), "target");
		testSet.add(testInstance);
		testInstance.setDataset(testSet);
		
		LibSVM svm = new LibSVM();
		svm.setSVMType( new SelectedTag(Integer.parseInt("2"), LibSVM.TAGS_SVMTYPE));
		//OneClassClassifier x = new OneClassClassifier();
		
		//x.setSeed(1);
		//x.setTargetClassLabel("target");
		try {
			svm.buildClassifier(dataSet);
			double[][] p = svm.distributionsForInstances(testSet);
			System.out.println(p[0][0]);// + " " + p[0][1]);
			System.out.println(Double.compare(svm.classifyInstance(testInstance), 0.0));
		
		} catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage());
		}
		
		String os = null;
		try {
			os = StringUtil.serializeToBase64String(svm);
			LibSVM cls = (LibSVM)StringUtil.deserializeFromBase64String(os);
			double[][] p = cls.distributionsForInstances(testSet);
			System.out.println(p[0][0] + " " + p[0][1]);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}*/
}
