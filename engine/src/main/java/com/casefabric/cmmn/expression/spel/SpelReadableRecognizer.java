/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.casefabric.cmmn.expression.spel;

import com.casefabric.actormodel.ModelActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * Hook for Spel through which we can read CaseFabric specific API properties in expressions.
 */
public class SpelReadableRecognizer implements PropertyAccessor {
    private final static Logger logger = LoggerFactory.getLogger(SpelReadableRecognizer.class);

    public final ModelActor model;

    public SpelReadableRecognizer(ModelActor model) {
        this.model = model;
    }

    @Override
    public boolean canRead(EvaluationContext context, Object object, String propertyName) {
        boolean canRead = false;
        if (object instanceof SpelReadable) {
            canRead = ((SpelReadable) object).canRead(propertyName);
        }
        return canRead;
    }

    @Override
    public TypedValue read(EvaluationContext arg0, Object object, String propertyName) {
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

