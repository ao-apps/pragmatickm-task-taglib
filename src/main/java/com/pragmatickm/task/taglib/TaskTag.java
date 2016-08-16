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

import com.aoindustries.io.TempFileList;
import com.aoindustries.io.buffer.AutoTempFileWriter;
import com.aoindustries.io.buffer.BufferResult;
import com.aoindustries.io.buffer.BufferWriter;
import com.aoindustries.io.buffer.SegmentedWriter;
import com.aoindustries.servlet.filter.TempFileContext;
import com.aoindustries.util.CalendarUtils;
import com.aoindustries.util.schedule.DayDuration;
import com.aoindustries.util.schedule.Recurring;
import com.pragmatickm.task.model.Priority;
import com.pragmatickm.task.model.Task;
import com.pragmatickm.task.model.TaskException;
import com.pragmatickm.task.model.TaskLookup;
import com.pragmatickm.task.model.User;
import com.pragmatickm.task.servlet.impl.TaskImpl;
import com.semanticcms.core.model.ElementContext;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.CurrentPage;
import com.semanticcms.core.taglib.ElementTag;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;

public class TaskTag extends ElementTag<Task> {

	public TaskTag() {
		super(new Task());
	}

	private String style;
	public void setStyle(String style) {
		if(style!=null && style.isEmpty()) style = null;
		this.style = style;
	}

	public void setLabel(String label) {
		element.setLabel(label);
    }

    public void setOn(String on) {
		if(on != null) {
			on = on.trim();
			if(on.isEmpty()) on = null;
		}
		element.setOn(CalendarUtils.parseDate(on));
    }

	public void setRecurring(String recurring) throws IllegalArgumentException {
		if(recurring != null) {
			recurring = recurring.trim();
			if(recurring.isEmpty()) recurring = null;
		}
		element.setRecurring(recurring==null ? null : Recurring.parse(recurring));
    }

	public void setRelative(boolean relative) throws IllegalArgumentException {
		element.setRelative(relative);
    }

	public void setAssignedTo(String assignedTo) {
		User user =
			(assignedTo==null || assignedTo.isEmpty())
			? User.Unassigned
			: User.valueOf(assignedTo);
		if(user.isPerson()) element.addAssignedTo(user, DayDuration.ZERO_DAYS);
    }

	public void setPay(String pay) {
		element.setPay(pay);
	}

	public void setCost(String cost) {
		element.setCost(cost==null || cost.isEmpty() ? null : cost);
	}

	public void setPriority(String priority) {
		element.addPriority(
			(priority==null || priority.isEmpty())
			? Priority.DEFAULT_PRIORITY
			: Priority.valueOf(priority.toUpperCase(Locale.ROOT)),
			DayDuration.ZERO_DAYS
		);
	}

	void addDoBefore(TaskLookup taskLookup) {
		element.addDoBefore(taskLookup);
	}

	void addCustomLog(String name) {
		element.addCustomLog(name);
	}

	private BufferResult beforeBody;

	@Override
	protected void doBody(CaptureLevel captureLevel) throws JspException, IOException {
		try {
			final PageContext pageContext = (PageContext)getJspContext();
			final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
			final HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();

			final Page currentPage = CurrentPage.getCurrentPage(request);
			if(currentPage == null) throw new ServletException("<task:task> tag must be nested inside a <p:page> tag.");

			// Label defaults to page short title
			if(element.getLabel() == null) {
				element.setLabel(currentPage.getShortTitle());
			}

			// Locate the persistence file
			element.setXmlFile(TaskImpl.getTaskLogXmlFile(currentPage.getPageRef(), element.getId()));

			super.doBody(captureLevel);

			// Determine what goes before the body
			BufferWriter out;
			if(captureLevel == CaptureLevel.BODY) {
				out = new SegmentedWriter();
				// Enable temp files if temp file context active
				// Java 1.8: out = TempFileContext.wrapTempFileList(out, request, AutoTempFileWriter::new);
				out = TempFileContext.wrapTempFileList(
					out,
					request,
					new TempFileContext.Wrapper<BufferWriter>() {
						@Override
						public BufferWriter call(BufferWriter original, TempFileList tempFileList) {
							return new AutoTempFileWriter(original, tempFileList);
						}
					}
				);
			} else {
				out = null;
			}
			try {
				TaskImpl.writeBeforeBody(
					pageContext.getServletContext(),
					request,
					response,
					captureLevel,
					out,
					element,
					style
				);
			} finally {
				if(out != null) out.close();
			}
			if(out != null) {
				beforeBody = out.getResult();
			} else {
				beforeBody = null;
			}
		} catch(TaskException e) {
			throw new JspTagException(e);
		} catch(ServletException e) {
			throw new JspTagException(e);
		}
	}

	@Override
	public void writeTo(Writer out, ElementContext context) throws IOException {
		if(beforeBody != null) beforeBody.writeTo(out);
		TaskImpl.writeAfterBody(element, out, context);
	}
}
