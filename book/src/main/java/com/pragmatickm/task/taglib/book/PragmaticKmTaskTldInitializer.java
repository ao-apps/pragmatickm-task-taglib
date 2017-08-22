/*
 * pragmatickm-task-taglib - Tasks nested within SemanticCMS pages and elements in a JSP environment.
 * Copyright (C) 2017  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of pragmatickm-task-taglib.
 *
 * pragmatickm-task-taglib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * pragmatickm-task-taglib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with pragmatickm-task-taglib.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pragmatickm.task.taglib.book;

import com.aoindustries.net.Path;
import com.aoindustries.validation.ValidationException;
import com.semanticcms.core.model.BookRef;
import com.semanticcms.core.model.ResourceRef;
import com.semanticcms.tagreference.TagReferenceInitializer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author  AO Industries, Inc.
 */
public class PragmaticKmTaskTldInitializer extends TagReferenceInitializer {

	private static final Map<String,String> additionalApiLinks = new LinkedHashMap<String,String>();
	static {
		// Self
		additionalApiLinks.put("com.pragmatickm.task.taglib.", Maven.properties.getProperty("documented.url") + "apidocs/");
		// Dependencies
		additionalApiLinks.put("com.aoindustries.util.", "https://aoindustries.com/ao-lang/apidocs/");
		additionalApiLinks.put("com.pragmatickm.task.model.", "https://pragmatickm.com/task/model/apidocs/");
		additionalApiLinks.put("com.pragmatickm.task.servlet.", "https://pragmatickm.com/task/servlet/apidocs/");
		additionalApiLinks.put("com.semanticcms.core.model.", "https://semanticcms.com/core/model/apidocs/");
	}

	public PragmaticKmTaskTldInitializer() throws ValidationException {
		super(
			"Task Taglib Reference",
			"Taglib Reference",
			new ResourceRef(
				new BookRef(
					"pragmatickm.com",
					Path.valueOf("/task/taglib")
				),
				Path.valueOf("/pragmatickm-task.tld")
			),
			Maven.properties.getProperty("javac.link.javaApi.jdk16"),
			Maven.properties.getProperty("javac.link.javaeeApi.6"),
			additionalApiLinks
		);
	}
}
