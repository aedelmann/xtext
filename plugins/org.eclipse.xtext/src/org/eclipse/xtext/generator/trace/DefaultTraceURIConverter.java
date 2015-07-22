/*******************************************************************************
 * Copyright (c) 2013 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.generator.trace;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.workspace.IProjectConfig;
import org.eclipse.xtext.workspace.IWorkspaceConfig;
import org.eclipse.xtext.workspace.IWorkspaceConfigProvider;

import com.google.inject.Inject;

/**
 * Default implementation of the {@link ITraceURIConverter} that is based on the
 * workspace data (see {@link IWorkspaceConfigProvider}).
 * 
 * @author Moritz Eysholdt - Initial contribution and API
 */
public class DefaultTraceURIConverter implements ITraceURIConverter {
	
	@Inject(optional = true) 
	private IWorkspaceConfigProvider configProvider;

	@Override
	public SourceRelativeURI getURIForTrace(IProjectConfig projectConfig, AbsoluteURI absoluteURI) {
		SourceRelativeURI result = absoluteURI.deresolve(projectConfig);
		if (result == null) {
			return getURIForTrace(absoluteURI.getURI());
		}
		return result;
	}

	@Override
	public SourceRelativeURI getURIForTrace(Resource resource) {
		if (configProvider != null) {
			IWorkspaceConfig workspaceConfig = configProvider.getWorkspaceConfig(resource.getResourceSet());
			IProjectConfig projectConfig = workspaceConfig.findProjectContaining(resource.getURI());
			if (projectConfig != null) {
				return getURIForTrace(projectConfig, new AbsoluteURI(resource.getURI()));
			}
		}
		return getURIForTrace(resource.getURI());
	}

	private SourceRelativeURI getURIForTrace(URI qualifiedURI) {
		if (qualifiedURI.isPlatform()) {
			// create a URI that is relative to the containing project or bundle
			List<String> segments = qualifiedURI.segmentsList().subList(2, qualifiedURI.segmentCount());
			return new SourceRelativeURI(URI.createHierarchicalURI(segments.toArray(new String[segments.size()]), null, null));
		}
		return SourceRelativeURI.fromAbsolute(qualifiedURI);
	}
}
