/*
 * pragmatickm-task-taglib - Tasks nested within SemanticCMS pages and elements in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2019, 2020, 2021  AO Industries, Inc.
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

import com.aoindustries.encoding.Doctype;
import com.aoindustries.encoding.Serialization;
import com.aoindustries.encoding.servlet.DoctypeEE;
import com.aoindustries.encoding.servlet.SerializationEE;
import com.aoindustries.encoding.taglib.EncodingBufferedTag;
import com.aoindustries.html.Document;
import com.aoindustries.html.servlet.DocumentEE;
import com.aoindustries.io.buffer.BufferResult;
import com.aoindustries.io.buffer.BufferWriter;
import com.aoindustries.lang.Strings;
import static com.aoindustries.taglib.AttributeUtils.resolveValue;
import com.aoindustries.util.CalendarUtils;
import com.aoindustries.util.schedule.DayDuration;
import com.aoindustries.util.schedule.Recurring;
import com.pragmatickm.task.model.Priority;
import com.pragmatickm.task.model.Task;
import com.pragmatickm.task.model.TaskException;
import com.pragmatickm.task.model.User;
import com.pragmatickm.task.renderer.html.TaskHtmlRenderer;
import com.semanticcms.core.controller.Book;
import com.semanticcms.core.controller.SemanticCMS;
import com.semanticcms.core.model.BookRef;
import com.semanticcms.core.model.ElementContext;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.pages.CaptureLevel;
import com.semanticcms.core.pages.local.CurrentPage;
import com.semanticcms.core.taglib.ElementTag;
import com.semanticcms.core.taglib.PageTag;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;

public class TaskTag extends ElementTag<Task> /*implements StyleAttribute*/ {

	public static final String TAG_NAME = "<task:task>";

	private ValueExpression style;
	public void setStyle(ValueExpression style) {
		this.style = style;
	}

	private ValueExpression label;
	public void setLabel(ValueExpression label) {
		this.label = label;
    }

	private ValueExpression on;
    public void setOn(ValueExpression on) {
		this.on = on;
    }

	private ValueExpression recurring;
	public void setRecurring(ValueExpression recurring) throws IllegalArgumentException {
		this.recurring = recurring;
    }

	private ValueExpression relative;
	public void setRelative(ValueExpression relative) throws IllegalArgumentException {
		this.relative = relative;
    }

	private ValueExpression assignedTo;
	public void setAssignedTo(ValueExpression assignedTo) {
		this.assignedTo = assignedTo;
    }

	private ValueExpression pay;
	public void setPay(ValueExpression pay) {
		this.pay = pay;
	}

	private ValueExpression cost;
	public void setCost(ValueExpression cost) {
		this.cost = cost;
	}

	private ValueExpression priority;
	public void setPriority(ValueExpression priority) {
		this.priority = priority;
	}

	@Override
	protected Task createElement() throws JspException {
		final PageContext pageContext = (PageContext)getJspContext();
		Page currentPage = CurrentPage.getCurrentPage(pageContext.getRequest());
		if(currentPage == null) throw new JspTagException(TAG_NAME + " tag must be nested inside a " + PageTag.TAG_NAME + " tag.");
		BookRef bookRef = currentPage.getPageRef().getBookRef();
		Book book = SemanticCMS.getInstance(pageContext.getServletContext()).getBook(bookRef);
		if(!book.isAccessible()) {
			throw new IllegalArgumentException("Book is not accessible: " + bookRef);
		}
		return new Task(book.getResources());
	}

	@Override
	protected void evaluateAttributes(Task task, ELContext elContext) throws JspTagException, IOException {
		super.evaluateAttributes(task, elContext);
		task.setLabel(resolveValue(label, String.class, elContext));
		task.setOn(CalendarUtils.parseDate(Strings.nullIfEmpty(resolveValue(on, String.class, elContext))));
		task.setRecurring(Recurring.parse(Strings.nullIfEmpty(resolveValue(recurring, String.class, elContext))));
		Boolean relativeObj = resolveValue(relative, Boolean.class, elContext);
		if(relativeObj != null) task.setRelative(relativeObj);
		String assignedToStr = Strings.nullIfEmpty(resolveValue(assignedTo, String.class, elContext));
		User user =
			(assignedToStr==null)
			? User.Unassigned
			: User.valueOf(assignedToStr);
		if(user.isPerson()) task.addAssignedTo(user, DayDuration.ZERO_DAYS);
		task.setPay(resolveValue(pay, String.class, elContext));
		task.setCost(resolveValue(cost, String.class, elContext));
		String priorityStr = Strings.nullIfEmpty(resolveValue(priority, String.class, elContext));
		if(priorityStr != null) {
			task.addPriority(
				Priority.valueOf(priorityStr.toUpperCase(Locale.ROOT)),
				DayDuration.ZERO_DAYS
			);
		}
	}

	/*
	void addDoBefore(ElementRef doBefore) {
		getElement().addDoBefore(doBefore);
	}

	void addCustomLog(String name) {
		getElement().addCustomLog(name);
	}*/

	private BufferResult beforeBody;
	private Serialization serialization;
	private Doctype doctype;

	@Override
	protected void doBody(Task task, CaptureLevel captureLevel) throws JspException, IOException {
		try {
			PageContext pageContext = (PageContext)getJspContext();
			ServletContext servletContext = pageContext.getServletContext();
			HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
			HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();

			final Page currentPage = CurrentPage.getCurrentPage(request);
			if(currentPage == null) throw new ServletException(TAG_NAME + " tag must be nested inside a " + PageTag.TAG_NAME + " tag.");

			// Label defaults to page short title
			if(task.getLabel() == null) {
				task.setLabel(currentPage.getShortTitle());
			}

			// Locate the persistence file
			task.setXmlFile(TaskHtmlRenderer.getTaskLogXmlFile(currentPage.getPageRef(), task.getId()));

			super.doBody(task, captureLevel);

			// Determine what goes before the body
			BufferWriter capturedOut;
			if(captureLevel == CaptureLevel.BODY) {
				capturedOut = EncodingBufferedTag.newBufferWriter(request);
			} else {
				capturedOut = null;
			}
			try {
				TaskHtmlRenderer.writeBeforeBody(servletContext,
					pageContext.getELContext(),
					request,
					response,
					captureLevel,
					(capturedOut == null) ? null : DocumentEE.get(servletContext, request, response, capturedOut),
					task,
					style
				);
			} finally {
				if(capturedOut != null) capturedOut.close();
			}
			if(capturedOut != null) {
				beforeBody = capturedOut.getResult();
				serialization = SerializationEE.get(servletContext, request);
				doctype = DoctypeEE.get(servletContext, request);
			} else {
				beforeBody = null;
			}
		} catch(TaskException | ServletException e) {
			throw new JspTagException(e);
		}
	}

	@Override
	public void writeTo(Writer out, ElementContext context) throws IOException {
		assert beforeBody != null : "writeTo is only called on captureLevel=BODY, so this should have been set in doBody";
		beforeBody.writeTo(out);
		TaskHtmlRenderer.writeAfterBody(
			getElement(),
			new Document(serialization, doctype, out),
			context
		);
	}
}
