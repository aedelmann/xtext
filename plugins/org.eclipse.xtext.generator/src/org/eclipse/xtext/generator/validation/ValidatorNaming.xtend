/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.generator.validation

import com.google.inject.Inject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.xtext.Grammar
import org.eclipse.xtext.generator.Naming

import static org.eclipse.xtext.GrammarUtil.*
import static extension org.eclipse.xtext.generator.IInheriting.Util.*

/**
 * @author Jan Koehnlein
 * @since 2.4
 */
class ValidatorNaming {
	
	@Inject Grammar grammar
	
	@Inject extension Naming
	
	def getValidatorName(Grammar g) {
		'''�g.basePackageRuntime�.validation.�getName(g)�Validator'''.toString
	}

	def getAbstractValidatorName() {
		'''�grammar.basePackageRuntime�.validation.Abstract�getName(grammar)�Validator'''.toString
	}
	
	def getValidatorSuperClassName(boolean isInheritImplementation) {
		val superGrammar = grammar.nonTerminalsSuperGrammar
		if(isInheritImplementation && superGrammar != null) 
			superGrammar.validatorName 
		else
			'org.eclipse.xtext.validation.AbstractDeclarativeValidator'
	}
		
	def String getGeneratedEPackageName(EPackage pack) {
		return '''�grammar.basePackageRuntime�.�pack.getName()�.�pack.name.toFirstUpper�Package'''
	}
	
}