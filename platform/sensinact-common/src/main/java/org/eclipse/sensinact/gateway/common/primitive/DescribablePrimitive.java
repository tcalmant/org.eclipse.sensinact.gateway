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
package org.eclipse.sensinact.gateway.common.primitive;

import org.eclipse.sensinact.gateway.common.bundle.Mediator;
import org.json.JSONObject;

/**
 * A Primitive is a data structure mapping a name to a value whose type is
 * specified
 *
 * @author <a href="mailto:christophe.munilla@cea.fr">Christophe Munilla</a>
 */
public abstract class DescribablePrimitive extends Primitive implements Describable {
    /**
     * Creates and returns the {@link PrimitiveDescription}
     * of this DescribablePrimitive
     *
     * @return a {@link PrimitiveDescription} of this
     * DescribablePrimitive
     */
    protected abstract <P extends PrimitiveDescription> P createDescription();

    /**
     * WeakReference to this DescribablePrimitive's
     * {@link PrimitiveDescription}
     */
    @SuppressWarnings("rawtypes")
    protected WeakDescription weakDescription;

    /**
     * Constructor
     *
     * @param name the name of the Primitive to instantiate
     * @param type the type of the Primitive to instantiate
     */
    protected DescribablePrimitive(Mediator mediator, String name, Class<?> type) throws InvalidValueException {
        super(mediator, name, type);
    }

    /**
     * @param name
     * @param string
     * @throws InvalidValueException
     */
    protected DescribablePrimitive(Mediator mediator, String name, String type) throws InvalidValueException {
        super(mediator, name, type);
    }

    /**
     * Constructor
     *
     * @param name  the name of the Primitive to instantiate
     * @param type  the type of the Primitive to instantiate
     * @param value the value of the Primitive to instantiate
     * @throws InvalidConstraintDefinitionException
     */
    protected DescribablePrimitive(Mediator mediator, String name, Class<?> type, Object value) throws InvalidValueException {
        super(mediator, name, type, value);
    }

    /**
     * Constructor
     *
     * @param jsonObject the JSONObject describing the Primitive
     *                   to instantiate
     */
    protected DescribablePrimitive(Mediator mediator, JSONObject jsonObject) throws InvalidValueException {
        super(mediator, jsonObject);
    }

    /**
     * @inheritDoc
     * @see Primitive#beforeChange(java.lang.Object)
     */
    @Override
    protected void beforeChange(Object value) throws InvalidValueException {
        //do nothing
    }

    /**
     * @inheritDoc
     * @see Primitive#
     * setValue(java.lang.Object, long)
     */
    @Override
    public void afterChange(Object value) throws InvalidValueException {
        if (this.weakDescription != null) {
            this.weakDescription.update(value);
        }
    }

    /**
     * @inheritDoc
     * @see Describable#getDescription()
     */
    @Override
    public <D extends Description> D getDescription() {
        D description = null;

        if (this.weakDescription == null || (description = (D) this.weakDescription.get()) == null) {
            description = (D) this.createDescription();
            this.weakDescription = new WeakDescription((PrimitiveDescription) description);
        }
        return description;
    }
}
