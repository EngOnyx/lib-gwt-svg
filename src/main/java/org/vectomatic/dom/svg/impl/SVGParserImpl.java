/**********************************************
 * Copyright (C) 2010 Lukas Laag
 * This file is part of lib-gwt-svg.
 * 
 * libgwtsvg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * libgwtsvg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with libgwtsvg.  If not, see http://www.gnu.org/licenses/
 **********************************************/
package org.vectomatic.dom.svg.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.vectomatic.dom.svg.OMSVGScriptElement;
import org.vectomatic.dom.svg.utils.DOMHelper;
import org.vectomatic.dom.svg.utils.ParserException;
import org.vectomatic.dom.svg.utils.SVGConstants;
import org.vectomatic.dom.svg.utils.SVGPrefixResolver;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;

public class SVGParserImpl {
	@SuppressWarnings("unused")
	private final JavaScriptObject domParser = createDOMParser();
	
	private native JavaScriptObject createDOMParser() /*-{
	  return new DOMParser();
	}-*/;

	public final native Document parseFromString(String rawText, String contentType) /*-{
      return this.@org.vectomatic.dom.svg.impl.SVGParserImpl::domParser.parseFromString(rawText, contentType);
	}-*/;

	/**
	 * Parses the supplied SVG text into a document
	 * @param rawSvg
	 * raw xml to be parsed
	 * @param enableScripts
	 * true to enable embedded scripts, false otherwise
	 * @return
	 * the document resulting from the parse
	 */
	public SVGSVGElement parse(String rawSvg, boolean enableScripts) throws ParserException {
		SVGDocument doc = parseFromString(rawSvg, "text/xml").cast();
		Element elt = doc.getDocumentElement();
		if ("parsererror".equals(DOMHelper.getLocalName(elt))) {
			String message = "Parsing error";
			if (elt.getFirstChild() != null) {
				message = elt.getFirstChild().<Text>cast().getData();
			}
			throw new ParserException(ParserException.Type.NotWellFormed, message);
		}
		SVGSVGElement svg = DOMHelper.importNode(DOMHelper.getCurrentDocument(), elt, true).<SVGSVGElement>cast();
		return enableScripts ? enableScriptElements(svg) : svg;
	}

	/**
	 * Re-enables scripts disabled by the DOMParser.parseFromString method.
	 * @param svg
	 * The svg for which scripts are to be re-enabled.
	 * @return
	 * The svg with re-enabled scripts.
	 */
	protected static SVGSVGElement enableScriptElements(SVGSVGElement svg) {
		// Put all scripts in a list (XPath result sets cannot be modified during traversal).
		List<SVGScriptElement> scripts = new ArrayList<SVGScriptElement>();
		Iterator<Node> iterator = DOMHelper.evaluateNodeListXPath(svg, "//svg:script", SVGPrefixResolver.INSTANCE);
		while (iterator.hasNext()) {
			scripts.add(iterator.next().<SVGScriptElement>cast());
		}
		for (SVGScriptElement script : scripts) {
			// Reparent the script subtree under a fresh script node
			SVGScriptElement newScript = new OMSVGScriptElement().getElement().<SVGScriptElement>cast();
			Node node;
			while((node = script.getFirstChild()) != null) {
				newScript.appendChild(script.removeChild(node));
			}
			script.getParentNode().replaceChild(newScript, script);
		}
		return svg;
	}

}
