package com.pizzaplatform.api.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Hypermedia link to another resource of the API.
 *
 * @param href relative URL of the linked resource
 */
public record LinkDto(@JacksonXmlProperty(isAttribute = true) String href) {
}
