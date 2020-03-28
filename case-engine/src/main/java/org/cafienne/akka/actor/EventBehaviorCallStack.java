package org.cafienne.akka.actor;

import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.cmmn.akka.event.file.CaseFileEvent;
import org.cafienne.cmmn.akka.event.plan.PlanItemEvent;

import java.util.ArrayList;
import java.util.List;

class EventBehaviorCallStack {
    private static final String SINGLE_INDENT = "  ";

    private final MessageHandler handler;
    private Frame currentFrame = null;
    private String indent = SINGLE_INDENT;

    EventBehaviorCallStack(MessageHandler handler) {
        this.handler = handler;
    }

    void pushEvent(ModelEvent event) {
        if (event.hasBehavior()) {
            Frame frame = new Frame(event, currentFrame);
            frame.runNow();
        }
//        internalLog("Putting handler for " + event.getClass().getSimpleName() + " one level deeper  (at level " +depth +"[" + sourceDescription()+ "])");
    }

    String getIndent() {
        return indent;
    }

    private class Frame {
        private final ModelEvent event;
        private final Frame parent;
        private final List<Frame> children = new ArrayList<>();
        private final int depth;
        private String mainIndent;
        private String subIndent;

        Frame(ModelEvent event, Frame parent) {
            this.event = event;
            this.parent = parent;
            this.depth = parent == null ? 1 : parent.depth + 1;
            this.mainIndent = createIndent();
            this.subIndent = this.mainIndent + " ";
        }

        private String createIndent() {
            StringBuilder sb = new StringBuilder("");
            for (int i=0; i<depth; i++) {
                sb.append(SINGLE_INDENT);
            }
            return sb.toString();
        }

        void runNow() {
            indent = this.mainIndent;
            if (currentFrame == null) {
                currentFrame = this;
            }
            if (handler.indentedConsoleLoggingEnabled) handler.debugIndentedConsoleLogging("\n-------- " + this + "Running immmediate behavior for " + event.getDescription() );
            indent = this.subIndent;
            this.event.runImmediateBehavior();
            indent = this.mainIndent;
            if (handler.indentedConsoleLoggingEnabled) handler.debugIndentedConsoleLogging("-------- " + this + "Finished immmediate behavior for " + event.getDescription() +"\n");
            if (currentFrame == this) {
                // Top level, immediately execute the delayed behavior
                runLater();
            } else {
                // Postpone the execution of the delayed behavior
                if (handler.indentedConsoleLoggingEnabled) handler.debugIndentedConsoleLogging("* postponing delayed behavior for " + event.getDescription());
                currentFrame.children.add(this);
            }
        }

        void runLater() {
            currentFrame = this;
            indent = this.mainIndent;
            if (handler.indentedConsoleLoggingEnabled) handler.debugIndentedConsoleLogging("\n******** " + this + "Running delayed behavior for " + event.getDescription());
            indent = subIndent;
            event.runDelayedBehavior();
            if (children.size() > 0) {
                if (handler.indentedConsoleLoggingEnabled) handler.debugIndentedConsoleLogging(this + "Loading " + children.size()+" nested frames at level ["+(depth+1)+"] as a consequence of " + event.getDescription());
            }
            children.forEach(frame -> frame.runLater());
            indent = mainIndent;
            if (handler.indentedConsoleLoggingEnabled) handler.debugIndentedConsoleLogging("******** " + this + "Completed delayed behavior for " + event.getDescription());
            currentFrame = parent;
            indent = parent != null ? parent.mainIndent : "";
        }

        @Override
        public String toString() {
            return "StackFrame[" + depth + "]: ";
        }
    }
}
