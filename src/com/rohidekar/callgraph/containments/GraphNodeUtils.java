package com.rohidekar.callgraph.containments;

import com.rohidekar.callgraph.common.*;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Set;

import javax.swing.tree.TreeModel;

/**
 * Generic routines related to graph structures
 */
class GraphNodeUtils {

  static Multimap<Integer, TreeModel> removeCyclicCalls(Set<GraphNode> rootClasses) {
    Multimap<Integer, TreeModel> depthToTree;
    depthToTree = HashMultimap.create();
    for (GraphNode aRootNodeWithCyclesNode : rootClasses) {
      GraphNode aRootNodeNoCycles = new CycleRemovingTreeVisitor().visit(aRootNodeWithCyclesNode);
      TreeModel tree = new MyTreeModel(aRootNodeNoCycles);
      depthToTree.put(TreeDepthCalculator.getTreeDepth(tree), tree);
    }
    return depthToTree;
  }
  //
  // private static boolean hasChildRellationship(
  // Map<String, GraphNode> allMethodNamesToMethods, String callingMethod, String calledMethod)
  // throws IllegalAccessError {
  // boolean found = false;
  //
  // GraphNode graphNode = allMethodNamesToMethods.get(callingMethod);
  // if (graphNode == null) {
  // // not applicable
  // } else {
  // if (graphNode.getChildren().size() < 1) {
  // throw new IllegalAccessError();
  // }
  // for (GraphNode n : graphNode.getChildren()) {
  // String classNameQualified = ((MyInstructionImpl) n.getSource()).getUniqueName();
  // if (calledMethod.equals(classNameQualified)) {
  // found = true;
  // break;
  // } else {
  // }
  // }
  // }
  // return found;
  // }
}
