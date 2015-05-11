package edu.cmu.ml.rtw.generic.data.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;
import edu.cmu.ml.rtw.generic.util.CounterTable;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;

public class FeatureStringForm<D extends Datum<L>, L> extends Feature<D, L> {
	protected BidirectionalLookupTable<String, Integer> vocabulary;
	protected Datum.Tools.StringExtractor<D, L> stringExtractor;
	protected int minFeatureOccurrence;
	protected String[] parameterNames = { "stringExtractor", "minFeatureOccurrence" };
	
	public FeatureStringForm() {
		
	}
	
	public FeatureStringForm(Context<D, L> context) {
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
		this.context = context;
	}
	
	
	@Override
	public boolean init(FeaturizedDataSet<D, L> dataSet) {
		final CounterTable<String> counter = new CounterTable<String>();
		dataSet.map(new ThreadMapper.Fn<D, Boolean>() {
			@Override
			public Boolean apply(D datum) {
				List<String> forms = computeForms(datum);
				for (String form : forms)
					counter.incrementCount(form);

				return true;
			}
		});
		
		counter.removeCountsLessThan(this.minFeatureOccurrence);
		
		this.vocabulary = new BidirectionalLookupTable<String, Integer>(counter.buildIndex());
		
		return true;
	}
	
	
	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		List<String> forms = computeForms(datum);
		
		for (String form : forms) {
			if (!this.vocabulary.containsKey(form))
				continue;
			vector.put(this.vocabulary.get(form) + offset, 1.0);
		}
		
		return vector;
	}

	
	protected List<String> computeForms(D datum) {
		String[] strs = this.stringExtractor.extract(datum);
		List<String> forms = new ArrayList<String>(strs.length);
		
		for (String str : strs) {
			StringBuilder form = new StringBuilder();
			
			char[] characters = str.toCharArray();
			char prevFormCharacter = '\0';
			for (char character : characters) {
				char formCharacter = '\0';
				if (Character.isAlphabetic(character)) {
					if (Character.isLowerCase(character)) {
						formCharacter = 'a';
					} else {
						formCharacter = 'A';
					}
				} else if (Character.isDigit(character)) {
					formCharacter = 'D';
				} else if (Character.isWhitespace(character)) {
					formCharacter = 'w';
				} else {
					formCharacter = 'S';
				}
				
				if (formCharacter != prevFormCharacter)
					form.append(formCharacter);
				prevFormCharacter = formCharacter;
			}
			
			forms.add(form.toString());
		}
		
		return forms;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("minFeatureOccurrence"))
			return Obj.stringValue(String.valueOf(this.minFeatureOccurrence));
		else if (parameter.equals("stringExtractor"))
			return Obj.stringValue((this.stringExtractor == null) ? "" : this.stringExtractor.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("minFeatureOccurrence"))
			this.minFeatureOccurrence = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("stringExtractor"))
			this.stringExtractor = this.context.getDatumTools().getStringExtractor(this.context.getMatchValue(parameterValue));
		else 
			return false;
		return true;
	}
	
	@Override
	public String getVocabularyTerm(int index) {
		if (this.vocabulary == null)
			return null;
		return this.vocabulary.reverseGet(index);
	}

	@Override
	protected boolean setVocabularyTerm(int index, String term) {
		if (this.vocabulary == null)
			return true;
		this.vocabulary.put(term, index);
		return true;
	}

	@Override
	public int getVocabularySize() {
		if (this.vocabulary == null)
			return 1;
		return this.vocabulary.size();
	}

	@Override
	public String getGenericName() {
		return "StringForm";
	}


	@Override
	public Feature<D, L> makeInstance(Context<D, L> context) {
		return new FeatureStringForm<D, L>(context);
	}

	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureStringForm<T, Boolean> binaryFeatureStrForm = (FeatureStringForm<T, Boolean>)binaryFeature;
		
		binaryFeatureStrForm.vocabulary = this.vocabulary;
		
		return binaryFeatureStrForm;
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		return internalAssignments;
	}	
	
	@Override
	protected boolean cloneHelper(Feature<D, L> clone) {
		FeatureStringForm<D, L> cloneStrForm = (FeatureStringForm<D, L>)clone;
		cloneStrForm.vocabulary = this.vocabulary;
		return true;
	}
}
