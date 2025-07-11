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

package org.cafienne.engine.processtask.implementation.mail.definition;

import org.cafienne.engine.cmmn.definition.CMMNElementDefinition;
import org.cafienne.engine.cmmn.definition.ModelDefinition;
import org.cafienne.engine.cmmn.expression.spel.Resolver;
import org.cafienne.engine.processtask.definition.ProcessInputResolver;
import org.w3c.dom.Element;

public class AttachmentDefinition extends ProcessInputResolver {
    private final Resolver fileNameResolver;
    private final Resolver mimeTypeResolver;
//    private final String encoding;

    public AttachmentDefinition(Element element, ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.fileNameResolver = parseAttributeResolver("name", "");
        this.mimeTypeResolver = parseAttributeResolver("type", "application/octet-stream");

        // Now, content must be delivered always as base64 encoded. Perhaps we can make this a variable as well.
//        this.encoding = parseAttribute("encoding", false, "base64");
    }

    public Resolver getFileNameResolver() {
        return fileNameResolver;
    }

    public Resolver getContentResolver() {
        return getResolver();
    }

    public Resolver getMimeTypeResolver() {
        return mimeTypeResolver;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameAttachmentDefinition);
    }

    private boolean sameAttachmentDefinition(AttachmentDefinition other) {
        return super.sameResolverDefinition(other)
                && same(fileNameResolver, other.fileNameResolver)
                && same(mimeTypeResolver, other.mimeTypeResolver);
    }
}
