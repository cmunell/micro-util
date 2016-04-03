package edu.cmu.ml.rtw.generic.model.annotator.nlp.matetools;

import java.io.File;
import java.util.Map;

import edu.cmu.ml.rtw.generic.util.Properties;
import se.lth.cs.srl.CompletePipeline;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.options.CompletePipelineCMDLineOptions;
import se.lth.cs.srl.util.FileExistenceVerifier;

public class MateTools {
	private CompletePipeline pipeline;
	
	public MateTools(Properties properties) {
		Map<String, File> modelFiles = properties.getMateToolsModelFiles();
		
		String[] args = {
				"eng",
				"-lemma",
				modelFiles.get("lemma").getAbsolutePath(),
				"-parser",
				modelFiles.get("parser").getAbsolutePath(),
				"-tagger",
				modelFiles.get("tagger").getAbsolutePath(),
				"-srl",
				modelFiles.get("srl").getAbsolutePath(),
				//"-reranker",
				"-tokenize",
				"-test",
				"" // Input file path? I think.
			};
		
		CompletePipelineCMDLineOptions options = new CompletePipelineCMDLineOptions();
		options.parseCmdLineArgs(args);
		String error = FileExistenceVerifier
				.verifyCompletePipelineAllNecessaryModelFiles(options);
		if (error != null) {
			System.err.println(error);
			System.err.println();
			System.err.println("ERROR: Mate plus failed to load models.");
			throw new IllegalArgumentException();
		}
		
		try {
			this.pipeline = CompletePipeline.getCompletePipeline(options);
		} catch (Exception e) {
			System.err.println("ERROR: Mate plus failed to construct pipeline.");
			throw new IllegalArgumentException();
		}
	}
	
	public Sentence parseSentence(String sentenceStr) throws Exception {
		return this.pipeline.parse(sentenceStr);
	}
}
