package org.cafienne.processtask.implementation.mail.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.definition.Resolver;
import org.cafienne.processtask.definition.SubProcessInputMappingDefinition;
import org.cafienne.processtask.implementation.mail.Attachment;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

public class AttachmentDefinition extends SubProcessInputMappingDefinition {
    private final Resolver fileNameResolver;
    private final Resolver mimeTypeResolver;
//    private final String encoding;

    public AttachmentDefinition(Element element, ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.fileNameResolver = parseResolver("name", false, "");
        this.mimeTypeResolver = parseResolver("type", false, "application/octet-stream");

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
    protected boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
