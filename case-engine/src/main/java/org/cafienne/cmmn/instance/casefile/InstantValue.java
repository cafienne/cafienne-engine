/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.casefile;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;

import java.io.IOException;
import java.time.Instant;

public class InstantValue extends PrimitiveValue<Instant> {
	public InstantValue(Instant value) {
		super(value);
	}

	@Override
	public InstantValue cloneValueNode() {
		return new InstantValue(value);
	}
	@Override
	public boolean matches(PropertyDefinition.PropertyType propertyType) {
		switch (propertyType) {
		case Date:
		case Time: // Hmmm, do we really match strings?
		case DateTime:
		case Unspecified:
			return true;
		default:
			return baseMatch(propertyType);
		}
	}

	@Override
	public void print(JsonGenerator generator) throws IOException {
		generator.writeString(value.toString());
	}
}
