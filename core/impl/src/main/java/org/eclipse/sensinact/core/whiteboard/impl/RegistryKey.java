/*********************************************************************
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   Kentyou - initial API and implementation
**********************************************************************/
package org.eclipse.sensinact.core.whiteboard.impl;

import java.util.Objects;

class RegistryKey {
    private final String model;
    private final String service;
    private final String resource;

    public RegistryKey(String model, String service, String resource) {
        this.model = model;
        this.service = service;
        this.resource = resource;
    }

    public String getModel() {
        return model;
    }

    public String getService() {
        return service;
    }

    public String getResource() {
        return resource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, resource, service);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RegistryKey other = (RegistryKey) obj;
        return Objects.equals(model, other.model) && Objects.equals(resource, other.resource)
                && Objects.equals(service, other.service);
    }

    @Override
    public String toString() {
        return "RegistryKey [model=" + model + ", service=" + service + ", resource=" + resource + "]";
    }
}
