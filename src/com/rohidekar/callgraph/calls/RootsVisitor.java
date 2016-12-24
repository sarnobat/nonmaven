package com.rohidekar.callgraph.calls;

import java.util.HashSet;
import java.util.Set;

class RootsVisitor {
	Set<GraphNode> visitedNodes = new HashSet<GraphNode>();

	public boolean visited(GraphNode aNode) {
		return visitedNodes.contains(aNode);
	}

	public void addVisited(GraphNode aNode) {
		visitedNodes.add(aNode);

	}
}
