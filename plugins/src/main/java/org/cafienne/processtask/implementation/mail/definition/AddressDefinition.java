/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.processtask.implementation.mail.definition;

import jakarta.mail.internet.InternetAddress;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.expression.spel.Resolver;
import org.cafienne.processtask.definition.ProcessInputResolver;
import org.cafienne.processtask.implementation.mail.InvalidMailAddressException;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

import java.io.UnsupportedEncodingException;

public class AddressDefinition extends ProcessInputResolver {
    private final Resolver emailResolver;
    private final Resolver nameResolver;

    public AddressDefinition(Element element, ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.emailResolver = this.getResolver();
        this.nameResolver = parseAttributeResolver("name", "");
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

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameAddressDefinition);
    }

    private boolean sameAddressDefinition(AddressDefinition other) {
        return super.sameResolverDefinition(other)
                && same(emailResolver, other.emailResolver)
                && same(nameResolver, other.nameResolver);
    }
}
