/*******************************************************************************
 * Copyright (c) 2007, 2013 Symbian Software Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bala Torati (Symbian) - Initial API and implementation
 * Doug Schaefer (QNX) - Added overridable start and end patterns
 * Liviu Ionescu - Add Liquid code
 *******************************************************************************/
package ilg.gnumcueclipse.templates.core.processes;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.templateengine.TemplateCore;
import org.eclipse.cdt.core.templateengine.TemplateEngineHelper;
import org.eclipse.cdt.core.templateengine.process.ProcessArgument;
import org.eclipse.cdt.core.templateengine.process.ProcessFailureException;
import org.eclipse.cdt.core.templateengine.process.ProcessHelper;
import org.eclipse.cdt.core.templateengine.process.ProcessRunner;
import org.eclipse.cdt.core.templateengine.process.processes.Messages;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import ilg.gnumcueclipse.templates.core.Utils;

/**
 * Adds Files to the Project
 */
public class ConditionalAddLiquidFiles extends ProcessRunner {

	/**
	 * This method Adds the list of Files to the corresponding Project.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void process(TemplateCore template, ProcessArgument[] args, String processId, IProgressMonitor monitor)
			throws ProcessFailureException {
		IProject projectHandle = null;
		ProcessArgument[][] files = null;
		String condition = null;

		Map<String, String> valueStore = template.getValueStore();
		Map<String, Object> liquidMap = new HashMap<String, Object>();
		// Use all definitions in the store, including from other plug-ins.
		liquidMap.putAll(valueStore);
		liquidMap.put("language", valueStore.get("fileExtension"));
		
		Calendar now = Calendar.getInstance();   // Gets the current date and time
		int year = now.get(Calendar.YEAR);
		liquidMap.put("year", Integer.toString(year));
		liquidMap.put("authorName", "<your-name-here>");

		for (ProcessArgument arg : args) {
			String argName = arg.getName();
			if (argName.equals("projectName")) { //$NON-NLS-1$
				projectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(arg.getSimpleValue());
			} else if (argName.equals("files")) { //$NON-NLS-1$
				files = arg.getComplexArrayValue();
			} else if (argName.equals("condition")) { //$NON-NLS-1$
				condition = arg.getSimpleValue();
			}
			// Ignore start/end pattern.
		}

		if (projectHandle == null)
			throw new ProcessFailureException(
					getProcessMessage(processId, IStatus.ERROR, Messages.getString("AddFiles.8"))); //$NON-NLS-1$

		if (files == null) {
			throw new ProcessFailureException(
					getProcessMessage(processId, IStatus.ERROR, Messages.getString("AddFiles.9"))); //$NON-NLS-1$
		}

		if (!Utils.isConditionSatisfied(condition)) {
			return;
		}

		for (int i = 0; i < files.length; i++) {
			ProcessArgument[] file = files[i];
			String fileSourcePath = file[0].getSimpleValue();
			String fileTargetPath = file[1].getSimpleValue();
			boolean replaceable = file[2].getSimpleValue().equals("true"); //$NON-NLS-1$

			URL sourceURL;
			try {
				File f = new File(fileSourcePath);
				if (f.isAbsolute()) {
					sourceURL = f.toURL(); // using .toURI().toURL() fails, due
											// to
											// spaces substitution
				} else {
					sourceURL = TemplateEngineHelper.getTemplateResourceURLRelativeToTemplate(template, fileSourcePath);
					if (sourceURL == null) {
						throw new ProcessFailureException(getProcessMessage(processId, IStatus.ERROR,
								Messages.getString("AddFiles.1") + fileSourcePath)); //$NON-NLS-1$
					}
				}
			} catch (MalformedURLException e2) {
				throw new ProcessFailureException(Messages.getString("AddFiles.2") + fileSourcePath); //$NON-NLS-1$
			} catch (IOException e) {
				throw new ProcessFailureException(Messages.getString("AddFiles.2") + fileSourcePath); //$NON-NLS-1$
			}

			InputStream contents = null;
			if (replaceable) {
				String fileContents;
				try {
					fileContents = ProcessHelper.readFromFile(sourceURL);
				} catch (IOException e) {
					throw new ProcessFailureException(Messages.getString("AddFiles.3") + fileSourcePath); //$NON-NLS-1$
				}

				// Do not substitute old macros, use only liquid syntax.

				try {
					// The option simplifies usage, by automatically stripping spaces around tags;
					// All previous \n are preserved; the first next \n is stripped.
					liqp.ParseSettings liqSettings = new liqp.ParseSettings.Builder().withStripSpaceAroundTags(false)
							.build();
					liqp.Template liqTemplate = liqp.Template.parse(fileContents, liqSettings);
					String liqRendered = liqTemplate.render(liquidMap);
					// System.out.println(liqRendered);
					contents = new ByteArrayInputStream(liqRendered.getBytes());
				} catch (Exception e) {
					throw new ProcessFailureException(getProcessMessage(processId, IStatus.ERROR,
							"Error in expanding template " + fileSourcePath + ": " + e.getMessage())); //$NON-NLS-1$
				}

			} else {
				try {
					contents = sourceURL.openStream();
				} catch (IOException e) {
					throw new ProcessFailureException(getProcessMessage(processId, IStatus.ERROR,
							Messages.getString("AddFiles.4") + fileSourcePath)); //$NON-NLS-1$
				}
			}

			try {
				IFile iFile = projectHandle.getFile(fileTargetPath);
				if (!iFile.getParent().exists()) {
					ProcessHelper.mkdirs(projectHandle,
							projectHandle.getFolder(iFile.getParent().getProjectRelativePath()));
				}

				if (iFile.exists()) {
					// honor the replaceable flag and replace the file contents
					// if the file already exists.
					if (replaceable) {
						iFile.setContents(contents, true, true, null);
					} else {
						throw new ProcessFailureException(Messages.getString("AddFiles.5")); //$NON-NLS-1$
					}

				} else {
					iFile.create(contents, true, null);
					iFile.refreshLocal(IResource.DEPTH_ONE, null);
				}
			} catch (CoreException e) {
				throw new ProcessFailureException(Messages.getString("AddFiles.6") + e.getMessage(), e); //$NON-NLS-1$
			}
		}
		try {
			projectHandle.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			throw new ProcessFailureException(Messages.getString("AddFiles.7") + e.getMessage(), e); //$NON-NLS-1$
		}
	}
}
