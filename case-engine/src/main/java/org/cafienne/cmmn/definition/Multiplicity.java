/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

/**
 * Enum specifying the multiplicity of a case file item.
 *
 */
public enum Multiplicity {
    ZeroOrOne, ZeroOrMore, ExactlyOne, OneOrMore, Unspecified, Unknown;
    
    /**
     * Indicates whether this multiplicity is iterable (i.e., whether the case file item may contain multiple contents or just one)
     * @return
     */
    public boolean isIterable()
    {
        return this == ZeroOrMore || this == OneOrMore;
    }
}
