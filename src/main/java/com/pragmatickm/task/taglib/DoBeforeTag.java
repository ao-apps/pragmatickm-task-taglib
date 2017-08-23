/*
 * pragmatickm-task-taglib - Tasks nested within SemanticCMS pages and elements in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017  AO Industries, Inc.
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
package com.pragmatickm.task.taglib;

import com.aoindustries.net.DomainName;
import com.aoindustries.net.Path;
import static com.aoindustries.taglib.AttributeUtils.resolveValue;
import static com.aoindustries.util.StringUtility.nullIfEmpty;
import com.aoindustries.validation.ValidationException;
import com.pragmatickm.task.model.Task;
import com.semanticcms.core.model.Element;
import com.semanticcms.core.model.ElementRef;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.pages.CaptureLevel;
import com.semanticcms.core.pages.local.CurrentCaptureLevel;
import com.semanticcms.core.pages.local.CurrentNode;
import com.semanticcms.core.servlet.PageRefResolver;
import java.io.IOException;
import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class DoBeforeTag extends SimpleTagSupport {

	private ValueExpression domain;
	public void setDomain(ValueExpression domain) {
		this.domain = domain;
	}

	private ValueExpression book;
	public void setBook(ValueExpression book) {
		this.book = book;
	}

	private ValueExpression page;
	public void setPage(ValueExpression page) {
		this.page = page;
	}

	private ValueExpression task;
	public void setTask(ValueExpression task) throws JspTagException {
		this.task = task;
	}

	@Override
    public void doTag() throws JspTagException, IOException {
		try {
			PageContext pageContext = (PageContext)getJspContext();
			final ServletContext servletContext = pageContext.getServletContext();
			final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
			final HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();

			// Find the required task
			Node currentNode = CurrentNode.getCurrentNode(request);
			if(!(currentNode instanceof Task)) throw new JspTagException("<task:doBefore> tag must be nested inside a <task:task> tag.");
			Task currentTask = (Task)currentNode;

			assert
				CurrentCaptureLevel.getCaptureLevel(request).compareTo(CaptureLevel.META) >= 0
				: "This is always contained by a task tag, so this is only invoked at captureLevel >= META";

			// Evaluate expressions
			ELContext elContext = pageContext.getELContext();
			DomainName domainObj = DomainName.valueOf(
				nullIfEmpty(
					resolveValue(domain, String.class, elContext)
				)
			);
			Path bookPath = Path.valueOf(
				nullIfEmpty(
					resolveValue(book, String.class, elContext)
				)
			);
			String pageStr = nullIfEmpty(resolveValue(page, String.class, elContext));
			String taskStr = resolveValue(task, String.class, elContext);

			if(!Element.isValidId(taskStr)) throw new JspTagException("Invalid task id: " + taskStr);

			// Resolve the book-relative page path
			final PageRef pageRef;
			{
				if(domainObj != null && bookPath == null) {
					throw new JspTagException("book must be provided when domain is provided.");
				}
				if(pageStr==null) {
					// Use this page when none specified
					if(bookPath != null) throw new JspTagException("page must be provided when book is provided.");
					pageRef = PageRefResolver.getCurrentPageRef(servletContext, request);
				} else {
					// Default to current domain
					if(domainObj == null) {
						domainObj = PageRefResolver.getCurrentPageRef(servletContext, request).getBookRef().getDomain();
					}
					// Resolve context-relative page path from page-relative
					pageRef = PageRefResolver.getPageRef(
						servletContext,
						request,
						domainObj,
						bookPath,
						pageStr
					);
				}
			}
			currentTask.addDoBefore(new ElementRef(pageRef, taskStr));
		} catch(ServletException e) {
			throw new JspTagException(e);
		} catch(ValidationException e) {
			throw new JspTagException(e);
		}
	}
}
