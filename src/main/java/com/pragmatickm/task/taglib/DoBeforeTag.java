/*
 * pragmatickm-task-taglib - Tasks nested within SemanticCMS pages and elements in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2019, 2020, 2021, 2022, 2023  AO Industries, Inc.
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

package com.pragmatickm.task.taglib;

import static com.aoapps.lang.Strings.nullIfEmpty;
import static com.aoapps.servlet.el.ElUtils.resolveValue;

import com.aoapps.lang.xml.XmlUtils;
import com.pragmatickm.task.model.Task;
import com.semanticcms.core.model.ElementRef;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.CurrentNode;
import com.semanticcms.core.servlet.PageRefResolver;
import java.io.IOException;
import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class DoBeforeTag extends SimpleTagSupport {

  public static final String TAG_NAME = "<task:doBefore>";

  private ValueExpression book;

  public void setBook(ValueExpression book) {
    this.book = book;
  }

  private ValueExpression page;

  public void setPage(ValueExpression page) {
    this.page = page;
  }

  private ValueExpression task;

  public void setTask(ValueExpression task) {
    this.task = task;
  }

  @Override
  public void doTag() throws JspException, IOException {
    final PageContext pageContext = (PageContext) getJspContext();
    final ServletContext servletContext = pageContext.getServletContext();
    final HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
    final HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

    // Find the required task
    final Node currentNode = CurrentNode.getCurrentNode(request);
    if (!(currentNode instanceof Task)) {
      throw new JspTagException(TAG_NAME + " tag must be nested inside a " + TaskTag.TAG_NAME + " tag.");
    }
    final Task currentTask = (Task) currentNode;

    assert
        CaptureLevel.getCaptureLevel(request).compareTo(CaptureLevel.META) >= 0
        : "This is always contained by a task tag, so this is only invoked at captureLevel >= META";

    // Evaluate expressions
    ELContext elContext = pageContext.getELContext();
    String bookStr = nullIfEmpty(resolveValue(book, String.class, elContext));
    String pageStr = nullIfEmpty(resolveValue(page, String.class, elContext));
    String taskStr = resolveValue(task, String.class, elContext);

    if (!XmlUtils.isValidName(taskStr)) {
      throw new JspTagException("Invalid task id: " + taskStr);
    }

    // Resolve the book-relative page path
    final PageRef pageRef;
    try {
      if (pageStr == null) {
        // Use this page when none specified
        if (bookStr != null) {
          throw new JspTagException("page must be provided when book is provided.");
        }
        pageRef = PageRefResolver.getCurrentPageRef(servletContext, request);
      } else {
        // Resolve context-relative page path from page-relative
        pageRef = PageRefResolver.getPageRef(servletContext, request, bookStr, pageStr);
      }
    } catch (ServletException e) {
      throw new JspTagException(e);
    }
    currentTask.addDoBefore(new ElementRef(pageRef, taskStr));
  }
}
