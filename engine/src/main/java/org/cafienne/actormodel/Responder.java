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

package org.cafienne.actormodel;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.response.CommandFailureListener;
import org.cafienne.actormodel.response.CommandResponseListener;

public class Responder {
    public final ModelCommand command;
    public final CommandResponseListener right;
    public final CommandFailureListener left;

    public Responder(ModelCommand command, CommandFailureListener left, CommandResponseListener... right) {
        this.command = command;
        this.left = left == null ? e -> {} : left;
        this.right = right.length > 0 && right[0] != null ? right[0] : e -> {};
    }
}
