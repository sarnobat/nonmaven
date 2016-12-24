// Copyright 2012 Google Inc. All Rights Reserved.

package com.rohidekar.callgraph.common;

import org.apache.bcel.classfile.JavaClass;

/**
 * When the child does not exist
 * 
 * @author ssarnobat@google.com (Sridhar Sarnobat)
 */
class DeferredChildContainment {
  private String childClassQualifiedName ;
  private JavaClass parentClass;
  DeferredChildContainment(JavaClass parentClass, String childClassQualifiedName){
    this.childClassQualifiedName = childClassQualifiedName;
    this.parentClass = parentClass;
  }

  String getClassQualifiedName() {
    return childClassQualifiedName;
  }

  JavaClass getParentClass() {
    return parentClass;
  }
}
