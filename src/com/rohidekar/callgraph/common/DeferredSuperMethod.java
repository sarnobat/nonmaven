package com.rohidekar.callgraph.common;

import org.apache.bcel.classfile.JavaClass;

/**
 * @author ssarnobat@google.com (Sridhar Sarnobat)
 *
 */
class DeferredSuperMethod {

  JavaClass parentClassOrInterface;
  String unqualifiedMethodName;
  MyInstruction target;

  DeferredSuperMethod(
      JavaClass parentClassOrInterface, String unqualifiedMethodName, MyInstruction target) {
    this.parentClassOrInterface = parentClassOrInterface;
    this.unqualifiedMethodName = unqualifiedMethodName;
    this.target = target;
  }

  MyInstruction gettarget() {
    return target;
  }

  JavaClass getparentClassOrInterface() {
    return parentClassOrInterface;
  }

  String getunqualifiedMethodName() {
    return unqualifiedMethodName;
  }

}
