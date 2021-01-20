package org.cafienne.platform;

import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.identity.PlatformUser;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.UpdateCaseWithPlatformInformation;
import org.cafienne.cmmn.akka.command.platform.CaseUpdate;
import org.cafienne.cmmn.akka.command.platform.TenantUpdate;
import org.cafienne.tenant.akka.command.platform.UpdateTenantWithPlatformInformation;

public abstract class InformJob {
    protected final TenantUser user;

    public InformJob(TenantUser user) {
        this.user = user;
    }

    abstract ModelCommand getCommand();

    abstract Object action();

    abstract String getActorId();
}

class InformCaseJob extends InformJob {
    public final CaseUpdate action;

    InformCaseJob(TenantUser user, CaseUpdate action) {
        super(user);
        this.action = action;
    }

    @Override
    ModelCommand getCommand() {
        return new UpdateCaseWithPlatformInformation(PlatformUser.from(user), action);
    }

    @Override
    Object action() {
        return action;
    }

    @Override
    String getActorId() {
        return action.caseId();
    }

    @Override
    public String toString() {
        return "Job to inform case " + action.caseId() +" in tenant " + action.tenant() +" about new users";
    }
}

class InformTenantJob extends InformJob {
    public final TenantUpdate action;

    InformTenantJob(TenantUser user, TenantUpdate action) {
        super(user);
        this.action = action;
    }

    @Override
    Object action() {
        return action;
    }

    @Override
    ModelCommand getCommand() {
        return new UpdateTenantWithPlatformInformation(PlatformUser.from(user), action);
    }

    @Override
    String getActorId() {
        return action.tenant();
    }

    @Override
    public String toString() {
        return "Job to inform tenant " + action.tenant() +" about new users";
    }
}