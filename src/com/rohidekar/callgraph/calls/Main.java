// Copyright 2012 Google Inc. All Rights Reserved.

package com.rohidekar.callgraph.calls;

/**
 * put -Xmx1024m in the VM args
 * 
 * @author ssarnobat@google.com (Sridhar Sarnobat)
 */
public class Main {

	public static void main(String[] args) {
		String resource;
		if (args == null || args.length < 1) {
			// resource = "/Users/ssarnobat/work/src/saas/services/subscriber";
			// resource =
			// "/Users/ssarnobat/work/src/saas/services/plancycle/target";
			// resource =
			// "/sarnobat.garagebandbroken/eclipse.git/bin/main/java/";
			resource = "/sarnobat.garagebandbroken/Desktop/github-repositories/fuse-java-helloworld-not-groovy/maven/fuse4j-core/target/classes/";
			// resource = "/Users/ssarnobat/github/nanohttpd/target";
			// resource = "/Users/ssarnobat/github/java_callgraph_csv/target";
			// TODO: use the current working directory as the class folder, not
			// an arbitrary jar
		} else {
			resource = args[0];
		}
		printGraphs(resource);
	}

	private static void printGraphs(String classDirOrJar) {
		Relationships relationships = new Relationships(classDirOrJar);
		relationships.validate();
		RelationshipToGraphTransformerCallHierarchy.printCallGraph(relationships);
	}
}
