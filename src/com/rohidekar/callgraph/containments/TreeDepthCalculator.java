// Copyright 2012 Google Inc. All Rights Reserved.

package com.rohidekar.callgraph.containments;

import com.rohidekar.callgraph.common.*;

import javax.swing.tree.TreeModel;

/**
 * @author ssarnobat@google.com (Sridhar Sarnobat)
 *
 */
public class TreeDepthCalculator {
  public static int getTreeDepth(TreeModel tree) {
    TreeDepthVisitor tdv = new TreeDepthVisitor();
    int childCount = tree.getChildCount(tree.getRoot());
    int maxDepth = 0;
    for (int i = 0; i < childCount; i++) {
      int aDepth = getTreeDepth((GraphNode) tree.getChild(tree.getRoot(), i), 1, tdv);
      if (aDepth > maxDepth) {
        maxDepth = aDepth;
      }
    }
    return 1 + maxDepth;
  }

  private static int getTreeDepth(GraphNode iParent, int levelsAbove, TreeDepthVisitor tdv) {
    int maxDepth = 0;
    tdv.visit(iParent);
    for (GraphNode aChild : iParent.getChildren()) {
      if (tdv.isVisited(aChild)) {
        continue;
      }

      if (iParent.toString().equals(aChild.toString())) {
        throw new AssertionError("cycle");
      }
      int aDepth = getTreeDepth(aChild, levelsAbove + 1, tdv);
      if (aDepth > maxDepth) {
        maxDepth = aDepth;
      }
    }
    return levelsAbove + maxDepth;
  }


}
