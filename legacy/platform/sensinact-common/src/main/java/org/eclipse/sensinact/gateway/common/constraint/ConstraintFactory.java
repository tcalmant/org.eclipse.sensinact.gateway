/*********************************************************************
* Copyright (c) 2021 Kentyou and others
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.sensinact.gateway.common.constraint;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.ServiceLoader;

import javax.swing.SpringLayout.Constraints;

import org.eclipse.sensinact.gateway.common.bundle.Mediator;
import org.eclipse.sensinact.gateway.util.CastUtils;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

/**
 * {@link Constraint} factory service
 *
 * @author <a href="mailto:christophe.munilla@cea.fr">Christophe Munilla</a>
 */
public interface ConstraintFactory {

    public static final class Loader {
        /**
         * @param constraint
         * @param referenceClass
         * @param reference
         * @return
         * @throws InvalidConstraintDefinitionException
         */
        public static Constraint load(ClassLoader classloader, String constraint, Class<?> referenceClass, Object reference, boolean complement) throws InvalidConstraintDefinitionException {
            ConstraintFactory factory = new DefaultConstraintFactory();

            if (!factory.handle(constraint)) {
                factory = null;

                ServiceLoader<ConstraintFactory> serviceLoader = ServiceLoader.load(ConstraintFactory.class, classloader);

                Iterator<ConstraintFactory> iterator = serviceLoader.iterator();

                while (iterator.hasNext()) {
                    factory = (ConstraintFactory) iterator.next();
                    if (factory.handle(constraint)) {
                        break;
                    }
                    factory = null;
                }
            }
            if (factory == null) {
                throw new InvalidConstraintDefinitionException("No factory found");
            }
            return factory.newInstance(constraint, referenceClass, reference, complement);
        }

        /**
         * Returns the {@link Constraint} described by the constraint object
         * passed as parameter
         *
         * @param classloader the {@link Mediator} allowing to interact with the
         *                    OSGi host environment and from which to retrieve
         *                    the appropriate ClassLoader
         * @param constraint  the constraint object
         * @return the {@link Constraint} described by the specified
         * constraint object
         * @throws InvalidConstraintDefinitionException
         */
        public static Constraint load(ClassLoader classloader, Object constraint) throws InvalidConstraintDefinitionException {
            if (constraint == null) {
                throw new InvalidConstraintDefinitionException("Invalid constraint definition : null");
            }
            if (Constraint.class.isAssignableFrom(constraint.getClass())) {
                return (Constraint) constraint;
            }
            if (JsonObject.class.isAssignableFrom(constraint.getClass())) {
            	return Loader.load(classloader, (JsonObject) constraint);
            }
            if (JsonArray.class.isAssignableFrom(constraint.getClass())) {
            	return Loader.load(classloader, (JsonArray) constraint);
            }
            throw new InvalidConstraintDefinitionException(new StringBuilder().append("Unable to cast ").append(constraint.getClass()).append(" into a constraint").toString());
        }

        /**
         * @param constraint
         * @return
         * @throws InvalidConstraintDefinitionException
         */
        public static Constraint load(ClassLoader classloader, JsonObject constraint) throws InvalidConstraintDefinitionException {
            Object operand = null;
            String type = constraint.getString(Constraint.TYPE_KEY, null);
            Class<?> clazz = null;

            if (type != null && type.length() > 0) {
                try {
                    clazz = CastUtils.loadClass(classloader, type);
                } catch (ClassNotFoundException e) {

                    throw new InvalidConstraintDefinitionException(new StringBuilder().append("Invalid type :").append(type).toString());
                }
            }
            String operator = constraint.getString(Constraint.OPERATOR_KEY, null);
            if (operator == null || operator.length() == 0) {
                throw new InvalidConstraintDefinitionException("Null operator");
            }
            JsonValue operandObject = constraint.get(Constraint.OPERAND_KEY);
            if (operandObject != null) {
                if (operandObject.getValueType() == ValueType.ARRAY) {
                    JsonArray operandArray = operandObject.asJsonArray();
                    int length = operandArray.size();
                    switch (length) {
                        case 0:
                            break;
                        case 1:
                            operand = operandArray.get(0);
                            break;
                        default:
                            operand = new Object[length];
                            int index = 0;
                            for (; index < length; index++) {
                                Array.set(operand, index, operandArray.get(index));
                            }
                    }
                } else {
                    operand = operandObject;
                }
            }
            boolean complement = constraint.getBoolean(Constraint.COMPLEMENT_KEY, false);

            return Loader.load(classloader, operator, clazz, operand, complement);
        }

        /**
         * Returns the {@link Expression} gathering the {@link Constraints}
         * described in the {@link JSONArray} passed as parameter linked by a
         * {@link Expression.LogicalOperator.AND} logical operator
         *
         * @param mediator   the {@link Mediator} allowing to interact with the
         *                   OSGi host environment and from which to retrieve
         *                   the appropriate ClassLoader
         * @param constraint the JSON formated array of constraints
         * @return the {@link Expression} gathering the specified JSON
         * formated array of constraints
         * @throws InvalidConstraintDefinitionException
         */
        public static Constraint load(ClassLoader classloader, JsonArray constraint) throws InvalidConstraintDefinitionException {
            if (constraint == null) {
                throw new InvalidConstraintDefinitionException("Invalid constraint definition : null");
            }
            Expression.LogicalOperator logicalOperator = null;
            try {
                logicalOperator = Expression.LogicalOperator.valueOf(constraint.getString(0));
                constraint.remove(0);

            } catch (Exception e) {
                JsonObject uniqueConstraint = null;

                if (constraint.size() == 1 && (uniqueConstraint = constraint.getJsonObject(0)) != null) {
                    return Loader.load(classloader, uniqueConstraint);
                }
                logicalOperator = Expression.LogicalOperator.AND;
            }
            return Loader.load(classloader, constraint, logicalOperator);
        }

        /**
         * Returns the {@link Expression} gathering the
         * {@link Constraints} described in the {@link JSONArray}
         * passed as parameter
         *
         * @param mediator        the {@link Mediator} allowing to interact with the
         *                        OSGi host environment and from which to retrieve
         *                        the appropriate ClassLoader
         * @param constraint      the JSON formated array of constraints
         * @param logicalOperator the {@link Expression.LogicalOperator} between the
         *                        set of constraints
         * @return {@link Expression} gathering the specified JSON
         * formated array of constraints
         * @throws InvalidConstraintDefinitionException
         */
        public static Constraint load(ClassLoader classloader, JsonArray constraint, Expression.LogicalOperator logicalOperator) throws InvalidConstraintDefinitionException {
            int index = 0;
            int length = constraint == null ? 0 : constraint.size();

            Expression expression = new Expression(logicalOperator);
            for (; index < length; index++) {
                Object embeddedConstraint = constraint.get(index);
                if (JsonObject.class.isAssignableFrom(embeddedConstraint.getClass())) {
                    expression.add(Loader.load(classloader, (JsonObject) embeddedConstraint));

                } else if (JsonArray.class.isAssignableFrom(embeddedConstraint.getClass())) {
                    expression.add(Loader.load(classloader, (JsonArray) embeddedConstraint));

                } else {
                    throw new InvalidConstraintDefinitionException(new StringBuilder().append("Invalid constraint definition : ").append(embeddedConstraint).toString());
                }
            }
            return expression;
        }
    }

    /**
     * Returns true if this ConstraintFactory handles
     * the {@link Constraint} whose name (operator) is
     * passed as parameter and is so able to instantiate
     * one. Returns false otherwise
     *
     * @param constraint the name (operator) of the {@link Constraint}
     * @return <ul>
     * <li>true if the {@link Constraint} can
     * be instantiated by this factory</li>
     * <li>false otherwise</li>
     * </ul>
     */
    boolean handle(String constraint);

    /**
     * Returns a new instantiated {@link Constraint} using
     * the name (operator) passed as parameter, as well as
     * the reference type and the reference value
     *
     * @param constraint    the name (operator) of the {@link Constraint}
     *                      to instantiate
     * @param referenceType the expected type of the checked value
     * @param reference     the reference value
     * @param complement    defines whether to create the logical
     *                      complement constraint
     * @return the new created {@link Constraint}
     * @throws InvalidConstraintDefinitionException
     */
    Constraint newInstance(String constraint, Class<?> referenceType, Object reference, boolean complement) throws InvalidConstraintDefinitionException;

}