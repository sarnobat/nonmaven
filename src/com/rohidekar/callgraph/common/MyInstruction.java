package com.rohidekar.callgraph.common;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.commons.lang.ClassUtils;

public class MyInstruction {

  private String _qualifiedMethodName;

  public MyInstruction(ObjectType iClass, String unqualifiedMethodName) {
    this(iClass.getClassName(), unqualifiedMethodName);
  }

  public MyInstruction(String classNameQualified, String unqualifiedMethodName) {
    String qualifiedMethodName = getQualifiedMethodName(classNameQualified, unqualifiedMethodName);
    this._qualifiedMethodName = qualifiedMethodName;
    if (qualifiedMethodName.equals(
        "com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
      throw new IllegalAccessError();
    }
  }

  public static String getQualifiedMethodName(MethodGen methodGen, JavaClass visitedClass) {
    return getQualifiedMethodName(visitedClass.getClassName(), methodGen.getName());
  }

  public static String getQualifiedMethodName(String className, String methodName) {
    return className + "." + methodName + "()";
  }

  public String getMethodNameQualified() {
    return this._qualifiedMethodName;
  }

  @Override
  public boolean equals(Object that) {
    return this.getMethodNameQualified().equals(((MyInstruction) that).getMethodNameQualified());
  }

  @Override
  public int hashCode() {
    return this.getMethodNameQualified().hashCode();
  }

  @Override
  public String toString() {
    return this._qualifiedMethodName;
  }

  public String printInstruction(boolean printPackage) {
    String methodNameUnqualified = getMethodNameUnqualified();
    String classNameQualified = getClassNameQualified();
    String classNameUnqualified = ClassUtils.getShortCanonicalName(classNameQualified);
    if (classNameUnqualified.contains("cassandra.db")) {
      System.err.println("do not display package: " + classNameUnqualified);
    }
    return printPackage ? this.getMethodNameQualified()
        : classNameUnqualified + "." + methodNameUnqualified;
  }

  public String getClassNameQualified() {
    return ClassUtils.getPackageCanonicalName(this.toString());
  }

  public String getMethodNameUnqualified() {
    return getMethodNameUnqualified(this.getMethodNameQualified());
  }

  public static String getMethodNameUnqualified(String qualifiedMethodName) {
    return ClassUtils.getShortCanonicalName(qualifiedMethodName);
  }
}
