package com.rohidekar.callgraph.calls;

class GraphNodeInstruction extends GraphNode {
  private MyInstruction instruction;

  public GraphNodeInstruction(MyInstruction parentMethodInstruction) {
    super(parentMethodInstruction);
    if (parentMethodInstruction == null) {
      throw new IllegalAccessError("Do not allow this. It will interfere with drawing a graph");
    }
    instruction = parentMethodInstruction;
  }

  @Override
  public int getPackageDepth() {
    return instruction == null ? Integer.MAX_VALUE
        : Relationships.getPackageDepth(instruction.getMethodNameQualified());
  }

  @Override
  protected String printTreeNode() {
    //return this.instruction.printInstruction((getParents().size() < 1));
    return this.instruction.printInstruction(true);
  }
}
