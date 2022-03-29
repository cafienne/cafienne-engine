/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.expression.spel;

import org.cafienne.actormodel.ModelActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * Hook for Spel through which we can read Cafienne specific API properties in expressions.
 */
public class SpelReadableRecognizer implements PropertyAccessor {
    private final static Logger logger = LoggerFactory.getLogger(SpelReadableRecognizer.class);

    public final ModelActor model;

    public SpelReadableRecognizer(ModelActor model) {
        this.model = model;
    }

    @Override
    public boolean canRead(EvaluationContext context, Object object, String propertyName) throws AccessException {
        boolean canRead = false;
        if (object instanceof SpelReadable) {
            canRead = ((SpelReadable) object).canRead(propertyName);
        }
        return canRead;
    }

    @Override
    public TypedValue read(EvaluationContext arg0, Object object, String propertyName) throws AccessException {
        if (object instanceof SpelReadable) {
            // Read the value
            Object value = ((SpelReadable) object).read(propertyName);
            // Check if it is the native value itself, or that the object is an indirect reference to the value.
            if (value instanceof SpelPropertyValueProvider) {
                value = ((SpelPropertyValueProvider) value).getValue();
            }
            model.addDebugInfo(() -> "Reading property '" + propertyName + "' results in value: ", value);
            return new TypedValue(value);
        } else {
            // It is actually weird if we end up in this code. Since it means that on 'canRead' we have returned true...
            if (object == null) {
                model.addDebugInfo(() -> "Cannot read property " + propertyName + " from null object");
            } else {
                model.addDebugInfo(() -> "Cannot read property " + propertyName + " from strange context of type " + object.getClass().getName() + ": with value ", object);
            }
            return null;
        }
    }

    @Override
    public Class<?>[] getSpecificTargetClasses() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canWrite(EvaluationContext arg0, Object object, String propertyName) throws AccessException {
        logger.warn("Writing into properties is not supported. Expression is trying to write into property " + propertyName);
        return true;
    }

    @Override
    public void write(EvaluationContext arg0, Object object, String propertyName, Object propertyValue) throws AccessException {
        logger.warn("Not supported: writing into property " + propertyName + " with value " + propertyValue);
    }
}

