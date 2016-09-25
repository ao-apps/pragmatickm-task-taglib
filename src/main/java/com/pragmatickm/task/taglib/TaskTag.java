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
import static com.aoindustries.taglib.AttributeUtils.resolveValue;
import com.aoindustries.taglib.StyleAttribute;
import com.aoindustries.util.CalendarUtils;
import com.aoindustries.util.StringUtility;
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
import javax.el.ELContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;

public class TaskTag extends ElementTag<Task> implements StyleAttribute {

	private Object style;
	@Override
	public void setStyle(Object style) {
		this.style = style;
	}

	private Object label;
	public void setLabel(Object label) {
		this.label = label;
    }

	private Object on;
    public void setOn(Object on) {
		this.on = on;
    }

	private Object recurring;
	public void setRecurring(Object recurring) throws IllegalArgumentException {
		this.recurring = recurring;
    }

	private Object relative;
	public void setRelative(Object relative) throws IllegalArgumentException {
		this.relative = relative;
    }

	private Object assignedTo;
	public void setAssignedTo(Object assignedTo) {
		this.assignedTo = assignedTo;
    }

	private Object pay;
	public void setPay(Object pay) {
		this.pay = pay;
	}

	private Object cost;
	public void setCost(Object cost) {
		this.cost = cost;
	}

	private Object priority;
	public void setPriority(Object priority) {
		this.priority = priority;
	}

	@Override
	protected Task createElement() {
		return new Task();
	}

	@Override
	protected void evaluateAttributes(Task task, ELContext elContext) throws JspTagException, IOException {
		super.evaluateAttributes(task, elContext);
		task.setLabel(resolveValue(label, String.class, elContext));
		task.setOn(CalendarUtils.parseDate(StringUtility.nullIfEmpty(resolveValue(on, String.class, elContext))));
		task.setRecurring(Recurring.parse(StringUtility.nullIfEmpty(resolveValue(recurring, String.class, elContext))));
		Boolean relativeObj = resolveValue(relative, Boolean.class, elContext);
		if(relativeObj != null) task.setRelative(relativeObj);
		String assignedToStr = StringUtility.nullIfEmpty(resolveValue(assignedTo, String.class, elContext));
		User user =
			(assignedToStr==null)
			? User.Unassigned
			: User.valueOf(assignedToStr);
		if(user.isPerson()) task.addAssignedTo(user, DayDuration.ZERO_DAYS);
		task.setPay(resolveValue(pay, String.class, elContext));
		task.setCost(resolveValue(cost, String.class, elContext));
		String priorityStr = StringUtility.nullIfEmpty(resolveValue(priority, String.class, elContext));
		task.addPriority(
			(priorityStr==null)
			? Priority.DEFAULT_PRIORITY
			: Priority.valueOf(priorityStr.toUpperCase(Locale.ROOT)),
			DayDuration.ZERO_DAYS
		);
	}

	void addDoBefore(TaskLookup taskLookup) {
		getElement().addDoBefore(taskLookup);
	}

	void addCustomLog(String name) {
		getElement().addCustomLog(name);
	}

	private BufferResult beforeBody;

	@Override
	protected void doBody(Task task, CaptureLevel captureLevel) throws JspException, IOException {
		try {
			final PageContext pageContext = (PageContext)getJspContext();
			final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
			final HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();

			final Page currentPage = CurrentPage.getCurrentPage(request);
			if(currentPage == null) throw new ServletException("<task:task> tag must be nested inside a <core:page> tag.");

			// Label defaults to page short title
			if(task.getLabel() == null) {
				task.setLabel(currentPage.getShortTitle());
			}

			// Locate the persistence file
			task.setXmlFile(TaskImpl.getTaskLogXmlFile(currentPage.getPageRef(), task.getId()));

			super.doBody(task, captureLevel);

			// Determine what goes before the body
			BufferWriter capturedOut;
			if(captureLevel == CaptureLevel.BODY) {
				// Enable temp files if temp file context active
				capturedOut = TempFileContext.wrapTempFileList(
					new SegmentedWriter(),
					request,
					// Java 1.8: AutoTempFileWriter::new
					new TempFileContext.Wrapper<BufferWriter>() {
						@Override
						public BufferWriter call(BufferWriter original, TempFileList tempFileList) {
							return new AutoTempFileWriter(original, tempFileList);
						}
					}
				);
			} else {
				capturedOut = null;
			}
			try {
				TaskImpl.writeBeforeBody(
					pageContext.getServletContext(),
					pageContext.getELContext(),
					request,
					response,
					captureLevel,
					capturedOut,
					task,
					style
				);
			} finally {
				if(capturedOut != null) capturedOut.close();
			}
			if(capturedOut != null) {
				beforeBody = capturedOut.getResult();
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
		assert beforeBody != null : "writeTo is only called on captureLevel=BODY, so this should have been set in doBody";
		beforeBody.writeTo(out);
		TaskImpl.writeAfterBody(getElement(), out, context);
	}
}
