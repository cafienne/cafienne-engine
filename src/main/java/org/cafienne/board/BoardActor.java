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

package org.cafienne.board;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.board.actorapi.command.BoardCommand;
import org.cafienne.board.actorapi.event.BoardEvent;
import org.cafienne.board.actorapi.event.BoardModified;
import org.cafienne.board.state.BoardState;
import org.cafienne.board.state.definition.BoardDefinition;
import org.cafienne.system.CaseSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class BoardActor extends ModelActor {
    private final static Logger logger = LoggerFactory.getLogger(BoardActor.class);

    public final BoardState state = new BoardState(this);

    public BoardActor(CaseSystem caseSystem) {
        super(caseSystem);
    }

    @Override
    protected boolean supportsCommand(Object msg) {
        return msg instanceof BoardCommand;
    }

    @Override
    protected boolean supportsEvent(ModelEvent msg) {
        return msg instanceof BoardEvent;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public BoardDefinition getDefinition() {
        return state.definition();
    }

    @Override
    protected void recoveryCompleted() {
        super.recoveryCompleted();
        state.recoveryCompleted();
    }

    @Override
    protected void completeTransaction(IncomingActorMessage source) {
        addEvent(new BoardModified(this, source));
    }
}
