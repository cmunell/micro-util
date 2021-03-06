package edu.cmu.ml.rtw.generic.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.DataTools.StringPairIndicator;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsable;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

public class WeightedStructureGraph extends WeightedStructure {
	public static enum RelationMode {
		SINGLE,
		MULTI
	}
	
	public static enum OverwriteOperator {
		MAX,
		CONSERVE
	}
	
	private RelationMode edgeMode = RelationMode.SINGLE;
	private RelationMode nodeMode = RelationMode.SINGLE;
	private OverwriteOperator overwriteOperator = OverwriteOperator.MAX;
	private StringPairIndicator edgeMutexFn = null;  // FIXME: Note that this only works with conserve mode, but should also work with max
	private static String[] parameterNames = { "edgeMode", "nodeMode", "overwriteOperator", "edgeMutexFn" };
	
	private Map<String, Map<WeightedStructureRelationUnary, Pair<Double, Object>>> nodes;
	private Map<String, Map<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>>> edges;
	private Context context;
	private int itemCount = 0;
	private double totalWeight = 0.0;
	
	public WeightedStructureGraph() {
		this(null);
	}
	
	public WeightedStructureGraph(Context context) {
		this.context = context;
		this.nodes = new HashMap<String, Map<WeightedStructureRelationUnary, Pair<Double, Object>>>();
		this.edges = new HashMap<String, Map<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>>>();
	}
	
	public WeightedStructureGraph(Context context, RelationMode edgeMode, RelationMode nodeMode, OverwriteOperator overwriteOperator, StringPairIndicator edgeMutexFn) {
		this(context);
		this.edgeMode = edgeMode;
		this.nodeMode = nodeMode;
		this.overwriteOperator = overwriteOperator;
		this.edgeMutexFn = edgeMutexFn;
	}

	@Override
	public int getItemCount() {
		return this.itemCount;
	}
	
	@Override
	public double getTotalWeight() {
		return this.totalWeight;
	}
	
	@Override
	public boolean remove(CtxParsable item) {
		if (item instanceof WeightedStructureRelationBinary) {
			WeightedStructureRelationBinary edge = (WeightedStructureRelationBinary)item;
			if (!hasEdge(edge))
				return false;
			String id1 = edge.getFirst().getId();
			String id2 = edge.getSecond().getId();
			
			this.totalWeight -= this.edges.get(id1).get(id2).get(edge).getFirst();
			this.edges.get(id1).get(id2).remove(edge);
			this.itemCount--;
			if (this.edges.get(id1).get(id2).size() == 0) {
				this.edges.get(id1).remove(id2);
				if (this.edges.get(id1).size() == 0)
					this.edges.remove(id1);
			}
			
			if (!edge.isOrdered()) {
				WeightedStructureRelationBinary reverseEdge = edge.getReverse();
				String rid1 = reverseEdge.getFirst().getId();
				String rid2 = reverseEdge.getSecond().getId();
				
				this.totalWeight -= this.edges.get(rid1).get(rid2).get(reverseEdge).getFirst();
				this.edges.get(rid1).get(rid2).remove(reverseEdge);
				this.itemCount--;
				if (this.edges.get(rid1).get(rid2).size() == 0) {
					this.edges.get(rid1).remove(rid2);
					if (this.edges.get(rid1).size() == 0)
						this.edges.remove(rid1);
				}
			} 
		} else {
			WeightedStructureRelationUnary node = (WeightedStructureRelationUnary)item;
			if (!hasNode(node))
				return false;
			
			this.totalWeight -= this.nodes.get(node.getId()).get(node).getFirst();
			this.nodes.get(node.getId()).remove(node);
			if (this.nodes.get(node.getId()).size() == 0)
				this.nodes.remove(node.getId());
			this.itemCount--;
			
		}
		
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public WeightedStructure add(CtxParsable item, double w, Object source, Collection<?> changes) {
		if (item instanceof WeightedStructureRelationBinary) {
			addEdge((WeightedStructureRelationBinary)item, w, source, (Collection<WeightedStructureRelation>)changes);
		} else if (item instanceof WeightedStructureRelationUnary) {
			addNode((WeightedStructureRelationUnary)item, w, source, (Collection<WeightedStructureRelation>)changes);
		}
		
		return this;
	}
	
	private boolean addEdge(WeightedStructureRelationBinary edge, double w, Object source, Collection<WeightedStructureRelation> changes) {
		String id1 = edge.getFirst().getId();
		String id2 = edge.getSecond().getId();
		boolean changed = true;
		if (this.edgeMode == RelationMode.MULTI && this.overwriteOperator == OverwriteOperator.CONSERVE) {
			changed = addEdgeMultiConserve(id1, id2, edge, w, source);
		} else if (this.edgeMode == RelationMode.MULTI && this.overwriteOperator == OverwriteOperator.MAX) {
			changed = addEdgeMultiMax(id1, id2, edge, w, source);
		} else if (this.edgeMode == RelationMode.SINGLE && this.overwriteOperator == OverwriteOperator.CONSERVE) {
			changed = addEdgeSingleConserve(id1, id2, edge, w, source);
		} else if (this.edgeMode == RelationMode.SINGLE && this.overwriteOperator == OverwriteOperator.MAX) {
			changed = addEdgeSingleMax(id1, id2, edge, w, source);
		} else {
			throw new UnsupportedOperationException();
		}
		
		if (changed) {
			if (changes != null)
				changes.add(edge);
		}
		
		return changed;
	}
	
	private boolean addEdgeMultiMax(String id1, String id2, WeightedStructureRelationBinary edge, double w, Object source) {
		if (hasEdge(edge)) {
			if (Double.compare(w, getEdgeWeight(edge)) > 0) {
				return replaceEdge(id1, id2, edge, w, source);
			} else {
				return false;
			}
		} else {
			return putEdge(id1, id2, edge, w, source);
		}
	}
	
	private boolean addEdgeMultiConserve(String id1, String id2, WeightedStructureRelationBinary edge, double w, Object source) {
		if (hasEdge(edge))
			return false;
		return putEdge(id1, id2, edge, w, source);
	}
	
	private boolean addEdgeSingleMax(String id1, String id2, WeightedStructureRelationBinary edge, double w, Object source) {
		if (edge.isOrdered()) {
			if (hasEdge(id1, id2)) {
				WeightedStructureRelationBinary currentEdge = this.edges.get(id1).get(id2).keySet().iterator().next();
				if (Double.compare(w, getEdgeWeight(currentEdge)) > 0) {
					remove(currentEdge);
					return putEdge(id1, id2, edge, w, source);
				} else {
					return false;
				}
			} else {
				return putEdge(id1, id2, edge, w, source);
			}
		} else {
			WeightedStructureRelationBinary currentEdge = null;
			WeightedStructureRelationBinary currentReverseEdge = null;
			if (hasEdge(id1, id2)) {
				currentEdge = this.edges.get(id1).get(id2).keySet().iterator().next();
				if (Double.compare(w, getEdgeWeight(currentEdge)) <= 0)
					return false;
			}
				
			if (hasEdge(id2, id1)) {
				currentReverseEdge = this.edges.get(id2).get(id1).keySet().iterator().next();
				if (Double.compare(w, getEdgeWeight(currentReverseEdge)) <= 0)
					return false;
			}
				
			if (currentEdge != null)
				remove(currentEdge);
			if (currentReverseEdge != null)
				remove(currentReverseEdge);
			
			return putEdge(id1, id2, edge, w, source);
		}
	}
	
	private boolean addEdgeSingleConserve(String id1, String id2, WeightedStructureRelationBinary edge, double w, Object source) {
		if (hasEdge(id1, id2, edge))
			return false;
		else if (!edge.isOrdered() && hasEdge(id2, id1, edge))
			return false;
		else 
			return putEdge(id1, id2, edge, w, source);
	}
	
	private boolean replaceEdge(String id1, String id2, WeightedStructureRelationBinary edge, double w, Object source) {
		this.totalWeight += w - this.edges.get(id1).get(id2).get(edge).getFirst();
		this.edges.get(id1).get(id2).put(edge, new Pair<>(w, source));
		if (!edge.isOrdered()) {
			this.totalWeight += w - this.edges.get(id2).get(id1).get(edge).getFirst();
			this.edges.get(id2).get(id1).put(edge.getReverse(), new Pair<>(w, source));
		}
		
		return true;
	}
	
	private boolean putEdge(String id1, String id2, WeightedStructureRelationBinary edge, double w, Object source) {
		if (!this.edges.containsKey(id1))
			this.edges.put(id1, new HashMap<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>>());
		if (!this.edges.get(id1).containsKey(id2))
			this.edges.get(id1).put(id2, new HashMap<WeightedStructureRelationBinary, Pair<Double, Object>>());
		this.edges.get(id1).get(id2).put(edge, new Pair<>(w, source));
		this.itemCount++;
		this.totalWeight += w;
		
		if (!edge.isOrdered()) {
			if (!this.edges.containsKey(id2))
				this.edges.put(id2, new HashMap<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>>());
			if (!this.edges.get(id2).containsKey(id1))
				this.edges.get(id2).put(id1, new HashMap<WeightedStructureRelationBinary, Pair<Double, Object>>());
			this.edges.get(id2).get(id1).put(edge.getReverse(), new Pair<>(w, source));
			this.itemCount++;
			this.totalWeight += w;
		}
		
		return true;
	}
	
	private boolean addNode(WeightedStructureRelationUnary node, double w, Object source, Collection<WeightedStructureRelation> changes) {
		boolean changed = false;
		if (this.nodeMode == RelationMode.MULTI && this.overwriteOperator == OverwriteOperator.CONSERVE) {
			changed = addNodeMultiConserve(node, w, source);
		} else if (this.nodeMode == RelationMode.MULTI && this.overwriteOperator == OverwriteOperator.MAX) {
			changed = addNodeMultiMax(node, w, source);
		} else if (this.nodeMode == RelationMode.SINGLE && this.overwriteOperator == OverwriteOperator.CONSERVE) {
			changed = addNodeSingleConserve(node, w, source);
		} else if (this.nodeMode == RelationMode.SINGLE && this.overwriteOperator == OverwriteOperator.MAX) {
			changed = addNodeSingleMax(node, w, source);
		} else {
			throw new UnsupportedOperationException();
		}
		
		if (changed) {
			if (changes != null)
				changes.add(node);
		}
		return changed;
	}
	
	private boolean addNodeMultiMax(WeightedStructureRelationUnary node, double w, Object source) {
		if (hasNode(node)) {
			if (Double.compare(w, getNodeWeight(node)) > 0)
				return replaceNode(node, w, source);
			else
				return false;
		} else {
			return putNode(node, w, source);
		}
	}
	
	private boolean addNodeMultiConserve(WeightedStructureRelationUnary node, double w, Object source) {
		if (hasNode(node))
			return false;
		return putNode(node, w, source);
	}
	
	private boolean addNodeSingleMax(WeightedStructureRelationUnary node, double w, Object source) {
		if (hasNode(node)) {
			if (Double.compare(w, getNodeWeight(node)) > 0)
				return replaceNode(node, w, source);
			else 
				return false;
		} else if (this.nodes.containsKey(node.getId())) {
			WeightedStructureRelationUnary currentNode = this.nodes.get(node.getId()).keySet().iterator().next();
			if (Double.compare(w, getNodeWeight(currentNode)) > 0) {
				remove(currentNode);
				return putNode(node, w, source);
			} else {
				return false;
			}
		} else {
			return putNode(node, w, source);
		}

	}
	
	private boolean addNodeSingleConserve(WeightedStructureRelationUnary node, double w, Object source) {
		if (this.nodes.containsKey(node.getId()))
			return false;
		return putNode(node, w, source);
	}
	
	private boolean replaceNode(WeightedStructureRelationUnary node, double w, Object source) {
		this.nodes.get(node.getId()).put(node, new Pair<>(w, source));
		return true;
	}
	
	private boolean putNode(WeightedStructureRelationUnary node, double w, Object source) {
		if (!this.nodes.containsKey(node.getId()))
			this.nodes.put(node.getId(), new HashMap<WeightedStructureRelationUnary, Pair<Double, Object>>());
		this.nodes.get(node.getId()).put(node, new Pair<>(w, source));
		this.itemCount++;
		return true;
	}

	@Override
	public double getWeight(CtxParsable item) {
		if (item instanceof WeightedStructureRelationBinary) {
			WeightedStructureRelationBinary edge = (WeightedStructureRelationBinary)item;
			Double weight = getEdgeWeight(edge);
			if (weight == null)
				throw new IllegalArgumentException();
			return weight;
		} else {
			WeightedStructureRelationUnary node = (WeightedStructureRelationUnary)item;
			Double weight = getNodeWeight(node);
			if (weight == null)
				throw new IllegalArgumentException();
			return weight;
		}
	}
	
	@Override
	public Object getSource(CtxParsable item) {
		if (item instanceof WeightedStructureRelationBinary) {
			WeightedStructureRelationBinary edge = (WeightedStructureRelationBinary)item;
			return getEdgeSource(edge);
		} else {
			WeightedStructureRelationUnary node = (WeightedStructureRelationUnary)item;
			return getNodeSource(node);
		}
	}

	@Override
	public WeightedStructure merge(WeightedStructure s) {
		if (!(s instanceof WeightedStructureGraph))
			throw new IllegalArgumentException();
		
		WeightedStructureGraph g = (WeightedStructureGraph)s;
		for (Entry<String, Map<WeightedStructureRelationUnary, Pair<Double, Object>>> entry : g.nodes.entrySet()) {
			for (Entry<WeightedStructureRelationUnary, Pair<Double, Object>> entry2 : entry.getValue().entrySet()) {
				g = (WeightedStructureGraph)add(entry2.getKey(), entry2.getValue().getFirst(), entry2.getValue().getSecond());
			}
		}
		
		for (Entry<String, Map<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>>> entry : g.edges.entrySet()) {
			for (Entry<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>> entry2 : entry.getValue().entrySet()) {
				for (Entry<WeightedStructureRelationBinary, Pair<Double, Object>> entry3 : entry2.getValue().entrySet()) {
					g = (WeightedStructureGraph)add(entry3.getKey(), entry3.getValue().getFirst(), entry3.getValue().getSecond());
				}
			}
		}
		
		return g;
	}

	@Override
	public String getGenericName() {
		return "Graph";
	}

	@Override
	public WeightedStructure makeInstance(Context context) {
		return new WeightedStructureGraph(context);
	}

	@Override
	public String[] getParameterNames() {
		return parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("edgeMode"))
			return Obj.stringValue(this.edgeMode.toString());
		else if (parameter.equals("nodeMode"))
			return Obj.stringValue(this.nodeMode.toString());
		else if (parameter.equals("overwriteOperator"))
			return Obj.stringValue(this.overwriteOperator.toString());
		else if (parameter.equals("edgeMutexFn"))
			return this.edgeMutexFn == null ? null : Obj.stringValue(this.edgeMutexFn.toString());
		else 
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("edgeMode"))
			this.edgeMode = RelationMode.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("nodeMode"))
			this.nodeMode = RelationMode.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("overwriteOperator"))
			this.overwriteOperator = OverwriteOperator.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("edgeMutexFn"))
			this.edgeMutexFn = parameterValue == null ? null : this.context.getDataTools().getStringPairIndicatorFn(this.context.getMatchValue(parameterValue));
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
	
	public boolean hasNode(WeightedStructureRelationUnary node) {
		return this.nodes.containsKey(node.getId());
	}
	
	public boolean hasEdge(WeightedStructureRelationBinary edge) {
		String id1 = edge.getFirst().getId();
		String id2 = edge.getSecond().getId();
		return this.edges.containsKey(id1) &&
			   this.edges.get(id1).containsKey(id2) &&
			   this.edges.get(id1).get(id2).containsKey(edge);
	}
	
	public Double getEdgeWeight(WeightedStructureRelationBinary edge) {
		if (!hasEdge(edge))
			return null;
		return this.edges.get(edge.getFirst().getId()).get(edge.getSecond().getId()).get(edge).getFirst();
	}
	
	public Object getEdgeSource(WeightedStructureRelationBinary edge) {
		if (!hasEdge(edge))
			return null;
		return this.edges.get(edge.getFirst().getId()).get(edge.getSecond().getId()).get(edge).getSecond();
	}
	
	public Double getNodeWeight(WeightedStructureRelationUnary node) {
		if (!hasNode(node))
			return null;
		return this.nodes.get(node.getId()).get(node).getFirst();
	}

	public Object getNodeSource(WeightedStructureRelationUnary node) {
		if (!hasNode(node))
			return null;
		return this.nodes.get(node.getId()).get(node).getSecond();
	}
	
	public boolean hasNode(String id) {
		return this.nodes.containsKey(id);
	}
	
	public boolean hasEdge(String id1, String id2) {
		return this.edges.containsKey(id1) && this.edges.get(id1).containsKey(id2);
	}
	
	public boolean hasEdge(String id1, String id2, WeightedStructureRelationBinary edge) {
		if (!this.edges.containsKey(id1) || !this.edges.get(id1).containsKey(id2))
			return false;
		
		if (this.edgeMutexFn == null)
			return true;
		
		Map<WeightedStructureRelationBinary, Pair<Double, Object>> existingEdges = this.edges.get(id1).get(id2);
		for (WeightedStructureRelationBinary existingEdge : existingEdges.keySet()) {
			if (this.edgeMutexFn.compute(existingEdge.getType(), edge.getType()))
				return true;
		}
		
		return false;	
	}
	
	public boolean hasEdgeFrom(String id) {
		return this.edges.containsKey(id);
	}
	
	public List<WeightedStructureRelationBinary> getEdges(String id1, String id2) {
		List<WeightedStructureRelationBinary> edges = new ArrayList<WeightedStructureRelationBinary>();
		if (!hasEdge(id1, id2))
			return edges;
		edges.addAll(this.edges.get(id1).get(id2).keySet());
		return edges;
	}
	
	public List<WeightedStructureRelationUnary> getNodes(String id) {
		List<WeightedStructureRelationUnary> nodes = new ArrayList<>();
		if (!hasNode(id))
			return nodes;
		nodes.addAll(this.nodes.get(id).keySet());
		return nodes;
	}
	
	// FIXME This assumes filter given for edges only contains edges and not nodes
	private List<WeightedStructureSequence> getEdgesAsPaths(Set<String> ignoreTypes, Collection<WeightedStructureRelation> edges) {
		List<WeightedStructureSequence> paths = new ArrayList<WeightedStructureSequence>();
		
		for (WeightedStructureRelation edge : edges) {
			if (!hasEdge((WeightedStructureRelationBinary)edge))
				continue;
			
			if (ignoreTypes != null && ignoreTypes.contains(edge.getType()))
				continue;
			
			
			WeightedStructureSequence seq = new WeightedStructureSequence(this.context);
			seq.add(edge, getWeight(edge), getSource(edge));
			paths.add(seq);
		}
		
		return paths;
	}
	
	// FIXME This assumes filter given for edges only contains edges and not nodes
	private List<WeightedStructureSequence> getLengthTwoPathsContainingEdges(Set<String> ignoreTypes, Collection<WeightedStructureRelation> edges) {
		List<WeightedStructureSequence> paths = new ArrayList<WeightedStructureSequence>();
		
		for (WeightedStructureRelation rel : edges) {
			WeightedStructureRelationBinary edge = (WeightedStructureRelationBinary)rel;
			if (!hasEdge(edge))
				continue;
			
			if (ignoreTypes != null && ignoreTypes.contains(edge.getType()))
				continue;
			
			// Get all paths starting with this edge
			Map<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>> nextEdges = new HashMap<>();
			if (this.edges.containsKey(edge.getSecond().getId()))
				nextEdges.putAll(this.edges.get(edge.getSecond().getId()));
			if (!edge.isOrdered() && this.edges.containsKey(edge.getFirst().getId()))
				nextEdges.putAll(this.edges.get(edge.getFirst().getId()));
			if (nextEdges != null) {
				for (Entry<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>> entry : nextEdges.entrySet()) {
					for (Entry<WeightedStructureRelationBinary, Pair<Double, Object>> entry2 : entry.getValue().entrySet()) {
						if (ignoreTypes != null && ignoreTypes.contains(entry2.getKey().getType()))
							continue;
						
						WeightedStructureSequence seq = new WeightedStructureSequence(this.context);
						
						if (entry2.getKey().getFirst().getId().equals(edge.getSecond().getId())) {
							seq.add(edge, getWeight(edge), getSource(edge));
						} else {
							seq.add(edge.getReverse(), getWeight(edge), getSource(edge));
						}
						
						seq.add(entry2.getKey(), entry2.getValue().getFirst(), entry2.getValue().getSecond());
						paths.add(seq);
					}
				}
			}
			
			// Get all paths ending with this edge
			for (Entry<String, Map<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>>> entry : this.edges.entrySet()) {
				for (Entry<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>> entry2 : entry.getValue().entrySet()) {
					if (!entry2.getKey().equals(edge.getFirst().getId()) && (edge.isOrdered() || !entry2.getKey().equals(edge.getSecond().getId())))
						continue;
					for (Entry<WeightedStructureRelationBinary, Pair<Double, Object>> entry3 : entry2.getValue().entrySet()) {
						if (ignoreTypes != null && ignoreTypes.contains(entry3.getKey().getType()))
							continue;
						if (entry3.getKey().equals(edge) || (!entry3.getKey().isOrdered() && edges.contains(entry3.getKey())))
							continue;
						WeightedStructureSequence seq = new WeightedStructureSequence(this.context);
						seq.add(entry3.getKey(), entry3.getValue().getFirst(), entry3.getValue().getSecond());
						
						if (entry2.getKey().equals(edge.getFirst().getId()))
							seq.add(edge, getWeight(edge), getSource(edge));
						else
							seq.add(edge.getReverse(), getWeight(edge), getSource(edge));
						paths.add(seq);
					}
				}
			}
		}
		
		return paths;
	}
	
	private List<WeightedStructureSequence> getEdgePaths(String startNodeId, int length, List<WeightedStructureSequence> paths, Set<String> ignoreTypes, Collection<WeightedStructureRelation> filter) {
		List<WeightedStructureSequence> currentPaths = new ArrayList<WeightedStructureSequence>();
		
		Map<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>> neighbors = this.edges.get(startNodeId);
		for (Entry<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>> entry : neighbors.entrySet()) {
			for (Entry<WeightedStructureRelationBinary, Pair<Double, Object>> edge : entry.getValue().entrySet()) {
				if (ignoreTypes != null && ignoreTypes.contains(edge.getKey().getType()))
					continue;
				WeightedStructureSequence seq = new WeightedStructureSequence(this.context);
				seq.add(edge.getKey(), edge.getValue().getFirst(), edge.getValue().getSecond());
				
				currentPaths.add(seq);
			}
		}
		
		for (int i = 1; i < length; i++) {
			List<WeightedStructureSequence> nextPaths = new ArrayList<WeightedStructureSequence>();
			for (WeightedStructureSequence currentPath : currentPaths) {
				String currentNodeId = ((WeightedStructureRelationBinary)currentPath.get(currentPath.size() - 1)).getSecond().getId();
				neighbors = this.edges.get(currentNodeId);
				if (neighbors != null) {
					for (Entry<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>> entry : neighbors.entrySet()) {
						for (Entry<WeightedStructureRelationBinary, Pair<Double, Object>> edge : entry.getValue().entrySet()) {
							if (ignoreTypes != null && ignoreTypes.contains(edge.getKey().getType()))
								continue;
							WeightedStructureSequence seq = currentPath.clone();
							seq.add(edge.getKey(), edge.getValue().getFirst(), edge.getValue().getSecond());
							nextPaths.add(seq);
						}
					}
				}
			}
			
			currentPaths = nextPaths;
		}
		
		if (filter != null) {
			for (WeightedStructureSequence path : currentPaths) {
				boolean matchesFilter = false;
				for (int i = 0; i < path.getItemCount(); i++) {
					if (filter.contains(path.get(i))) {
						matchesFilter = true;
						break;
					}
				}
				
				if (matchesFilter) {
					synchronized (paths) {
						paths.add(path);
					}
				}
			}
		} else {
			synchronized (paths) {
				paths.addAll(currentPaths);
			}
		}
		
		return paths;
	}
	
	public List<WeightedStructureSequence> getEdgePaths(int length) {
		return getEdgePaths(length, null);
	}
	
	public List<WeightedStructureSequence> getEdgePaths(int length, Set<String> ignoreTypes) {
		return getEdgePaths(length, ignoreTypes, null);
	}
	
	public List<WeightedStructureSequence> getEdgePaths(int length, Set<String> ignoreTypes, Collection<WeightedStructureRelation> filter) {
		List<WeightedStructureSequence> paths = new ArrayList<WeightedStructureSequence>();
		if (length <= 0)
			return paths;
		
		if (length == 1 && filter != null) {
			return getEdgesAsPaths(ignoreTypes, filter);
		} else if (length == 2 && filter != null && filter.size() < 5) {
			return getLengthTwoPathsContainingEdges(ignoreTypes, filter);
		}
		
		for (String nodeId : this.edges.keySet()) {
			getEdgePaths(nodeId, length, paths, ignoreTypes, filter);
		}
		
		return paths;
	}
	
	public List<WeightedStructureSequence> getOpenTriangles() {
		return getOpenTriangles(null);
	}

	public List<WeightedStructureSequence> getOpenTriangles(Set<String> ignoreTypes) {
		return getOpenTriangles(ignoreTypes, null);
	}
	
	public List<WeightedStructureSequence> getOpenTriangles(Set<String> ignoreTypes, Collection<WeightedStructureRelation> filter) {
		List<WeightedStructureSequence> paths = getEdgePaths(2, ignoreTypes, filter);
		List<WeightedStructureSequence> openPaths = new ArrayList<>();
		
		for (WeightedStructureSequence path : paths) {
			String sourceId = ((WeightedStructureRelationBinary)path.get(0)).getFirst().getId();
			String targetId = ((WeightedStructureRelationBinary)path.get(1)).getSecond().getId();
			
			if (!hasEdge(sourceId, targetId))
				openPaths.add(path);
		}
		
		return openPaths;
	}
	
	public Set<String> getClusterIds(String nodeId, Set<String> validRelationTypes) {
		Set<String> visited = new HashSet<String>();
		Stack<String> toVisit = new Stack<String>();
		toVisit.add(nodeId);
		while (!toVisit.isEmpty()) {
			String current = toVisit.pop();
			visited.add(current);
			
			Set<String> neighbors = getNeighborIds(current, validRelationTypes);
			for (String neighbor : neighbors) {
				if (!visited.contains(neighbor)) {
					toVisit.push(neighbor);
					visited.add(neighbor);
				}
			}
		}
		
		return visited;
	}
	
	public Set<String> getNeighborIds(String nodeId, Set<String> validRelationTypes) {
		Set<String> neighbors = new HashSet<String>();
		if (!this.edges.containsKey(nodeId))
			return neighbors;
		Map<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>> allNeighbors = this.edges.get(nodeId);
		for (Entry<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>> neighborEntry : allNeighbors.entrySet()) {
			boolean hasValidRelation = false;
			for (Entry<WeightedStructureRelationBinary, Pair<Double, Object>> relationEntry : neighborEntry.getValue().entrySet()) {
				if (validRelationTypes.contains(relationEntry.getKey().getType())) {
					hasValidRelation = true;
					break;
				}
			}
			
			if (hasValidRelation)
				neighbors.add(neighborEntry.getKey());
		}
		
		return neighbors;
	}

	@Override
	public List<CtxParsable> toList() {
		List<CtxParsable> list = new ArrayList<CtxParsable>();
		
		for (Map<WeightedStructureRelationUnary, Pair<Double, Object>> map : this.nodes.values())
			for (WeightedStructureRelationUnary rel : map.keySet())
				list.add(rel);
		
		for (Map<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>> value : this.edges.values()) {
			for (Map<WeightedStructureRelationBinary, Pair<Double, Object>> edgeEntry : value.values()) {
				list.addAll(edgeEntry.keySet());
			}
		}
		
		return list;
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("Size: " + this.itemCount + "\n");
		for (Entry<String, Map<WeightedStructureRelationUnary, Pair<Double, Object>>> entry : this.nodes.entrySet()) {
			for (Entry<WeightedStructureRelationUnary, Pair<Double, Object>> entry2 : entry.getValue().entrySet()) {
				str.append(entry.getKey() + " " + entry2.getKey().getType() + " " + entry2.getValue() + "\n");
			}
		}
		
		for (Entry<String, Map<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>>> entry : this.edges.entrySet()) {
			for (Entry<String, Map<WeightedStructureRelationBinary, Pair<Double, Object>>> entry2 : entry.getValue().entrySet()) {
				for (Entry<WeightedStructureRelationBinary, Pair<Double, Object>> entry3 : entry2.getValue().entrySet()) {
					str.append(entry.getKey() + " " + entry2.getKey() + " " + entry3.getKey().getType() + " " + entry3.getValue() + "\n");
				}
			}
		}
		
		return str.toString();
	}
}
