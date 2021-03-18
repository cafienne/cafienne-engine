/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.expression.spel;

/**
 * Interface that is implemented by objects that can return the value from their own defined location,
 * instead of treating the object itself.
 */
@FunctionalInterface
public interface SpelPropertyValueProvider {
    /**
     * Get the value of the property
     * @return
     */
    Object getValue();
}
