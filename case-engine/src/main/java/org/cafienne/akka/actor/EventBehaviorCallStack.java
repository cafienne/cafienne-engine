package org.cafienne.akka.actor;

import org.cafienne.akka.actor.event.ModelEvent;

import java.util.ArrayList;
import java.util.List;

class EventBehaviorCallStack {
    private Frame currentFrame = null;

    EventBehaviorCallStack() {
    }

    void pushEvent(ModelEvent event) {
        if (event.hasBehavior()) {
            Frame frame = new Frame(event, currentFrame);
            frame.invokeImmediateBehavior();
            frame.postponeDelayedBehavior();
        }
    }

    private class Frame {
        private final ModelEvent event;
        private final Frame parent;
        private final List<Frame> children = new ArrayList();
        private final int depth;

        Frame(ModelEvent event, Frame parent) {
            this.event = event;
            this.parent = parent;
            this.depth = parent == null ? 1 : parent.depth + 1;
        }

        private void postponeDelayedBehavior() {
            if (currentFrame == null) {
                // Top level, immediately execute the delayed behavior
                invokeDelayedBehavior();
            } else {
                // Postpone the execution of the delayed behavior
                currentFrame.children.add(0, this);
                if (CaseSystem.devDebugLogger().enabled()) {
                    CaseSystem.devDebugLogger().debugIndentedConsoleLogging("* postponing delayed behavior for " + event.getDescription());
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
                CaseSystem.devDebugLogger().debugIndentedConsoleLogging("\n-------- " + this + "Running immmediate behavior for " + event.getDescription());
            }
            CaseSystem.devDebugLogger().indent(1);
            this.event.runImmediateBehavior();
            CaseSystem.devDebugLogger().outdent(1);
            if (CaseSystem.devDebugLogger().enabled()) {
                CaseSystem.devDebugLogger().debugIndentedConsoleLogging("-------- " + this + "Finished immmediate behavior for " + event.getDescription() + "\n");
            }
            CaseSystem.devDebugLogger().outdent(2);
            currentFrame = next;
        }

        void invokeDelayedBehavior() {
            Frame next = currentFrame;
            currentFrame = this;
            CaseSystem.devDebugLogger().indent(2);
            if (CaseSystem.devDebugLogger().enabled()) {
                CaseSystem.devDebugLogger().debugIndentedConsoleLogging("-------- " + this + "Finished immmediate behavior for " + event.getDescription() + "\n");
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
                CaseSystem.devDebugLogger().debugIndentedConsoleLogging("******** " + this + "Completed delayed behavior for " + event.getDescription());
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
