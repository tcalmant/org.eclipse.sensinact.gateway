/*********************************************************************
* Copyright (c) 2021 Kentyou and others
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.sensinact.gateway.datastore.api;

/**
 *
 */
@SuppressWarnings("serial")
public class UnableToLoadDriverClassException extends DataStoreException {
    /**
     * Constructor
     *
     * @param message the error message
     */
    public UnableToLoadDriverClassException(String message) {
        super(message);
    }

    /**
     * Constructor
     *
     * @param message   the error message
     * @param throwable wrapped exception that has made the current one thrown
     */
    public UnableToLoadDriverClassException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
