package org.cafienne.processtask.implementation.mail;

import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.implementation.mail.definition.AttachmentDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.util.ByteArrayDataSource;
import java.util.Base64;

public class Attachment {
    private final ProcessTaskActor task;

    private final String fileName;
    private final String content;
    private final String mimeType;

    public Attachment(ValueMap dynamicAttachment, ProcessTaskActor task) {
        this.task = task;
        this.content = dynamicAttachment.readString(Fields.content, "");
        this.fileName = dynamicAttachment.readString(Fields.fileName, "");
        this.mimeType = dynamicAttachment.readString(Fields.mimeType, "application/octet-stream");
        if (content.isBlank()) {
            task.addDebugInfo(() -> "Attachment must be a json object with 'content' (base64 coded) and optional 'fileName' and 'mimeType'; skipping attachment, because 'content' is missing.");
        }
    }

    public Attachment(AttachmentDefinition definition, ProcessTaskActor task) {
        this.task = task;

        content = definition.getContentResolver().getValue(task, "");
        fileName = definition.getFileNameResolver().getValue(task, "");
        mimeType = definition.getMimeTypeResolver().getValue(task, "application/octet-stream");
        if (content.isBlank()) {
            task.addDebugInfo(() -> "Attachment must be a json object with 'content' (base64 coded) and optional 'fileName' and 'mimeType'; skipping attachment, because 'content' is missing.");
        }
    }

    public boolean hasContent() {
        return !content.isBlank();
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getContent() {
        return content;
    }

    public BodyPart getBodyPart() {
        if (content.isBlank()) {
            return null;
        } else {
            BodyPart bodyPart = new MimeBodyPart();
            DataSource source = new ByteArrayDataSource(Base64.getDecoder().decode(content), mimeType);
            try {
                bodyPart.setDataHandler(new DataHandler(source));
                bodyPart.setFileName(fileName);
                final String attachmentFileName = fileName;
                task.addDebugInfo(() -> "Added attachment '" + attachmentFileName + "' of length " + content.length() + " bytes");
                return bodyPart;
            } catch (MessagingException e) {
                throw new InvalidMailException("Cannot add attachment with file name '" + fileName + "'", e);
            }
        }
    }
}