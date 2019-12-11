/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.expression.spel;

import org.cafienne.cmmn.instance.casefile.Value;

/**
 * Interface to make case file values readable for the spel engine.
 *
 */
public interface SpelReadable {
    Value<?> read(String propertyName);

    boolean canRead(String propertyName);
}
