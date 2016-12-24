package com.rohidekar.callgraph.common;
import com.rohidekar.callgraph.common.*;
import java.util.LinkedList;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class MyTreeModel implements TreeModel {
	GraphNode root;

	public MyTreeModel(GraphNode aNode) {
		
		this.root = aNode;
	}

	@Override
	public Object getChild(Object parentNode, int index) {
		Object ret = ((GraphNode) parentNode).getChild(index);
		if (ret == null) {
			throw new IllegalAccessError();
		}
		return ret;
	}

	@Override
	public int getChildCount(Object node) {
		return ((GraphNode) node).getChildren().size();
	}

	@Override
	public int getIndexOfChild(Object parentNode, Object childNode) {
		int ret = new LinkedList<GraphNode>(((GraphNode) parentNode).getChildren()).indexOf(childNode);
		if (this.getChild(parentNode, ret) == null) {
			throw new IllegalAccessError();
		}
		return ret;
	}

	@Override
	public Object getRoot() {
		return root;
	}

	@Override
	public boolean isLeaf(Object node) {
		return ((GraphNode) node).getChildren().size() < 1;
	}

	@Override
	public void removeTreeModelListener(TreeModelListener arg0) {

	}

	@Override
	public void valueForPathChanged(TreePath arg0, Object arg1) {

	}

	@Override
	public void addTreeModelListener(TreeModelListener arg0) {

	}

}
