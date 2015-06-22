package edu.cmu.ml.rtw.generic.model.evaluation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.Assignment.AssignmentTyped;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.OutputWriter;

/**
 * GridSearch performs a grid-search for hyper-parameter values
 * of a given model using a training and test (dev) data
 * set.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class GridSearch<D extends Datum<L>, L> extends CtxParsableFunction {
	private static final String DIMENSION_STR = "dimension";
	
	public static class GridDimension extends CtxParsableFunction {
		private String name = "";
		private Obj.Array values = new Obj.Array();
		private boolean trainingDimension = true;
		
		private Context<?, ?> context;
		
		public GridDimension(Context<?, ?> context) {
			this.context = context;
		}
		
		public boolean isTrainingDimension() {
			return this.trainingDimension;
		}
		
		public Obj.Array getValues() {
			return this.values;
		}
		
		public String getName() {
			return this.name;
		}

		@Override
		public String[] getParameterNames() {
			return new String[] { "name", "values", "trainingDimension" };
		}

		@Override
		public Obj getParameterValue(String parameter) {
			if (parameter.equals("name"))
				return Obj.stringValue(this.name);
			else if (parameter.equals("values"))
				return this.values;
			else if (parameter.equals("trainingDimension"))
				return Obj.stringValue(String.valueOf(this.trainingDimension));
			else
				return null;
		}

		@Override
		public boolean setParameterValue(String parameter, Obj parameterValue) {
			if (parameter.equals("name"))
				this.name = this.context.getMatchValue(parameterValue);
			else if (parameter.equals("values"))
				this.values = (Obj.Array)parameterValue;
			else if (parameter.equals("trainingDimension"))
				this.trainingDimension = Boolean.valueOf(this.context.getMatchValue(parameterValue));
			else
				return false;
			return true;
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
			return "Dimension";
		}
	}
	
	/**
	 * GridPosition represents a position in the grid
	 * of parameter values (a setting of values for the
	 * parameters)
	 * 
	 * @author Bill McDowell
	 *
	 */
	public class GridPosition {
		protected TreeMap<String, Obj> coordinates;
		protected Context<?, ?> context;
		
		public GridPosition(Context<?, ?> context) {
			this.coordinates = new TreeMap<String, Obj>();
			this.context = context;
		}
		
		public Obj getParameterValue(String parameter) {
			return this.coordinates.get(parameter);
		}
		
		public void setParameterValue(String parameter, Obj value) {
			this.coordinates.put(parameter, value);
		}
		
		public Map<String, Obj> getCoordinates() {
			return this.coordinates;
		}
		
		public GridPosition clone() {
			GridPosition clonePosition = new GridPosition(this.context);
			for (Entry<String, Obj> entry : this.coordinates.entrySet())
				clonePosition.setParameterValue(entry.getKey(), entry.getValue());
			return clonePosition;
		}
		
		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append("(");
			for (Entry<String, Obj> entry : this.coordinates.entrySet()) {
				str.append(entry.getKey()).append("=").append(this.context.getMatchValue(entry.getValue())).append(",");
			}
			str.delete(str.length() - 1, str.length());
			str.append(")");
			return str.toString();
		}
		
		public String toValueString(String separator) {
			StringBuilder str = new StringBuilder();
			for (Entry<String, Obj> entry : this.coordinates.entrySet()) {
				str.append(this.context.getMatchValue(entry.getValue())).append(separator);
			}
			str.delete(str.length() - 1, str.length());
			return str.toString();
		}
		
		public String toKeyString(String separator) {
			StringBuilder str = new StringBuilder();
			for (Entry<String, Obj> entry : this.coordinates.entrySet()) {
				str.append(entry.getKey()).append(separator);
			}
			str.delete(str.length() - 1, str.length());
			return str.toString();
		}
		
		public String toKeyValueString(String separator, String keyValueGlue) {
			StringBuilder str = new StringBuilder();
			
			for (Entry<String, Obj> entry : this.coordinates.entrySet()) {
				str.append(entry.getKey())
				   .append(keyValueGlue)
				   .append(this.context.getMatchValue(entry.getValue()))
				   .append(separator);
			}
			
			str.delete(str.length() - 1, str.length());
			
			return str.toString();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object o) {
			GridPosition g = (GridPosition)o;
			
			if (g.coordinates.size() != this.coordinates.size())
				return false;
			
			for (Entry<String, Obj> entry : this.coordinates.entrySet())
				if (!g.coordinates.containsKey(entry.getKey()) || !g.coordinates.get(entry.getKey()).equals(entry.getValue()))
					return false;
			
			return true;
		}
		
		@Override
		public int hashCode() {
			int hashCode = 0;
			
			for (Entry<String, Obj> entry : this.coordinates.entrySet())
				hashCode ^= entry.getKey().hashCode() ^ entry.getValue().hashCode();
			
			return hashCode;
		}
	}
	
	/**
	 * EvaluatedGridPosition is a GridPosition that has been
	 * evaluated according to some measure and given a value
	 * 
	 * @author Bill McDowell
	 *
	 */
	public class EvaluatedGridPosition extends GridPosition {
		private double positionValue;
		private ValidationTrainTest<D, L> validation;
		
		public EvaluatedGridPosition(Context<?, ?> context, GridPosition position, double positionValue, ValidationTrainTest<D, L> validation) {
			super(context);
			this.coordinates = position.coordinates;
			this.positionValue = positionValue;
			this.validation = validation;
		}

		
		public double getPositionValue() {
			return this.positionValue;
		}
		
		public ValidationTrainTest<D, L> getValidation() {
			return this.validation;
		}
	}
	
	private Context<D, L> context;
	private Obj modelObj;
	private Obj evaluationObj;
	
	private FeaturizedDataSet<D, L> trainData;
	private FeaturizedDataSet<D, L> testData;
	
	private List<GridDimension> dimensions;
	// grid of evaluated grid positions (evaluated parameter settings)
	private List<EvaluatedGridPosition> gridEvaluation;
	
	private DecimalFormat cleanDouble;
	
	public GridSearch(Context<D, L> context) {
		this.context = context;
		this.dimensions = new ArrayList<GridDimension>();
		this.cleanDouble = new DecimalFormat("0.00000");
	}
	
	public Context<D, L> getContext() {
		return this.context;
	}
	
	/**
	 * 
	 * @param name
	 * @param model
	 * @param trainData
	 * @param testData
	 * @param dimensions - Grid dimensions and their possible values
	 * @param evaluation - Evaluation measure by which to search
	 */
	public boolean init(FeaturizedDataSet<D, L> trainData, 
					FeaturizedDataSet<D, L> testData) {
		this.trainData = trainData;
		this.testData = testData;
		this.gridEvaluation = null;
		
		return true;
	}
	
	public String toString() {
		List<EvaluatedGridPosition> gridEvaluation = getGridEvaluation();
		StringBuilder gridEvaluationStr = new StringBuilder();
		
		gridEvaluationStr = gridEvaluationStr.append(gridEvaluation.get(0).toKeyString("\t")).append("\t").append(this.evaluationObj.toString()).append("\n");
		for (EvaluatedGridPosition positionEvaluation : gridEvaluation) {
			gridEvaluationStr = gridEvaluationStr.append(positionEvaluation.toValueString("\t"))
							 					 .append("\t")
							 					 .append(this.cleanDouble.format(positionEvaluation.getPositionValue()))
							 					 .append("\n");
		}
		
		return gridEvaluationStr.toString();
	}
	
	public List<GridDimension> getDimensions() {
		return this.dimensions;
	}
	
	public List<EvaluatedGridPosition> getGridEvaluation() {
		return getGridEvaluation(1);
	}
	
	public List<EvaluatedGridPosition> getGridEvaluation(int maxThreads) {
		if (this.gridEvaluation != null)
			return this.gridEvaluation;
		
		this.gridEvaluation = new ArrayList<EvaluatedGridPosition>();
		List<GridPosition> grid = constructGrid();
		
		try {
	 		if (maxThreads > 1) {
	 			ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);
	 			List<PositionThread> tasks = new ArrayList<PositionThread>();
	 	 		for (GridPosition position : grid) {
	 				tasks.add(new PositionThread(position));
	 			}
	 	 		
				
				List<Future<List<EvaluatedGridPosition>>> results = threadPool.invokeAll(tasks);
				threadPool.shutdown();
				threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
				for (Future<List<EvaluatedGridPosition>> futureResult : results) {
					List<EvaluatedGridPosition> result = futureResult.get();
					if (result == null)
						return null;
					this.gridEvaluation.addAll(result);
				}
	 		} else {
	 			for (GridPosition position : grid)
	 				this.gridEvaluation.addAll((new PositionThread(position)).call());
	 		}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return this.gridEvaluation;
	}
	
	public EvaluatedGridPosition getBestPosition() {
		return getBestPosition(1);
	}
	
	public EvaluatedGridPosition getBestPosition(int maxThreads) {
		List<EvaluatedGridPosition> gridEvaluation = getGridEvaluation(maxThreads);
		double maxValue = Double.NEGATIVE_INFINITY;
		EvaluatedGridPosition maxPosition = null;
		
		for (EvaluatedGridPosition position: gridEvaluation) {
			if (position.getPositionValue() > maxValue) {
				maxValue = position.getPositionValue();
				maxPosition = position;
			}
		}
		
		// FIXME This is a hack.  Currently non-training parameters will be same across
		// all resulting models since models aren't cloned for non-training parameters
		maxPosition.getValidation().getModel().setParameterValues(maxPosition.coordinates);
		
		return maxPosition;
	}
	
	private List<GridPosition> constructGrid() {
		return constructGrid(null, true);
	}
	
	private List<GridPosition> constructGrid(GridPosition initialPosition, boolean training) {
		List<GridPosition> positions = new ArrayList<GridPosition>();
		
		if (initialPosition != null) 
			positions.add(initialPosition);
		else
			positions.add(new GridPosition(this.context));
		
		for (GridDimension dimension : this.dimensions) {
			if (dimension.isTrainingDimension() != training)
				continue;
			
			List<GridPosition> newPositions = new ArrayList<GridPosition>();
			
			for (GridPosition position : positions) {
				for (int i = 0; i < dimension.getValues().size(); i++) {
					GridPosition newPosition = position.clone();
					newPosition.setParameterValue(dimension.getName(), dimension.getValues().get(i));
					newPositions.add(newPosition);
				}
			}
			
			positions = newPositions;
		}
		
		return positions;
	}
	
	private class PositionThread implements Callable<List<EvaluatedGridPosition>> {
		private GridPosition position;
		private SupervisedModel<D, L> positionModel;
		private SupervisedModelEvaluation<D, L> positionEvaluation;
		private Context<D, L> context;
		
		public PositionThread(GridPosition position) {
			this.position = position;
			this.context = GridSearch.this.context.clone(false);
			
			for (Entry<String, Obj> entry : this.position.getCoordinates().entrySet())
				this.context.addValue(entry.getKey(), this.context.getMatchValue(entry.getValue()));
			
			this.positionModel = this.context.getMatchModel(GridSearch.this.modelObj);
			this.positionEvaluation = this.context.getMatchEvaluation(GridSearch.this.evaluationObj);
		}
		
		@Override
		public List<EvaluatedGridPosition> call() throws Exception {
			List<GridPosition> positions = constructGrid(this.position, false); // Positions for non-training dimensions
			List<EvaluatedGridPosition> evaluatedPositions = new ArrayList<EvaluatedGridPosition>();
			boolean skipTraining = false;
			for (GridPosition position : positions) {
				evaluatedPositions.add(evaluatePosition(position, skipTraining));
				skipTraining = true;
			}
			
			return evaluatedPositions;
		}
		
		private EvaluatedGridPosition evaluatePosition(GridPosition position, boolean skipTraining) {
			OutputWriter output = trainData.getDatumTools().getDataTools().getOutputWriter();
			
			output.debugWriteln("Grid search evaluating " + GridSearch.this.evaluationObj.toString() + " of model (" + GridSearch.this.referenceName + " " + position.toString() + ")");
			
			this.positionModel.setParameterValues(position.getCoordinates());
			
			List<SupervisedModelEvaluation<D, L>> evaluations = new ArrayList<SupervisedModelEvaluation<D, L>>(1);
			evaluations.add(this.positionEvaluation);
			
			ValidationTrainTest<D, L> validation = new ValidationTrainTest<D, L>(GridSearch.this.referenceName + " " + position.toString(), 1, this.positionModel, trainData, testData, evaluations, null);
			double computedEvaluation = validation.run(skipTraining).get(0);
			if (computedEvaluation  < 0) {
				output.debugWriteln("Error: Grid search evaluation failed at position " + position.toString());
				return null;
			}
			
			output.debugWriteln("Finished grid search evaluating model with hyper parameters (" + GridSearch.this.referenceName + " " + position.toString() + ")");
			
			return new EvaluatedGridPosition(this.context, position, computedEvaluation, validation);
		}
	}

	@Override
	public String[] getParameterNames() {
		return new String[0];
	}

	@Override
	public Obj getParameterValue(String parameter) {
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		return false;
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		for (int i = 0; i < internalAssignments.size(); i++) {
			AssignmentTyped assignment = (AssignmentTyped)internalAssignments.get(i);
			if (assignment.getType().equals(DIMENSION_STR)) {
				GridDimension dimension = new GridDimension(this.context);
				if (!dimension.fromParse(assignment.getModifiers(), assignment.getName(), assignment.getValue()))
					return false;
				this.dimensions.add(dimension);
			} else if (assignment.getType().equals(Context.MODEL_STR)) {
				this.modelObj = assignment.getValue();
			} else if (assignment.getType().equals(Context.EVALUATION_STR)) {
				this.evaluationObj = assignment.getValue();
			}
		}
		
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		AssignmentList assignmentList = new AssignmentList();
	
		for (GridDimension dimension : this.dimensions) {
			assignmentList.add(
				Assignment.assignmentTyped(null, DIMENSION_STR, dimension.getReferenceName(), dimension.toParse())
			);
		}
		
		assignmentList.add(Assignment.assignmentTyped(null, Context.MODEL_STR, Context.MODEL_STR, this.modelObj));
		assignmentList.add(Assignment.assignmentTyped(null, Context.EVALUATION_STR, Context.EVALUATION_STR, this.evaluationObj));
		
		return assignmentList;
	}
	
	public <T extends Datum<Boolean>> GridSearch<T, Boolean> makeBinary(Context<T, Boolean> binaryContext, LabelIndicator<L> labelIndicator) {
		GridSearch<T, Boolean> gridSearch = new GridSearch<T, Boolean>(binaryContext);
		
		gridSearch.referenceName = this.referenceName;
		gridSearch.modelObj = this.context.getMatchModel(this.modelObj).makeBinary(binaryContext, labelIndicator).toParse();
		gridSearch.evaluationObj = this.context.getMatchEvaluation(this.evaluationObj).makeBinary(binaryContext, labelIndicator).toParse();
		
		if (this.trainData != null) {
			gridSearch.trainData = (FeaturizedDataSet<T, Boolean>)this.trainData.makeBinary(labelIndicator, binaryContext);
		}
		
		if (this.testData != null) {
			gridSearch.testData = (FeaturizedDataSet<T, Boolean>)this.testData.makeBinary(labelIndicator, binaryContext);
		}
		
		gridSearch.dimensions = this.dimensions;
		gridSearch.cleanDouble = this.cleanDouble;
		
		return gridSearch;
	}

	@Override
	public String getGenericName() {
		return "GridSearch";
	}
}
