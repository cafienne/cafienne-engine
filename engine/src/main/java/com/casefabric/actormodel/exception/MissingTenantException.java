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

package org.cafienne.actormodel.exception;

import org.cafienne.actormodel.command.ModelCommand;

/**
 * Every {@link ModelCommand} must have a tenant set.
 * If this is missing, then the command cannot be handled, and this exception is thrown.
 */
public class MissingTenantException extends InvalidCommandException {
    public MissingTenantException(String msg) {
        super(msg);
    }
}
