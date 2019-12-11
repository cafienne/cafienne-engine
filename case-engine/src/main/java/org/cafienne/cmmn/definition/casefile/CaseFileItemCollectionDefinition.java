package org.cafienne.cmmn.definition.casefile;

import java.util.ArrayList;
import java.util.Collection;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.w3c.dom.Element;

public class CaseFileItemCollectionDefinition extends CMMNElementDefinition {
	private final Collection<CaseFileItemDefinition> items = new ArrayList<CaseFileItemDefinition>();

	public CaseFileItemCollectionDefinition(Element element, Definition definition, CMMNElementDefinition parentElement)
	{
		super(element, definition, parentElement);
	}
	
	public Collection<CaseFileItemDefinition> getItems()
	{
		return items;
	}
}
