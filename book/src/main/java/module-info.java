/*
 * pragmatickm-task-taglib - Tasks nested within SemanticCMS pages and elements in a JSP environment.
 * Copyright (C) 2021, 2022  AO Industries, Inc.
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
module com.pragmatickm.task.taglib.book {
  exports com.pragmatickm.task.taglib.book;
  provides javax.servlet.ServletContainerInitializer with com.pragmatickm.task.taglib.book.PragmaticKmTaskTldInitializer;
  // Direct
  requires com.aoapps.badges; // <groupId>com.aoapps</groupId><artifactId>ao-badges</artifactId>
  requires com.aoapps.lang; // <groupId>com.aoapps</groupId><artifactId>ao-lang</artifactId>
  requires com.aoapps.net.types; // <groupId>com.aoapps</groupId><artifactId>ao-net-types</artifactId>
  requires com.aoapps.servlet.util; // <groupId>com.aoapps</groupId><artifactId>ao-servlet-util</artifactId>
  requires com.aoapps.taglib; // <groupId>com.aoapps</groupId><artifactId>ao-taglib</artifactId>
  requires javax.servlet.api; // <groupId>javax.servlet</groupId><artifactId>javax.servlet-api</artifactId>
  requires javax.servlet.jsp.api; // <groupId>javax.servlet.jsp</groupId><artifactId>javax.servlet.jsp-api</artifactId>
  requires com.semanticcms.changelog.taglib; // <groupId>com.semanticcms</groupId><artifactId>semanticcms-changelog-taglib</artifactId>
  requires com.semanticcms.core.taglib; // <groupId>com.semanticcms</groupId><artifactId>semanticcms-core-taglib</artifactId>
  requires com.semanticcms.section.taglib; // <groupId>com.semanticcms</groupId><artifactId>semanticcms-section-taglib</artifactId>
  requires com.semanticcms.tagreference; // <groupId>com.semanticcms</groupId><artifactId>semanticcms-tag-reference</artifactId>
  requires taglibs.standard.spec; // <groupId>org.apache.taglibs</groupId><artifactId>taglibs-standard-spec</artifactId>
}
