/*
 * pragmatickm-task-taglib - Tasks nested within SemanticCMS pages and elements in a JSP environment.
 * Copyright (C) 2017, 2019, 2020  AO Industries, Inc.
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

import com.aoindustries.net.DomainName;
import com.aoindustries.net.Path;
import com.aoindustries.validation.ValidationException;
import com.semanticcms.core.model.BookRef;
import com.semanticcms.core.model.ResourceRef;
import com.semanticcms.tagreference.TagReferenceInitializer;
import java.util.Collections;

public class PragmaticKmTaskTldInitializer extends TagReferenceInitializer {

	@SuppressWarnings("unchecked")
	public PragmaticKmTaskTldInitializer() throws ValidationException {
		super(
			Maven.properties.getProperty("project.name") + " Reference",
			"Taglib Reference",
			new ResourceRef(
				new BookRef(
					DomainName.valueOf("pragmatickm.com"),
					Path.valueOf("/task/taglib")
				),
				Path.valueOf("/pragmatickm-task.tld")
			),
			true,
			Maven.properties.getProperty("documented.javadoc.link.javase"),
			Maven.properties.getProperty("documented.javadoc.link.javaee"),
			// Self
			Collections.singletonMap("com.pragmatickm.task.taglib", Maven.properties.getProperty("project.url") + "apidocs/"),
			// Dependencies
			Collections.singletonMap("com.aoindustries.util", "https://aoindustries.com/ao-lang/apidocs/"),
			Collections.singletonMap("com.pragmatickm.task.model", "https://pragmatickm.com/task/model/apidocs/"),
			Collections.singletonMap("com.pragmatickm.task.servlet", "https://pragmatickm.com/task/servlet/apidocs/"),
			Collections.singletonMap("com.semanticcms.core.model", "https://semanticcms.com/core/model/apidocs/")
		);
	}
}
