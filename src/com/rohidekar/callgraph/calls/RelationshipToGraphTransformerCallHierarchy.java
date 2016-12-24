package com.rohidekar.callgraph.calls;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Transforms relationships into graphs
 */
class RelationshipToGraphTransformerCallHierarchy {

  public static void printCallGraph(Relationships relationships) {
    Map<String, GraphNode> allMethodNamesToMethodNodes = RelationshipToGraphTransformerCallHierarchy
        .determineCallHierarchy(relationships);
    relationships.validate();
    Set<GraphNode> rootMethodNodes = RelationshipToGraphTransformerCallHierarchy
        .findRootCallers(allMethodNamesToMethodNodes);
  }

  private static Set<GraphNode> findRootCallers(Map<String, GraphNode> allMethodNamesToMethods) {
    Set<GraphNode> rootMethodNodes;
    rootMethodNodes = new HashSet<GraphNode>();
    for (GraphNode aNode : allMethodNamesToMethods.values()) {
      Set<GraphNode> roots = new HashSet<GraphNode>();
      RootsVisitor rootsVisitor = new RootsVisitor();
      RootFinder.getRoots(aNode, roots, rootsVisitor);
      rootMethodNodes.addAll(roots);
    }
    return rootMethodNodes;
  }

  private static Map<String, GraphNode> determineCallHierarchy(Relationships relationships) {
    relationships.validate();
    Map<String, GraphNode> allMethodNamesToMethods = new LinkedHashMap<String, GraphNode>();
    // Create a custom call graph structure from the multimap (flatten)
    for (String parentMethodNameKey : relationships.getAllMethodCallers()) {
      GraphNodeInstruction parentEnd = (GraphNodeInstruction) allMethodNamesToMethods.get(parentMethodNameKey);
      if (parentEnd == null) {
        MyInstruction parentMethodInstruction = relationships.getMethod(parentMethodNameKey);
        if (parentMethodInstruction == null) {
          continue;
        }
        parentEnd = new GraphNodeInstruction(parentMethodInstruction);
        allMethodNamesToMethods.put(parentMethodNameKey, parentEnd);
        if (parentEnd.toString().contains("Millis") && parentMethodNameKey.contains("Repository")) {
          throw new IllegalAccessError();
        }
      }
      if (parentEnd.toString().contains("Millis") && parentMethodNameKey.contains("Repository")) {
        throw new IllegalAccessError();
      }
      Collection<MyInstruction> calledMethods = relationships.getCalledMethods(parentMethodNameKey);
      for (MyInstruction childMethod : calledMethods) {
        GraphNodeInstruction child = (GraphNodeInstruction) allMethodNamesToMethods
            .get(childMethod.getMethodNameQualified());
        if (child == null) {
          child = new GraphNodeInstruction(childMethod);
          allMethodNamesToMethods.put(childMethod.getMethodNameQualified(), child);
        }
        parentEnd.addChild(child);
        child.addParent(parentEnd);
      }
    }
    relationships.validate();
    return allMethodNamesToMethods;
  }
}
