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
package org.olat.course.nodes.en;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiTableDataModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiColumnDef;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModel;

/**
 * 
 * Initial date: 23 Dec 2019<br>
 * 
 * @author aboeckle, alexander.boeckle@frentix.com, http://www.frentix.com
 *
 */
public class ENEditGroupTableModel extends DefaultFlexiTableDataModel<ENEditGroupTableContentRow>
		implements FlexiTableDataModel<ENEditGroupTableContentRow> {

	private final Locale locale;

	public ENEditGroupTableModel(FlexiTableColumnModel columnModel, Locale locale) {
		super(columnModel);
		this.locale = locale;
	}

	@Override
	public Object getValueAt(int row, int col) {
		ENEditGroupTableContentRow group = getObject(row);
		switch (ENEditGroupTableColumns.values()[col]) {
		case up:
			return row == 0 ? Boolean.FALSE : Boolean.TRUE;
		case down:
			return row >= (getRowCount() - 1) ? Boolean.FALSE : Boolean.TRUE;
		default:
		}
		return getValueAt(group, col);
	}

	public Object getValueAt(ENEditGroupTableContentRow row, int col) {
		switch (ENEditGroupTableColumns.values()[col]) {
		case key:
			return row.getKey();
		case groupName:
			return row.getGroupName();

		default:
			return "ERROR";
		}
	}

	@Override
	public DefaultFlexiTableDataModel<ENEditGroupTableContentRow> createCopyWithEmptyList() {
		return new ENEditGroupTableModel(getTableColumnModel(), locale);
	}

	public List<String> getNames() {
		List<String> names = new ArrayList<String>();

		for (ENEditGroupTableContentRow row : getObjects()) {
			names.add(row.getGroupName());
		}

		return names;
	}

	public List<Long> getKeys() {
		List<Long> keys = new ArrayList<Long>();

		for (ENEditGroupTableContentRow row : getObjects()) {
			keys.add(row.getKey());
		}

		return keys;
	}

	public enum ENEditGroupTableColumns implements FlexiColumnDef {
		key("engroupedit.table.key"), groupName("engroupedit.table.groupName"), up("engroupedit.table.up"),
		down("engroupedit.table.down"), remove("engroupedit.table.remove");

		private final String i18nHeaderKey;

		private ENEditGroupTableColumns(String i18nHeaderKey) {
			this.i18nHeaderKey = i18nHeaderKey;
		}

		@Override
		public String i18nHeaderKey() {
			return i18nHeaderKey;
		}
	}
}