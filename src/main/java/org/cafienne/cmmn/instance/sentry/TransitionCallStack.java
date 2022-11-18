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

package org.cafienne.cmmn.instance.sentry;

import org.cafienne.infrastructure.enginedeveloper.EngineDeveloperConsole;

import java.util.ArrayList;
import java.util.List;

class TransitionCallStack {
    private final SentryNetwork handler;
    private Frame currentFrame = null;

    TransitionCallStack(SentryNetwork handler) {
        this.handler = handler;
    }

    void pushEvent(StandardEvent event) {
        if (event.hasBehavior()) {
            Frame frame = new Frame(event, currentFrame);
            frame.invokeImmediateBehavior();
            frame.postponeDelayedBehavior();
        }
    }

    private class Frame {
        private final StandardEvent event;
        private final Frame parent;
        private final List<Frame> children = new ArrayList<>();
        private final int depth;

        Frame(StandardEvent event, Frame parent) {
            this.event = event;
            this.parent = parent;
            this.depth = parent == null ? 1 : parent.depth + 1;
        }

        private String print(String msg) {
            return msg +" for " + event.getSource().getDescription() +"." + event.getTransition();
        }

        private void postponeDelayedBehavior() {
            if (currentFrame == null) {
                // Top level, immediately execute the delayed behavior
                invokeDelayedBehavior();
            } else {
                // Postpone the execution of the delayed behavior
                currentFrame.children.add(0, this);
                if (EngineDeveloperConsole.enabled()) {
                    EngineDeveloperConsole.debugIndentedConsoleLogging(print("* postponing delayed behavior"));
                    EngineDeveloperConsole.indent(2);
                    currentFrame.children.forEach(frame -> {
                        EngineDeveloperConsole.debugIndentedConsoleLogging("- " + frame.event.getDescription());
                    });
                    EngineDeveloperConsole.outdent(2);
                }
            }
        }

        void invokeImmediateBehavior() {
            EngineDeveloperConsole.indent(2);
            Frame next = currentFrame;
            currentFrame = this;
            if (EngineDeveloperConsole.enabled()) {
                EngineDeveloperConsole.debugIndentedConsoleLogging("\n-------- " + this + print("Running immmediate behavior"));
            }
            EngineDeveloperConsole.indent(1);
            this.event.runImmediateBehavior();
            EngineDeveloperConsole.outdent(1);
            if (EngineDeveloperConsole.enabled()) {
                EngineDeveloperConsole.debugIndentedConsoleLogging("-------- " + this + print("Finished immmediate behavior") + "\n");
            }
            EngineDeveloperConsole.outdent(2);
            currentFrame = next;
        }

        void invokeDelayedBehavior() {
            Frame next = currentFrame;
            currentFrame = this;
            EngineDeveloperConsole.indent(2);
            if (EngineDeveloperConsole.enabled()) {
                EngineDeveloperConsole.debugIndentedConsoleLogging("\n******** " + this + print("Running delayed behavior"));
            }
            EngineDeveloperConsole.indent(1);
            event.runDelayedBehavior();
            if (children.size() > 0) {
                if (EngineDeveloperConsole.enabled()) {
                    EngineDeveloperConsole.debugIndentedConsoleLogging(this + "Loading " + children.size() + " nested frames at level [" + (depth + 1) + "] as a consequence of " + event.getDescription());
                }
            }
            children.forEach(frame -> frame.invokeDelayedBehavior());
            EngineDeveloperConsole.outdent(1);
            if (EngineDeveloperConsole.enabled()) {
                EngineDeveloperConsole.debugIndentedConsoleLogging("******** " + this + print("Completed delayed behavior"));
            }
            EngineDeveloperConsole.outdent(2);
            currentFrame = next;
        }

        @Override
        public String toString() {
            return "StackFrame[" + depth + "]: ";
        }
    }
}
