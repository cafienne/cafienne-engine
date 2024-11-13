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

package com.casefabric.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.cmmn.definition.casefile.PropertyDefinition;

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
