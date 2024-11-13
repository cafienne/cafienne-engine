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

package com.casefabric.cmmn.actorapi.command.casefile;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.actorapi.command.CaseCommand;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.Path;
import com.casefabric.cmmn.instance.casefile.CaseFileItemCollection;
import com.casefabric.cmmn.instance.casefile.CaseFileItemTransition;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.json.Value;
import com.casefabric.json.ValueMap;

import java.io.IOException;

/**
 * Holds some generic validation and processing behavior for CaseFile operations.
 */
abstract class CaseFileItemCommand extends CaseCommand {
    protected Path path;
    protected final Value<?> content;
    protected final CaseFileItemTransition intendedTransition;
    protected CaseFileItemCollection<?> caseFileItem;

    /**
     * Determine path and content for the CaseFileItem to be touched.
     *
     * @param caseInstanceId     The id of the case in which to perform this command.
     * @param newContent         A value structure with contents of the new case file item
     * @param path   Path to the case file item to be created
     * @param intendedTransition
     */
    protected CaseFileItemCommand(CaseUserIdentity user, String caseInstanceId, Value<?> newContent, Path path, CaseFileItemTransition intendedTransition) {
        super(user, caseInstanceId);
        this.path = path;
        this.content = newContent;
        this.intendedTransition = intendedTransition;
    }

    protected CaseFileItemCommand(ValueMap json, CaseFileItemTransition intendedTransition) {
        super(json);
        this.path = json.readPath(Fields.path);
        this.content = json.get(Fields.content);
        this.intendedTransition = intendedTransition;
    }

    @Override
    public void validate(Case caseInstance) {
        // First do the validation in the super class. Then only our own, but also only if there were no validation errors from the super class.
        super.validate(caseInstance);
        // Resolve the path on the case file, this validates whether the path matches the case file definition
        caseFileItem = path.resolve(caseInstance);
        // Validate current item state against intended operation
        caseFileItem.validateTransition(intendedTransition, content);
    }

    @Override
    public void processCaseCommand(Case caseInstance) {
        apply(caseInstance, caseFileItem, content);
    }

    abstract void apply(Case caseInstance, CaseFileItemCollection<?> caseFileItem, Value<?> content);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + path + "]";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.path, path);
        writeField(generator, Fields.content, content);
    }
}
