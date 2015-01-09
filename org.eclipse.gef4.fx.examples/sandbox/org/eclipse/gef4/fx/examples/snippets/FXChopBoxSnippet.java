/*******************************************************************************
 * Copyright (c) 2014 itemis AG and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 * 
 *******************************************************************************/
package org.eclipse.gef4.fx.examples.snippets;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import org.eclipse.gef4.fx.anchors.FXChopBoxAnchor;
import org.eclipse.gef4.fx.nodes.FXConnection;

public class FXChopBoxSnippet extends AbstractFXSnippet {

	public static void main(String[] args) {
		launch();
	}

	private Rectangle nodeA;
	private Rectangle nodeB;
	private Rectangle nodeC;
	private FXChopBoxAnchor anchorA;
	private FXChopBoxAnchor anchorB;
	private FXChopBoxAnchor anchorC;

	@Override
	public Scene createScene() {
		BorderPane root = new BorderPane();
		Scene scene = new Scene(root, 640, 480);

		nodeA = new Rectangle(50, 50);
		nodeA.setFill(Color.RED);

		nodeB = new Rectangle(50, 50);
		nodeB.setFill(Color.BLUE);

		nodeC = new Rectangle(50, 50);
		nodeC.setFill(Color.GREEN);

		Button btnA = new Button("move A");
		btnA.setOnAction(createMoveHandler("A", nodeA, 100, 100, 200));
		btnA.relocate(0, 0);

		Button btnB = new Button("move B");
		btnB.setOnAction(createMoveHandler("B", nodeB, 300, 100, 200));
		btnB.relocate(70, 0);

		Button btnC = new Button("move C");
		btnC.setOnAction(createMoveHandler("C", nodeC, 200, 200, 300));
		btnC.relocate(140, 0);

		FXConnection connectionAB = new FXConnection();
		FXConnection connectionBC = new FXConnection();

		Group group = new Group(nodeA, nodeB, nodeC, connectionAB,
				connectionBC, btnA, btnB, btnC);
		root.getChildren().add(group);

		anchorA = new FXChopBoxAnchor(nodeA);
		anchorB = new FXChopBoxAnchor(nodeB);
		anchorC = new FXChopBoxAnchor(nodeC);
		connectionAB.setStartAnchor(anchorA);
		connectionAB.setEndAnchor(anchorB);
		connectionBC.setStartAnchor(anchorB);
		connectionBC.setEndAnchor(anchorC);

		nodeA.relocate(100, 100);
		nodeB.relocate(300, 100);
		nodeC.relocate(200, 200);

		return scene;
	}

	private EventHandler<ActionEvent> createMoveHandler(final String label,
			final Node node, final double x, final double y0, final double y1) {
		return new EventHandler<ActionEvent>() {
			boolean flag = false;

			@Override
			public void handle(ActionEvent event) {
				node.relocate(x, flag ? y0 : y1);
				flag = !flag;
			}
		};
	}

}
