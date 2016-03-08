/********************************************************************************************
 * Copyright (c) 2009, 2015 Fabian Steeg, and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     Fabian Steeg - initial API and implementation (see bug #277380)
 *     Alexander Nyßen (alexander.nyssen@itemis.de) - fixed NPE (see bug #473011)
 *     Tamas Miklossy (itemis AG) - Refactoring of preferences (bug #446639)
 *                                - Exporting *.dot files in different formats (bug #446647)
 *                                - Naming of output file (bug #484198)
 *
 *********************************************************************************************/
package org.eclipse.gef4.dot.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Class for drawing dot graphs by calling the dot executable.
 * 
 * @author Fabian Steeg (fsteeg)
 * @author Alexander Nyßen (anyssen)
 */
final public class DotNativeDrawer {
	private DotNativeDrawer() {/* Enforce non-instantiability */
	}

	/**
	 * @param dotExecutablePath
	 *            The path of the local Graphviz 'dot' executable, e.g.
	 *            "C:\\Program Files (x86)\\Graphviz2.38\\bin\\dot.exe"
	 * @param dotInputFile
	 *            The DOT content to render
	 * @param format
	 *            The image format to export the graph to (e.g. 'pdf' or 'png')
	 * @param outputFileName
	 *            The output image file name, e.g. "output.pdf" or
	 *            <code>null</code> if the input file name should be used (where
	 *            the file extension is changed dependent on the format)
	 * @return The image file generated by rendering the dotInputFile with
	 *         Graphviz, using the specified format
	 */
	public static File renderImage(final File dotExecutablePath,
			final File dotInputFile, final String format,
			final String outputFileName) {
		String dotFile = dotInputFile.getName();
		String resultFile = outputFileName == null
				? dotFile.substring(0, dotFile.lastIndexOf('.') + 1) + format
				: outputFileName; // $NON-NLS-1$
		String inputFolder = new File(dotInputFile.getParent())
				.getAbsolutePath() + File.separator;
		String outputFolder = outputFileName == null ? inputFolder
				: new File(new File(outputFileName).getAbsolutePath())
						.getParentFile().getAbsolutePath() + File.separator;
		File outputFile = new File(outputFolder, resultFile);
		String[] outputs = executeDot(dotExecutablePath, dotInputFile,
				outputFile, format);
		if (!outputs[0].isEmpty()) {
			System.out.println("Output from dot call: " + outputs[0]); //$NON-NLS-1$
		}
		if (!outputs[1].isEmpty()) {
			System.err.println("Errors from dot call: " + outputs[1]); //$NON-NLS-1$
		}
		return outputFile;
	}

	/**
	 * Calls the Graphviz 'dot' executable with the given arguments.
	 * 
	 * @param dotExecutablePath
	 *            The path of the local Graphviz 'dot' executable, e.g.
	 *            "C:\\Program Files (x86)\\Graphviz2.38\\bin\\dot.exe"
	 * @param dotInputFile
	 *            The input file to pass to 'dot'.
	 * @param outputFile
	 *            The output file to pass to 'dot' via the -o option. May be
	 *            <code>null</code>.
	 * @param outputFormat
	 *            The output format to pass to 'dot' via the -T option. May be
	 *            <code>null</code>.
	 * @return A String array with two Strings, where the first contains the
	 *         output of the input stream and the second contains the output of
	 *         the error stream.
	 */
	public static String[] executeDot(final File dotExecutablePath,
			final File dotInputFile, final File outputFile,
			final String outputFormat) {
		String[] commands = new String[] { dotExecutablePath.getAbsolutePath(),
				outputFormat == null ? "" : "-T" + outputFormat,
				outputFile == null ? ""
						: "-o " + outputFile.toPath().toString(),
				dotInputFile.toPath().toString() };
		return call(commands);
	}

	/***
	 * @param dotExecutable
	 *            path to the dot executable
	 * @return String array of the supported export formats
	 */
	public static String[] getSupportedExportFormats(String dotExecutable) {

		String[] commands = { dotExecutable, "-T?" };

		String[] outputs = call(commands);
		String output = outputs[1];
		if (!output.isEmpty()) {
			String supportedFormats = output
					.substring(output.lastIndexOf(": ") + 2);
			supportedFormats = supportedFormats.trim();
			return supportedFormats.split(" ");
		} else {
			return new String[] {};
		}
	}

	/***
	 * @param commands
	 *            commands to be executed
	 * @return String array with two Strings The first String contains the
	 *         output of the input stream The second String contains the output
	 *         of the error stream
	 */
	private static String[] call(final String[] commands) {
		System.out.print("Calling '" + Arrays.asList(commands) + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		String[] outputs = { "", "" };
		Runtime runtime = Runtime.getRuntime();
		Process p = null;
		try {
			p = runtime.exec(commands);
			p.waitFor();
			System.out.println(
					" resulted in exit status: " + p.exitValue() + "."); //$NON-NLS-1$//$NON-NLS-2$
		} catch (Exception e) {
			System.out
					.println(" failed with exception " + e.getMessage() + "."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		// handle input and error stream only if process succeeded.
		if (p != null) {
			String output = read(p.getInputStream());
			outputs[0] = output.trim();
			String errors = read(p.getErrorStream());
			outputs[1] = errors.trim();
		}
		return outputs;
	}

	private static String read(InputStream s) {
		StringBuilder builder = new StringBuilder();
		try {
			int current = -1;
			while ((current = s.read()) != -1) {
				builder.append((char) current);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return builder.toString();
	}

}
