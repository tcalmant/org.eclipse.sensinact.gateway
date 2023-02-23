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
*   Kentyou - initial implementation
**********************************************************************/
package org.eclipse.sensinact.northbound.ws.dto;

import java.util.Map;

/**
 * Represents a WebSocket query root body
 */
public class WsQueryDTO {

    /**
     * Target path: protocol, host and port are ignored if given
     */
    public String uri;

    /**
     * JSON object of parameters
     */
    public Map<String, Object> parameters;
}
