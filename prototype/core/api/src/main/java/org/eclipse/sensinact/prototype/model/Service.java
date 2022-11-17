/*********************************************************************
* Copyright (c) 2022 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   Kentyou - initial implementation 
**********************************************************************/
package org.eclipse.sensinact.prototype.model;

import java.util.Map;

/**
 * The model for a Service
 */
public interface Service extends Modelled {

    ResourceBuilder<?> createResource(String resource);

    Map<String, ? extends Resource> getResources();

    Provider getProvider();

}