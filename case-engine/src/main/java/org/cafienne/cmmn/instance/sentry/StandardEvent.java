package org.cafienne.cmmn.instance.sentry;

public interface StandardEvent<T extends Enum> {
    T getTransition();
}
