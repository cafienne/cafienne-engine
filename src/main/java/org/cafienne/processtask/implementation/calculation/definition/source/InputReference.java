package org.cafienne.processtask.implementation.calculation.definition.source;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.implementation.calculation.CalculationDefinition;
import org.cafienne.processtask.implementation.calculation.definition.StepDefinition;
import org.w3c.dom.Element;

public class InputReference extends CMMNElementDefinition {
    private final String sourceReference;
    private final String elementName;
    private SourceDefinition source;

    public InputReference(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.sourceReference = parseAttribute("source", true);
        this.elementName = parseAttribute("as", false, sourceReference);
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        StepDefinition step = getParentElement();
        CalculationDefinition calculationDefinition = step.getParentElement();
        source = calculationDefinition.getSource(sourceReference);
        // Make sure source is defined
        if (source == null) {
            this.getProcessDefinition().addDefinitionError("Cannot find input '" + sourceReference + "' in step['" + step.getIdentifier() + "']");
        }
        // Make sure source is not dependent on us too
        if (source.hasDependency(step)) {
            this.getProcessDefinition().addDefinitionError(step.getDescription() + " has a recursive reference to " + source.getDescription());
        }
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public String getElementName() {
        return elementName;
    }

    public SourceDefinition getSource() {
        return source;
    }

    @Override
    protected boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
