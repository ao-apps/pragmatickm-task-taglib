/*
 * pragmatickm-task-taglib - Tasks nested within SemanticCMS pages and elements in a JSP environment.
 * Copyright (C) 2017, 2019, 2020, 2021, 2022, 2025, 2026  AO Industries, Inc.
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
 * along with pragmatickm-task-taglib.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.pragmatickm.task.taglib.book;

import com.semanticcms.tagreference.TagReferenceInitializer;
import jakarta.servlet.ServletContainerInitializer;

/**
 * Initializes a tag reference during {@linkplain ServletContainerInitializer application start-up}.
 */
public class PragmaticKmTaskTldInitializer extends TagReferenceInitializer {

  /**
   * Parses the TLD file.
   */
  public PragmaticKmTaskTldInitializer() {
    super(
        Maven.properties.getProperty("documented.name") + " Reference",
        "Taglib Reference",
        "/task/taglib",
        "/pragmatickm-task.tld",
        true,
        Maven.properties.getProperty("documented.javadoc.link.javase"),
        Maven.properties.getProperty("documented.javadoc.link.javaee"),
        // Self
        "com.pragmatickm.task.taglib", Maven.properties.getProperty("project.url") + "apidocs/com.pragmatickm.task.taglib/",
        // Dependencies
        "com.aoapps.lang.util", "https://oss.aoapps.com/lang/apidocs/com.aoapps.lang/",
        "com.pragmatickm.task.model", "https://pragmatickm.com/task/model/apidocs/com.pragmatickm.task.model/",
        "com.pragmatickm.task.servlet", "https://pragmatickm.com/task/servlet/apidocs/com.pragmatickm.task.servlet/",
        "com.semanticcms.core.model", "https://semanticcms.com/core/model/apidocs/com.semanticcms.core.model/"
    );
  }
}
