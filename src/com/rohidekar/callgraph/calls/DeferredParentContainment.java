package com.rohidekar.callgraph.calls;

import org.apache.bcel.classfile.JavaClass;

/**
 * When the parent doesn't yet exist.
 *
 * @author ssarnobat@google.com (Sridhar Sarnobat)
 */
class DeferredParentContainment {

  private String parentClassName;
  private JavaClass childClass;

  DeferredParentContainment(String parentClassName, JavaClass childClass) {
    this.setParentClassName(parentClassName);
    this.setChildClass(childClass);
  }

  String getParentClassName() {
    return parentClassName;
  }

  private void setParentClassName(String parentClassName) {
    this.parentClassName = parentClassName;
  }

  JavaClass getChildClass() {
    return childClass;
  }

  private void setChildClass(JavaClass childClass) {
    this.childClass = childClass;
  }

}
