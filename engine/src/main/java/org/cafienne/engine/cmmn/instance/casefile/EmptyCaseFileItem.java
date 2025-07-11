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

package org.cafienne.engine.cmmn.instance.casefile;

import org.cafienne.json.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Case file item that represents an empty item.
 * See CMMN 1.0 specification page 107 ("an empty case file item must be returned")
 */
class EmptyCaseFileItem extends CaseFileItem {
    private final static Logger logger = LoggerFactory.getLogger(EmptyCaseFileItem.class);

    EmptyCaseFileItem(CaseFileItem parent, String creationReason) {
        super(parent.getCaseInstance(), parent.getDefinition(), parent);
        logger.warn(creationReason);
    }

    @Override
    public void createContent(Value<?> newContent) {
        logger.warn("Creating content in EmptyCaseFileItem " + getPath());
    }

    @Override
    public void updateContent(Value<?> newContent) {
        logger.warn("Updating content in EmptyCaseFileItem " + getPath());
    }

    @Override
    public void replaceContent(Value<?> newContent) {
        logger.warn("Replacing content in EmptyCaseFileItem " + getPath());
    }

    @Override
    public void deleteContent() {
        logger.warn("Deleting content in EmptyCaseFileItem " + getPath());
    }

    @Override
    public Value<?> getValue() {
        logger.warn("Returning value from EmptyCaseFileItem " + getPath());
        return Value.NULL;
    }

    @Override
    protected void setValue(Value<?> newValue) {
        logger.warn("Setting value in EmptyCaseFileItem " + getPath());
    }
}
