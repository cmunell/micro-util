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

package edu.cmu.ml.rtw.generic.cluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.util.CommandRunner;
import edu.cmu.ml.rtw.generic.util.OutputWriter;

/**
 * 
 * ClustererBrown is a wrapper for the Brown clustering implementation from
 * https://github.com/percyliang/brown-cluster.
 * 
 * FIXME: This currently takes a pre-cleaned document with special characters
 * removed and numbers replaced with "[NUMBER]".  The cleaning step should be
 * added to this class.
 * 
 * @author Bill McDowell 
 * 
 */
public class ClustererStringBrown extends ClustererString {
	private String name;
	private File sourceDocument;
	private int numClusters;
	private String cmdPath;
	private OutputWriter output;
	
	private String sourceName;
	private File clusterDocument;
	
	private Map<String, String> wordsToClusters;
	
	public ClustererStringBrown(String name, String cmdPath, File sourceDocument, int numClusters, OutputWriter output, DataTools.StringTransform cleanFn) {
		super(cleanFn);
		this.name = name;
		this.sourceDocument = sourceDocument;
		this.numClusters = numClusters;
		this.cmdPath = cmdPath;
		this.output = output;
		
		if (sourceDocument.getName().contains("."))
			this.sourceName = sourceDocument.getName().split(".")[0];
		else
			this.sourceName = sourceDocument.getName();
		this.clusterDocument = new File(this.sourceDocument.getParentFile().getAbsolutePath(), 
										this.sourceName + "-c" + this.numClusters + "-p1.out/paths");
		this.wordsToClusters = null;
	}
	
	public String getName() {
		return this.name;
	}
	
	public synchronized List<String> getClustersHelper(String word) {
		if (this.wordsToClusters == null)
			if (!loadWordsToClusters())
				return null;	
		
		if (!this.wordsToClusters.containsKey(word))
			return null;
		List<String> clusters = new ArrayList<String>();
		clusters.add(this.wordsToClusters.get(word));
		return clusters;
	}
	
	public synchronized Map<String, String> getClusterMap() {
		if (this.wordsToClusters == null)
			if (!loadWordsToClusters())
				return null;
		
		return this.wordsToClusters;
	}
	
	private synchronized boolean loadWordsToClusters() {
		if (!this.clusterDocument.exists())
			if (!runClustering())
				return false;
		
		if (!this.clusterDocument.exists())
			return false;
		
		this.wordsToClusters = new HashMap<String, String>();
		
		this.output.debugWriteln("Loading clusters for Brown clusterer " + this.name + "...");
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(this.clusterDocument));
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] lineParts = line.split("\t");
				if (lineParts.length < 3) {
					br.close();
					this.wordsToClusters = null;
					return false;
				}
				String cluster = lineParts[0];
				String word = lineParts[1];
				this.wordsToClusters.put(word, cluster);
			}
			br.close();
		} catch (Exception e) {
			this.wordsToClusters = null;
			e.printStackTrace();
			return false;
		}
		
		this.output.debugWriteln("Loaded " + this.wordsToClusters.size() + " clusters for Brown clusterer " + this.name + ".");
		
		return true;
	}
	
	private boolean runClustering() {
		String clusteringCmd = this.cmdPath + " --text " + this.sourceDocument.getAbsolutePath() + " --c " + this.numClusters;
		clusteringCmd = clusteringCmd.replace("\\", "/");
		
		this.output.debugWriteln("Running Brown clustering " + this.name + "...");
		
		if (!CommandRunner.run(clusteringCmd, this.sourceDocument.getParentFile()))
			return false;
		
		this.output.debugWriteln("Finished running Brown clustering.");
		
		return true;
	}
}
