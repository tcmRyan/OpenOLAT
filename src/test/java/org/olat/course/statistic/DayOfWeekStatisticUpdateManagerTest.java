/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.course.statistic;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.olat.core.gui.util.SyntheticUserRequest;
import org.olat.core.id.Identity;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.nodes.CourseNode;
import org.olat.course.statistic.dayofweek.DayOfWeekStatisticManager;
import org.olat.repository.RepositoryEntry;
import org.olat.test.JunitTestHelper;


/**
 * Test the daily statistics with 2 courses.
 * 
 * 
 * Initial date: 5 mai 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class DayOfWeekStatisticUpdateManagerTest extends AbstractStatisticUpdateManagerTest {
	
	private final DayOfWeekStatisticManager dayOfWeekStatisticManager = new DayOfWeekStatisticManager();

	@Test
	public void statistics_dayOfWeek() {
		Assume.assumeTrue(!isOracleConfigured());
		
		Assert.assertNotNull(statisticUpdateManager);
		statisticUpdateManager.setEnabled(true);
		Assert.assertTrue(statisticUpdateManager.isEnabled());
		
		Identity id = JunitTestHelper.createAndPersistIdentityAsRndUser("log-3");
		RepositoryEntry re1 = JunitTestHelper.deployBasicCourse(id);
		ICourse course1 = CourseFactory.loadCourse(re1);
		CourseNode rootNode1 = course1.getRunStructure().getRootNode();
		CourseNode firstNode1 = (CourseNode)course1.getRunStructure().getRootNode().getChildAt(0);
		
		RepositoryEntry re2 = JunitTestHelper.deployBasicCourse(id);
		ICourse course2 = CourseFactory.loadCourse(re2);
		CourseNode rootNode2 = course2.getRunStructure().getRootNode();
		CourseNode firstNode2 = (CourseNode)course2.getRunStructure().getRootNode().getChildAt(0);
		
		Calendar ref = Calendar.getInstance();
		String date1 = null;
		String date2 = null;
		
		cleanUpLog();
		for(int i=0; i<7; i++) {
			addLogEntry(re1, rootNode1, ref, 0, 0, 0, i + 1);
			addLogEntry(re2, firstNode2, ref, 0, 0, 0, i + 1);
		}
		for(int i=0; i<15; i++) {
			addLogEntry(re1, rootNode1, ref, 3, 2, 1, 1);
			addLogEntry(re2, rootNode2, ref, 4, 2, 1, 1);
		}
		for(int i=0; i<3; i++) {
			date1 = addLogEntry(re1, firstNode1, ref, 1, 3, 1, 1);
			addLogEntry(re1, rootNode1, ref, 7, 3, 1, 1);
		}
		for(int i=0; i<6; i++) {
			addLogEntry(re1, rootNode1, ref, 3 + i, 5 + i, 1, 1);
			addLogEntry(re1, rootNode1, ref, 3, 5 + i, 1, 1);
			date2 = addLogEntry(re2, rootNode2, ref, 4 + (i * 2), 5, 3, 1);
		}
		
		setLastUpdate(ref, 20);
		dbInstance.commitAndCloseSession();

		updateStatistics();
		
		// first log analyze
		String date = getDayOfWeekString(ref);
		checkStatistics(course1, rootNode1, date);
		checkStatistics(course1, firstNode1, date1);
		checkStatistics(course2, rootNode2, date2);

		// add log the same day
		Calendar now = Calendar.getInstance();
		addLogEntry(re1, rootNode1, ref, 0, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND) + 1);
		addLogEntry(re1, rootNode1, ref, 0, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND) + 2);
		addLogEntry(re2, rootNode2, ref, 0, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND) + 2);
		dbInstance.commitAndCloseSession();
		sleep(5000);

		//update stats incremental
		updateStatistics();
		checkStatistics(course1, rootNode1, date);
		checkStatistics(course1, firstNode1, date1);
		checkStatistics(course2, rootNode2, date2);
		
		//update all stats
		updateAllStatistics();
		checkStatistics(course1, rootNode1, date);
		checkStatistics(course1, firstNode1, date1);
		checkStatistics(course2, rootNode2, date2);
	}
	
	private String getDayOfWeekString(Calendar start) {
		int day = start.get(Calendar.DAY_OF_WEEK);
		return Integer.toString(day);
	}
	
	private void checkStatistics(ICourse course, CourseNode node, String date) {
		RepositoryEntry re = course.getCourseEnvironment().getCourseGroupManager().getCourseEntry();
		StatisticResult updatedResult = dayOfWeekStatisticManager.generateStatisticResult(new SyntheticUserRequest(null, Locale.ENGLISH), course, re.getKey());
		Map<String,Integer> updatedRootStats = updatedResult.getStatistics(node);
		Integer updated_stats_inMemory = getInMemoryStatistics(re, node, date);
		Integer updated_stats_today = updatedRootStats.get(date);
		Assert.assertEquals(updated_stats_inMemory, updated_stats_today);
	}
	
	protected String addLogEntry(RepositoryEntry repositoryEntry, CourseNode courseNode, Calendar start,
			int dayInPast, int hour, int minute, int second) {
		Calendar cal = addLog(repositoryEntry.getKey(), courseNode.getIdent(), start, dayInPast, hour, minute, second);
		String week = getDayOfWeekString(cal);
		incrementInMemoryStatistics(repositoryEntry.getKey(), courseNode.getIdent(), week);
		return week;
	}
}