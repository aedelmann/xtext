/*******************************************************************************
 * Copyright (c) 2013 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.generator.trace;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.workspace.IProjectConfig;

import com.google.inject.ImplementedBy;


/**
 * This class converts URIs between their absolute forms and a relative form, which
 * is persisted in trace files. Since trace files may be packed in JARs, the URIs should not make assumptions about the
 * environments outside of the JARs, such as project name and source folders.
 * 
 * @author Moritz Eysholdt - Initial contribution and API
 * @author Holger Schill
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ImplementedBy(DefaultTraceURIConverter.class)
public interface ITraceURIConverter {
	
	URI getURIForTrace(IProjectConfig projectConfig, URI qualifiedUri);
	
	URI getURIForTrace(Resource resource);

}
