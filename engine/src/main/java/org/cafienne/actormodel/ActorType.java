package org.cafienne.actormodel;

import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.engine.cmmn.actorapi.event.CaseEvent;
import org.cafienne.engine.cmmn.instance.Case;
import org.cafienne.engine.processtask.actorapi.event.ProcessEvent;
import org.cafienne.engine.processtask.instance.ProcessTaskActor;
import org.cafienne.userregistration.consentgroup.ConsentGroupActor;
import org.cafienne.userregistration.consentgroup.actorapi.event.ConsentGroupEvent;
import org.cafienne.userregistration.tenant.TenantActor;
import org.cafienne.userregistration.tenant.actorapi.event.TenantEvent;

public enum ActorType {
    Case(Case.class, CaseEvent.class),
    Process(ProcessTaskActor.class, ProcessEvent.class),
    Group(ConsentGroupActor.class, ConsentGroupEvent.class),
    Tenant(TenantActor.class, TenantEvent.class);

    public final String value;
    public final Class<? extends ModelActor> actorClass;
    public final Class<? extends ModelEvent> actorEventClass;

    ActorType(Class<? extends ModelActor> actorClass, Class<? extends ModelEvent> actorEventClass) {
        this.actorClass = actorClass;
        this.actorEventClass = actorEventClass;
        this.value = actorClass.getSimpleName();
    }

    public static ActorType getEnum(String value) {
        if (value == null) return null;
        for (ActorType type : values()) {
            if (type.value.equalsIgnoreCase(value)) return type;
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}
