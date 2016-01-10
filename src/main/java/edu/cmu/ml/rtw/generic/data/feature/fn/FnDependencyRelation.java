package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse.Dependency;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnDependencyRelation takes a collection of token spans
 * and returns (single token) spans that are
 * connected with them by a single relation
 * in a dependency parse.
 * 
 * Parameters:
 *  mode - determines whether parent or
 *  child dependencies are included in the output
 *  
 *  spanMaxLength - maximum output token span length
 * 
 * @author Bill McDowell
 * 
 */
public class FnDependencyRelation extends Fn<TokenSpan, TokenSpan> {
	public enum Mode {
		PARENTS,
		CHILDREN,
		PARENTS_AND_CHILDREN
	}
	
	private Context<?, ?> context;
	
	private String[] parameterNames = { "mode" };
	private Mode mode = Mode.PARENTS_AND_CHILDREN;
	
	public FnDependencyRelation() {
		
	}
	
	public FnDependencyRelation(Context<?, ?> context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("mode"))
			return Obj.stringValue(String.valueOf(this.mode));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("mode"))
			this.mode = Mode.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<TokenSpan>> C compute(Collection<TokenSpan> input, C output) {
		for (TokenSpan span : input) {
			DocumentNLP document = span.getDocument();
			DependencyParse parse = document.getDependencyParse(span.getSentenceIndex());
		
			for (int i = span.getStartTokenIndex(); i < span.getEndTokenIndex(); i++) {
				if (this.mode == Mode.PARENTS || this.mode == Mode.PARENTS_AND_CHILDREN) {
					List<Dependency> dependencies = parse.getGoverningDependencies(i);
					for (Dependency dependency : dependencies)
						output.add(new TokenSpan(document, span.getSentenceIndex(), dependency.getGoverningTokenIndex(), dependency.getGoverningTokenIndex() + 1));
				} 
				
				if (this.mode == Mode.CHILDREN || this.mode == Mode.PARENTS_AND_CHILDREN) {
					List<Dependency> dependencies = parse.getGovernedDependencies(i);
					for (Dependency dependency : dependencies)
						output.add(new TokenSpan(document, span.getSentenceIndex(), dependency.getDependentTokenIndex(), dependency.getDependentTokenIndex() + 1));
					
				}
			}
		}
		
		return output;
	}

	@Override
	public Fn<TokenSpan, TokenSpan> makeInstance(
			Context<?, ?> context) {
		return new FnDependencyRelation(context);
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		return null;
	}
 
	@Override
	public String getGenericName() {
		return "DependencyRelation";
	}
	
	@Override
	public List<String> computeRelations(TokenSpan input, TokenSpan output) {
		List<String> relations = new ArrayList<String>();
		
		for (int i = input.getStartTokenIndex(); i < input.getEndTokenIndex(); i++) {
			for (int j = output.getStartTokenIndex(); j < output.getEndTokenIndex(); j++) {
				DocumentNLP document = input.getDocument();
				DependencyParse parse = document.getDependencyParse(input.getSentenceIndex());
				Dependency dependency = parse.getDependency(i, j);
				Dependency dependencyReverse = parse.getDependency(j, i);
				if (dependency != null)
					relations.add(this.getReferenceName() + "P" + dependency.getType());
				else if (dependencyReverse != null)
					relations.add(this.getReferenceName() + "C" + dependencyReverse.getType());
			}
		}
		
		return relations;
	}
}
