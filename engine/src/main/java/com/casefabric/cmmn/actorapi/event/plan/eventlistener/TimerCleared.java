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

package com.casefabric.cmmn.actorapi.event.plan.eventlistener;

import com.casefabric.cmmn.instance.TimerEvent;
import com.casefabric.json.ValueMap;

/**
 * Base class when the case no longer needs the timer to "actively" run.
 * Either due to completion or due to suspension.
 */
public abstract class TimerCleared extends TimerBaseEvent {
    protected TimerCleared(TimerEvent timerEvent) {
        super(timerEvent);
    }

    protected TimerCleared(ValueMap json) {
        super(json);
    }
}
