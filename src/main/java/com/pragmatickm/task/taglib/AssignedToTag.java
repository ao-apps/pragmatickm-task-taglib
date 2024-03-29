/*
 * pragmatickm-task-taglib - Tasks nested within SemanticCMS pages and elements in a JSP environment.
 * Copyright (C) 2015, 2016, 2017, 2020, 2021, 2022, 2023  AO Industries, Inc.
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

import com.aoapps.hodgepodge.schedule.DayDuration;
import com.pragmatickm.task.model.Task;
import com.pragmatickm.task.model.User;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.CurrentNode;
import java.io.IOException;
import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class AssignedToTag extends SimpleTagSupport {

  public static final String TAG_NAME = "<task:assignedTo>";

  private ValueExpression who;

  public void setWho(ValueExpression who) {
    this.who = who;
  }

  private ValueExpression after;

  public void setAfter(ValueExpression after) {
    this.after = after;
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

    // Evaluate expressions
    ELContext elContext = pageContext.getELContext();
    User whoObj = User.valueOf(resolveValue(who, String.class, elContext));
    String afterStr = nullIfEmpty(resolveValue(after, String.class, elContext));

    if (!whoObj.isPerson()) {
      throw new IllegalArgumentException("Not a person: " + whoObj);
    }
    DayDuration afterObj = afterStr == null ? DayDuration.ZERO_DAYS : DayDuration.valueOf(afterStr);

    currentTask.addAssignedTo(
        whoObj,
        afterObj
    );
  }
}
