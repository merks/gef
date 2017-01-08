/*******************************************************************************
 * Copyright (c) 2015 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.gef.mvc.fx.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The {@link MvcFxUiBundle} is the plug-in that integrates MVC.FX with the
 * Eclipse platform UI.
 *
 * @author anyssen
 *
 */
public class MvcFxUiBundle extends AbstractUIPlugin {

	/**
	 * The plug-in ID.
	 */
	public static final String PLUGIN_ID = "org.eclipse.gef.mvc.fx.ui"; //$NON-NLS-1$

	/**
	 * Symbolic name of the zoom-in icon image.
	 */
	public static final String IMG_ICONS_ZOOM_IN = "IMG_ICONS_ZOOM_IN";

	/**
	 * Symbolic name of the zoom-in icon image.
	 */
	public static final String IMG_ICONS_ZOOM_OUT = "IMG_ICONS_ZOOM_OUT";

	/**
	 * Symbolic name of the scroll-center icon image.
	 */
	public static final String IMG_ICONS_SCROLL_CENTER = "IMG_ICONS_SCROLL_CENTER";

	/**
	 * Symbolic name of the scroll-top-left icon image.
	 */
	public static final String IMG_ICONS_SCROLL_TOP_LEFT = "IMG_ICONS_SCROLL_TOP_LEFT";

	/**
	 * Symbolic name of the scroll-top-right icon image.
	 */
	public static final String IMG_ICONS_SCROLL_TOP_RIGHT = "IMG_ICONS_SCROLL_TOP_RIGHT";

	/**
	 * Symbolic name of the scroll-bottom-left icon image.
	 */
	public static final String IMG_ICONS_SCROLL_BOTTOM_LEFT = "IMG_ICONS_SCROLL_BOTTOM_LEFT";

	/**
	 * Symbolic name of the scroll-bottom-right icon image.
	 */
	public static final String IMG_ICONS_SCROLL_BOTTOM_RIGHT = "IMG_ICONS_SCROLL_BOTTOM_RIGHT";

	/**
	 * Symbolic name of the fit-to-viewport icon image.
	 */
	public static final String IMG_ICONS_FIT_TO_VIEWPORT = "IMG_ICONS_FIT_TO_VIEWPORT";

	// IMAGES map contains symbolic names and image paths
	private static final Map<String, String> IMAGES = new HashMap<>();
	static {
		IMAGES.put(IMG_ICONS_ZOOM_IN, "icons/zoomIn.png");
		IMAGES.put(IMG_ICONS_ZOOM_OUT, "icons/zoomOut.png");
		IMAGES.put(IMG_ICONS_SCROLL_CENTER, "icons/scrollCenter.png");
		IMAGES.put(IMG_ICONS_SCROLL_TOP_LEFT, "icons/scrollTopLeft.png");
		IMAGES.put(IMG_ICONS_SCROLL_TOP_RIGHT, "icons/scrollTopRight.png");
		IMAGES.put(IMG_ICONS_SCROLL_BOTTOM_RIGHT,
				"icons/scrollBottomRight.png");
		IMAGES.put(IMG_ICONS_SCROLL_BOTTOM_LEFT, "icons/scrollBottomLeft.png");
		IMAGES.put(IMG_ICONS_FIT_TO_VIEWPORT, "icons/fitToViewport.png");
	}

	// The shared instance
	private static MvcFxUiBundle plugin;

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static MvcFxUiBundle getDefault() {
		return plugin;
	}

	/**
	 * The constructor
	 */
	public MvcFxUiBundle() {
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		// put action images into the registry
		Bundle bundle = getBundle();
		for (Entry<String, String> e : IMAGES.entrySet()) {
			reg.put(e.getKey(), ImageDescriptor.createFromURL(
					FileLocator.find(bundle, new Path(e.getValue()), null)));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
	 * BundleContext )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

}
