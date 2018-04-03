/*********************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.tests;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.debug.ToggleBreakpointsTargetFactory;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.junit.Test;

@SuppressWarnings("restriction")
public class TestDebugIntegration extends AbstractCorrosionTest {

	@Test
	public void testPrettyPrinter() throws Exception {
		// Add breakpoint
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor = null;
		editor = IDE.openEditor(activePage, getProject("basic").getFolder("src").getFile("main.rs"));
		ToggleBreakpointsTargetFactory adapter = new ToggleBreakpointsTargetFactory();
		IToggleBreakpointsTarget target = adapter.createToggleTarget(CorrosionPlugin.PLUGIN_ID + "BreakpointFactory");
		target.toggleLineBreakpoints(editor, new TextSelection(55, 1));
		// Launch debug
		ILaunchConfiguration config = getLaunchConfiguration("debug",
				((IFileEditorInput) editor.getEditorInput()).getFile());
		if (config != null) {
			DebugUITools.launch(config, ILaunchManager.DEBUG_MODE);
		}
		// open debug variable view
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
		.showView("org.eclipse.debug.ui.VariableView");
		// check the variable tree
		IViewReference[] references = activePage.getViewReferences();
		for (IViewReference iViewReference : references) {
			if (iViewReference.getId().equals("org.eclipse.debug.ui.VariableView")) {
				VariablesView view = (VariablesView) iViewReference.getView(true);
				view.refreshDetailPaneContents();
			}
		}
	}

	private ILaunchConfiguration getLaunchConfiguration(String mode, IResource resource) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType = launchManager
				.getLaunchConfigurationType("org.eclipse.corrosion.debug.RustDebugDelegate");
		try {
			ILaunchConfiguration[] launchConfigurations = launchManager.getLaunchConfigurations(configType);
			final IProject project = resource.getProject();
			final String projectName = project.getName();

			for (ILaunchConfiguration iLaunchConfiguration : launchConfigurations) {
				if (iLaunchConfiguration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, "")
						.equals(projectName)) {
					return iLaunchConfiguration;
				}
			}
			String configName = launchManager.generateLaunchConfigurationName(projectName);
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, configName);
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME,
					project.getLocation().toString() + "/target/debug/" + projectName);
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, project.getLocation().toString());
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN, false);
			wc.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, "rust-gdb");
			wc.doSave();
			return wc;
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
		return null;
	}

}
