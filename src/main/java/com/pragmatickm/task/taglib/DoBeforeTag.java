/*
 * pragmatickm-task-taglib - Tasks nested within SemanticCMS pages and elements in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016  AO Industries, Inc.
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

import com.pragmatickm.task.model.Task;
import com.pragmatickm.task.model.TaskException;
import com.pragmatickm.task.model.TaskLookup;
import com.semanticcms.core.model.Element;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.CapturePage;
import com.semanticcms.core.servlet.CurrentNode;
import com.semanticcms.core.servlet.PageRefResolver;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class DoBeforeTag extends SimpleTagSupport {

	private String book;
	public void setBook(String book) {
		this.book = book;
	}

	private String page;
	public void setPage(String page) {
		this.page = page;
	}

	private String task;
	public void setTask(String task) throws JspTagException {
		if(task != null && task.isEmpty()) task = null;
		if(task != null && !Element.isValidId(task)) throw new JspTagException("Invalid task id: " + task);
		this.task = task;
	}

	@Override
    public void doTag() throws JspTagException, IOException {
		if(page==null && task==null) throw new JspTagException("Neither page nor task has been provided.");

		PageContext pageContext = (PageContext)getJspContext();
		final ServletContext servletContext = pageContext.getServletContext();
		final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		final HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();
		// Find the required task
		Node currentNode = CurrentNode.getCurrentNode(request);
		if(!(currentNode instanceof Task)) throw new JspTagException("<d:doBefore> tag must be nested inside a <d:task> tag.");
		Task currentTask = (Task)currentNode;
		// Resolve the book-relative page path
		final PageRef pageRef;
		try {
			if(page==null) {
				// Use this page when none specified
				if(this.book != null) throw new JspTagException("page must be provided when book is provided.");
				pageRef = PageRefResolver.getCurrentPageRef(servletContext, request);
			} else {
				// Resolve context-relative page path from page-relative
				pageRef = PageRefResolver.getPageRef(servletContext, request, this.book, this.page);
			}
		} catch(ServletException e) {
			throw new JspTagException(e);
		}
		final String taskId = task;
		currentTask.addDoBefore(
			new TaskLookup(pageRef, taskId) {
				private Task task;
				@Override
				public Task getTask() throws TaskException {
					if(task == null) {
						try {
							// Capture page when short-cut doesn't work
							Page capturedAfterPage = CapturePage.capturePage(
								servletContext,
								request,
								response,
								pageRef,
								CaptureLevel.META
							);
							Element element = capturedAfterPage.getElementsById().get(taskId);
							if(!(element instanceof Task)) throw new TaskException("Task not found: " + pageRef + '#' + taskId);
							if(capturedAfterPage.getGeneratedIds().contains(taskId)) throw new TaskException("Not allowed to reference task by generated id, set an explicit id on the task: " + element);
							task = (Task)element;
						} catch(ServletException e) {
							throw new TaskException(e);
						} catch(IOException e) {
							throw new TaskException(e);
						}
					}
					return task;
				}
			}
		);
	}
}
