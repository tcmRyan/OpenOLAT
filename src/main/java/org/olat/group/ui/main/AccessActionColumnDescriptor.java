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
 * 12.10.2011 by frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.group.ui.main;

import static org.olat.group.ui.main.AbstractBusinessGroupListController.*;

import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.translator.Translator;
import org.olat.group.model.BGMembership;

/**
 * 
 * Description:<br>
 * 
 * <P>
 * Initial Date:  11 oct. 2011 <br>
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class AccessActionColumnDescriptor extends DefaultColumnDescriptor {
	
	private final Translator translator;
	
	public AccessActionColumnDescriptor(final String headerKey, final int dataColumn, final Translator translator) {
		super(headerKey, dataColumn, null, translator.getLocale());
		this.translator = translator;
	}

	@Override
	public String getAction(int row) {
		int sortedRow = table.getSortedRow(row);
		
		Object memberObj = table.getTableDataModel().getValueAt(sortedRow, BusinessGroupTableModelWithType.Cols.role.ordinal());
		if(memberObj instanceof BGMembership) {
			//owner, participant, or in waiting list can leave
			return TABLE_ACTION_LEAVE;
		}
		
		Object wrapper = table.getTableDataModel().getValueAt(sortedRow, BusinessGroupTableModelWithType.Cols.wrapper.ordinal());
		if(wrapper instanceof BGTableItem) {
			BGTableItem item = (BGTableItem)wrapper;
			if(item.isFull()) {
				return null;
			}
			return TABLE_ACTION_ACCESS;
		}
		return null;
	}

	@Override
	public void renderValue(StringOutput sb, int row, Renderer renderer) {
		int sortedRow = table.getSortedRow(row);
		Object memberObj = table.getTableDataModel().getValueAt(sortedRow, BusinessGroupTableModelWithType.Cols.role.ordinal());
		if(memberObj instanceof BGMembership) {
			switch((BGMembership)memberObj) {
				case owner:
				case participant:
					sb.append(translator.translate("table.header.leave")); break;
				case waiting:
					sb.append(translator.translate("table.header.leave.waiting")); break;
			}
		} else {
			Object wrapper = table.getTableDataModel().getValueAt(sortedRow, BusinessGroupTableModelWithType.Cols.wrapper.ordinal());
			if(wrapper instanceof BGTableItem) {
				BGTableItem item = (BGTableItem)wrapper;
				if(item.isFull()) {
					sb.append(translator.translate("table.header.group.full"));
				} else if(item.isWaitingListEnabled()) {
					sb.append(translator.translate("table.access.waitingList"));
				} else {
					sb.append(translator.translate("table.access"));
				}
			}
		}
	}
}
