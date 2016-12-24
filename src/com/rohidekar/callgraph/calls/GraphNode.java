package com.rohidekar.callgraph.calls;


import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

public abstract class GraphNode {

  // TODO: for better performance, create a parentsClosure set
  private Set<GraphNode> parents = new LinkedHashSet<GraphNode>();
  private Set<GraphNode> children = new LinkedHashSet<GraphNode>();
  boolean isVisited = false;
  private Object nodeData;

  public GraphNode(Object nodeData) {
    this.nodeData = nodeData;
  }

  public boolean isVisited() {
    return isVisited;
  }

  public void setVisited(boolean b) {
    isVisited = b;

  }

  public void addParent(GraphNode parent) {
    getParents().add(parent);
  }

  public void addChild(GraphNode child) {
    if (child.toString().contains("AbstractGcompRepository.loadEmployees()") && this.toString().contains("Millis")){
      throw new IllegalAccessError("Wrong way round");
    }
    if (getChildren().contains(child)) {
      // throw new AssertionError();
    } else {
      int sizeBefore = getChildren().size();
      getChildren().add(child);
      if (!getChildren().contains(child)) {
        throw new AssertionError();
      }
      int sizeAfter = getChildren().size();
      if (sizeBefore == sizeAfter) {
        throw new AssertionError();
      }
    }
  }

  /**
   * The tree printer uses this
   */
  @Override
  public String toString() {
    return this.printTreeNode();
  }

  protected abstract String printTreeNode();

  public Set<GraphNode> getParents() {
    return parents;
  }

  public Set<GraphNode> getChildren() {
    return children;
  }

  @Override
  public boolean equals(Object obj) {
    return nodeData == null ? false
        : ((MyInstruction) nodeData).getMethodNameQualified().equals(
            ((MyInstruction) (((GraphNode) obj).getSource())).getMethodNameQualified());
  }

  public Object getSource() {
    return nodeData;
  }

  @Override
  public int hashCode() {
    return nodeData == null ? 1 : nodeData.hashCode();
  }

  public GraphNode getChild(int index) {
    return new LinkedList<GraphNode>(children).get(index);
  }

  public abstract int getPackageDepth();

}
