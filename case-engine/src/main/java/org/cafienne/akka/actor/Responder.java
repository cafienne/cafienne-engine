package org.cafienne.akka.actor;

import org.cafienne.akka.actor.command.response.CommandFailureListener;
import org.cafienne.akka.actor.command.response.CommandResponseListener;

public class Responder {
    public final CommandResponseListener right;
    public final CommandFailureListener left;

    public Responder(CommandFailureListener left, CommandResponseListener... right) {
        this.left = left == null ? e -> {} : left;
        this.right = right.length > 0 && right[0] != null ? right[0] : e -> {};
    }
}
