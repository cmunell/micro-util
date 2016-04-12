package edu.cmu.ml.rtw.generic.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsable;
import edu.cmu.ml.rtw.generic.parse.Obj;

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
	private static String[] parameterNames = { "edgeMode", "nodeMode", "overwriteOperator" };
	
	private Map<String, Map<WeightedStructureRelationUnary, Double>> nodes;
	private Map<String, Map<String, Map<WeightedStructureRelationBinary, Double>>> edges;
	private Context context;
	private int itemCount = 0;
	
	public WeightedStructureGraph() {
		this(null);
	}
	
	public WeightedStructureGraph(Context context) {
		this.context = context;
		this.nodes = new HashMap<String, Map<WeightedStructureRelationUnary, Double>>();
		this.edges = new HashMap<String, Map<String, Map<WeightedStructureRelationBinary, Double>>>();
	}

	@Override
	public int getItemCount() {
		return this.itemCount;
	}
	
	@Override
	public boolean remove(CtxParsable item) {
		if (item instanceof WeightedStructureRelationBinary) {
			WeightedStructureRelationBinary edge = (WeightedStructureRelationBinary)item;
			if (!hasEdge(edge))
				return false;
			String id1 = edge.getFirst().getId();
			String id2 = edge.getSecond().getId();
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
			this.nodes.get(node.getId()).remove(node);
			if (this.nodes.get(node.getId()).size() == 0)
				this.nodes.remove(node.getId());
			this.itemCount--;
		}
		
		return true;
	}

	// FIXME This is spaghetti
	@Override
	public WeightedStructure add(CtxParsable item, double w) {
		if (item instanceof WeightedStructureRelationBinary) {
			WeightedStructureRelationBinary edge = (WeightedStructureRelationBinary)item;
			String id1 = edge.getFirst().getId();
			String id2 = edge.getSecond().getId();
			if (hasEdge(edge)) {
				if (this.overwriteOperator == OverwriteOperator.MAX
						&& w >= getEdgeWeight(edge)) {
					this.edges.get(id1).get(id2).put(edge, w);
					if (!edge.isOrdered())
						this.edges.get(id2).get(id1).put(edge.getReverse(), w);
				} else {
					return this;
				}
			} else if (hasEdge(id1, id2)) {
				if (this.edgeMode == RelationMode.SINGLE) {
					WeightedStructureRelationBinary currentEdge = this.edges.get(id1).get(id2).keySet().iterator().next();
					if (this.overwriteOperator == OverwriteOperator.MAX && w >= getEdgeWeight(currentEdge)) {
						if (edge.isOrdered()) {
							if (!remove(currentEdge))
								return this;
							this.edges.get(id1).get(id2).put(edge, w);
						} else {
							if (!hasEdge(id2, id1)) {
								if (!remove(currentEdge))
									return this;
								this.edges.get(id1).get(id2).put(edge, w);
								
								if (!this.edges.containsKey(id2))
									this.edges.put(id2, new HashMap<String, Map<WeightedStructureRelationBinary, Double>>());
								if (!this.edges.get(id2).containsKey(id1))
									this.edges.get(id2).put(id1, new HashMap<WeightedStructureRelationBinary, Double>());
								this.edges.get(id2).get(id1).put(edge.getReverse(), w);
								this.itemCount++;
							} else {
								WeightedStructureRelationBinary currentReverseEdge = this.edges.get(id2).get(id1).keySet().iterator().next();
								if (w >= getWeight(currentReverseEdge)) {
									if (!remove(currentEdge)) 
										return this;
									remove(currentReverseEdge); // Don't check for true because may fail if undirected
									this.edges.get(id1).get(id2).put(edge, w);
									this.edges.get(id2).get(id1).put(edge.getReverse(), w);
								} else {
									return this;
								}
							}
						}
					} else {
						return this;
					}
				} else {
					if (!this.edges.get(id1).get(id2).containsKey(edge))
						this.itemCount++;
					this.edges.get(id1).get(id2).put(edge, w);
					if (!edge.isOrdered()) {
						if (!this.edges.containsKey(id2))
							this.edges.put(id2, new HashMap<String, Map<WeightedStructureRelationBinary, Double>>());
						if (!this.edges.get(id2).containsKey(id1))
							this.edges.get(id2).put(id1, new HashMap<WeightedStructureRelationBinary, Double>());
						if (!this.edges.get(id2).get(id1).containsKey(edge))
							this.itemCount++;
						
						this.edges.get(id2).get(id1).put(edge.getReverse(), w);
					}
				}
			} else {			
				if (edge.isOrdered()) {
					if (!this.edges.containsKey(id1))
						this.edges.put(id1, new HashMap<String, Map<WeightedStructureRelationBinary, Double>>());
					if (!this.edges.get(id1).containsKey(id2))
						this.edges.get(id1).put(id2, new HashMap<WeightedStructureRelationBinary, Double>());
					this.edges.get(id1).get(id2).put(edge, w);
					this.itemCount++;
				} else if (!hasEdge(id2, id1) || this.edgeMode == RelationMode.MULTI) {
					if (!this.edges.containsKey(id1))
						this.edges.put(id1, new HashMap<String, Map<WeightedStructureRelationBinary, Double>>());
					if (!this.edges.get(id1).containsKey(id2))
						this.edges.get(id1).put(id2, new HashMap<WeightedStructureRelationBinary, Double>());
					this.edges.get(id1).get(id2).put(edge, w);
					this.itemCount++;
					
					if (!this.edges.containsKey(id2))
						this.edges.put(id2, new HashMap<String, Map<WeightedStructureRelationBinary, Double>>());
					if (!this.edges.get(id2).containsKey(id1))
						this.edges.get(id2).put(id1, new HashMap<WeightedStructureRelationBinary, Double>());
					if (!this.edges.get(id2).get(id1).containsKey(edge))
						this.itemCount++;
					
					this.edges.get(id2).get(id1).put(edge.getReverse(), w);
				} else { // There's a reverse edge and mode is single
					WeightedStructureRelationBinary currentReverseEdge = this.edges.get(id2).get(id1).keySet().iterator().next();
					if (this.overwriteOperator == OverwriteOperator.MAX && w >= getEdgeWeight(currentReverseEdge)) {
						if (!remove(currentReverseEdge))
							return this;
						if (!this.edges.containsKey(id1))
							this.edges.put(id1, new HashMap<String, Map<WeightedStructureRelationBinary, Double>>());
						if (!this.edges.get(id1).containsKey(id2))
							this.edges.get(id1).put(id2, new HashMap<WeightedStructureRelationBinary, Double>());
						this.edges.get(id1).get(id2).put(edge, w);
						this.itemCount++;
						
						if (!this.edges.containsKey(id2))
							this.edges.put(id2, new HashMap<String, Map<WeightedStructureRelationBinary, Double>>());
						if (!this.edges.get(id2).containsKey(id1))
							this.edges.get(id2).put(id1, new HashMap<WeightedStructureRelationBinary, Double>());
						if (!this.edges.get(id2).get(id1).containsKey(edge))
							this.itemCount++;
						
						this.edges.get(id2).get(id1).put(edge.getReverse(), w);
					} else {
						return this;
					}
				}
			}
		} else {
			WeightedStructureRelationUnary node = (WeightedStructureRelationUnary)item;
			if (hasNode(node)) {
				if (this.overwriteOperator == OverwriteOperator.MAX && w >= getNodeWeight(node))
					this.nodes.get(node.getId()).put(node, w);
				else 
					return this;
			} else {
				if (this.nodes.containsKey(node.getId())) {
					WeightedStructureRelationUnary currentNode = this.nodes.get(node.getId()).keySet().iterator().next();
					if (this.nodeMode == RelationMode.MULTI) {
						this.nodes.get(node.getId()).put(node, w);
					} else if (this.overwriteOperator == OverwriteOperator.MAX && w >= getNodeWeight(currentNode)) {
						this.nodes.get(node.getId()).remove(currentNode);
						this.nodes.get(node.getId()).put(node, w);
					} else
						return this;
				} else {
					this.nodes.put(node.getId(), new HashMap<WeightedStructureRelationUnary, Double>());
					this.nodes.get(node.getId()).put(node, w);
					this.itemCount++;	
				}
			}
		}
		
		return this;
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
	public WeightedStructure merge(WeightedStructure s) {
		if (!(s instanceof WeightedStructureGraph))
			throw new IllegalArgumentException();
		
		WeightedStructureGraph g = (WeightedStructureGraph)s;
		for (Entry<String, Map<WeightedStructureRelationUnary, Double>> entry : g.nodes.entrySet()) {
			for (Entry<WeightedStructureRelationUnary, Double> entry2 : entry.getValue().entrySet()) {
				g = (WeightedStructureGraph)add(entry2.getKey(), entry2.getValue());
			}
		}
		
		for (Entry<String, Map<String, Map<WeightedStructureRelationBinary, Double>>> entry : g.edges.entrySet()) {
			for (Entry<String, Map<WeightedStructureRelationBinary, Double>> entry2 : entry.getValue().entrySet()) {
				for (Entry<WeightedStructureRelationBinary, Double> entry3 : entry2.getValue().entrySet()) {
					g = (WeightedStructureGraph)add(entry3.getKey(), entry3.getValue());
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
		return this.edges.get(edge.getFirst().getId()).get(edge.getSecond().getId()).get(edge);
	}
	
	public Double getNodeWeight(WeightedStructureRelationUnary node) {
		if (!hasNode(node))
			return null;
		return this.nodes.get(node.getId()).get(node);
	}
	
	public boolean hasNode(String id) {
		return this.nodes.containsKey(id);
	}
	
	public boolean hasEdge(String id1, String id2) {
		return this.edges.containsKey(id1) && this.edges.get(id1).containsKey(id2);
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
	
	private List<WeightedStructureSequence> getEdgePaths(String startNodeId, int length, List<WeightedStructureSequence> paths, Set<String> ignoreTypes) {
		List<WeightedStructureSequence> currentPaths = new ArrayList<WeightedStructureSequence>();

		Map<String, Map<WeightedStructureRelationBinary, Double>> neighbors = this.edges.get(startNodeId);
		for (Entry<String, Map<WeightedStructureRelationBinary, Double>> entry : neighbors.entrySet()) {
			for (Entry<WeightedStructureRelationBinary, Double> edge : entry.getValue().entrySet()) {
				if (ignoreTypes != null && ignoreTypes.contains(edge.getKey().getType()))
					continue;
				WeightedStructureSequence seq = new WeightedStructureSequence(this.context);
				seq.add(edge.getKey(), edge.getValue());
				currentPaths.add(seq);
			}
		}
		
		for (int i = 1; i < length; i++) {
			List<WeightedStructureSequence> nextPaths = new ArrayList<WeightedStructureSequence>();
			for (WeightedStructureSequence currentPath : currentPaths) {
				String currentNodeId = ((WeightedStructureRelationBinary)currentPath.get(currentPath.size() - 1)).getSecond().getId();
				neighbors = this.edges.get(currentNodeId);
				if (neighbors != null) {
					for (Entry<String, Map<WeightedStructureRelationBinary, Double>> entry : neighbors.entrySet()) {
						for (Entry<WeightedStructureRelationBinary, Double> edge : entry.getValue().entrySet()) {
							if (ignoreTypes != null && ignoreTypes.contains(edge.getKey().getType()))
								continue;
							WeightedStructureSequence seq = currentPath.clone();
							seq.add(edge.getKey(), edge.getValue());
							nextPaths.add(seq);
						}
					}
				}
			}
			
			currentPaths = nextPaths;
		}

		paths.addAll(currentPaths);
		return paths;
	}
	
	public List<WeightedStructureSequence> getEdgePaths(int length) {
		return getEdgePaths(length, null);
	}
	
	public List<WeightedStructureSequence> getEdgePaths(int length, Set<String> ignoreTypes) {
		List<WeightedStructureSequence> paths = new ArrayList<WeightedStructureSequence>();
		if (length <= 0)
			return paths;
		
		for (String nodeId : this.edges.keySet())
			getEdgePaths(nodeId, length, paths, ignoreTypes);
		return paths;
	}

	@Override
	public List<CtxParsable> toList() {
		List<CtxParsable> list = new ArrayList<CtxParsable>();
		
		for (Map<WeightedStructureRelationUnary, Double> map : this.nodes.values())
			for (WeightedStructureRelationUnary rel : map.keySet())
				list.add(rel);
		
		for (Map<String, Map<WeightedStructureRelationBinary, Double>> value : this.edges.values()) {
			for (Map<WeightedStructureRelationBinary, Double> edgeEntry : value.values()) {
				list.addAll(edgeEntry.keySet());
			}
		}
		
		return list;
	}
}
