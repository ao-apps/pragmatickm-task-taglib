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

import static com.aoindustries.servlet.filter.FunctionContext.getRequest;
import static com.aoindustries.servlet.filter.FunctionContext.getResponse;
import static com.aoindustries.servlet.filter.FunctionContext.getServletContext;
import com.pragmatickm.task.model.Task;
import com.pragmatickm.task.model.TaskLog;
import com.pragmatickm.task.model.User;
import com.pragmatickm.task.servlet.TaskUtil;
import com.semanticcms.core.model.Page;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;

final public class Functions {

	public static TaskLog getTaskLogInBook(String book, String page, String taskId) throws ServletException, IOException {
		return TaskUtil.getTaskLogInBook(
			getServletContext(),
			getRequest(),
			book,
			page,
			taskId
		);
	}

	public static TaskLog getTaskLog(String page, String taskId) throws ServletException, IOException {
		return getTaskLogInBook(null, page, taskId);
	}

	/**
	 * @see  TaskUtil#getDoAfters(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, com.pragmatickm.task.model.Task)
	 */
	public static List<Task> getDoAfters(Task task) throws ServletException, IOException {
		return TaskUtil.getDoAfters(
			getServletContext(),
			getRequest(),
			getResponse(),
			task
		);
	}

	public static User getUser() {
		return TaskUtil.getUser(
			getRequest(),
			getResponse()
		);
	}

	public static List<Task> prioritizeTasks(List<Task> tasks, boolean dateFirst) throws ServletException, IOException {
		return TaskUtil.prioritizeTasks(
			getServletContext(),
			getRequest(),
			getResponse(),
			tasks,
			dateFirst
		);
	}

	public static List<Task> getAllTasks(Page rootPage, User user) throws IOException, ServletException {
		return TaskUtil.getAllTasks(
			getServletContext(),
			getRequest(),
			getResponse(),
			rootPage,
			user
		);
	}

	public static boolean hasAssignedTask(Page page, User user, boolean recursive) throws ServletException, IOException {
		return TaskUtil.hasAssignedTask(
			getServletContext(),
			getRequest(),
			getResponse(),
			page,
			user,
			recursive
		);
	}

	public static List<Task> getReadyTasks(Page rootPage, User user) throws IOException, ServletException {
		return TaskUtil.getReadyTasks(
			getServletContext(),
			getRequest(),
			getResponse(),
			rootPage,
			user
		);
	}

	public static List<Task> getBlockedTasks(Page rootPage, User user) throws IOException, ServletException {
		return TaskUtil.getBlockedTasks(
			getServletContext(),
			getRequest(),
			getResponse(),
			rootPage,
			user
		);
	}

	public static List<Task> getFutureTasks(Page rootPage, User user) throws IOException, ServletException {
		return TaskUtil.getFutureTasks(
			getServletContext(),
			getRequest(),
			getResponse(),
			rootPage,
			user
		);
	}

	/**
	 * Make no instances.
	 */
	private Functions() {
	}
}