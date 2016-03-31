package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.io.File;
import java.io.IOException;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import edu.cmu.ml.rtw.generic.util.Properties;

public class Word2Vec {
	private WordVectors vec;
	
	public Word2Vec(Properties properties) {
		
		File gModel = properties.getWord2VecVectorFile();
		
		try {
			this.vec = WordVectorSerializer.loadGoogleModel(gModel, true);
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to load vectors from file specified in properties");
		}
		
	}
	
	public double computeSimilarity(String str1, String str2) {
		return this.vec.similarity(str1, str2);
	}
	
	public double[] computeVector(String str) {
		return this.vec.getWordVector(str);
	}
}
