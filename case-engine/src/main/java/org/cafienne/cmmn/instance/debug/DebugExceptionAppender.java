package org.cafienne.cmmn.instance.debug;

@FunctionalInterface
public interface DebugExceptionAppender {
    Throwable exceptionInfo();
}
