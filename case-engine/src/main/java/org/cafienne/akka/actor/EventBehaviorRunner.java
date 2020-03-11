package org.cafienne.akka.actor;

import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.cmmn.akka.event.CaseFileEvent;
import org.cafienne.cmmn.akka.event.plan.PlanItemEvent;

import java.util.ArrayList;
import java.util.List;

class EventBehaviorRunner {
    private static final String SINGLE_INDENT = "  ";

    private final MessageHandler handler;
    private final ModelEvent source;
    private final int depth;
    private final List<EventBehaviorRunner> runners = new ArrayList<>();
    private EventBehaviorRunner currentRunner = this;
    private String indent;

    EventBehaviorRunner(MessageHandler handler, ModelEvent event) {
        this(handler, event, 1);
    }

    EventBehaviorRunner(MessageHandler handler, ModelEvent event, int depth) {
        this.handler = handler;
        this.source = event;
        this.depth = depth;
        this.indent = createIndent();
    }

    private String createIndent() {
        StringBuilder sb = new StringBuilder("");
        for (int i=0; i<depth; i++) {
            sb.append(SINGLE_INDENT);
        }
        return sb.toString();
    }

    void start() {
        if (handler.indentedConsoleLoggingEnabled) handler.debugIndentedConsoleLogging("\nExecuting behavior for " + sourceDescription() +"...");
        indent = indent + " ";
        source.runBehavior();
        if (runners.size() > 0) {
            if (handler.indentedConsoleLoggingEnabled) handler.debugIndentedConsoleLogging("Completed top level behavior. Starting " + runners.size()+" behaviors at level ["+(depth+1)+"] for " + sourceDescription()+".");
        }
        runners.forEach(runner -> {
            currentRunner = runner;
            runner.start();
        });
        if (handler.indentedConsoleLoggingEnabled) handler.debugIndentedConsoleLogging("...Finished run for " + sourceDescription());
        currentRunner = this;
    }

    void addEvent(ModelEvent event) {
//        oldApproach(event);
        newApproach(event);
    }

    private void oldApproach(ModelEvent event) {
        event.runBehavior();
    }

    private void newApproach(ModelEvent event) {
//        internalLog("Putting handler for " + event.getClass().getSimpleName() + " one level deeper  (at level " +depth +"[" + sourceDescription()+ "])");
        if (currentRunner == this) {
            EventBehaviorRunner runner = new EventBehaviorRunner(handler, event, this.depth + 1);
            if (handler.indentedConsoleLoggingEnabled) handler.debugIndentedConsoleLogging("\nCreated " + runner);
            runners.add(runner);
        } else {
            currentRunner.addEvent(event);
        }
    }

    String getIndent() {
        if (currentRunner == this) {
            return indent;
        } else {
            return currentRunner.getIndent();
        }
    }

    private String sourceDescription() {
        String sourceDescription = source instanceof PlanItemEvent || source instanceof CaseFileEvent ? source.toString() : source.getClass().getSimpleName();
        return sourceDescription;
    }

    @Override
    public String toString() {
        return "EventBehaviorRunner[depth=" + depth + "]: " + sourceDescription();
    }
}
