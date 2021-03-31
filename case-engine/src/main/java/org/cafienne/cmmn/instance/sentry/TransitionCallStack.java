package org.cafienne.cmmn.instance.sentry;

import org.cafienne.akka.actor.CaseSystem;

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
        private final List<Frame> children = new ArrayList();
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
                if (CaseSystem.devDebugLogger().enabled()) {
                    CaseSystem.devDebugLogger().debugIndentedConsoleLogging(print("* postponing delayed behavior"));
                    CaseSystem.devDebugLogger().indent(2);
                    currentFrame.children.forEach(frame -> {
                        CaseSystem.devDebugLogger().debugIndentedConsoleLogging("- " + frame.event.getDescription());
                    });
                    CaseSystem.devDebugLogger().outdent(2);
                }
            }
        }

        void invokeImmediateBehavior() {
            CaseSystem.devDebugLogger().indent(2);
            Frame next = currentFrame;
            currentFrame = this;
            if (CaseSystem.devDebugLogger().enabled()) {
                CaseSystem.devDebugLogger().debugIndentedConsoleLogging("\n-------- " + this + print("Running immmediate behavior"));
            }
            CaseSystem.devDebugLogger().indent(1);
            this.event.runImmediateBehavior();
            CaseSystem.devDebugLogger().outdent(1);
            if (CaseSystem.devDebugLogger().enabled()) {
                CaseSystem.devDebugLogger().debugIndentedConsoleLogging("-------- " + this + print("Finished immmediate behavior") + "\n");
            }
            CaseSystem.devDebugLogger().outdent(2);
            currentFrame = next;
        }

        void invokeDelayedBehavior() {
            Frame next = currentFrame;
            currentFrame = this;
            CaseSystem.devDebugLogger().indent(2);
            if (CaseSystem.devDebugLogger().enabled()) {
                CaseSystem.devDebugLogger().debugIndentedConsoleLogging("\n******** " + this + print("Running delayed behavior"));
            }
            CaseSystem.devDebugLogger().indent(1);
            event.runDelayedBehavior();
            if (children.size() > 0) {
                if (CaseSystem.devDebugLogger().enabled()) {
                    CaseSystem.devDebugLogger().debugIndentedConsoleLogging(this + "Loading " + children.size() + " nested frames at level [" + (depth + 1) + "] as a consequence of " + event.getDescription());
                }
            }
            children.forEach(frame -> frame.invokeDelayedBehavior());
            CaseSystem.devDebugLogger().outdent(1);
            if (CaseSystem.devDebugLogger().enabled()) {
                CaseSystem.devDebugLogger().debugIndentedConsoleLogging("******** " + this + print("Completed delayed behavior"));
            }
            CaseSystem.devDebugLogger().outdent(2);
            currentFrame = next;
        }

        @Override
        public String toString() {
            return "StackFrame[" + depth + "]: ";
        }
    }
}
