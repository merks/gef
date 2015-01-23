/*******************************************************************************
 * Copyright (c) 2015 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.gef4.mvc.fx.policies;

import javafx.scene.Node;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.gef4.mvc.fx.operations.FXResizeNodeOperation;
import org.eclipse.gef4.mvc.fx.operations.FXRevealOperation;
import org.eclipse.gef4.mvc.fx.parts.FXCircleSegmentHandlePart;
import org.eclipse.gef4.mvc.operations.ForwardUndoCompositeOperation;
import org.eclipse.gef4.mvc.operations.ITransactional;
import org.eclipse.gef4.mvc.policies.AbstractPolicy;

public class FXResizePolicy extends AbstractPolicy<Node> implements
		ITransactional {

	protected FXResizeNodeOperation resizeOperation;
	protected ForwardUndoCompositeOperation forwardUndoOperation;

	// can be overridden by subclasses to add an operation for model changes
	@Override
	public IUndoableOperation commit() {
		/*
		 * TODO: Take out any "empty" operations.
		 */
		IUndoableOperation commit = forwardUndoOperation;
		forwardUndoOperation = null;
		resizeOperation = null;
		return commit;
	}

	protected double getMinimumHeight() {
		return FXCircleSegmentHandlePart.DEFAULT_SIZE;
	}

	protected double getMinimumWidth() {
		return FXCircleSegmentHandlePart.DEFAULT_SIZE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef4.mvc.fx.policies.ITransactionalPolicy#init()
	 */
	@Override
	public void init() {
		// create "empty" operation
		resizeOperation = new FXResizeNodeOperation(getHost().getVisual());
		FXRevealOperation revealOperation = new FXRevealOperation(getHost());
		forwardUndoOperation = new ForwardUndoCompositeOperation(
				resizeOperation.getLabel());
		forwardUndoOperation.add(resizeOperation);
		forwardUndoOperation.add(revealOperation);
	}

	public void performResize(double dw, double dh) {
		Node visual = getHost().getVisual();
		boolean resizable = visual.isResizable();

		// convert resize into relocate in case node is not resizable
		double layoutDw = resizable ? dw : 0;
		double layoutDh = resizable ? dh : 0;

		// ensure visual is not resized below threshold
		if (resizable) {
			if (resizeOperation.getOldSize().width + layoutDw < getMinimumWidth()) {
				layoutDw = getMinimumWidth()
						- resizeOperation.getOldSize().width;
			}
			if (resizeOperation.getOldSize().height + layoutDh < getMinimumHeight()) {
				layoutDh = getMinimumHeight()
						- resizeOperation.getOldSize().height;
			}
		}

		updateOperation(layoutDw, layoutDh);

		// locally execute operation
		try {
			forwardUndoOperation.execute(null, null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	protected void updateOperation(double layoutDw, double layoutDh) {
		resizeOperation.setDw(layoutDw);
		resizeOperation.setDh(layoutDh);
	}

}
