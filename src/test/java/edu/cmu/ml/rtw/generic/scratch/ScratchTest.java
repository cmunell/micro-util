package edu.cmu.ml.rtw.generic.scratch;

import java.io.IOException;

import edu.cmu.ml.rtw.generic.util.StringUtil;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;


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

	public void testLibSVM() {
		int dataCount = 100;
		
		svm_problem prob = new svm_problem();
		prob.y = new double[dataCount];
		prob.l = dataCount;
		prob.x = new svm_node[dataCount][];     
		
		//Random r = new Random();
		
		for (int i = 0; i < dataCount; i++){  
			prob.x[i] = new svm_node[2];
			
			svm_node f0 = new svm_node();
			f0.index = 0;
			f0.value = 1.0;// + (r.nextDouble() - .5)*.05;
			prob.x[i][0] = f0;
			
			svm_node f1 = new svm_node();
			f1.index = 1;
			f1.value = .5;// + (r.nextDouble() - .5)*.05;
			prob.x[i][1] = f1;
			
			prob.y[i] = 1.0;
		}      
		
		svm_parameter param = new svm_parameter();
	    param.probability = 1; // Determines whether p is estimated
	    param.gamma = 0.5; // Adjust
	    param.nu = 0.5; // Adjust
	    param.C = 1;
	    param.svm_type = svm_parameter.ONE_CLASS;
	    param.kernel_type = svm_parameter.LINEAR;       
	    param.cache_size = 20000;
	    param.eps = 0.001;     
		
		svm_model model = svm.svm_train(prob, param);
		
		
		// Evaluate
		
		svm_node[] pinstance = new svm_node[2];
		svm_node f0 = new svm_node();
		f0.index = 0;
		f0.value = 1.0;
		pinstance[0] = f0;
		svm_node f1 = new svm_node();
		f1.index = 1;
		f1.value = .5;
		pinstance[1] = f1;
		
		svm_node[] ninstance = new svm_node[2];
		svm_node f0n = new svm_node();
		f0.index = 0;
		f0.value = -100.0;
		ninstance[0] = f0n;
		svm_node f1n = new svm_node();
		f1.index = 1;
		f1.value = 1000.0;
		ninstance[1] = f1n;

	    int totalClasses = 2;       
	    int[] labels = new int[totalClasses];
	    svm.svm_get_labels(model,labels);

	    try {
			model = (svm_model)StringUtil.deserializeFromBase64String(StringUtil.serializeToBase64String(model));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    double[] prob_estimates = new double[totalClasses];
	    double v = svm.svm_predict_probability(model, pinstance, prob_estimates);
	    
	    for (int i = 0; i < totalClasses; i++){
	        System.out.print("(" + labels[i] + ":" + prob_estimates[i] + ")");
	    }
	    System.out.println("(Actual:" + 1 + " Prediction:" + v + ")");  
	    
	    prob_estimates = new double[totalClasses];
	    v = svm.svm_predict_probability(model, ninstance, prob_estimates);
	    
	    for (int i = 0; i < totalClasses; i++){
	        System.out.print("(" + labels[i] + ":" + prob_estimates[i] + ")");
	    }
	    System.out.println("(Actual:" + -1 + " Prediction:" + v + ")");  
	}
	
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
