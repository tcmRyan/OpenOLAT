/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/
package org.olat.course.nodes.projectbroker.service;

import java.util.List;

import org.olat.basesecurity.SecurityGroup;
import org.olat.core.gui.UserRequest;
import org.olat.core.id.Identity;
import org.olat.course.ICourse;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupAddResponse;
import org.olat.resource.OLATResource;

public interface ProjectGroupManager {

	//////////////////////
	// ACCOUNT MANAGEMENT
	//////////////////////
	
	/**
	 * Return business-group for account-managers.
	 * Group-ID will be stored in CoursePropertyManager
	 * @param cpm
	 * @param courseNode
	 * @param course
	 * @param groupName
	 * @param groupDescription
	 * @param identity
	 */
	public abstract BusinessGroup getAccountManagerGroupFor(CoursePropertyManager cpm, CourseNode courseNode, ICourse course, String groupName, String groupDescription, Identity identity);

	/**
	 * Return true when identity is account-manager (could create projects).
	 * @param identity
	 * @param cpm
	 * @param courseNode
	 * @return
	 */
	public abstract boolean isAccountManager(Identity identity, CoursePropertyManager cpm, CourseNode courseNode);

	/**
	 * Delete an account-manager group and the key in the CoursePropertyManager. 
	 * Get account-manager-group form CoursePropertyManager
	 * @param cpm
	 * @param courseNode
	 */
	public void deleteAccountManagerGroup(CoursePropertyManager cpm, CourseNode courseNode);

	/**
	 * Update group-name or description for certain account manager group.
	 * @param groupName
	 * @param groupDescription
	 * @param accountManagerGroup
	 */
	public BusinessGroup updateAccountManagerGroupName(String groupName, String groupDescription, BusinessGroup accountManagerGroup);

	////////////////////////////
	// PROJECT GROUP MANAGEMENT
	////////////////////////////
	
	/**
	 * Create a business-group for a project. Check that group-name does not already exists.  
	 * When a group-name already exists, add a counter-value for uniqueness.
	 * @param projectBrokerId
	 * @param identity
	 * @param groupName
	 * @param groupDescription
	 * @param courseId
	 * @return
	 */
	public abstract BusinessGroup createProjectGroupFor(Long projectBrokerId, Identity identity, String groupName, String groupDescription, Long courseId);

	/**
	 * Delete project-group for certain project.
	 * @param project
	 */
	public abstract void deleteProjectGroupFor(Project project);


	/**
	 * Change project-group name and description for certain business-group of a project.
	 * @param projectGroup      Change name and description of this group
	 * @param initialGroupName  New group-name, if the group name already exists, append a number e.g. ' _2' for uniqueness 
	 * @param groupDescription  New description of group
	 */
	public BusinessGroup changeProjectGroupName(BusinessGroup projectGroup, String initialGroupName, String groupDescription, OLATResource courseResource);

	/**
	 * Accept candidates as participants, move identity from candidate-group to participant-group 
	 * @param identities
	 * @param project
	 * @param actionIdentity   Identity who did the action
	 * @param autoSignOut
	 * @param isAcceptSelectionManually
	 * @return
	 */
	public BusinessGroupAddResponse acceptCandidates(List<Identity> identities, Project project, Identity actionIdentity, boolean autoSignOut, boolean isAcceptSelectionManually);

	/**
	 * Add identities as candidates.
	 * @param addIdentities
	 * @param project
	 * @return
	 */
	public abstract List<Identity> addCandidates(List<Identity> addIdentities, Project project);

	/**
	 * Remove identities as candidates.
	 * @param addIdentities
	 * @param project
	 */
	public void removeCandidates(List<Identity> addIdentities, Project project);

	/**
	 * Send a multi-user-event to inform about added identities.
	 * @param project
	 * @param courseResourceableId
	 * @param identity
	 */
	public abstract void sendGroupChangeEvent(Project project, Long courseResourceableId, Identity identity);

	/**
	 * Accept all candidates for a project broker as participants. 
	 * @param projectBrokerId
	 * @param actionIdentity   Identity who did the action
	 * @param autoSignOut
	 * @param isAcceptSelectionManually
	 */
	public abstract void acceptAllCandidates(Long projectBrokerId, Identity actionIdentity, boolean autoSignOut, boolean isAcceptSelectionManually);

  /**
   * Returns true when a certain project-broker has any candidates in any of his projects
   * @param projectBrokerId
   * @return
   */
	public abstract boolean hasProjectBrokerAnyCandidates(Long projectBrokerId);
	
	/**
	 * Check if a certain identity has the role of project-manager for certain project.   
	 * Project-managers can edit, delete there projects. They can manage participants. 
	 * They can lock into the drop-boxes and give feedback via return-boxes.
	 * @param identity
	 * @param project
	 * @return
	 */
	public boolean isProjectManager(Identity identity, Project project);

	public boolean isProjectManagerOrAdministrator(UserRequest ureq, CourseEnvironment courseEnv, Project project);	

	/**
	 * Check if a certain identity is a participant of a project.
	 * @param identity
	 * @param project
	 * @return
	 */
	public boolean isProjectParticipant(Identity identity, Project project);
	
	/**
	 * Check if a certain identity is a candidate for a project.
	 * @param identity
	 * @param project
	 * @return
	 */
	public boolean isProjectCandidate(Identity identity, Project project);

	/**
	 * Set max-participants of a project-group with max-members value.
	 * @param projectGroup
	 * @param maxMembers
	 */
  public BusinessGroup setProjectGroupMaxMembers(BusinessGroup projectGroup, int maxMembers );
  
  /**
   * Returns true when candidate-list is empty.
   * @param candidateGroup
   * @return
   */
  public boolean isCandidateListEmpty(SecurityGroup candidateGroup);
}