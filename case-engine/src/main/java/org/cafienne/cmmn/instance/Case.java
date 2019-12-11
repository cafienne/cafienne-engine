/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.handler.CommandHandler;
import org.cafienne.akka.actor.handler.ResponseHandler;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.event.EngineVersionChanged;
import org.cafienne.cmmn.akka.event.PlanItemCreated;
import org.cafienne.cmmn.akka.event.debug.DebugDisabled;
import org.cafienne.cmmn.akka.event.debug.DebugEnabled;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.parameter.CaseInputParameter;
import org.cafienne.cmmn.instance.parameter.CaseOutputParameter;
import org.cafienne.cmmn.instance.sentry.SentryNetwork;
import org.cafienne.cmmn.user.CaseTeam;
import org.cafienne.cmmn.user.CaseTeamMember;
import org.cafienne.util.Guid;
import org.cafienne.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Case extends ModelActor<CaseCommand, CaseInstanceEvent> {

    private final static Logger logger = LoggerFactory.getLogger(Case.class);

    /**
     * The moment at which the case is created
     */
    private Instant createdOn;

    /**
     * The version of the engine that this case currently uses; this defaults to what comes from the BuildInfo.
     * If a Case instance is recovered by Akka, then the version will be overwritten in {@link Case#recoverVersion(ValueMap)}.
     * Whenever then a new incoming message is handled by the Case actor - one leading to events, i.e., state changes, then
     * the actor will insert a new event EngineVersionChanged.
     * For new Cases, the CaseDefinitionApplied event will generate the current version
     */
    private ValueMap engineVersion = CaseSystem.version();

    /**
     * List of plan items in the case.
     */
    private Collection<PlanItem> planItems = new ArrayList<PlanItem>();
    /**
     * Pointer to the case file instance of the case.
     */
    private CaseFile caseFile;
    /**
     * List of sentries active within the case.
     */
    private final SentryNetwork sentryNetwork;
    /**
     * Case model that is interpreted for this case
     */
    private CaseDefinition definition;
    /**
     * Root level plan item of the case.
     */
    private PlanItem casePlan;

    /**
     * Identifier of parent case that caused this case to start.
     */
    private String parentCaseId;

    /**
     * Identifier of top-most case that caused this case to start.
     */
    private String rootCaseId;

    /**
     * Workers in the case team
     */
    private final CaseTeam caseTeam = new CaseTeam(this);

    /**
     * The set of failing plan items keeps track of those plan items that have {@link State#Failed}. This list is used
     * to notify the event stream about failures at case instance level, rather than at individual plan item level.
     */
    final Set<PlanItem> failingPlanItems = new HashSet<>();

    public Case() {
        super(CaseCommand.class, CaseInstanceEvent.class);
        this.createdOn = Instant.now();
        this.sentryNetwork = new SentryNetwork(this);

        logger.info("Recovering/creating case " + this.getId() + " with path " + self().path());
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public String getParentActorId() {
        return this.getParentCaseId();
    }

    @Override
    public String getRootActorId() {
        return this.getRootCaseId();
    }

    @Override
    protected CommandHandler createCommandHandler(CaseCommand msg) {
        return new CaseCommandHandler(this, msg);
    }

    @Override
    protected ResponseHandler createResponseHandler(ModelResponse response) {
        return new CaseResponseMessageHandler(this, response);
    }

    @Override
    public CaseCommand getCurrentCommand() {
        CaseCommandHandler c = currentHandler();
        return c.getCommand();
    }

    /**
     * Check whether the current engine version is equal to the one used until now.
     * If they differ, then a {@link EngineVersionChanged} is logged.
     */
    EngineVersionChanged checkEngineVersion() {
        ValueMap currentEngineVersion = CaseSystem.version();
        if (!currentEngineVersion.equals(engineVersion)) {
            logger.info("Case " + getId() + " changed engine version from\n" + engineVersion + " to\n" + currentEngineVersion);
            this.engineVersion = currentEngineVersion;
            return new EngineVersionChanged(this, currentEngineVersion);
        }
        return null;
    }

    /**
     * Internal framework method to notify the case instance that the plan item is in {@link State#Failed}
     *
     * @param planItem
     */
    void addFailure(PlanItem planItem) {
        failingPlanItems.add(planItem);
    }

    /**
     * Internal framework method to notify the case instance that the plan item is not in {@link State#Failed}
     *
     * @param planItem
     */
    void noFailure(PlanItem planItem) {
        failingPlanItems.remove(planItem);
    }

    /**
     * Returns the user context under which the current actions in the engine are being processed
     *
     * @return
     */
    public CaseTeamMember getCurrentTeamMember() {
        return getCaseTeam().getTeamMember(getCurrentUser());
    }

    /**
     * This methods is invoked by the case to buffer the events that originate from a command
     *
     * @param event
     */
    public <T extends CaseInstanceEvent> T storeInternallyGeneratedEvent(T event) {
        if (recoveryRunning()) {
            logger.debug("Not storing internally generated event because recovery is running. Event " + event);
            return event;
        }

        if (currentHandler() instanceof CaseCommandHandler) {
            CaseCommandHandler h = currentHandler();
            return h.storeInternallyGeneratedEvent(event);
        } else {
            currentHandler().addEvent(event);
            return event;
        }
    }

    /**
     * Internal framework method, used to set the current event back to it's parent if the current event finished
     *
     * @param event
     */
    void setCurrentEvent(CaseInstanceEvent event) {
        if (!recoveryRunning()) {
            if (currentHandler() instanceof CaseCommandHandler) {
                CaseCommandHandler h = currentHandler();
                h.setCurrentEvent(event);
            }
        }
    }

    /**
     * Internal engine method to register any newly created plan items in the global plan item collection
     *
     * @param planItem
     */
    void registerPlanItem(PlanItem planItem) {
        if (planItem.getStage() == null) {
            casePlan = planItem;
        }
        planItems.add(planItem);
    }

    /**
     * Helper method to dump the state of the case into an XML document.
     *
     * @return
     */
    public String stateToXMLString() {
        Document xmlDocument;
        try {
            if (definition == null) {
                xmlDocument = XMLHelper.loadXML("<Case id=\"" + getId() + "\"/>");
            } else {
                xmlDocument = XMLHelper.loadXML("<Case name=\"" + definition.getName() + "\" id=\"" + getId() + "\"/>");
                getCaseTeam().dumpMemoryStateToXML(xmlDocument.getDocumentElement());
                getCaseFile().dumpMemoryStateToXML(xmlDocument.getDocumentElement());
                getCasePlan().dumpMemoryStateToXML(xmlDocument.getDocumentElement());
            }
            return XMLHelper.printXMLNode(xmlDocument);
        } catch (SAXException | ParserConfigurationException | IOException willNotOccur) {
            throw new RuntimeException("Cannot parse xml???", willNotOccur);
        }
    }

    /**
     * Returns the moment of case creation.
     *
     * @return
     */
    public Instant getCreatedOn() {
        return createdOn;
    }

    /**
     * Returns the definition according to which this case behaves
     *
     * @return
     */
    public CaseDefinition getDefinition() {
        return definition;
    }

    /**
     * Returns a plan item based on it's id, or null if no plan item with the specified id was found.
     *
     * @param id
     * @return
     */
    public PlanItem getPlanItemById(String id) {
        return planItems.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Returns the plan item with the corresponding name, or null if none is found.
     * If there are multiple plan items with the same name, the last one that has been added to the case is returned.
     *
     * @param name
     * @return
     */
    public PlanItem getPlanItemByName(String name) {
        PlanItem lastWithThisName = null;
        for (PlanItem planItem : planItems) {
            if (planItem.getName().equals(name)) {
                lastWithThisName = planItem;
            }
        }
        return lastWithThisName;
    }

    /**
     * Returns a collection with the currently available plan items of the case
     *
     * @return
     */
    public Collection<PlanItem> getPlanItems() {
        return planItems;
    }

    /**
     * Returns a collection of plan items with the specified identifier.
     * This can either be an ID (in which case obviously there may be only 1 plan item in the collection),
     * or it can be the plan item name, in which case multiple (e.g. repeating) plan items are in the collection.
     * The collection is sorted reversely, i.e., the last plan item that was created, is at the front of the collection.
     *
     * @param identifier
     * @return
     */
    public Collection<PlanItem> getPlanItems(String identifier) {
        ArrayList<PlanItem> list = new ArrayList();
        for (PlanItem planItem : getPlanItems()) {
            if (planItem.getName().equals(identifier) || planItem.getId().equals(identifier)) {
                list.add(0, planItem);
            }
        }
        return list;
    }

    /**
     * Returns the sentries currently instantiated within this case
     *
     * @return
     */
    public SentryNetwork getSentryNetwork() {
        return sentryNetwork;
    }

    /**
     * Returns the root plan item
     *
     * @return
     */
    public PlanItem getCasePlan() {
        if (casePlan == null) {
            // Note: although we assign to case plan, this is also done in registerPlanItem method
            // so not per se needed here. (it is done in registerPlanItem because of Akka recovery)
            casePlan = new PlanItem(new Guid().toString(), definition.getCasePlanModel(), this);
        }

        return casePlan;
    }

    /**
     * Returns the case file. Note that this is a wrapper structure around the actual data.
     *
     * @return
     */
    public CaseFile getCaseFile() {
        if (caseFile == null) {
            // Let's create the case file. It is empty in the beginning, without any data.
            this.caseFile = new CaseFile(this, getDefinition().getCaseFileModel());
        }
        return caseFile;
    }

    /**
     * Returns a collection of all discretionary items currently applicable
     *
     * @return
     */
    public Collection<DiscretionaryItem> getDiscretionaryItems() {
        Collection<DiscretionaryItem> items = new ArrayList<DiscretionaryItem>();
        getCasePlan().getInstance().retrieveDiscretionaryItems(items);
        return items;
    }

    /**
     * Internal framework method to enable/disable debug mode
     *
     * @param debugMode
     */
    public void switchDebugMode(boolean debugMode) {
        if (this.debugMode != debugMode) {
            this.debugMode = debugMode;
            if (debugMode) {
                storeInternallyGeneratedEvent(new DebugEnabled(this)).finished();
            } else {
                storeInternallyGeneratedEvent(new DebugDisabled(this)).finished();
            }
            logger.debug("Changed debug mode in case " + getId() + " to " + debugMode);
        }
    }

    /**
     * Internal framework method to support Akka command handling and recovery
     */
    public void applyCaseDefinition(CaseDefinition definition, String parentCaseId, String rootCaseId) {
        // Set the definition and tenant.
        this.definition = definition;
        // Link the (optional) ancestor information
        this.parentCaseId = parentCaseId;
        this.rootCaseId = rootCaseId;
    }

    public void setInputParameters(ValueMap inputParameters) {
        // TODO: figure out whether we should actually re-use the CaseParameter objects, or
        // copy them. Currently we copy the parameters.
        CaseDefinition definition = getDefinition();
        inputParameters.getValue().forEach((name, externalParameter) -> {
            InputParameterDefinition inputParameterDefinition = definition.getInputParameters().get(name);
            CaseInputParameter parameter = new CaseInputParameter(inputParameterDefinition, this);
            parameter.setValue(externalParameter);
        });
    }

    /**
     * Returns a map with output parameters by name with their "raw" value
     *
     * @return
     */
    ValueMap getOutputParameters() {
        ValueMap outputParameters = new ValueMap();
        getDefinition().getOutputParameters().forEach((name, outputParameterDefinition) -> {
            CaseOutputParameter outputParameter = new CaseOutputParameter(outputParameterDefinition, this);
            // Doing getValue() forces the parameter to take it's value from the case file (if bound to it)
            outputParameters.put(name, outputParameter.getValue());
        });
        return outputParameters;
    }

    /**
     * Internal framework method to support Akka command handling and recovery
     */
    public void makePlanItemTransition(PlanItem planItem, Transition transition) {
        planItem.makeTransition(transition);
    }

    /**
     * Returns the id of the case that started this case (if at all)
     *
     * @return
     */
    public String getParentCaseId() {
        return parentCaseId;
    }

    /**
     * Returns the id of the outer-most case that caused this case to start (could be the id of this case itself)
     *
     * @return
     */
    public String getRootCaseId() {
        return rootCaseId;
    }

    /**
     * Returns the team with the workers for this case.
     *
     * @return
     */
    public CaseTeam getCaseTeam() {
        return caseTeam;
    }

    public void recoverLastModified(Instant lastModified) {
        setLastModified(lastModified);
    }

    public void recoverVersion(ValueMap version) {
        this.engineVersion = version;
    }

    public void recoverPlanItem(PlanItemCreated event) {
        if (event.stageId.isEmpty()) { // then it is the caseplan, so use a different constructor
            new PlanItem(event.planItemId, this.getDefinition().getCasePlanModel(), this);
        } else {
            // Lookup the stage to which the plan item belongs,
            // then lookup the definition for the plan item
            // and then instantiate it.
            PlanItem owningPlanItem = this.getPlanItemById(event.stageId);
            if (owningPlanItem == null) {
                logger.error("MAJOR ERROR: we cannot find the stage with id " + event.stageId + ", and therefore cannot recover  plan item " + event);
                return;
            }
            Stage<?> stage = owningPlanItem.getInstance();
            stage.recoverPlanItem(event);
        }
    }
}
