package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse.Dependency;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.util.OutputWriter;

public class DependencyParseTest {
	@Test
	public void testPath() {
		PipelineNLPStanford stanfordPipe = new PipelineNLPStanford();
		stanfordPipe.initialize(AnnotationTypeNLP.NER);
		
		DataTools dataTools = new DataTools(new OutputWriter());
		DocumentNLP document = new DocumentNLPInMemory(dataTools, 
				"theDocument", 
				"Jim learned to read at school. It was horrible, but he had to do it anyway.",
				Language.English, 
				stanfordPipe);
		
		DependencyParse parse = document.getDependencyParse(0);

		Assert.assertEquals("mark", parse.getPath(3-1, 5-1).getDependencyType(0));
		Assert.assertEquals("nmod", parse.getPath(3-1, 5-1).getDependencyType(1));
		Assert.assertEquals("case", parse.getPath(3-1, 5-1).getDependencyType(2));
		Assert.assertEquals("nsubj", parse.getPath(3-1, 1-1).getDependencyType(2));
	}
	
	@Test
	public void testDepsAndGovs() {
		PipelineNLPStanford stanfordPipe = new PipelineNLPStanford();
		stanfordPipe.initialize(AnnotationTypeNLP.NER);
		
		DataTools dataTools = new DataTools(new OutputWriter());
		DocumentNLP document = new DocumentNLPInMemory(dataTools, 
				"theDocument", 
				"Jim learned to read at school. It was horrible, but he had to do it anyway.",
				Language.English, 
				stanfordPipe);
		
		DependencyParse parse = document.getDependencyParse(0);
		System.out.println(parse);
	
		List<Dependency> deps = parse.getGovernedDependencies(4-1);
		Assert.assertEquals("mark", deps.get(0).getType());
		Assert.assertEquals("nmod", deps.get(1).getType());
	}
}
