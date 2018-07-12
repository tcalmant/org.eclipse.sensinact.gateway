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
package org.eclipse.sensinact.gateway.common.constraint;

import java.util.logging.Level;

/**
 * @param <T>
 * @author <a href="mailto:christophe.munilla@cea.fr">Christophe Munilla</a>
 */
public class Fixed<T> extends ConstraintOnComparable<T> {
    public static final String OPERATOR = "=";

    /**
     * Constructor
     *
     * @param operator
     * @param operandClass
     * @param operand
     * @throws InvalidConstraintDefinitionException
     */
    public Fixed(ClassLoader classloader, Class<T> operandClass, Object operand, boolean complement) throws InvalidConstraintDefinitionException {
        super(classloader, OPERATOR, operandClass, operand, complement);
    }

    /**
     * @inheritDoc
     * @see Constraint#
     * doComplies(java.lang.Object)
     */
    @Override
    public boolean doComplies(T castedValue) {
        return (super.operand.compareTo(castedValue) == 0) ^ isComplement();
    }

    /**
     * Returns this Fixed constraint operand value
     *
     * @return this constraint operand value
     */
    public final Object getValue() {
        return super.operand;
    }

    /**
     * @inheritDoc
     * @see Constraint#getComplement()
     */
    @Override
    public Constraint getComplement() {
        Fixed complement = null;
        try {
            complement = new Fixed(super.classloader, (Class<Comparable<T>>) super.operand.getClass(), super.operand, !this.complement);
        } catch (InvalidConstraintDefinitionException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return complement;
    }
}
