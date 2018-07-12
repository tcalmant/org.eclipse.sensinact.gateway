/*
 * Copyright (c) 2017 CEA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    CEA - initial API and implementation
 */
package org.eclipse.sensinact.gateway.nthbnd.endpoint;

import org.eclipse.sensinact.gateway.core.FilteringCollection;
import org.eclipse.sensinact.gateway.util.UriUtils;

public class ResourcesRequest extends ServiceRequest {
    /**
     * @param mediator
     * @param responseFormat
     * @param serviceProvider
     * @param service
     * @param filteringCollection
     */
    public ResourcesRequest(NorthboundMediator mediator, String requestIdentifier, String serviceProvider, String service, FilteringCollection filteringCollection) {
        super(mediator, requestIdentifier, serviceProvider, service, filteringCollection);
    }

    /**
     * @inheritDoc
     * @see ServiceRequest#getName()
     */
    public String getName() {
        return null;
    }

    /**
     * @inheritDoc
     * @see ServiceRequest#getPath()
     */
    @Override
    public String getPath() {
        return new StringBuilder().append(super.getPath()).append(UriUtils.PATH_SEPARATOR).append("resources").toString();
    }

    /**
     * @inheritDoc
     * @see ServiceRequest#getMethod()
     */
    @Override
    protected String getMethod() {
        return "resourcesList";
    }

    /**
     * @inheritDoc
     * @see NorthboundRequest#getExecutionArguments()
     */
    @Override
    protected Argument[] getExecutionArguments() {
        if (this.getClass() == ResourcesRequest.class && super.filteringCollection != null) {
            Argument[] superArguments = super.getExecutionArguments();
            int length = superArguments == null ? 0 : superArguments.length;
            Argument[] arguments = new Argument[length + 1];
            if (length > 0) {
                System.arraycopy(superArguments, 0, arguments, 0, length);
            }
            arguments[length] = new Argument(FilteringCollection.class, super.filteringCollection);
            return arguments;
        }
        return super.getExecutionArguments();
    }

}
