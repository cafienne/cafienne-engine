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

package org.cafienne.infrastructure.serialization;

/**
 * Big long enum with all field names that are used in places with CafienneJson (ValueMap and so)
 */
public enum Fields {
    messageId,
    actor,
    actorId,
    user,

    lastModified,
    timestamp,
    commandType,
    command,
    messages,
    version,
    tenant,
    group,
    groupId,
    mappings,
    removedMappings,
    userId,
    origin,
    tenantRole,
    groupRole,
    caseRole,
    tenants,
    cases,
    update,
    jobs,
    jobCount,
    modelEvent,
    roles,
    caseRoles,
    unassignedRoles,
    removeRoles,
    rolesRemoved,
    name,
    description,
    email,
    isOwner,
    enabled,
    identifier,
    planItemId,
    parentId,
    parentStage,
    parentName,
    parentType,
    definitionId,
    transition,
    planItemName,
    caseInstanceId,
    parentCaseId,
    rootCaseId,
    file,
    form,
    response,
    taskId,
    taskOutput,

    parentActorId,
    rootActorId,
    targetActorId,
    sourceActorId,
    inputParameters,
    definition,
    debugMode,
    content,
    fileName,
    mimeType,
    path,
    childPath,
    formerPath,
    memberId,
    memberType,
    team,
    groups,
    members,
    member,
    value,
    type,
    index,
    stageId,
    milestoneId,
    eventId,
    currentState,
    historyState,
    discretionaryItems,
    isRepeating,
    isRequired,
    timerId,
    targetMoment,
    taskParameters,
    mappedInputParameters,
    parameters,
    rawOutputParameters,
    role,
    elementId,
    source,
    script,
    waitTime,
    assignee,
    delegate,
    dueDate,
    performer,
    taskModel,
    input,
    owner,
    processDefinition,
    output,
    engineVersion,
    newTenantUser,
    isTenantUser,
    taskName,
    planitem,
    seqNo,
    createdOn,
    completedOn,
    createdBy,
    numFailures,
    failure,
    state,
    caseName,
    users,
    tenantRoles,
    owners,
    moment,
    timers,

    metadata,
    archive,
    events,
    children,
    manifest,
    sequenceNr,
    parent,

    className,
    message,
    cause,
    exception
}
