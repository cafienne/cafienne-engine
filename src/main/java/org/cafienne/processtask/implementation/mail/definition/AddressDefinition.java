package org.cafienne.processtask.implementation.mail.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.definition.Resolver;
import org.cafienne.processtask.definition.SubProcessInputMappingDefinition;
import org.cafienne.processtask.implementation.mail.InvalidMailAddressException;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;

public class AddressDefinition extends SubProcessInputMappingDefinition {
    private final Resolver emailResolver;
    private final Resolver nameResolver;

    public AddressDefinition(Element element, ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.emailResolver = this.getResolver();
        this.nameResolver = parseResolver("name", false, "");
    }

    public Resolver getEmailResolver() {
        return emailResolver;
    }

    public Resolver getNameResolver() {
        return nameResolver;
    }

    public InternetAddress toAddress(ProcessTaskActor task) {
        String email = emailResolver.getValue(task, "");
        String name = nameResolver.getValue(task, "");
        try {
            return new InternetAddress(email, name);
        } catch (UnsupportedEncodingException ex) {
            throw new InvalidMailAddressException("Invalid email address " + email + " " + ex.getMessage(), ex);
        }
    }
}
