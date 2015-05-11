/**
 * Copyright 2014 Bill McDowell 
 *
 * This file is part of theMess (https://github.com/forkunited/theMess)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 */

package edu.cmu.ml.rtw.generic.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.CommandRunner;
import edu.cmu.ml.rtw.generic.util.OutputWriter;

/**
 * SupervisedModelCreg is a wrapper for the creg 
 * (https://github.com/redpony/creg) logistic 
 * regression implementation.  The train and posterior methods write the given
 * data set to a file that creg can handle, call the creg command, and read
 * in the files that creg outputs.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class SupervisedModelCreg<D extends Datum<L>, L> extends SupervisedModel<D, L> {
	// Actual paths are stored in DataTools from a properties configuration file
	// and these paths are referred to by their reference names in the experiment
	// configuration files
	private DataTools.Path cmdPath; // path to creg command
	private DataTools.Path modelPath; // path to creg model output
	private double l1;
	private double l2;
	private boolean warmRestart;
	private String[] hyperParameterNames = { "cmdPath", "modelPath", "l1", "l2", "warmRestart" };

	public SupervisedModelCreg() {
		
	}
	
	public SupervisedModelCreg(Context<D, L> context) {
		this.context = context;
	}
	
	@Override
	public boolean train(FeaturizedDataSet<D, L> data, FeaturizedDataSet<D, L> testData, List<SupervisedModelEvaluation<D, L>> evaluations) {
		OutputWriter output = data.getDatumTools().getDataTools().getOutputWriter();
		
		String trainXPath = this.modelPath.getValue() + ".train.x";
		String trainYPath = this.modelPath.getValue() + ".train.y";
		
		output.debugWriteln("Creg outputting training data (" + this.modelPath.getName() + ")");
		
		if (!outputXData(trainXPath, data, true))
			return false;
		if (!outputYData(trainYPath, data))
			return false;
		
		output.debugWriteln("Creg training model (" + this.modelPath.getName() + ")");
		
		File outputFile = new File(this.modelPath.getValue());
		
		String trainCmd = this.cmdPath.getValue() + 
						" -x " + trainXPath + 
						" -y " + trainYPath + 
						" --l1 " + this.l1 + 
						" --l2 " + this.l2 + 
						((this.warmRestart && outputFile.exists()) ? " --weights " + this.modelPath.getValue() : "") +
						" --z " + this.modelPath.getValue();
		trainCmd = trainCmd.replace("\\", "/"); 
		if (!CommandRunner.run(trainCmd))
			return false;
		
		output.debugWriteln("Creg finished training model (" + this.modelPath.getName() + ")");
		
		return true;
	}

	@Override
	public Map<D, Map<L, Double>> posterior(FeaturizedDataSet<D, L> data) {
		String predictPPath = predict(data);
		if (predictPPath == null)
			return null;
		else 
			return loadPData(predictPPath, data, false);
	}
	
	private boolean outputXData(String outputPath, FeaturizedDataSet<D, L> data, boolean requireLabels) {
        try {
    		BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
  
    		for (D datum : data) {
    			L label = mapValidLabel(datum.getLabel());
    			if (requireLabels && label == null)
    				continue;
    			
    			Map<Integer, Double> featureValues = data.getFeatureVocabularyValuesAsMap(datum);
    			Map<Integer, String> featureNames = data.getFeatureVocabularyNamesForIndices(featureValues.keySet());
    			
    			StringBuilder datumStr = new StringBuilder();
    			datumStr = datumStr.append("id").append(datum.getId()).append("\t{");
    			for (Entry<Integer, Double> feature : featureValues.entrySet()) {
    				datumStr = datumStr.append("\"")
    								   .append(featureNames.get(feature.getKey()))
    								   .append("\": ")
    								   .append(feature.getValue())
    								   .append(", ");
    			}
    			
    			if (featureValues.size() > 0) {
    				datumStr = datumStr.delete(datumStr.length() - 2, datumStr.length());
    			}
    			
				datumStr = datumStr.append("}");
				
				writer.write(datumStr.toString());
				writer.write("\n");
    		}    		
    		
            writer.close();
            return true;
        } catch (IOException e) { e.printStackTrace(); return false; }
	}
	
	private boolean outputYData(String outputPath, FeaturizedDataSet<D, L> data) {
        try {
    		BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

    		for (D datum : data) {
    			L label = mapValidLabel(datum.getLabel());
    			if (label == null)
    				continue;
    			
    			StringBuilder labelStr = new StringBuilder();
    			labelStr = labelStr.append("id")
    							   .append(datum.getId())
    							   .append("\t")
    							   .append(label.toString());
    			
				writer.write(labelStr.toString());
				writer.write("\n");
    		}    		
    		
            writer.close();
            return true;
        } catch (IOException e) { e.printStackTrace(); return false; }
	}

	private Map<D, Map<L, Double>> loadPData(String path, FeaturizedDataSet<D, L> data, boolean requireLabels) {
		Map<D, Map<L, Double>> pData = new HashMap<D, Map<L, Double>>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			
			for (D datum : data) {
				L label = mapValidLabel(datum.getLabel());
    			if (requireLabels && label == null)
    				continue;
				
				String line = br.readLine();
				if (line == null) {
					br.close();
					return null;
				}
					
				String[] lineParts = line.split("\t");
				if (lineParts.length < 3) {
					br.close();
					return null;
				}
				
				if (!lineParts[0].equals("id" + datum.getId())) {
					br.close();
					return null;
				}
				
				JSONObject jsonPosterior = new JSONObject(lineParts[2]);
				Map<L, Double> posterior = new HashMap<L, Double>();
				for (L validLabel : this.validLabels) {
					String labelStr = validLabel.toString();
					if (jsonPosterior.has(labelStr))
						posterior.put(validLabel, jsonPosterior.getDouble(labelStr));
					else 
						posterior.put(validLabel, 0.0);
				}
				
				pData.put(datum, posterior);
			}
	        
	        br.close();
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	return null;
	    }
		
		return pData;
	}
	
	private String predict(FeaturizedDataSet<D, L> data) {
		OutputWriter output = data.getDatumTools().getDataTools().getOutputWriter();
		String predictXPath = this.modelPath.getValue() + ".predict.x";
		String predictOutPath = this.modelPath.getValue() + ".predict.y";
		
		output.debugWriteln("Creg outputting prediction data (" + this.modelPath.getName() + ")");
		
		if (!outputXData(predictXPath, data, false)) {
			output.debugWriteln("Error: Creg failed to output feature data (" + this.modelPath.getName() + ")");
			return null;
		}
		
		String predictCmd = this.cmdPath.getValue() + " -w " + this.modelPath.getValue() + " -W -D --tx " + predictXPath + " > " + predictOutPath;
		predictCmd = predictCmd.replace("\\", "/"); 
		if (!CommandRunner.run(predictCmd)) {
			output.debugWriteln("Error: Creg failed to run on output data (" + this.modelPath.getName() + ")");
			return null;
		}
		
		output.debugWriteln("Creg predicting data (" + this.modelPath.getName() + ")");
		
		return predictOutPath;
	}

	@Override
	public String getGenericName() {
		return "Creg";
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("cmdPath"))
			return Obj.stringValue(((this.cmdPath == null) ? "" : this.cmdPath.getName()));
		else if (parameter.equals("modelPath"))
			return Obj.stringValue((this.modelPath == null) ? "" : this.modelPath.getName());
		else if (parameter.equals("l1"))
			return Obj.stringValue(String.valueOf(this.l1));
		else if (parameter.equals("l2"))
			return Obj.stringValue(String.valueOf(this.l2));
		else if (parameter.equals("warmRestart"))
			return Obj.stringValue(String.valueOf(this.warmRestart));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("cmdPath"))
			this.cmdPath = this.context.getDatumTools().getDataTools().getPath(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("modelPath"))
			this.modelPath = this.context.getDatumTools().getDataTools().getPath(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("l1"))
			this.l1 = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("l2"))
			this.l2 = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("warmRestart"))
			this.warmRestart = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.hyperParameterNames;
	}

	@Override
	public SupervisedModel<D, L> makeInstance(Context<D, L> context) {
		return new SupervisedModelCreg<D, L>(context);
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		TreeMap<Double, List<String>> sortedWeights = new TreeMap<Double, List<String>>();
		File modelFile = new File(this.modelPath.getValue());
		
		if (!modelFile.exists())
			return internalAssignments;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(modelFile));
			String line = null;
			
			while ((line = br.readLine()) != null) {
				String[] lineParts = line.split("\t");
				Double value = null;
				if (lineParts.length < 3)
					continue;
				try {
					value = Math.abs(Double.parseDouble(lineParts[2]));
				} catch (NumberFormatException e) {
					continue;
				}
				
				if (!sortedWeights.containsKey(value))
					sortedWeights.put(value, new ArrayList<String>());
				sortedWeights.get(value).add(line);
			}
	        
	        br.close();
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
		
		NavigableMap<Double, List<String>> descendingWeights = sortedWeights.descendingMap();
		int i = 0;
		for (List<String> lines : descendingWeights.values()) {
			for (String line : lines) {
				internalAssignments.add(
					Assignment.assignmentTyped(null, Context.ARRAY_STR, "w-" + i, Obj.stringValue(line))
				);
				
				i++;
			}
		}
		
		return internalAssignments;
	}

	@Override
	protected <T extends Datum<Boolean>> SupervisedModel<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			SupervisedModel<T, Boolean> binaryModel) {
		return binaryModel;
	}
}
