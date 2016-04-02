package edu.cmu.ml.rtw.generic.model.annotator.nlp.mateplus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import se.lth.cs.srl.CompletePipeline;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.io.CoNLL09Writer;
import se.lth.cs.srl.io.SentenceWriter;
import se.lth.cs.srl.options.CompletePipelineCMDLineOptions;
import se.lth.cs.srl.util.ChineseDesegmenter;
import se.lth.cs.srl.util.FileExistenceVerifier;

public class MatePlus {
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
	
	public MatePlus() {
		String lemmaModel = "/data_reitter/nlp_tools/mateplus/models/CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model";
		String parserModel = "/data_reitter/nlp_tools/mateplus/models/CoNLL2009-ST-English-ALL.anna-3.3.parser.model";
		String taggerModel = "/data_reitter/nlp_tools/mateplus/models/CoNLL2009-ST-English-ALL.anna-3.3.postagger.model";
		String srlModel = "/data_reitter/nlp_tools/mateplus/models/CoNLL2009-ST-English-ALL.anna-3.3.srl-4.1.srl.model";
		//String srlModel = "/data_reitter/nlp_tools/mateplus/models/srl-EMNLP14+fs-eng.model";
		String inputText = "John baked a cake.\n  Sally ate it for dinner.\n John frustratedly threw his fork at Sally.\n Jim flew his spaceship to Texas.\n Debra went to the store on Sunday.\n The game starts at 5:00 in Madrid.";
		run(lemmaModel, parserModel, taggerModel, srlModel, inputText);
	}
	
	private void run(String lemmaModel, String parserModel, String taggerModel, String srlModel, String inputText) {
		String[] args = {
				"eng",
				"-lemma",
				lemmaModel,
				"-parser",
				parserModel,
				"-tagger",
				taggerModel,
				"-srl",
				srlModel,
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
			System.exit(1);
		}

		try {
			CompletePipeline pipeline = CompletePipeline.getCompletePipeline(options);
			
			BufferedReader in = new BufferedReader(new StringReader(inputText));
			
			SentenceWriter writer = new CoNLL09Writer(options.output);
	
			if (options.loadPreprocessorWithTokenizer) {
				parseNonSegmentedLineByLine(options, pipeline, in);
			} else {
				parseCoNLL09(options, pipeline, in);
			}
	
			in.close();
			writer.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.err.println();
			System.err.println("ERROR: Mate plus failed to load models.");
			System.exit(1);
		}
	}
	
	private int parseNonSegmentedLineByLine(
			CompletePipelineCMDLineOptions options, CompletePipeline pipeline,
			BufferedReader in) throws IOException,
			Exception {
		int senCount = 0;
		String str;

		while ((str = in.readLine()) != null) {
			Sentence s = pipeline.parse(str);
			outputSentence(s);
			senCount++;
			if (senCount % 100 == 0)
				System.out.println("Processing sentence " + senCount); // TODO,
																		// same
																		// as
																		// below.
		}

		return senCount;
	}

	private int parseCoNLL09(CompletePipelineCMDLineOptions options,
			CompletePipeline pipeline, BufferedReader in)
			throws IOException, Exception {
		List<String> forms = new ArrayList<String>();
		forms.add("<root>");
		List<Boolean> isPred = new ArrayList<Boolean>();
		isPred.add(false);
		String str;
		int senCount = 0;

		while ((str = in.readLine()) != null) {
			if (str.trim().equals("")) {
				Sentence s;
				if (options.desegment) {
					s = pipeline.parse(ChineseDesegmenter.desegment(forms
							.toArray(new String[0])));
				} else {
					s = options.skipPI ? pipeline.parseOraclePI(forms, isPred)
							: pipeline.parse(forms);
				}
				forms.clear();
				forms.add("<root>");
				isPred.clear();
				isPred.add(false); // Root is not a predicate
				outputSentence(s);
				senCount++;
				if (senCount % 100 == 0) { // TODO fix output in general, don't
											// print to System.out. Wrap a
											// printstream in some (static)
											// class, and allow people to adjust
											// this. While doing this, also add
											// the option to make the output
											// file be -, ie so it prints to
											// stdout. All kinds of errors
											// should goto stderr, and nothing
											// should be printed to stdout by
											// default
					System.out.println("Processing sentence " + senCount);
				}
			} else {
				String[] tokens = WHITESPACE_PATTERN.split(str);
				forms.add(tokens[1]);
				if (options.skipPI)
					isPred.add(tokens[12].equals("Y"));
			}
		}

		if (forms.size() > 1) { // We have the root token too, remember!
			Sentence sentence = pipeline.parse(forms);
			outputSentence(sentence);
			senCount++;
		}
		return senCount;
	}
	
	private void outputSentence(Sentence s) {
		System.out.println(s);
		
		for (Predicate p : s.getPredicates()) {
			System.out.println("  Sense: " + p.getSense() + " Index: " + p.getIdx() + " " + p.getForm() + " ");
			Map<Word, String> args = p.getArgMap();

			for (Entry<Word, String> arg : args.entrySet()) {
				System.out.println("    Arg " + arg.getKey().getIdx() + " " + arg.getKey().getLemma() +	" Tag: " + arg.getValue() + " ");
				
			}
			System.out.println("  " + p);
		}
	}
}
