/*******************************************************************************
 * Copyright (c) 2014, 2015 Fabian Steeg, hbz.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabian Steeg, hbz - initial API & implementation
 *
 *******************************************************************************/
package org.eclipse.gef4.internal.dot.ui;

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.gef4.dot.DotImport;
import org.eclipse.gef4.graph.Graph;
import org.eclipse.gef4.internal.dot.DotExtractor;
import org.eclipse.gef4.internal.dot.DotFileUtils;
import org.eclipse.gef4.internal.dot.DotNativeDrawer;
import org.eclipse.gef4.internal.dot.parser.ui.internal.DotActivator;
import org.eclipse.gef4.zest.fx.ui.parts.ZestFxUiView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.xtext.ui.editor.XtextEditor;

import javafx.scene.Scene;

/**
 * Render DOT content with ZestFx and Graphviz
 *
 * @author Fabian Steeg (fsteeg)
 * @author Alexander Nyßen (anyssen)
 *
 */
/* provisional API */@SuppressWarnings("restriction")
public class DotGraphView extends ZestFxUiView {

	public static final String STYLES_CSS_FILE = DotGraphView.class
			.getResource("styles.css") //$NON-NLS-1$
			.toExternalForm();
	private static final String EXTENSION = "dot"; //$NON-NLS-1$
	private static final String LOAD_DOT_FILE = DotUiMessages.DotGraphView_0;
	private static final String SYNC_EXPORT_PDF = DotUiMessages.DotGraphView_1;
	private static final String SYNC_IMPORT_DOT = DotUiMessages.DotGraphView_2;
	private static final String GRAPH_NONE = DotUiMessages.DotGraphView_3;
	private static final String GRAPH_RESOURCE = DotUiMessages.DotGraphView_4;
	private static final String FORMAT_PDF = "pdf"; //$NON-NLS-1$
	private boolean listenToDotContent = false;
	private boolean linkImage = false;
	private String currentDot = "digraph{}"; //$NON-NLS-1$
	private File currentFile = null;
	private ExportToggle exportAction;
	private Link resourceLabel = null;

	public DotGraphView() {
		setGraph(new DotImport(currentDot).newGraphInstance());
	}

	@Override
	public void createPartControl(final Composite parent) {
		exportAction = new ExportToggle();
		Action updateToggleAction = new UpdateToggle().action(this);
		Action loadFileAction = new LoadFile().action(this);
		add(updateToggleAction, ISharedImages.IMG_ELCL_SYNCED);
		add(loadFileAction, ISharedImages.IMG_OBJ_FILE);
		add(exportAction.action(this), ISharedImages.IMG_ETOOL_PRINT_EDIT);
		parent.setLayout(new GridLayout(1, true));
		initResourceLabel(parent, loadFileAction, updateToggleAction);
		super.createPartControl(parent);
		getCanvas().setLayoutData(new GridData(GridData.FILL_BOTH));
		Scene scene = getViewer().getScene();
		// specify stylesheet
		scene.getStylesheets().add(STYLES_CSS_FILE);
	}

	private void initResourceLabel(final Composite parent,
			final Action loadAction, final Action toggleAction) {
		resourceLabel = new Link(parent, SWT.WRAP);
		resourceLabel.setText(GRAPH_NONE);
		resourceLabel.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				processEvent(loadAction, toggleAction, GRAPH_NONE, e);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				processEvent(loadAction, toggleAction, GRAPH_NONE, e);
			}

			private void processEvent(final Action loadFileAction,
					final Action toggleAction, final String label,
					SelectionEvent e) {
				/*
				 * As we use a single string for the links for localization, we
				 * don't compare specific strings but say the first link
				 * triggers the loadAction, else the toggleAction:
				 */
				if (label.replaceAll("<a>", "").startsWith(e.text)) { //$NON-NLS-1$ //$NON-NLS-2$
					loadFileAction.run();
				} else {
					// toggle as if the button was pressed, then run the action:
					toggleAction.setChecked(!toggleAction.isChecked());
					toggleAction.run();
				}
			}
		});
		resourceLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private void add(Action action, String imageName) {
		action.setId(action.getText());
		action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(imageName));
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
		mgr.add(action);
	}

	private void setGraphAsync(final String dot, final File file) {
		final DotGraphView view = this;
		getViewSite().getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (!dot.trim().isEmpty()) {
					DotImport dotImport = new DotImport(dot);
					if (dotImport.getErrors().size() > 0) {
						String message = String.format(
								"Could not import DOT: %s, DOT: %s", //$NON-NLS-1$
								dotImport.getErrors(), dot);
						DotActivator.getInstance().getLog()
								.log(new Status(
										Status.ERROR, DotActivator.getInstance()
												.getBundle().getSymbolicName(),
										message));
						return;
					}
					setGraph(dotImport.newGraphInstance());
					exportAction.linkCorrespondingImage(view);
					resourceLabel.setText(
							String.format(GRAPH_RESOURCE, file.getName()));
					resourceLabel.setToolTipText(file.getAbsolutePath());
				}
			}
		});

	}

	@Override
	public void setGraph(Graph graph) {
		super.setGraph(new DotToZestGraphConverter(graph).convert());
	}

	private boolean toggle(Action action, boolean input) {
		action.setChecked(!action.isChecked());
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
		for (IContributionItem item : mgr.getItems()) {
			if (item instanceof ActionContributionItem
					&& ((ActionContributionItem) item).getAction() == action) {
				action.setChecked(!action.isChecked());
				return !input;
			}
		}
		return input;
	}

	private boolean updateGraph(File file) {
		if (file == null || !file.exists()) {
			return false;
		}
		String dotString = currentDot;
		if (file.getName().endsWith("." + EXTENSION)) { //$NON-NLS-1$
			dotString = DotFileUtils.read(file);
		} else {
			dotString = new DotExtractor(file).getDotString();
		}
		currentDot = dotString;
		currentFile = file;
		setGraphAsync(dotString, file);
		return true;
	}

	private IWorkspaceRunnable updateGraphRunnable(final File f) {
		if (!listenToDotContent
				&& !f.getAbsolutePath().toString().endsWith(EXTENSION)) {
			return null;
		}
		IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable() {
			@Override
			public void run(final IProgressMonitor monitor)
					throws CoreException {
				if (updateGraph(f)) {
					currentFile = f;
				}
			}
		};
		return workspaceRunnable;
	}

	private class ExportToggle {

		private File generateImageFromGraph(final boolean refresh,
				final String format, DotGraphView view) {
			File dotFile = DotFileUtils.write(view.currentDot);
			File image = DotNativeDrawer.renderImage(
					new File(DotDirStore.getDotDirPath()), dotFile, format,
					null);
			if (view.currentFile == null) {
				return image;
			}
			File copy = DotFileUtils.copySingleFile(
					view.currentFile.getParentFile(),
					view.currentFile.getName() + "." + format, image); //$NON-NLS-1$
			if (refresh) {
				refreshParent(view.currentFile);
			}
			return copy;
		}

		private void openFile(File file, DotGraphView view) {
			if (view.currentFile == null) { // no workspace file for cur. graph
				IFileStore fileStore = EFS.getLocalFileSystem()
						.getStore(new Path("")); //$NON-NLS-1$
				fileStore = fileStore.getChild(file.getAbsolutePath());
				if (!fileStore.fetchInfo().isDirectory()
						&& fileStore.fetchInfo().exists()) {
					IWorkbenchPage page = view.getSite().getPage();
					try {
						IDE.openEditorOnFileStore(page, fileStore);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}
			} else {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IPath location = Path.fromOSString(file.getAbsolutePath());
				IFile copy = workspace.getRoot().getFileForLocation(location);
				IEditorRegistry registry = PlatformUI.getWorkbench()
						.getEditorRegistry();
				if (registry.isSystemExternalEditorAvailable(copy.getName())) {
					try {
						view.getViewSite().getPage()
								.openEditor(new FileEditorInput(
										copy),
								IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}
			}
		}

		private void refreshParent(final File file) {
			try {
				ResourcesPlugin.getWorkspace().getRoot()
						.getFileForLocation(
								Path.fromOSString(file.getAbsolutePath()))
						.getParent().refreshLocal(IResource.DEPTH_ONE, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		Action action(final DotGraphView view) {
			return new Action(DotGraphView.SYNC_EXPORT_PDF, SWT.TOGGLE) {
				@Override
				public void run() {
					linkImage = toggle(this, linkImage);
					if (view.currentFile != null) {
						linkCorrespondingImage(view);
					}
				}
			};
		}

		void linkCorrespondingImage(DotGraphView view) {
			if (view.linkImage) {
				File image = generateImageFromGraph(true,
						DotGraphView.FORMAT_PDF, view);
				openFile(image, view);
			}
		}

	}

	private class LoadFile {

		private String lastSelection = null;

		Action action(final DotGraphView view) {
			return new Action(DotGraphView.LOAD_DOT_FILE) {
				@Override
				public void run() {
					FileDialog dialog = new FileDialog(
							view.getViewSite().getShell(), SWT.OPEN);
					dialog.setFileName(lastSelection);
					String dotFileNamePattern = "*." + EXTENSION; //$NON-NLS-1$
					String embeddedDotFileNamePattern = "*.*"; //$NON-NLS-1$
					dialog.setFilterExtensions(new String[] {
							dotFileNamePattern, embeddedDotFileNamePattern });
					dialog.setFilterNames(new String[] {
							String.format("DOT file (%s)", dotFileNamePattern), //$NON-NLS-1$
							String.format("Embedded DOT Graph (%s)", //$NON-NLS-1$
									embeddedDotFileNamePattern) });
					String selection = dialog.open();
					if (selection != null) {
						lastSelection = selection;
						view.updateGraph(new File(selection));
					}
				}
			};
		}

	}

	private class UpdateToggle {

		/** Listener that passes a visitor if a resource is changed. */
		private IResourceChangeListener resourceChangeListener;

		/** If a *.dot file is visited, update the graph. */
		private IResourceDeltaVisitor resourceVisitor;

		/** Listen to selection changes and update graph in view. */
		protected ISelectionListener selectionChangeListener = null;

		Action action(final DotGraphView view) {

			Action toggleUpdateModeAction = new Action(
					DotGraphView.SYNC_IMPORT_DOT, SWT.TOGGLE) {

				@Override
				public void run() {
					listenToDotContent = toggle(this, listenToDotContent);
					toggleResourceListener();
				}

				private void toggleResourceListener() {
					IWorkspace workspace = ResourcesPlugin.getWorkspace();
					ISelectionService service = getSite().getWorkbenchWindow()
							.getSelectionService();
					if (view.listenToDotContent) {
						workspace.addResourceChangeListener(
								resourceChangeListener,
								IResourceChangeEvent.POST_BUILD
										| IResourceChangeEvent.POST_CHANGE);
						service.addSelectionListener(selectionChangeListener);
					} else {
						workspace.removeResourceChangeListener(
								resourceChangeListener);
						service.removeSelectionListener(
								selectionChangeListener);
					}
				}

			};

			selectionChangeListener = new ISelectionListener() {
				@Override
				public void selectionChanged(IWorkbenchPart part,
						ISelection selection) {
					if (part instanceof XtextEditor) {
						XtextEditor editor = (XtextEditor) part;
						if ("org.eclipse.gef4.internal.dot.parser.Dot" //$NON-NLS-1$
								.equals(editor.getLanguageName())
								&& editor
										.getEditorInput() instanceof FileEditorInput) {
							try {
								File resolvedFile = DotFileUtils
										.resolve(((FileEditorInput) editor
												.getEditorInput()).getFile()
														.getLocationURI()
														.toURL());
								if (!resolvedFile.equals(view.currentFile)) {
									view.updateGraph(resolvedFile);
								}
							} catch (MalformedURLException e) {
								e.printStackTrace();
							}
						}
					}

				}
			};

			resourceChangeListener = new IResourceChangeListener() {
				@Override
				public void resourceChanged(final IResourceChangeEvent event) {
					if (event.getType() != IResourceChangeEvent.POST_BUILD
							&& event.getType() != IResourceChangeEvent.POST_CHANGE) {
						return;
					}
					IResourceDelta rootDelta = event.getDelta();
					try {
						rootDelta.accept(resourceVisitor);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			};

			resourceVisitor = new IResourceDeltaVisitor() {
				@Override
				public boolean visit(final IResourceDelta delta) {
					IResource resource = delta.getResource();
					if (resource.getType() == IResource.FILE
							&& ((IFile) resource).getName()
									.endsWith(EXTENSION)) {
						try {
							final IFile f = (IFile) resource;
							IWorkspaceRunnable workspaceRunnable = view
									.updateGraphRunnable(DotFileUtils.resolve(
											f.getLocationURI().toURL()));
							IWorkspace workspace = ResourcesPlugin
									.getWorkspace();
							if (!workspace.isTreeLocked()) {
								workspace.run(workspaceRunnable, null);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					return true;
				}

			};
			return toggleUpdateModeAction;
		}
	}
}
