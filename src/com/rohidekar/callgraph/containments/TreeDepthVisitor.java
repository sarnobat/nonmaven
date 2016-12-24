package com.rohidekar.callgraph.containments;

import com.rohidekar.callgraph.common.*;
import java.util.HashMap;
import java.util.Map;

class TreeDepthVisitor {
  private Map<String, GraphNode> visited = new HashMap<String, GraphNode>();

  void visit(GraphNode iParent) {
    visited.put(iParent.toString(), iParent);
  }

  boolean isVisited(GraphNode iParent) {
    return visited.keySet().contains(iParent.toString());
  }
}
