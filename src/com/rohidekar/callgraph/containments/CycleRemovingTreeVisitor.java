package com.rohidekar.callgraph.containments;

import java.util.HashSet;
import java.util.Set;

import com.rohidekar.callgraph.common.GraphNode;

class CycleRemovingTreeVisitor {

  GraphNode visit(GraphNode aRootNodeWithCycles) {
    GraphNodeString aRootNodeWithoutCycles = new GraphNodeString( // (JavaClass) //
                                                                  // aRootNodeWithCycles.toString(),
        (String) aRootNodeWithCycles.getSource());
    return visit(aRootNodeWithCycles, aRootNodeWithoutCycles);
  }

  GraphNode visit(GraphNode aRootNodeWithCycles, GraphNode aRootNodeWithoutCycles) {
    Set<GraphNode> visitedNodes = new HashSet<GraphNode>();
    visitNode(aRootNodeWithCycles, aRootNodeWithoutCycles, visitedNodes);
    return aRootNodeWithoutCycles;
  }

  private GraphNode visitNode(GraphNode aRootNodeWithCycles, GraphNode aRootNodeWithoutCycles,
      Set<GraphNode> visitedNodes) {

    for (GraphNode child : aRootNodeWithCycles.getChildren()) {
      if (visitedNodes.contains(child)) {
        continue;
      }

      visitedNodes.add(child);
      GraphNodeString aChildNodeWithoutCycles = new GraphNodeString((String) child.getSource());
      aRootNodeWithoutCycles.addChild(aChildNodeWithoutCycles);
      aChildNodeWithoutCycles.addParent(aRootNodeWithoutCycles);
      visitNode(child, aChildNodeWithoutCycles, visitedNodes);
    }
    return aRootNodeWithoutCycles;
  }

}
