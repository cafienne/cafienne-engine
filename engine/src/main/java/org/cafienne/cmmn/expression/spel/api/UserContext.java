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

package org.cafienne.cmmn.expression.spel.api;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.identity.UserIdentity;

import java.util.ArrayList;

/**
 * Wrapper to access user information from spel expressions
 */
public class UserContext extends APIObject<ModelActor> {
    public UserContext(ModelActor actor, UserIdentity user) {
        super(actor);
        addPropertyReader("id", user::id);
        addPropertyReader("token", user::token);
        addDeprecatedReader("roles", ArrayList<String>::new);
        addDeprecatedReader("name", () -> "");
        addDeprecatedReader("email", () -> "");
    }
}
