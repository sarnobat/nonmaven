package com.rohidekar.callgraph.containments;

import java.util.Collection;

import javax.swing.tree.TreeModel;

import com.google.common.collect.Multimap;
import com.rohidekar.callgraph.Main;
import com.rohidekar.callgraph.common.GraphNode;
import com.rohidekar.callgraph.common.Relationships;

import dnl.utils.text.tree.TextTree;

public class TreePrinterContainments {

  /**
   * Output suitable for D3
   */
  public static void printTrees(Relationships relationships, Multimap<Integer, TreeModel> depthToTree) {
    System.out.println("source,target");
    for (Integer treeDepth : depthToTree.keySet()) {
      Object o = depthToTree.get(treeDepth);
      if (o == null) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Collection<TreeModel> treeModels = (Collection<TreeModel>) o;
      for (TreeModel tree : treeModels) {
        if (treeDepth < Main.MIN_TREE_DEPTH) {
          continue;
        }
        if (treeDepth > Main.MAX_TREE_DEPTH) {
          continue;
        }

        if (((GraphNode) tree.getRoot()).getPackageDepth() > relationships.getMinPackageDepth() + Main.ROOT_DEPTH) {
          continue;
        }
        TextTree textTree = new TextTree(tree);
        printRelationships(tree);
      }
    }
  }

  private static void printRelationships(TreeModel tree) {
    for (int i = 0; i < tree.getChildCount(tree.getRoot()); i++) {
      Object child = tree.getChild(tree.getRoot(), i);
      System.out.println("\"" + child.toString() + "\",\"" + tree.getRoot().toString() + "\"");
    }
  }
}
