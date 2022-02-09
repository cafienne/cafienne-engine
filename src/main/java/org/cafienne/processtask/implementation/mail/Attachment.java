package org.cafienne.processtask.implementation.mail;

import org.cafienne.util.StringTemplate;

class Attachment {
	private final StringTemplate name;
	private final StringTemplate content;

	Attachment(String name, String content) {
		this.name = new StringTemplate(name);
		this.content = new StringTemplate(content);
	}

	public StringTemplate getName() {
		return this.name;
	}

	public StringTemplate getContent() {
		return this.content;
	}
}