/*
 * pragmatickm-task-taglib - Tasks nested within SemanticCMS pages and elements in a JSP environment.
 * Copyright (C) 2015, 2016  AO Industries, Inc.
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

import com.aoindustries.util.schedule.DayDuration;
import com.pragmatickm.task.model.Priority;
import com.pragmatickm.task.model.Task;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.servlet.CurrentNode;
import java.io.IOException;
import java.util.Locale;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class PriorityTag extends SimpleTagSupport {

	private Priority priority;
    public void setPriority(String priority) {
		this.priority = Priority.valueOf(priority.toUpperCase(Locale.ROOT));
    }

	private DayDuration after = DayDuration.ZERO_DAYS;
	public void setAfter(String after) {
		this.after = DayDuration.valueOf(after);
	}

	@Override
    public void doTag() throws JspTagException, IOException {
		PageContext pageContext = (PageContext)getJspContext();
		ServletRequest request = pageContext.getRequest();
		// Find the required task
		Node currentNode = CurrentNode.getCurrentNode(request);
		if(!(currentNode instanceof Task)) throw new JspTagException("<task:priority> tag must be nested inside a <task:task> tag.");
		Task currentTask = (Task)currentNode;
		currentTask.addPriority(priority, after);
	}
}
