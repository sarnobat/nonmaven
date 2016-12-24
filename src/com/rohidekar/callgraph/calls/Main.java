// Copyright 2012 Google Inc. All Rights Reserved.

package com.rohidekar.callgraph.calls;

import gr.gousiosg.javacg.stat.ClassVisitor;
import gr.gousiosg.javacg.stat.MethodVisitor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.Type;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

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

	private static class Relationships {

		// The top level package with classes in it
		int minPackageDepth = Integer.MAX_VALUE;

		// Relationships
		private Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap = LinkedHashMultimap
				.create();
		private Multimap<String, JavaClass> classNameToFieldTypesMultiMap = LinkedHashMultimap
				.create();
		private Multimap<String, String> classNameToFieldTypeNamesMultiMap = LinkedHashMultimap
				.create();
		private Multimap<String, String> parentPackageNameToChildPackageNameMultiMap = LinkedHashMultimap
				.create();

		// Name to Value mappings
		private Map<String, MyInstruction> allMethodNameToMyInstructionMap = new HashMap<String, MyInstruction>();

		// nodes
		private ImmutableMap<String, JavaClass> classNameToJavaClassMap;

		// Objects that cannot yet be found
		private Set<DeferredChildContainment> deferredChildContainments = new HashSet<DeferredChildContainment>();
		private Set<DeferredSuperMethod> deferredSuperMethod = new HashSet<DeferredSuperMethod>();
		private Set<DeferredParentContainment> deferredParentContainments = new HashSet<DeferredParentContainment>();

		private Set<String> classNames = new HashSet<String>();

		public Relationships(String resource) {
			Map<String, JavaClass> javaClasses = JavaClassGenerator
					.getJavaClassesFromResource(resource);
			this.classNameToJavaClassMap = ImmutableMap.copyOf(javaClasses);
			for (JavaClass jc : this.classNameToJavaClassMap.values()) {
				visitJavaClass(jc, this);
			}
			// These deferred relationships should not be necessary, but if you
			// debug them you'll see that
			// they find additional relationships.
			DeferredRelationships.handleDeferredRelationships(this);
		}

		private static void visitJavaClass(JavaClass javaClass, Relationships relationships) {
			try {
				new MyClassVisitor(javaClass, relationships).visitJavaClass(javaClass);
			} catch (ClassFormatException e) {
				e.printStackTrace();
			}
		}
		
		private static class MyClassVisitor extends ClassVisitor {

			  private JavaClass classToVisit;
			  private Relationships relationships;

			  private Map<String, JavaClass> visitedClasses = new HashMap<String, JavaClass>();

			  public MyClassVisitor(JavaClass classToVisit, Relationships relationships) {
			    super(classToVisit);
			    this.classToVisit = classToVisit;
			    relationships.addPackageOf(classToVisit);
			    this.relationships = relationships;
			  }

			  public void setVisited(JavaClass javaClass) {
			    this.visitedClasses.put(javaClass.getClassName(), javaClass);
			  }

			  public boolean isVisited(JavaClass javaClass) {
			    return this.visitedClasses.values().contains(javaClass);
			  }

			  @Override
			  public void visitJavaClass(JavaClass javaClass) {
			    if (this.isVisited(javaClass)) {
			      return;
			    }
			    this.setVisited(javaClass);
			    if (javaClass.getClassName().equals("java.lang.Object")) {
			      return;
			    }
			    relationships.addPackageOf(javaClass);
			    relationships.updateMinPackageDepth(javaClass);

			    // Parent classes
			    List<String> parentClasses = getInterfacesAndSuperClasses(javaClass);
			    for (String anInterfaceName : parentClasses) {
			      JavaClass anInterface = relationships.getClassDef(anInterfaceName);
			      if (anInterface == null) {
			        relationships.deferParentContainment(anInterfaceName, javaClass);
			        relationships.addContainmentRelationshipStringOnly(
			            anInterfaceName, classToVisit.getClassName());
			      } else {
			        relationships.addContainmentRelationship(anInterface.getClassName(), classToVisit);
			      }
			    }
			    // Methods
			    for (Method method : javaClass.getMethods()) {
			      method.accept(this);
			    }
			    // fields
			    Field[] fs = javaClass.getFields();
			    for (Field f : fs) {
			      f.accept(this);
			    }
			  }

			  public static List<String> getInterfacesAndSuperClasses(JavaClass javaClass) {
			    List<String> parentClasses =
			        Lists.asList(javaClass.getSuperclassName(), javaClass.getInterfaceNames());
			    return parentClasses;
			  }

			  @Override
			  public void visitMethod(Method method) {
			    String className = classToVisit.getClassName();
			    ConstantPoolGen classConstants = new ConstantPoolGen(classToVisit.getConstantPool());
			    MethodGen methodGen = new MethodGen(method, className, classConstants);
			    new MyMethodVisitor(methodGen, classToVisit, relationships).start();
			  }
			  private static class MyMethodVisitor extends MethodVisitor {
				  private final JavaClass visitedClass;
				  private final ConstantPoolGen constantsPool;
				  private final Relationships relationships;
				  private final String parentMethodQualifiedName;

				  MyMethodVisitor(MethodGen methodGen, JavaClass javaClass, Relationships relationships) {
				    super(methodGen, javaClass);
				    this.visitedClass = javaClass;
				    this.constantsPool = methodGen.getConstantPool();
				    this.parentMethodQualifiedName = MyInstruction.getQualifiedMethodName(methodGen, visitedClass);
				    this.relationships = relationships;
				    // main bit
				    if (methodGen.getInstructionList() != null) {
				      for (InstructionHandle instructionHandle = methodGen.getInstructionList().getStart();
				          instructionHandle != null; instructionHandle = instructionHandle.getNext()) {
				        Instruction anInstruction = instructionHandle.getInstruction();
				        if (!shouldVisitInstruction(anInstruction)) {
				          anInstruction.accept(this);
				        }
				      }
				    }
				    // We can't figure out the superclass method of the parent method because we don't know which
				    // parent classes' method is overriden (there are several)
				    // TODO: Wait, we can use the repository to get the java class.
				    String unqualifiedMethodName =
				        MyInstruction.getMethodNameUnqualified(parentMethodQualifiedName);
				    relationships.setVisitedMethod(parentMethodQualifiedName);
				    if (relationships.getMethod(parentMethodQualifiedName) == null) {
				      relationships.addMethodDefinition(
				          new MyInstruction(javaClass.getClassName(), unqualifiedMethodName));
				    }
				  }

				  private static boolean shouldVisitInstruction(Instruction iInstruction) {
				    return ((InstructionConstants.INSTRUCTIONS[iInstruction.getOpcode()] != null)
				        && !(iInstruction instanceof ConstantPushInstruction)
				        && !(iInstruction instanceof ReturnInstruction));
				  }

				  /** instance method */
				  @Override
				  public void visitINVOKEVIRTUAL(INVOKEVIRTUAL iInstruction) {
				    addMethodCallRelationship(iInstruction.getReferenceType(constantsPool),
				        iInstruction.getMethodName(constantsPool), iInstruction,
				        iInstruction.getArgumentTypes(constantsPool));
				  }

				  /** super method, private method, constructor */
				  @Override
				  public void visitINVOKESPECIAL(INVOKESPECIAL iInstruction) {
				    addMethodCallRelationship(iInstruction.getReferenceType(constantsPool),
				        iInstruction.getMethodName(constantsPool), iInstruction,
				        iInstruction.getArgumentTypes(constantsPool));
				  }

				  @Override
				  public void visitINVOKEINTERFACE(INVOKEINTERFACE iInstruction) {
				    addMethodCallRelationship(iInstruction.getReferenceType(constantsPool),
				        iInstruction.getMethodName(constantsPool), iInstruction,
				        iInstruction.getArgumentTypes(constantsPool));
				  }

				  @Override
				  public void visitINVOKESTATIC(INVOKESTATIC iInstruction) {
				    addMethodCallRelationship(iInstruction.getReferenceType(constantsPool),
				        iInstruction.getMethodName(constantsPool), iInstruction,
				        iInstruction.getArgumentTypes(constantsPool));
				  }

				  private void addMethodCallRelationship(Type iClass, String unqualifiedMethodName,
				      Instruction anInstruction,
				      Type[] argumentTypes) {
				    if (!(iClass instanceof ObjectType)) {
				      return;
				    }
				    // method calls
				    {
				      ObjectType childClass = (ObjectType) iClass;
				      MyInstruction target = new MyInstruction(childClass, unqualifiedMethodName);
				      relationships.addMethodCall(parentMethodQualifiedName, target, target.printInstruction(true));
				      if (relationships.getMethod(this.parentMethodQualifiedName) == null) {
				        relationships.addMethodDefinition(new MyInstruction(childClass.getClassName(), unqualifiedMethodName));
				      }
				      // link to superclass method - note: this will not work for the top-level
				      // method (i.e.
				      // parentMethodQualifiedName). Only for target.
				      // We can't do it for the superclass without a JavaClass object. We don't
				      // know which superclass
				      // the method overrides.
				      linkMethodToSuperclassMethod(unqualifiedMethodName, target);
				    }
				    // class dependencies for method calls
				  }

				  private void linkMethodToSuperclassMethod(String unqualifiedMethodName, MyInstruction target)
				      throws IllegalAccessError {

				    Collection<JavaClass> superClasses = relationships.getParentClassesAndInterfaces(visitedClass);
				    for (JavaClass parentClassOrInterface : superClasses) {
				      MyInstruction parentInstruction =
				          getInstruction(parentClassOrInterface, unqualifiedMethodName, relationships);
				      if (parentInstruction == null) {
				        // It may be that we're looking in the wrong superclass/interface and that we should just
				        // continue
				        // carry on
				        relationships.deferSuperMethodRelationshipCapture(
				            new DeferredSuperMethod(parentClassOrInterface, unqualifiedMethodName, target));
				      } else {
				        System.err.println(parentInstruction.getMethodNameQualified() + " -> "
				              + target.getMethodNameQualified());
				        relationships.addMethodCall(
				            parentInstruction.getMethodNameQualified(), target, target.getMethodNameQualified());
				      }
				      if (parentInstruction != null && target != null && !target.getClassNameQualified().equals(parentInstruction.getClassNameQualified())) {
				        // TODO: this should get printed later
				        System.out.println(
				            //"MyMethodVisitor.linkMethodToSuperclassMethod() - SRIDHAR: " +
				            "\""+parentInstruction.getClassNameQualified() + "\",\"" + target.getClassNameQualified() + "\"");
				        relationships.addContainmentRelationshipStringOnly(parentInstruction.getClassNameQualified(), target.getClassNameQualified());
				      }
				    }
				  }

				  public static MyInstruction getInstruction(JavaClass parentClassOrInterface,
				      String unqualifiedChildMethodName, Relationships relationships) {
				    String methodName = MyInstruction.getQualifiedMethodName(
				        parentClassOrInterface.getClassName(), unqualifiedChildMethodName);
				    MyInstruction instruction = relationships.getMethod(methodName);
				    return instruction;
				  }

				  @Override
				  public void start() {}
				}


			  @Override
			  public void visitField(Field field) {
			    Type fieldType = field.getType();
			    if (fieldType instanceof ObjectType) {
			      ObjectType objectType = (ObjectType) fieldType;
			      addContainmentRelationship(this.classToVisit, objectType.getClassName(), relationships, true);
			    }
			  }

			  public static void addContainmentRelationship(JavaClass classToVisit,
			      String childClassNameQualified, Relationships relationships, boolean allowDeferral) {
			    JavaClass jc = null;
			    try {
			      jc = Repository.lookupClass(childClassNameQualified);
			    } catch (ClassNotFoundException e) {

			    	e.printStackTrace();
			      if (allowDeferral) {
			        relationships.deferContainmentVisit(classToVisit, childClassNameQualified);
			      } else {
			        jc = relationships.getClassDef(childClassNameQualified);
			        if (jc == null) {
			        }
			      }
			    }
			    if (jc == null) {
			    } else {
			      relationships.addContainmentRelationship(classToVisit.getClassName(), jc);
			    }
			  }
			}


		private void addMethodCall(String parentMethodQualifiedName, MyInstruction childMethod,
				String childMethodQualifiedName) {
			if ("java.lang.System.currentTimeMillis()".equals(parentMethodQualifiedName)) {
				throw new IllegalAccessError("No such thing");
			}
			if ("java.lang.System.currentTimeMillis()".equals(childMethodQualifiedName)) {
				// throw new IllegalAccessError("No such thing");
			}
			allMethodNameToMyInstructionMap.put(childMethodQualifiedName, childMethod);
			if (!parentMethodQualifiedName.equals(childMethodQualifiedName)) {// don't
																				// allow
																				// cycles
				if (parentMethodQualifiedName.contains("Millis")) {
				}
				callingMethodToMethodInvocationMultiMap.put(parentMethodQualifiedName, childMethod);
				System.out.println("\"" + parentMethodQualifiedName + "\",\"" + childMethod + "\"");
			}
			if (!this.isVisitedMethod(childMethodQualifiedName)) {
				this.addUnvisitedMethod(childMethodQualifiedName);
			}
		}

		public boolean methodCallExists(String parentMethodQualifiedName,
				String childMethodQualifiedName) {
			for (MyInstruction childMethod : callingMethodToMethodInvocationMultiMap
					.get(parentMethodQualifiedName)) {
				if (childMethod.getMethodNameQualified().equals(childMethodQualifiedName)) {
					return true;
				}
			}
			return false;
		}

		private void addUnvisitedMethod(String childMethodQualifiedName) {
			this.isMethodVisited.put(childMethodQualifiedName, false);
		}

		@VisibleForTesting
		boolean isVisitedMethod(String childMethodQualifiedName) {
			if (!isMethodVisited.keySet().contains(childMethodQualifiedName)) {
				addUnvisitedMethod(childMethodQualifiedName);
			}
			return isMethodVisited.get(childMethodQualifiedName);
		}

		public void addContainmentRelationship(String parentClassFullName, JavaClass javaClass) {
			classNameToFieldTypesMultiMap.put(parentClassFullName, javaClass);
			addContainmentRelationshipStringOnly(parentClassFullName, javaClass.getClassName());
		}

		public void addContainmentRelationshipStringOnly(String parentClassName,
				String childClassName) {
			if (parentClassName.equals("java.lang.Object")) {
				// throw new IllegalAccessError();
			}
			if (childClassName.equals("java.lang.Object")) {
				// throw new IllegalAccessError();
			}

			classNameToFieldTypeNamesMultiMap.put(parentClassName, childClassName);
			this.classNames.add(parentClassName);
			this.classNames.add(childClassName);
		}

		public void updateMinPackageDepth(JavaClass javaClass) {
			int packageDepth = getPackageDepth(javaClass.getClassName());
			if (packageDepth < minPackageDepth) {
				minPackageDepth = packageDepth;
			}
		}

		public static int getPackageDepth(String qualifiedClassName) {
			String packageName = ClassUtils.getPackageName(qualifiedClassName);
			int periodCount = StringUtils.countMatches(packageName, ".");
			int packageDepth = periodCount + 1;
			return packageDepth;

		}

		public void addPackageOf(JavaClass classToVisit) {
			String pkgFullName = classToVisit.getPackageName();
			String parentPktFullName = ClassUtils.getPackageName(pkgFullName);
			this.parentPackageNameToChildPackageNameMultiMap.put(parentPktFullName, pkgFullName);
		}

		public JavaClass getClassDef(String aClassFullName) {
			JavaClass jc = null;
			try {
				jc = Repository.lookupClass(aClassFullName);
			} catch (ClassNotFoundException e) {
				if (this.classNameToJavaClassMap.get(aClassFullName) != null) {
				}
			}
			if (jc == null) {
				jc = this.classNameToJavaClassMap.get(aClassFullName);
			}
			return jc;
		}

		public Collection<JavaClass> getParentClassesAndInterfaces(JavaClass childClass) {
			Collection<JavaClass> superClassesAndInterfaces = new HashSet<JavaClass>();
			String[] interfaceNames = childClass.getInterfaceNames();
			for (String interfaceName : interfaceNames) {
				JavaClass anInterface = this.classNameToJavaClassMap.get(interfaceName);
				if (anInterface == null) {
					// Do it later
					deferParentContainment(interfaceName, childClass);
				} else {
					superClassesAndInterfaces.add(anInterface);
				}
			}
			String superclassNames = childClass.getSuperclassName();
			if (!superclassNames.equals("java.lang.Object")) {
				JavaClass theSuperclass = this.classNameToJavaClassMap.get(superclassNames);
				if (theSuperclass == null) {
					// Do it later
					deferParentContainment(superclassNames, childClass);
				} else {
					superClassesAndInterfaces.add(theSuperclass);
				}
			}
			if (superClassesAndInterfaces.size() > 0) {
			}
			return ImmutableSet.copyOf(superClassesAndInterfaces);
		}

		public boolean deferContainmentVisit(JavaClass parentClassToVisit,
				String childClassQualifiedName) {
			return this.deferredChildContainments.add(new DeferredChildContainment(
					parentClassToVisit, childClassQualifiedName));
		}

		public Set<DeferredChildContainment> getDeferredChildContainment() {
			return ImmutableSet.copyOf(this.deferredChildContainments);
		}

		public void validate() {
			if (this.allMethodNameToMyInstructionMap.keySet().contains(
					"com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
				throw new IllegalAccessError("No such thing");
			}
			if (this.callingMethodToMethodInvocationMultiMap.keySet().contains(
					"com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
				throw new IllegalAccessError("No such thing");
			}

		}

		public void deferSuperMethodRelationshipCapture(DeferredSuperMethod deferredSuperMethod) {
			this.deferredSuperMethod.add(deferredSuperMethod);
		}

		public Set<DeferredSuperMethod> getDeferSuperMethodRelationships() {
			return ImmutableSet.copyOf(this.deferredSuperMethod);
		}

		public void deferParentContainment(String parentClassName, JavaClass javaClass) {
			this.deferredParentContainments.add(new DeferredParentContainment(parentClassName,
					javaClass));
		}

		private Map<String, Boolean> isMethodVisited = new HashMap<String, Boolean>();

		public void setVisitedMethod(String parentMethodQualifiedName) {
			if (this.isMethodVisited.keySet().contains(parentMethodQualifiedName)) {
				this.isMethodVisited.remove(parentMethodQualifiedName);
			}
			this.isMethodVisited.put(parentMethodQualifiedName, true);
		}

		public MyInstruction getMethod(String qualifiedMethodName) {
			return this.allMethodNameToMyInstructionMap.get(qualifiedMethodName);
		}

		public void addMethodDefinition(MyInstruction myInstructionImpl) {
			allMethodNameToMyInstructionMap.put(myInstructionImpl.getMethodNameQualified(),
					myInstructionImpl);
		}

		private static class DeferredRelationships {

			  static void handleDeferredRelationships(Relationships relationships) {
			    for (DeferredChildContainment containment : relationships.getDeferredChildContainment()) {
			      MyClassVisitor.addContainmentRelationship(
			          containment.getParentClass(), containment.getClassQualifiedName(), relationships, false);
			    }
			    for (DeferredSuperMethod deferredSuperMethod :
			        relationships.getDeferSuperMethodRelationships()) {
			      handleDeferredSuperMethod(relationships, deferredSuperMethod);
			    }
			  }

			  private static void handleDeferredSuperMethod(
			      Relationships relationships, DeferredSuperMethod deferredSuperMethod) {
			    MyInstruction parentInstruction = MyMethodVisitor.getInstruction(
			        deferredSuperMethod.getparentClassOrInterface(),
			        deferredSuperMethod.getunqualifiedMethodName(), relationships);
			    if (parentInstruction == null) {
			    } else {
//			      System.err.println(parentInstruction.getMethodNameQualified() + " -> "
//			            + deferredSuperMethod.gettarget().getMethodNameQualified());
			      if (!relationships.methodCallExists(deferredSuperMethod.gettarget().getMethodNameQualified(),
			          parentInstruction.getMethodNameQualified())) {
			        relationships.addMethodCall(parentInstruction.getMethodNameQualified(),
			            deferredSuperMethod.gettarget(),
			            deferredSuperMethod.gettarget().getMethodNameQualified());
			      }
			    }
			  }
			  
			  private static class MyMethodVisitor extends MethodVisitor {
				  private final JavaClass visitedClass;
				  private final ConstantPoolGen constantsPool;
				  private final Relationships relationships;
				  private final String parentMethodQualifiedName;

				  MyMethodVisitor(MethodGen methodGen, JavaClass javaClass, Relationships relationships) {
				    super(methodGen, javaClass);
				    this.visitedClass = javaClass;
				    this.constantsPool = methodGen.getConstantPool();
				    this.parentMethodQualifiedName = MyInstruction.getQualifiedMethodName(methodGen, visitedClass);
				    this.relationships = relationships;
				    // main bit
				    if (methodGen.getInstructionList() != null) {
				      for (InstructionHandle instructionHandle = methodGen.getInstructionList().getStart();
				          instructionHandle != null; instructionHandle = instructionHandle.getNext()) {
				        Instruction anInstruction = instructionHandle.getInstruction();
				        if (!shouldVisitInstruction(anInstruction)) {
				          anInstruction.accept(this);
				        }
				      }
				    }
				    // We can't figure out the superclass method of the parent method because we don't know which
				    // parent classes' method is overriden (there are several)
				    // TODO: Wait, we can use the repository to get the java class.
				    String unqualifiedMethodName =
				        MyInstruction.getMethodNameUnqualified(parentMethodQualifiedName);
				    relationships.setVisitedMethod(parentMethodQualifiedName);
				    if (relationships.getMethod(parentMethodQualifiedName) == null) {
				      relationships.addMethodDefinition(
				          new MyInstruction(javaClass.getClassName(), unqualifiedMethodName));
				    }
				  }

				  private static boolean shouldVisitInstruction(Instruction iInstruction) {
				    return ((InstructionConstants.INSTRUCTIONS[iInstruction.getOpcode()] != null)
				        && !(iInstruction instanceof ConstantPushInstruction)
				        && !(iInstruction instanceof ReturnInstruction));
				  }

				  /** instance method */
				  @Override
				  public void visitINVOKEVIRTUAL(INVOKEVIRTUAL iInstruction) {
				    addMethodCallRelationship(iInstruction.getReferenceType(constantsPool),
				        iInstruction.getMethodName(constantsPool), iInstruction,
				        iInstruction.getArgumentTypes(constantsPool));
				  }

				  /** super method, private method, constructor */
				  @Override
				  public void visitINVOKESPECIAL(INVOKESPECIAL iInstruction) {
				    addMethodCallRelationship(iInstruction.getReferenceType(constantsPool),
				        iInstruction.getMethodName(constantsPool), iInstruction,
				        iInstruction.getArgumentTypes(constantsPool));
				  }

				  @Override
				  public void visitINVOKEINTERFACE(INVOKEINTERFACE iInstruction) {
				    addMethodCallRelationship(iInstruction.getReferenceType(constantsPool),
				        iInstruction.getMethodName(constantsPool), iInstruction,
				        iInstruction.getArgumentTypes(constantsPool));
				  }

				  @Override
				  public void visitINVOKESTATIC(INVOKESTATIC iInstruction) {
				    addMethodCallRelationship(iInstruction.getReferenceType(constantsPool),
				        iInstruction.getMethodName(constantsPool), iInstruction,
				        iInstruction.getArgumentTypes(constantsPool));
				  }

				  private void addMethodCallRelationship(Type iClass, String unqualifiedMethodName,
				      Instruction anInstruction,
				      Type[] argumentTypes) {
				    if (!(iClass instanceof ObjectType)) {
				      return;
				    }
				    // method calls
				    {
				      ObjectType childClass = (ObjectType) iClass;
				      MyInstruction target = new MyInstruction(childClass, unqualifiedMethodName);
				      relationships.addMethodCall(parentMethodQualifiedName, target, target.printInstruction(true));
				      if (relationships.getMethod(this.parentMethodQualifiedName) == null) {
				        relationships.addMethodDefinition(new MyInstruction(childClass.getClassName(), unqualifiedMethodName));
				      }
				      // link to superclass method - note: this will not work for the top-level
				      // method (i.e.
				      // parentMethodQualifiedName). Only for target.
				      // We can't do it for the superclass without a JavaClass object. We don't
				      // know which superclass
				      // the method overrides.
				      linkMethodToSuperclassMethod(unqualifiedMethodName, target);
				    }
				    // class dependencies for method calls
				  }

				  private void linkMethodToSuperclassMethod(String unqualifiedMethodName, MyInstruction target)
				      throws IllegalAccessError {

				    Collection<JavaClass> superClasses = relationships.getParentClassesAndInterfaces(visitedClass);
				    for (JavaClass parentClassOrInterface : superClasses) {
				      MyInstruction parentInstruction =
				          getInstruction(parentClassOrInterface, unqualifiedMethodName, relationships);
				      if (parentInstruction == null) {
				        // It may be that we're looking in the wrong superclass/interface and that we should just
				        // continue
				        // carry on
				        relationships.deferSuperMethodRelationshipCapture(
				            new DeferredSuperMethod(parentClassOrInterface, unqualifiedMethodName, target));
				      } else {
				        System.err.println(parentInstruction.getMethodNameQualified() + " -> "
				              + target.getMethodNameQualified());
				        relationships.addMethodCall(
				            parentInstruction.getMethodNameQualified(), target, target.getMethodNameQualified());
				      }
				      if (parentInstruction != null && target != null && !target.getClassNameQualified().equals(parentInstruction.getClassNameQualified())) {
				        // TODO: this should get printed later
				        System.out.println(
				            //"MyMethodVisitor.linkMethodToSuperclassMethod() - SRIDHAR: " +
				            "\""+parentInstruction.getClassNameQualified() + "\",\"" + target.getClassNameQualified() + "\"");
				        relationships.addContainmentRelationshipStringOnly(parentInstruction.getClassNameQualified(), target.getClassNameQualified());
				      }
				    }
				  }

				  public static MyInstruction getInstruction(JavaClass parentClassOrInterface,
				      String unqualifiedChildMethodName, Relationships relationships) {
				    String methodName = MyInstruction.getQualifiedMethodName(
				        parentClassOrInterface.getClassName(), unqualifiedChildMethodName);
				    MyInstruction instruction = relationships.getMethod(methodName);
				    return instruction;
				  }

				  @Override
				  public void start() {}
				}

			  
			  private static class MyClassVisitor extends ClassVisitor {

				  private JavaClass classToVisit;
				  private Relationships relationships;

				  private Map<String, JavaClass> visitedClasses = new HashMap<String, JavaClass>();

				  public MyClassVisitor(JavaClass classToVisit, Relationships relationships) {
				    super(classToVisit);
				    this.classToVisit = classToVisit;
				    relationships.addPackageOf(classToVisit);
				    this.relationships = relationships;
				  }

				  public void setVisited(JavaClass javaClass) {
				    this.visitedClasses.put(javaClass.getClassName(), javaClass);
				  }

				  public boolean isVisited(JavaClass javaClass) {
				    return this.visitedClasses.values().contains(javaClass);
				  }

				  @Override
				  public void visitJavaClass(JavaClass javaClass) {
				    if (this.isVisited(javaClass)) {
				      return;
				    }
				    this.setVisited(javaClass);
				    if (javaClass.getClassName().equals("java.lang.Object")) {
				      return;
				    }
				    relationships.addPackageOf(javaClass);
				    relationships.updateMinPackageDepth(javaClass);

				    // Parent classes
				    List<String> parentClasses = getInterfacesAndSuperClasses(javaClass);
				    for (String anInterfaceName : parentClasses) {
				      JavaClass anInterface = relationships.getClassDef(anInterfaceName);
				      if (anInterface == null) {
				        relationships.deferParentContainment(anInterfaceName, javaClass);
				        relationships.addContainmentRelationshipStringOnly(
				            anInterfaceName, classToVisit.getClassName());
				      } else {
				        relationships.addContainmentRelationship(anInterface.getClassName(), classToVisit);
				      }
				    }
				    // Methods
				    for (Method method : javaClass.getMethods()) {
				      method.accept(this);
				    }
				    // fields
				    Field[] fs = javaClass.getFields();
				    for (Field f : fs) {
				      f.accept(this);
				    }
				  }

				  public static List<String> getInterfacesAndSuperClasses(JavaClass javaClass) {
				    List<String> parentClasses =
				        Lists.asList(javaClass.getSuperclassName(), javaClass.getInterfaceNames());
				    return parentClasses;
				  }

				  @Override
				  public void visitMethod(Method method) {
				    String className = classToVisit.getClassName();
				    ConstantPoolGen classConstants = new ConstantPoolGen(classToVisit.getConstantPool());
				    MethodGen methodGen = new MethodGen(method, className, classConstants);
				    new MyMethodVisitor(methodGen, classToVisit, relationships).start();
				  }
				  
				  static class MyMethodVisitor extends MethodVisitor {
					  private final JavaClass visitedClass;
					  private final ConstantPoolGen constantsPool;
					  private final Relationships relationships;
					  private final String parentMethodQualifiedName;

					  MyMethodVisitor(MethodGen methodGen, JavaClass javaClass, Relationships relationships) {
					    super(methodGen, javaClass);
					    this.visitedClass = javaClass;
					    this.constantsPool = methodGen.getConstantPool();
					    this.parentMethodQualifiedName = MyInstruction.getQualifiedMethodName(methodGen, visitedClass);
					    this.relationships = relationships;
					    // main bit
					    if (methodGen.getInstructionList() != null) {
					      for (InstructionHandle instructionHandle = methodGen.getInstructionList().getStart();
					          instructionHandle != null; instructionHandle = instructionHandle.getNext()) {
					        Instruction anInstruction = instructionHandle.getInstruction();
					        if (!shouldVisitInstruction(anInstruction)) {
					          anInstruction.accept(this);
					        }
					      }
					    }
					    // We can't figure out the superclass method of the parent method because we don't know which
					    // parent classes' method is overriden (there are several)
					    // TODO: Wait, we can use the repository to get the java class.
					    String unqualifiedMethodName =
					        MyInstruction.getMethodNameUnqualified(parentMethodQualifiedName);
					    relationships.setVisitedMethod(parentMethodQualifiedName);
					    if (relationships.getMethod(parentMethodQualifiedName) == null) {
					      relationships.addMethodDefinition(
					          new MyInstruction(javaClass.getClassName(), unqualifiedMethodName));
					    }
					  }

					  private static boolean shouldVisitInstruction(Instruction iInstruction) {
					    return ((InstructionConstants.INSTRUCTIONS[iInstruction.getOpcode()] != null)
					        && !(iInstruction instanceof ConstantPushInstruction)
					        && !(iInstruction instanceof ReturnInstruction));
					  }

					  /** instance method */
					  @Override
					  public void visitINVOKEVIRTUAL(INVOKEVIRTUAL iInstruction) {
					    addMethodCallRelationship(iInstruction.getReferenceType(constantsPool),
					        iInstruction.getMethodName(constantsPool), iInstruction,
					        iInstruction.getArgumentTypes(constantsPool));
					  }

					  /** super method, private method, constructor */
					  @Override
					  public void visitINVOKESPECIAL(INVOKESPECIAL iInstruction) {
					    addMethodCallRelationship(iInstruction.getReferenceType(constantsPool),
					        iInstruction.getMethodName(constantsPool), iInstruction,
					        iInstruction.getArgumentTypes(constantsPool));
					  }

					  @Override
					  public void visitINVOKEINTERFACE(INVOKEINTERFACE iInstruction) {
					    addMethodCallRelationship(iInstruction.getReferenceType(constantsPool),
					        iInstruction.getMethodName(constantsPool), iInstruction,
					        iInstruction.getArgumentTypes(constantsPool));
					  }

					  @Override
					  public void visitINVOKESTATIC(INVOKESTATIC iInstruction) {
					    addMethodCallRelationship(iInstruction.getReferenceType(constantsPool),
					        iInstruction.getMethodName(constantsPool), iInstruction,
					        iInstruction.getArgumentTypes(constantsPool));
					  }

					  private void addMethodCallRelationship(Type iClass, String unqualifiedMethodName,
					      Instruction anInstruction,
					      Type[] argumentTypes) {
					    if (!(iClass instanceof ObjectType)) {
					      return;
					    }
					    // method calls
					    {
					      ObjectType childClass = (ObjectType) iClass;
					      MyInstruction target = new MyInstruction(childClass, unqualifiedMethodName);
					      relationships.addMethodCall(parentMethodQualifiedName, target, target.printInstruction(true));
					      if (relationships.getMethod(this.parentMethodQualifiedName) == null) {
					        relationships.addMethodDefinition(new MyInstruction(childClass.getClassName(), unqualifiedMethodName));
					      }
					      // link to superclass method - note: this will not work for the top-level
					      // method (i.e.
					      // parentMethodQualifiedName). Only for target.
					      // We can't do it for the superclass without a JavaClass object. We don't
					      // know which superclass
					      // the method overrides.
					      linkMethodToSuperclassMethod(unqualifiedMethodName, target);
					    }
					    // class dependencies for method calls
					  }

					  private void linkMethodToSuperclassMethod(String unqualifiedMethodName, MyInstruction target)
					      throws IllegalAccessError {

					    Collection<JavaClass> superClasses = relationships.getParentClassesAndInterfaces(visitedClass);
					    for (JavaClass parentClassOrInterface : superClasses) {
					      MyInstruction parentInstruction =
					          getInstruction(parentClassOrInterface, unqualifiedMethodName, relationships);
					      if (parentInstruction == null) {
					        // It may be that we're looking in the wrong superclass/interface and that we should just
					        // continue
					        // carry on
					        relationships.deferSuperMethodRelationshipCapture(
					            new DeferredSuperMethod(parentClassOrInterface, unqualifiedMethodName, target));
					      } else {
					        System.err.println(parentInstruction.getMethodNameQualified() + " -> "
					              + target.getMethodNameQualified());
					        relationships.addMethodCall(
					            parentInstruction.getMethodNameQualified(), target, target.getMethodNameQualified());
					      }
					      if (parentInstruction != null && target != null && !target.getClassNameQualified().equals(parentInstruction.getClassNameQualified())) {
					        // TODO: this should get printed later
					        System.out.println(
					            //"MyMethodVisitor.linkMethodToSuperclassMethod() - SRIDHAR: " +
					            "\""+parentInstruction.getClassNameQualified() + "\",\"" + target.getClassNameQualified() + "\"");
					        relationships.addContainmentRelationshipStringOnly(parentInstruction.getClassNameQualified(), target.getClassNameQualified());
					      }
					    }
					  }

					  static MyInstruction getInstruction(JavaClass parentClassOrInterface,
					      String unqualifiedChildMethodName, Relationships relationships) {
					    String methodName = MyInstruction.getQualifiedMethodName(
					        parentClassOrInterface.getClassName(), unqualifiedChildMethodName);
					    MyInstruction instruction = relationships.getMethod(methodName);
					    return instruction;
					  }

					  @Override
					  public void start() {}
					}


				  @Override
				  public void visitField(Field field) {
				    Type fieldType = field.getType();
				    if (fieldType instanceof ObjectType) {
				      ObjectType objectType = (ObjectType) fieldType;
				      addContainmentRelationship(this.classToVisit, objectType.getClassName(), relationships, true);
				    }
				  }

				  public static void addContainmentRelationship(JavaClass classToVisit,
				      String childClassNameQualified, Relationships relationships, boolean allowDeferral) {
				    JavaClass jc = null;
				    try {
				      jc = Repository.lookupClass(childClassNameQualified);
				    } catch (ClassNotFoundException e) {

				    	e.printStackTrace();
				      if (allowDeferral) {
				        relationships.deferContainmentVisit(classToVisit, childClassNameQualified);
				      } else {
				        jc = relationships.getClassDef(childClassNameQualified);
				        if (jc == null) {
				        }
				      }
				    }
				    if (jc == null) {
				    } else {
				      relationships.addContainmentRelationship(classToVisit.getClassName(), jc);
				    }
				  }
				}
			}

	}

	private static class RelationshipToGraphTransformerCallHierarchy {

		public static void printCallGraph(Relationships relationships) {
			relationships.validate();
		}
	}

}
