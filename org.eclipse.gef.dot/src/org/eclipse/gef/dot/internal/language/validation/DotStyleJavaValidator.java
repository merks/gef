/*******************************************************************************
 * Copyright (c) 2016 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *******************************************************************************/
package org.eclipse.gef.dot.internal.language.validation;

/**
 * This class contains custom validation rules.
 *
 * See
 * https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 */
public class DotStyleJavaValidator extends
		org.eclipse.gef.dot.internal.language.validation.AbstractDotStyleJavaValidator {

	// @Check
	// public void checkGreetingStartsWithCapital(Greeting greeting) {
	// if (!Character.isUpperCase(greeting.getName().charAt(0))) {
	// warning("Name should start with a capital",
	// MyDslPackage.Literals.GREETING__NAME);
	// }
	// }
	// TODO: validate edge style and node style
}