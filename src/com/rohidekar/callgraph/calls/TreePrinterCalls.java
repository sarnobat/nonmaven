package com.rohidekar.callgraph.calls;

import java.util.HashSet;
import java.util.Set;

import javax.swing.tree.TreeModel;

import com.google.common.base.Objects;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.rohidekar.callgraph.Main;
import com.rohidekar.callgraph.common.GraphNode;
import com.rohidekar.callgraph.common.MyInstruction;
import com.rohidekar.callgraph.common.MyTreeModel;
import com.rohidekar.callgraph.common.Relationships;
import com.rohidekar.callgraph.containments.TreeDepthCalculator;

public class TreePrinterCalls {

  /**
   * @param relationships
   * @param rootMethodNodes
   */

  public static void printTrees(Relationships relationships, Set<GraphNode> rootMethodNodes) {
    Multimap<Integer, TreeModel> depthToRootNodes = LinkedHashMultimap.create();
    for (GraphNode aRootNode : rootMethodNodes) {
      TreeModel tree = new MyTreeModel(aRootNode);
      int treeDepth = TreeDepthCalculator.getTreeDepth(tree);
      // TODO: move this to the loop below
      if (aRootNode.getPackageDepth() > relationships.getMinPackageDepth() + Main.ROOT_DEPTH) {
        continue;
      }
      depthToRootNodes.put(treeDepth, tree);
    }
    for (int i = Main.MIN_TREE_DEPTH; i < Main.MAX_TREE_DEPTH; i++) {
      Integer treeDepth = new Integer(i);
      if (treeDepth < Main.MIN_TREE_DEPTH) {
        continue;
      }
      if (treeDepth > Main.MAX_TREE_DEPTH) {
        continue;
      }
      for (Object aTreeModel : depthToRootNodes.get(treeDepth)) {
        TreeModel aTreeModel2 = (TreeModel) aTreeModel;
        // new TextTree(aTreeModel2).printTree();
        GraphNode rootNode = (GraphNode) aTreeModel2.getRoot();
        printTreeTest(rootNode, 0, new HashSet<GraphNode>());
      }
    }
  }


  private static void printTreeTest(GraphNode tn, int level, Set<GraphNode> visited) {
    if (visited.contains(tn)) {
      return;
    }
    visited.add(tn);
    if (((MyInstruction) tn.getSource()).getMethodNameQualified()
        .equals("com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
      throw new IllegalAccessError();
    }
    for (GraphNode child : tn.getChildren()) {
      System.out.println("\"" + tn.toString() + "\",\"" + child.toString() + "\"");
      printTreeTest(child, level + 1, visited);
    }

  }
}
