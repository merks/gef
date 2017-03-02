/*******************************************************************************
 * Copyright (c) 2014, 2016 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.gef.mvc.fx.tools;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.gef.geometry.planar.Point;
import org.eclipse.gef.mvc.fx.policies.IOnHoverPolicy;
import org.eclipse.gef.mvc.fx.viewer.IViewer;

import javafx.animation.Animation.Status;
import javafx.animation.PauseTransition;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * The {@link HoverTool} is an {@link AbstractTool} that handles mouse hover
 * changes.
 *
 * @author mwienand
 *
 */
public class HoverTool extends AbstractTool {

	/**
	 * Time in milliseconds until the hover handles are removed when the host is
	 * not hovered anymore.
	 */
	public static final int UNHOVER_LINGERING_MILLIS = 600;

	/**
	 * Time in milliseconds until the hover handles are created when the host is
	 * hovered.
	 */
	public static final int HOVER_LINGERING_MILLIS = 250;

	/**
	 * Distance in pixels which the mouse is allowed to move so that it is
	 * regarded to be stationary.
	 */
	public static final double LINGERING_MOUSE_MOVE_THRESHOLD = 4;

	/**
	 * The type of the policy that has to be supported by target parts.
	 */
	public static final Class<IOnHoverPolicy> ON_HOVER_POLICY_KEY = IOnHoverPolicy.class;

	private final Map<Scene, EventHandler<MouseEvent>> hoverFilters = new IdentityHashMap<>();
	// TODO: Investigate if lingering hover works with multiple scenes, or if
	// multiple scenes require special treatment.
	private Point initialLingeringScreenPosition;
	private PauseTransition lingeringDelay = new PauseTransition(
			Duration.millis(HOVER_LINGERING_MILLIS));
	private PauseTransition unlingeringDelay = new PauseTransition(
			Duration.millis(UNHOVER_LINGERING_MILLIS));
	private Node lingering;
	private Node potentiallyLingering;

	{
		lingeringDelay.setOnFinished((ae) -> onLingeringDelayFinished());
		unlingeringDelay.setOnFinished((ae) -> onUnlingeringDelayFinished());
	}

	/**
	 * Creates an {@link EventHandler} for hover {@link MouseEvent}s. The
	 * handler will search for a target part within the given {@link IViewer}
	 * and notify all hover policies of that target part about hover changes.
	 * <p>
	 * If no target part can be identified, then the root part of the given
	 * {@link IViewer} is used as the target part.
	 *
	 * @param viewer
	 *            The {@link IViewer} for which to create the
	 *            {@link EventHandler}.
	 * @return The {@link EventHandler} that handles hover changes for the given
	 *         {@link IViewer}.
	 */
	protected EventHandler<MouseEvent> createHoverFilter(final IViewer viewer) {
		return new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				// stop lingering delay if mouse moved
				if (lingeringDelay.getStatus().equals(Status.RUNNING)) {
					double dx = initialLingeringScreenPosition.x
							- event.getScreenX();
					double dy = initialLingeringScreenPosition.y
							- event.getScreenY();
					if (Math.abs(dx) > LINGERING_MOUSE_MOVE_THRESHOLD
							|| Math.abs(dy) > LINGERING_MOUSE_MOVE_THRESHOLD) {
						lingeringDelay.playFromStart();
						initialLingeringScreenPosition.x = event.getScreenX();
						initialLingeringScreenPosition.y = event.getScreenY();
					}
				}

				// only handle events where the mouse target changes
				if (!event.getEventType().equals(MouseEvent.MOUSE_MOVED)
						&& !event.getEventType()
								.equals(MouseEvent.MOUSE_ENTERED_TARGET)
						&& !event.getEventType()
								.equals(MouseEvent.MOUSE_EXITED_TARGET)) {
					return;
				}

				// determine new target
				EventTarget eventTarget = event.getTarget();
				if (eventTarget instanceof Node) {
					// determine hover policies
					Collection<? extends IOnHoverPolicy> policies = getTargetPolicyResolver()
							.getTargetPolicies(HoverTool.this,
									(Node) eventTarget, ON_HOVER_POLICY_KEY);
					getDomain().openExecutionTransaction(HoverTool.this);
					// active policies are unnecessary because hover is not a
					// gesture, just one event at one point in time
					for (IOnHoverPolicy policy : policies) {
						policy.hover(event);
					}
					getDomain().closeExecutionTransaction(HoverTool.this);

					if (eventTarget != lingering) {
						potentiallyLingering = (Node) eventTarget;
						initialLingeringScreenPosition = new Point(
								event.getScreenX(), event.getScreenY());
						lingeringDelay.playFromStart();

						if (lingering != null) {
							if (!unlingeringDelay.getStatus()
									.equals(Status.RUNNING)) {
								unlingeringDelay.playFromStart();
							}
						} else {
							unlingeringDelay.stop();
						}
					} else {
						lingeringDelay.stop();
						unlingeringDelay.stop();
					}
				}
			}
		};
	}

	/**
	 *
	 * @param lingering
	 *            The lingering hover {@link Node}.
	 */
	protected void delegateLingering(Node lingering) {
		// determine hover policies
		Collection<? extends IOnHoverPolicy> policies = getTargetPolicyResolver()
				.getTargetPolicies(HoverTool.this, this.lingering,
						ON_HOVER_POLICY_KEY);
		getDomain().openExecutionTransaction(HoverTool.this);
		// active policies are unnecessary because hover is not a
		// gesture, just one event at one point in time
		for (IOnHoverPolicy policy : policies) {
			policy.lingeringHover(lingering);
		}
		getDomain().closeExecutionTransaction(HoverTool.this);
	}

	@Override
	protected void doActivate() {
		super.doActivate();
		for (IViewer viewer : getDomain().getViewers().values()) {
			Scene scene = viewer.getCanvas().getScene();
			if (!hoverFilters.containsKey(scene)) {
				EventHandler<MouseEvent> hoverFilter = createHoverFilter(
						viewer);
				scene.addEventFilter(MouseEvent.ANY, hoverFilter);
				hoverFilters.put(scene, hoverFilter);
			}
		}
	}

	@Override
	protected void doDeactivate() {
		lingeringDelay.stop();
		unlingeringDelay.stop();
		for (Scene scene : hoverFilters.keySet()) {
			scene.removeEventFilter(MouseEvent.ANY, hoverFilters.remove(scene));
		}
		super.doDeactivate();
	}

	/**
	 * Callback method that is invoked when the mouse was stationary over a
	 * visual for some amount of time.
	 */
	protected void onLingeringDelayFinished() {
		unlingeringDelay.stop();
		lingering = potentiallyLingering;
		potentiallyLingering = null;
		delegateLingering(lingering);
	}

	/**
	 * Callback method that is invoked when the mouse was not stationary and did
	 * not move over the current lingering for some amount of time.
	 */
	protected void onUnlingeringDelayFinished() {
		delegateLingering(null);
		lingering = null;
	}
}
