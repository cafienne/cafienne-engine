/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.expression.spel;

import org.cafienne.cmmn.instance.casefile.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

class CaseFileAccessor implements PropertyAccessor {
    private final static Logger logger = LoggerFactory.getLogger(CaseFileAccessor.class);

    @Override
    public void write(EvaluationContext arg0, Object object, String propertyName, Object propertyValue) throws AccessException {
        logger.warn("Not supported: writing into property " + propertyName + " with value " + propertyValue);
    }

    @Override
    public TypedValue read(EvaluationContext arg0, Object object, String propertyName) throws AccessException {
        if (object instanceof SpelReadable) {
            Value<?> value = ((SpelReadable) object).read(propertyName);
            logger.debug("Reading property "+propertyName+" results in value "+value);
            return new TypedValueWrapper(value);
        } else {
            logger.error("Cannot read property " + propertyName + " from strange context : " + object);
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
    public boolean canRead(EvaluationContext context, Object object, String propertyName) throws AccessException {
        boolean canRead = false;
        if (object instanceof SpelReadable) {
            canRead = ((SpelReadable) object).canRead(propertyName);
        }
        return canRead;
    }
}

/**
 * Simple wrapper around value that extends SPEL's TypedValue, so that we can return the proper value from getValue()
 *
 */
class TypedValueWrapper extends TypedValue {

    private final Value<?> value;

    TypedValueWrapper(Value<?> value) {
        super(value);
        this.value = value;
    }

    @Override
    public Object getValue() {
        if (value == null) {
            return null;
        } else if (value.isPrimitive()) {
            return value.getValue();
        } else {
            return value;
        }
    }

}

