/*
 * pragmatickm-task-taglib - Tasks nested within SemanticCMS pages and elements in a JSP environment.
 * Copyright (C) 2013, 2014, 2016, 2017, 2020, 2021, 2022  AO Industries, Inc.
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

import static com.aoapps.taglib.AttributeUtils.resolveValue;

import com.pragmatickm.task.model.Task;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.CurrentNode;
import java.io.IOException;
import javax.el.ValueExpression;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class CustomLogTag extends SimpleTagSupport {

  public static final String TAG_NAME = "<task:customLog>";

  private ValueExpression name;

  public void setName(ValueExpression name) {
    this.name = name;
  }

  @Override
  public void doTag() throws JspException, IOException {
    PageContext pageContext = (PageContext) getJspContext();
    ServletRequest request = pageContext.getRequest();

    // Find the required task
    Node currentNode = CurrentNode.getCurrentNode(request);
    if (!(currentNode instanceof Task)) {
      throw new JspTagException(TAG_NAME + " tag must be nested inside a " + TaskTag.TAG_NAME + " tag.");
    }
    Task currentTask = (Task) currentNode;

    assert
        CaptureLevel.getCaptureLevel(request).compareTo(CaptureLevel.META) >= 0
        : "This is always contained by a task tag, so this is only invoked at captureLevel >= META";

    currentTask.addCustomLog(
        resolveValue(name, String.class, pageContext.getELContext())
    );
  }
}
