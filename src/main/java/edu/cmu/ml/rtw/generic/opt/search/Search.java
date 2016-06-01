package edu.cmu.ml.rtw.generic.opt.search;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.parse.Assignment.AssignmentTyped;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.Triple;

public abstract class Search extends CtxParsableFunction {
	protected Context context;

	protected String[] parameterNames = { };
	
	protected List<Dimension> dimensions;
	protected Map<Position, Pair<ParameterSearchable, Double>> evaluations;
	
	public Search() {
		this(null);
	}
	
	public Search(Context context) {
		this.context = context;
		this.dimensions = new ArrayList<Dimension>();
		//this.evaluations = new ConcurrentHashMap<Position, Pair<ParameterSearchable, Double>>();
		this.evaluations = new ConcurrentSkipListMap<Position, Pair<ParameterSearchable, Double>>();
	}
	
	protected abstract List<Position> getNextPositions();
	public abstract Search makeInstance(Context context);
	
	private ParameterSearchable setFnParameters(ParameterSearchable fn, Position position) {
		for (Dimension dimension : position.getCoordinates().keySet())
			if (!fn.setParameterValue(dimension.getReferenceName(), position.getDimensionValue(dimension)))
				return fn;
		return fn;
	}
	
	public Context getContext() {
		return this.context;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		DecimalFormat cleanDouble = new DecimalFormat("0.00000");
		for (Entry<Position, Pair<ParameterSearchable, Double>> entry : this.evaluations.entrySet()) {
			str.append(entry.getKey().toKeyValueString("\t", ": "))
					.append("\t")
					.append(cleanDouble.format(entry.getValue().getSecond()))
					.append("\n");
		}
		
		return str.toString();
	}
	
	public List<Dimension> getDimensions() {
		return this.dimensions;
	}
	
	public Set<Position> getEvaluatedPositions() {
		return this.evaluations.keySet();
	}
	
	public ParameterSearchable getPositionFn(Position position) {
		if (!this.evaluations.containsKey(position))
			throw new IllegalArgumentException();

		ParameterSearchable fn = this.evaluations.get(position).getFirst().clone();
		return setFnParameters(fn, position);
	}
	
	public double getPositionEvaluation(Position position) {
		if (!this.evaluations.containsKey(position))
			throw new IllegalArgumentException();
		return this.evaluations.get(position).getSecond();
	}
	
	
	public Position getBestPosition() {
		double maxValue = Double.NEGATIVE_INFINITY;
		Position maxPosition = null;
		for (Entry<Position, Pair<ParameterSearchable, Double>> entry : this.evaluations.entrySet()) {
			if (entry.getValue().getSecond() > maxValue) {
				maxValue = entry.getValue().getSecond();
				maxPosition = entry.getKey();
			}
		}
		
		return maxPosition;
	}
	
	public boolean run(ParameterSearchable fn) {
		List<Position> positions = getNextPositions();
		if (positions == null)
			return false;

		Map<Position, ParameterSearchable> previousStageFns = new ConcurrentHashMap<Position, ParameterSearchable>();
		previousStageFns.put(new Position(this.context), fn.clone());
		
		for (int i = 0; i < fn.getStageCount(); i++) {
			PositionStageThreadFn threadFn = new PositionStageThreadFn(i);
			ThreadMapper<Triple<List<Position>, ParameterSearchable, Boolean>, Boolean> threadMapper = 
				new ThreadMapper<>(threadFn);
			
			List<Triple<List<Position>, ParameterSearchable, Boolean>> threadInputs = new ArrayList<>();
			Set<Position> subPositions = getStageSubPositions(i, positions);
			if (i != fn.getStageCount() - 1 || fn.lastStageRequiresCloning()) {
				for (Entry<Position, ParameterSearchable> entry : previousStageFns.entrySet()) {
					for (Position position : subPositions) {
						if (entry.getKey().isSubPositionOf(position)) {
							List<Position> pList = new ArrayList<Position>();
							pList.add(position);
							threadInputs.add(new Triple<>(pList, entry.getValue(), true));
						}
					}
				}
			} else {
				for (Entry<Position, ParameterSearchable> entry : previousStageFns.entrySet()) {
					List<Position> entryStagePositions = new ArrayList<Position>();
					for (Position position : subPositions) {
						if (entry.getKey().isSubPositionOf(position)) {
							entryStagePositions.add(position);
						}
					}

					threadInputs.add(new Triple<>(entryStagePositions, entry.getValue(), false));
				}
			}
			
			List<Boolean> results = threadMapper.run(threadInputs, this.context.getMaxThreads());
			for (Boolean result : results)
				if (!result)
					return false;
		
			previousStageFns = threadFn.getPositionResults();
		}
		
		
		ThreadMapper<Entry<Position, ParameterSearchable>, Boolean> threadMapper = 
				new ThreadMapper<>(new PositionEvaluationThreadFn());
		threadMapper.run(previousStageFns.entrySet(), this.context.getMaxThreads());
		
		return true;
	}
	
	private Set<Position> getStageSubPositions(int stageIndex, List<Position> positions) {
		Set<Position> subPositions = new HashSet<Position>();
		
		for (Position position : positions) {
			subPositions.add(position.getSubPositionUpTo(stageIndex));
		}
		
		return subPositions;
	}
	
	private class PositionStageThreadFn implements ThreadMapper.Fn<Triple<List<Position>, ParameterSearchable, Boolean>, Boolean> {
		private Map<Position, ParameterSearchable> positionResults;
		private int stageIndex;
		
		public PositionStageThreadFn(int stageIndex) {
			this.positionResults = new ConcurrentHashMap<Position, ParameterSearchable>();
			this.stageIndex = stageIndex;
		}
		
		public Map<Position, ParameterSearchable> getPositionResults() {
			return this.positionResults;
		}
		
		@Override
		public Boolean apply(Triple<List<Position>, ParameterSearchable, Boolean> item) {
			for (Position position : item.getFirst()) {
				ParameterSearchable fn = item.getSecond();
				if (item.getThird())
					fn = fn.clone();
				
				for (Entry<Dimension, Obj> parameter : position.getCoordinates().entrySet())
					if (!fn.setParameterValue(parameter.getKey().getReferenceName(), parameter.getValue()))
						return false;
				
				if (!fn.runStage(this.stageIndex))
					return false;
				
				this.positionResults.put(position, fn);
			}
			
			return true;
		}
	}

	private class PositionEvaluationThreadFn implements ThreadMapper.Fn<Entry<Position, ParameterSearchable>, Boolean> {
		@Override
		public Boolean apply(Entry<Position, ParameterSearchable> item) {
			synchronized (item.getValue()) { // This is necessary when the last stage doesn't clone
				double evaluation = item.getValue().evaluate();
				Search.this.evaluations.put(item.getKey(), 
						new Pair<ParameterSearchable, Double>(item.getValue(), evaluation));
				
				return true;
			}
		}
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		return true;
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		for (int i = 0; i < internalAssignments.size(); i++) {
			AssignmentTyped assignment = (AssignmentTyped)internalAssignments.get(i);
			if (assignment.getType().equals(Dimension.DIMENSION_STR)) {
				Dimension dimension = new DimensionEnumerated(this.context); // FIXME Other dimesnion types available later
				if (!dimension.fromParse(assignment.getModifiers(), assignment.getName(), assignment.getValue()))
					return false;
				this.dimensions.add(dimension);
			} 
		}
		
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		AssignmentList assignmentList = new AssignmentList();
	
		for (Dimension dimension : this.dimensions) {
			assignmentList.add(
				Assignment.assignmentTyped(null, Dimension.DIMENSION_STR, dimension.getReferenceName(), dimension.toParse())
			);
		}
			
		return assignmentList;
	}
	
	public Search clone(String referenceName) {
		Search clone = this.makeInstance(this.context);
		if (!clone.fromParse(this.modifiers, referenceName, toParse()))
			return null;
		return clone;
	}
}
