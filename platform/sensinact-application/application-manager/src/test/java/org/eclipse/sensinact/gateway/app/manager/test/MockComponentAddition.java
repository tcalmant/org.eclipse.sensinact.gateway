/*********************************************************************
* Copyright (c) 2021 Kentyou and others
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.sensinact.gateway.app.manager.test;

import org.eclipse.sensinact.gateway.app.api.function.AbstractFunction;
import org.eclipse.sensinact.gateway.app.api.function.DataItf;
import org.eclipse.sensinact.gateway.app.manager.osgi.AppServiceMediator;
import org.eclipse.sensinact.gateway.util.CastUtils;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

class MockComponentAddition extends AbstractFunction<Integer> {
    private static final String JSON_SCHEMA = "mock_addition.json";
    private final AppServiceMediator mediator;

    MockComponentAddition(AppServiceMediator mediator) {
        this.mediator = mediator;
    }

    static JSONObject getJSONSchemaFunction(BundleContext context) {
        try {
            return new JSONObject(new JSONTokener(new InputStreamReader(context.getBundle().getResource("/" + JSON_SCHEMA).openStream())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void process(List<DataItf> variables) {
        super.update((Integer) CastUtils.cast(variables.get(0).getType(), variables.get(0).getValue()) + (Integer) CastUtils.cast(variables.get(1).getType(), variables.get(1).getValue()));
    }
}
