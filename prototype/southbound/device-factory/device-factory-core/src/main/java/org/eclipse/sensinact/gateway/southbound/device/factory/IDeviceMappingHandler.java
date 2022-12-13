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
package org.eclipse.sensinact.gateway.southbound.device.factory;

import org.eclipse.sensinact.gateway.southbound.device.factory.dto.DeviceMappingConfigurationDTO;
import org.eclipse.sensinact.prototype.SensiNactSession;

/**
 * Definition of the device mapping handler
 */
public interface IDeviceMappingHandler {

    /**
     * Handles the content of the given payload and updates resources accordingly
     *
     * @param session       sensiNact session to use to update resources
     * @param configuration Mapping configuration (must contain the parser ID)
     * @param payload       Raw content to parse
     * @throws DeviceFactoryException Error handling records
     */
    void handle(SensiNactSession session, DeviceMappingConfigurationDTO configuration, byte[] payload)
            throws MissingParserException, InvalidResourcePathException, ParserException, DeviceFactoryException;

}
