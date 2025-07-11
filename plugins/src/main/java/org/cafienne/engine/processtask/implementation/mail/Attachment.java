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

package org.cafienne.engine.processtask.implementation.mail;

import java.util.Base64;
import jakarta.activation.DataSource;

import org.cafienne.engine.processtask.instance.ProcessTaskActor;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;
import org.cafienne.engine.processtask.implementation.mail.definition.AttachmentDefinition;

import jakarta.activation.DataHandler;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.util.ByteArrayDataSource;

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