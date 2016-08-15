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
import com.aoindustries.util.StringUtility;
import com.aoindustries.util.UnmodifiableCalendar;
import com.aoindustries.util.WrappedException;
import com.pragmatickm.task.model.Priority;
import com.pragmatickm.task.model.Task;
import com.pragmatickm.task.model.TaskAssignment;
import com.pragmatickm.task.model.TaskException;
import com.pragmatickm.task.model.TaskLog;
import com.pragmatickm.task.model.TaskLookup;
import com.pragmatickm.task.model.User;
import com.pragmatickm.task.servlet.Cookies;
import com.semanticcms.core.model.Element;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.CapturePage;
import com.semanticcms.core.servlet.PageDags;
import com.semanticcms.core.servlet.PageRefResolver;
import com.semanticcms.core.servlet.SemanticCMS;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

final public class Functions {

	public static TaskLog getTaskLogInBook(String book, String page, String taskId) throws ServletException, IOException {
		PageRef pageRef = PageRefResolver.getPageRef(
			getServletContext(),
			getRequest(),
			book,
			page
		);
		if(pageRef.getBook()==null) throw new IllegalArgumentException("Book not found: " + pageRef.getBookName());
		return TaskLog.getTaskLog(
			TaskTag.getTaskLogXmlFile(pageRef, taskId)
		);
	}

	public static TaskLog getTaskLog(String page, String taskId) throws ServletException, IOException {
		return getTaskLogInBook(null, page, taskId);
	}

	public static TaskLog.Entry getMostRecentEntry(TaskLog taskLog, String statuses) throws IOException {
		List<String> split = StringUtility.splitStringCommaSpace(statuses);
		List<TaskLog.Entry> entries = taskLog.getEntries();
		for(int i=entries.size()-1; i>=0; i--) {
			TaskLog.Entry entry = entries.get(i);
			String label = entry.getStatus().getLabel();
			for(String status : split) {
				if(label.equalsIgnoreCase(status)) {
					return entry;
				}
			}
		}
		return null;
	}

	/**
	 * Finds all tasks that must be done after this task.
	 * This requires a capture of the entire page tree
	 * meta data to find any task that has a doBefore pointing to this task.
	 */
	public static List<Task> getDoAfters(Task task) throws ServletException, IOException, TaskException {
		final ServletContext servletContext = getServletContext();
		final HttpServletRequest request = getRequest();
		final HttpServletResponse response = getResponse();
		final String taskId = task.getId();
		final Page taskPage = task.getPage();
		List<Task> doAfters = null;
		for(
			Page page
			: PageDags.convertPageDagToList(
				servletContext,
				request,
				response,
				CapturePage.capturePage(
					servletContext,
					request,
					response,
					SemanticCMS.getInstance(servletContext).getRootBook().getContentRoot(),
					CaptureLevel.META
				),
				CaptureLevel.META
			)
		) {
			for(Element element : page.getElements()) {
				if(element instanceof Task) {
					Task pageTask = (Task)element;
					for(TaskLookup doBeforeLookup : pageTask.getDoBefores()) {
						Task doBefore = doBeforeLookup.getTask();
						if(
							doBefore.getPage().equals(taskPage)
							&& doBefore.getId().equals(taskId)
						) {
							if(doAfters == null) doAfters = new ArrayList<Task>();
							doAfters.add(pageTask);
						}
					}
				}
			}
		}
		if(doAfters == null) return Collections.emptyList();
		else return doAfters;
	}

	public static User getUser() {
		HttpServletRequest request = getRequest();
		String userParam = request.getParameter("user");
		if(userParam != null) {
			// Find and set cookie
			User user = userParam.isEmpty() ? null : User.valueOf(userParam);
			Cookies.setUser(request, getResponse(), user);
			return user;
		} else {
			// Get from cookie
			return Cookies.getUser(request);
		}
	}

	public static Set<User> getAllUsers() {
		return EnumSet.allOf(User.class);
	}

	private static class TaskKey {
		private final PageRef pageRef;
		private final String taskId;

		private TaskKey(PageRef pageRef, String taskId) {
			this.pageRef = pageRef;
			this.taskId = taskId;
		}

		@Override
		public boolean equals(Object o) {
			if(!(o instanceof TaskKey)) return false;
			TaskKey other = (TaskKey)o;
			return
				pageRef.equals(other.pageRef)
				&& taskId.equals(other.taskId)
			;
		}

		@Override
		public int hashCode() {
			return pageRef.hashCode() * 31 + taskId.hashCode();
		}
	}

	private static Priority getEffectivePriority(
		long now,
		Task task,
		Task.StatusResult status,
		Map<Task,List<Task>> doAftersByTask,
		Map<Task,Priority> effectivePriorities
	) throws TaskException, IOException {
		Priority cached = effectivePriorities.get(task);
		if(cached != null) return cached;
		// Find the maximum priority of this task and all that will be done after it
		Priority effective;
		if(status.getDate() != null) {
			effective = task.getPriority(status.getDate(), now);
		} else {
			effective = task.getZeroDayPriority();
		}
		List<Task> doAfters = doAftersByTask.get(task);
		if(doAfters != null) {
			for(Task doAfter : doAfters) {
				Task.StatusResult doAfterStatus = doAfter.getStatus();
				if(
					!doAfterStatus.isCompletedSchedule()
					&& !doAfterStatus.isReadySchedule()
					&& !doAfterStatus.isFutureSchedule()
				) {
					Priority inherited = getEffectivePriority(now, doAfter, doAfterStatus, doAftersByTask, effectivePriorities);
					if(inherited.compareTo(effective) > 0) effective = inherited;
				}
			}
		}
		// Cache result
		effectivePriorities.put(task, effective);
		return effective;
	}

	public static List<Task> prioritizeTasks(List<Task> tasks, final boolean dateFirst) throws ServletException, IOException, TaskException {
		final ServletContext servletContext = getServletContext();
		final long now = System.currentTimeMillis();
		// Priority inheritance
		List<Task> allTasks = getAllTasks(
			CapturePage.capturePage(
				servletContext,
				getRequest(),
				getResponse(),
				SemanticCMS.getInstance(servletContext).getRootBook().getContentRoot(),
				CaptureLevel.META
			),
			null
		);
		// Index tasks by page,id
		Map<TaskKey,Task> tasksByKey = new HashMap<TaskKey,Task>(allTasks.size()*4/3+1);
		for(Task task : allTasks) {
			if(
				tasksByKey.put(
					new TaskKey(
						task.getPage().getPageRef(),
						task.getId()
					),
					task
				) != null
			) throw new AssertionError("Duplicate task (page, id)");
		}
		// Invert dependency DAG for fast lookups for priority inheritance
		final Map<Task,List<Task>> doAftersByTask = new LinkedHashMap<Task,List<Task>>(allTasks.size()*4/3+1);
		for(Task task : allTasks) {
			for(TaskLookup doBeforeLookup : task.getDoBefores()) {
				Task doBefore = tasksByKey.get(
					new TaskKey(
						doBeforeLookup.getPageRef(),
						doBeforeLookup.getTaskId()
					)
				);
				if(doBefore==null) throw new AssertionError("Task not found: page=" + doBeforeLookup.getPageRef()+", id=" + doBeforeLookup.getTaskId());
				List<Task> doAfters = doAftersByTask.get(doBefore);
				if(doAfters == null) {
					doAfters = new ArrayList<Task>();
					doAftersByTask.put(doBefore, doAfters);
				}
				doAfters.add(task);
			}
		}
		// Caches the effective priorities for tasks being prioritized or any other resolved in processing
		final Map<Task,Priority> effectivePriorities = new HashMap<Task,Priority>();
		// Build new list and sort
		List<Task> sortedTasks = new ArrayList<Task>(tasks);
		Collections.sort(
			sortedTasks,
			new Comparator<Task>() {
				private int dateDiff(Task t1, Task t2) throws TaskException, IOException {
					// Sort by scheduled or unscheduled
					Task.StatusResult status1 = t1.getStatus();
					Task.StatusResult status2 = t2.getStatus();
					Calendar date1 = status1.getDate();
					Calendar date2 = status2.getDate();
					int diff = Boolean.compare(date2!=null, date1!=null);
					if(diff!=0) return diff;
					// Then sort by date (if have date in both statuses)
					if(date1!=null && date2!=null) {
						diff = date1.compareTo(date2);
						if(diff!=0) return diff;
					}
					// Dates equal
					return 0;
				}

				@Override
				public int compare(Task t1, Task t2) {
					try {
						// Sort by date (when date first)
						if(dateFirst) {
							int diff = dateDiff(t1, t2);
							if(diff!=0) return diff;
						}
						// Sort by priority (including priority inheritance)
						Priority priority1 = getEffectivePriority(now, t1, t1.getStatus(), doAftersByTask, effectivePriorities);
						Priority priority2 = getEffectivePriority(now, t2, t2.getStatus(), doAftersByTask, effectivePriorities);
						int diff = priority2.compareTo(priority1);
						if(diff!=0) return diff;
						// Sort by date (when priority first)
						if(!dateFirst) {
							diff = dateDiff(t1, t2);
							if(diff!=0) return diff;
						}
						// Equal
						return 0;
					} catch(TaskException e) {
						throw new WrappedException(e);
					} catch(IOException e) {
						throw new WrappedException(e);
					}
				}
			}
		);
		return Collections.unmodifiableList(sortedTasks);
	}

	public static List<Task> getAllTasks(Page rootPage, User user) throws TaskException, IOException, ServletException {
		List<Task> allTasks = new ArrayList<Task>();
		for(
			Page page
			: PageDags.convertPageDagToList(
				getServletContext(),
				getRequest(),
				getResponse(),
				rootPage,
				CaptureLevel.META
			)
		) {
			for(Element element : page.getElements()) {
				if(element instanceof Task) {
					Task task = (Task)element;
					if(
						user == null
						|| task.getAssignedTo(user) != null
					) allTasks.add(task);
				}
			}
		}
		return Collections.unmodifiableList(allTasks);
	}

	public static boolean hasAssignedTask(Page page, User user, boolean recursive) throws TaskException, ServletException, IOException {
		return hasAssignedTaskRecursive(
			getServletContext(),
			getRequest(),
			getResponse(),
			page,
			user,
			recursive,
			System.currentTimeMillis(),
			recursive ? new HashSet<PageRef>() : null
		);
	}

	private static boolean hasAssignedTaskRecursive(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Page page,
		User user,
		boolean recursive,
		long now,
		Set<PageRef> seenPages
	) throws TaskException, ServletException, IOException {
		for(Element element : page.getElements()) {
			if(element instanceof Task) {
				Task task = (Task)element;
				TaskAssignment assignedTo = user == null ? null : task.getAssignedTo(user);
				if(
					user == null
					|| assignedTo != null
				) {
					Task.StatusResult status = task.getStatus();
					Priority priority;
					if(status.getDate() != null) {
						priority = task.getPriority(status.getDate(), now);
					} else {
						priority = task.getZeroDayPriority();
					}
					// getReadyTasks logic
					if(
						priority != Priority.FUTURE
						&& !status.isCompletedSchedule()
						&& status.isReadySchedule()
					) {
						if(
							status.getDate() != null
							&& assignedTo != null
							&& assignedTo.getAfter().getCount() > 0
						) {
							// assignedTo "after"
							Calendar effectiveDate = UnmodifiableCalendar.unwrapClone(status.getDate());
							assignedTo.getAfter().offset(effectiveDate);
							if(now >= effectiveDate.getTimeInMillis()) {
								return true;
							}
						} else {
							// No time offset
							return true;
						}
					}
					// getBlockedTasks logic
					if(
						priority != Priority.FUTURE
						&& !status.isCompletedSchedule()
						&& !status.isReadySchedule()
						&& !status.isFutureSchedule()
					) {
						if(
							status.getDate() != null
							&& assignedTo != null
							&& assignedTo.getAfter().getCount() > 0
						) {
							// assignedTo "after"
							Calendar effectiveDate = UnmodifiableCalendar.unwrapClone(status.getDate());
							assignedTo.getAfter().offset(effectiveDate);
							if(now >= effectiveDate.getTimeInMillis()) {
								return true;
							}
						} else {
							// No time offset
							return true;
						}
					}
					// getFutureTasks logic
					if(
						priority == Priority.FUTURE
						|| status.isFutureSchedule()
					) {
						// When assignedTo "after" is non-zero, hide from this user
						if(
							assignedTo == null
							|| assignedTo.getAfter().getCount() == 0
						) {
							return true;
						}
					}
				}
			}
		}
		if(recursive) {
			seenPages.add(page.getPageRef());
			for(PageRef childRef : page.getChildPages()) {
				if(
					// Child not in missing book
					childRef.getBook() != null
					// Not already seen
					&& !seenPages.contains(childRef)
				) {
					if(
						hasAssignedTaskRecursive(
							servletContext,
							request,
							response,
							CapturePage.capturePage(servletContext, request, response, childRef, CaptureLevel.META),
							user,
							recursive,
							now,
							seenPages
						)
					) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static List<Task> getReadyTasks(Page rootPage, User user) throws TaskException, IOException, ServletException {
		final long now = System.currentTimeMillis();
		List<Task> readyTasks = new ArrayList<Task>();
		for(
			Page page
			: PageDags.convertPageDagToList(
				getServletContext(),
				getRequest(),
				getResponse(),
				rootPage,
				CaptureLevel.META
			)
		) {
			for(Element element : page.getElements()) {
				if(element instanceof Task) {
					Task task = (Task)element;
					TaskAssignment assignedTo = user == null ? null : task.getAssignedTo(user);
					if(
						user == null
						|| assignedTo != null
					) {
						Task.StatusResult status = task.getStatus();
						Priority priority;
						if(status.getDate() != null) {
							priority = task.getPriority(status.getDate(), now);
						} else {
							priority = task.getZeroDayPriority();
						}
						if(
							priority != Priority.FUTURE
							&& !status.isCompletedSchedule()
							&& status.isReadySchedule()
						) {
							if(
								status.getDate() != null
								&& assignedTo != null
								&& assignedTo.getAfter().getCount() > 0
							) {
								// assignedTo "after"
								Calendar effectiveDate = UnmodifiableCalendar.unwrapClone(status.getDate());
								assignedTo.getAfter().offset(effectiveDate);
								if(now >= effectiveDate.getTimeInMillis()) {
									readyTasks.add(task);
								}
							} else {
								// No time offset
								readyTasks.add(task);
							}
						}
					}
				}
			}
		}
		return Collections.unmodifiableList(readyTasks);
	}

	public static List<Task> getBlockedTasks(Page rootPage, User user) throws TaskException, IOException, ServletException {
		final long now = System.currentTimeMillis();
		List<Task> blockedTasks = new ArrayList<Task>();
		for(
			Page page
			: PageDags.convertPageDagToList(
				getServletContext(),
				getRequest(),
				getResponse(),
				rootPage,
				CaptureLevel.META
			)
		) {
			for(Element element : page.getElements()) {
				if(element instanceof Task) {
					Task task = (Task)element;
					TaskAssignment assignedTo = user == null ? null : task.getAssignedTo(user);
					if(
						user == null
						|| assignedTo != null
					) {
						Task.StatusResult status = task.getStatus();
						Priority priority;
						if(status.getDate() != null) {
							priority = task.getPriority(status.getDate(), now);
						} else {
							priority = task.getZeroDayPriority();
						}
						if(
							priority != Priority.FUTURE
							&& !status.isCompletedSchedule()
							&& !status.isReadySchedule()
							&& !status.isFutureSchedule()
						) {
							if(
								status.getDate() != null
								&& assignedTo != null
								&& assignedTo.getAfter().getCount() > 0
							) {
								// assignedTo "after"
								Calendar effectiveDate = UnmodifiableCalendar.unwrapClone(status.getDate());
								assignedTo.getAfter().offset(effectiveDate);
								if(now >= effectiveDate.getTimeInMillis()) {
									blockedTasks.add(task);
								}
							} else {
								// No time offset
								blockedTasks.add(task);
							}
						}
					}
				}
			}
		}
		return Collections.unmodifiableList(blockedTasks);
	}

	public static List<Task> getFutureTasks(Page rootPage, User user) throws TaskException, IOException, ServletException {
		final long now = System.currentTimeMillis();
		List<Task> futureTasks = new ArrayList<Task>();
		for(
			Page page
			: PageDags.convertPageDagToList(
				getServletContext(),
				getRequest(),
				getResponse(),
				rootPage,
				CaptureLevel.META
			)
		) {
			for(Element element : page.getElements()) {
				if(element instanceof Task) {
					Task task = (Task)element;
					TaskAssignment assignedTo = user == null ? null : task.getAssignedTo(user);
					if(
						user == null
						|| assignedTo != null
					) {
						Task.StatusResult status = task.getStatus();
						Priority priority;
						if(status.getDate() != null) {
							priority = task.getPriority(status.getDate(), now);
						} else {
							priority = task.getZeroDayPriority();
						}
						if(
							priority == Priority.FUTURE
							|| status.isFutureSchedule()
						) {
							// When assignedTo "after" is non-zero, hide from this user
							if(
								assignedTo == null
								|| assignedTo.getAfter().getCount() == 0
							) {
								futureTasks.add(task);
							}
						}
					}
				}
			}
		}
		return Collections.unmodifiableList(futureTasks);
	}

	/**
	 * Make no instances.
	 */
	private Functions() {
	}
}