/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.idea.shared

import org.eclipse.xtext.common.types.access.IJvmTypeProvider
import org.eclipse.xtext.idea.common.types.StubTypeProviderFactory
import org.eclipse.xtext.psi.IPsiModelAssociator
import org.eclipse.xtext.psi.PsiModelAssociations
import org.eclipse.xtext.service.AbstractGenericModule
import org.eclipse.xtext.resource.IResourceServiceProvider

/**
 * @author Jan Koehnlein - Initial contribution and API
 */
class IdeaSharedModule extends AbstractGenericModule {

	def Class<? extends IJvmTypeProvider.Factory> bindIJvmTypeProvider$Factory() {
		StubTypeProviderFactory
	}	
	
	def IResourceServiceProvider.Registry getIResourceServiceProvider$Registry() {
		IResourceServiceProvider.Registry.INSTANCE
	}

	def Class<? extends IPsiModelAssociator> bindIPsiModelAssociator() {
		PsiModelAssociations
	}
}