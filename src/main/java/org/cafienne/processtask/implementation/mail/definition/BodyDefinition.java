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

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.expression.spel.Resolver;
import org.cafienne.processtask.definition.ProcessInputResolver;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

public class BodyDefinition extends ProcessInputResolver {
    private final Resolver bodyResolver;
    private final Resolver bodyTypeResolver;

    public BodyDefinition(Element element, ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.bodyResolver = this.getResolver();
        this.bodyTypeResolver = parseAttributeResolver("type", "text/html");
    }

    public String getBody(ProcessTaskActor task) {
        return this.bodyResolver.getValue(task, "");
    }

    public String getBodyType(ProcessTaskActor task) {
        return bodyTypeResolver.getValue(task, "text/html");
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameBodyDefinition);
    }

    private boolean sameBodyDefinition(BodyDefinition other) {
        return super.sameResolverDefinition(other)
                && same(bodyResolver, other.bodyResolver)
                && same(bodyTypeResolver, other.bodyTypeResolver);
    }
}
