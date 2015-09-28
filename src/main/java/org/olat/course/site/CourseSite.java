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
package org.olat.course.site;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.gui.control.navigation.AbstractSiteInstance;
import org.olat.core.gui.control.navigation.DefaultNavElement;
import org.olat.core.gui.control.navigation.NavElement;
import org.olat.core.gui.control.navigation.SiteConfiguration;
import org.olat.core.gui.control.navigation.SiteDefinition;
import org.olat.core.gui.control.navigation.SiteSecurityCallback;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.StateSite;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentMode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.CourseRuntimeController;
import org.olat.course.run.RunMainController;
import org.olat.course.run.navigation.NavigationHandler;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.TreeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironmentImpl;
import org.olat.course.run.userview.VisibleTreeFilter;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.repository.model.RepositoryEntrySecurity;
import org.olat.repository.ui.RepositoryEntryRuntimeController.RuntimeControllerCreator;

/**
 * 
 * Description:<br>
 * based on Intranet-Site (goodsolutions) 
 * more config-options (see NetworkSiteDef / olat_extensions.xml) 
 * 
 * TODO:RH: use repositoryuifactory instead of manually do things like, incrementing, building businesspath, etc...
 * 
 * <P>
 * Initial Date: 19.07.2005 <br>
 * 
 * @author Felix Jost
 * @author Roman Haag, roman.haag@frentix.com, www.frentix.com
 */
public class CourseSite extends AbstractSiteInstance {
	private NavElement origNavElem;
	private NavElement curNavElem;

	private final String repositorySoftKey;
	private boolean showToolController;
	private SiteSecurityCallback siteSecCallback;

	/**
	 * @param loc
	 * @param alternativeControllerIfNotLaunchable
	 * @param titleKeyPrefix
	 */
	public CourseSite(SiteDefinition siteDef, String repositorySoftKey, boolean showToolController,
			SiteSecurityCallback siteSecCallback, String titleKeyPrefix, String navIconCssClass) {
		super(siteDef);
		this.repositorySoftKey = repositorySoftKey;
		origNavElem = new DefaultNavElement(titleKeyPrefix, titleKeyPrefix, navIconCssClass);
		curNavElem = new DefaultNavElement(origNavElem);
		this.showToolController = showToolController;
		this.siteSecCallback = siteSecCallback;
	}

	/**
	 * @see org.olat.navigation.SiteInstance#getNavElement()
	 */
	@Override
	public NavElement getNavElement() {
		return curNavElem;
	}

	@Override
	protected MainLayoutController createController(UserRequest ureq, WindowControl wControl, SiteConfiguration config) {
		RepositoryManager rm = RepositoryManager.getInstance();
		RepositoryService rs = CoreSpringFactory.getImpl(RepositoryService.class);
		RepositoryEntry entry = rm.lookupRepositoryEntryBySoftkey(repositorySoftKey, false);
		if(entry == null) {
			return getAlternativeController(ureq, wControl, config);
		}

		MainLayoutController c;
		ICourse course = CourseFactory.loadCourse(entry);

		// course-launch-state depending course-settings
		RepositoryEntrySecurity reSecurity = rm.isAllowed(ureq, entry);
		boolean isAllowedToLaunch = reSecurity.canLaunch();
		boolean hasAccess = false;
		if (isAllowedToLaunch) {
			// either check with securityCallback or use access-settings from course-nodes
			if (siteSecCallback != null) {
				hasAccess = siteSecCallback.isAllowedToLaunchSite(ureq);
			} else {
				// check within course: accessibility of course root node
				CourseNode rootNode = course.getRunStructure().getRootNode();
				UserCourseEnvironmentImpl uce = new UserCourseEnvironmentImpl(ureq.getUserSession().getIdentityEnvironment(), course
						.getCourseEnvironment());
				NodeEvaluation nodeEval = rootNode.eval(uce.getConditionInterpreter(), new TreeEvaluation(), new VisibleTreeFilter());
				boolean mayAccessWholeTreeUp = NavigationHandler.mayAccessWholeTreeUp(nodeEval);
				hasAccess = mayAccessWholeTreeUp && nodeEval.isVisible();
			}
		}
		
		
		// load course (admins always see content) or alternative controller if course is not launchable
		if (hasAccess || ureq.getUserSession().getRoles().isOLATAdmin()) {
			rs.incrementLaunchCounter(entry); 
			// build up the context path for linked course
			WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ureq, entry, new StateSite(this), wControl, true);	
			CourseRuntimeController runCtr = new CourseRuntimeController(ureq, bwControl, entry, reSecurity,
				new RuntimeControllerCreator() {
					@Override
					public Controller create(UserRequest uureq, WindowControl wwControl,
							TooledStackedPanel toolbarPanel, RepositoryEntry re, RepositoryEntrySecurity security, AssessmentMode assessmentMode) {
						return new RunMainController(uureq, wwControl, toolbarPanel,
								CourseFactory.loadCourse(re), re, security, assessmentMode);
					}
				}, false, true);
			
			// Configure run controller
			// a: don't show close link, is opened as site not tab
			runCtr.setCourseCloseEnabled(false);
			// b: don't show toolbar
			if (!showToolController) {
				runCtr.setToolControllerEnabled(false);
			}
			c = runCtr;
		} else {
			// access restricted (not in group / author) -> show controller
			// defined in olat_extensions (type autoCreator)
			c = getAlternativeController(ureq, wControl, config);
		}
		return c;
	}

	/**
	 * @see org.olat.navigation.SiteInstance#isKeepState()
	 */
	@Override
	public boolean isKeepState() {
		return true;
	}

	/**
	 * @see org.olat.navigation.SiteInstance#reset()
	 */
	@Override
	public void reset() {
		curNavElem = new DefaultNavElement(origNavElem);
	}
}
