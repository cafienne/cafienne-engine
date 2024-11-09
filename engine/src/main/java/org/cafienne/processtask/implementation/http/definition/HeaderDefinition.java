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

package org.cafienne.processtask.implementation.http.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.expression.spel.api.APIRootObject;
import org.cafienne.cmmn.expression.spel.Resolver;
import org.cafienne.processtask.definition.ProcessInputResolver;
import org.cafienne.processtask.implementation.http.Header;
import org.w3c.dom.Element;

public class HeaderDefinition extends ProcessInputResolver {
    private final Resolver nameResolver;
    private final Resolver valueResolver;

    public HeaderDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.nameResolver = parseAttributeResolver("name", "");
        this.valueResolver = this.getResolver();
    }

    public Header getHeader(APIRootObject<?> context) {
        String value = valueResolver.getValue(context);
        String name = nameResolver.getValue(context, "");
        return new Header(name, value);
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameAttachmentDefinition);
    }

    private boolean sameAttachmentDefinition(HeaderDefinition other) {
        return super.sameResolverDefinition(other)
                && same(nameResolver, other.nameResolver)
                && same(valueResolver, other.valueResolver);
    }
}