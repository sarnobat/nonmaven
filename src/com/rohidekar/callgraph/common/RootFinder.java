package com.rohidekar.callgraph.common;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author ssarnobat@google.com (Sridhar Sarnobat)
 *
 */
public class RootFinder {

  public static void getRoots(GraphNode aNode, Set<GraphNode> roots, RootsVisitor rootsVisitor) {
    if (rootsVisitor.visited(aNode)) {

    } else {
      rootsVisitor.addVisited(aNode);
      if (aNode.getParents().size() > 0) {
        for (GraphNode parentNode : aNode.getParents()) {
          getRoots(parentNode, roots, rootsVisitor);
        }
      } else {
        if (aNode.toString().equals("java.lang.System.currentTimeMillis()")) {
          throw new IllegalAccessError();
        }
        roots.add(aNode);
      }
    }
  }

  public static Set<GraphNode> findRootJavaClasses(Map<String, GraphNode> classNameToGraphNodeJavaClassMap) {
    Set<GraphNode> rootClasses;
    rootClasses = new HashSet<GraphNode>();
    for (GraphNode aNode : classNameToGraphNodeJavaClassMap.values()) {
      RootsVisitor rootsVisitor = new RootsVisitor();
      getRoots(aNode, rootClasses, rootsVisitor);
    }
    return rootClasses;
  }

}
