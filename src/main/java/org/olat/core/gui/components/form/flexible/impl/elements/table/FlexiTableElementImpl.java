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
* <p>
*/ 
package org.olat.core.gui.components.form.flexible.impl.elements.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.olat.core.commons.persistence.SortKey;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentEventListener;
import org.olat.core.gui.components.choice.Choice;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemCollection;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableFilter;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableSort;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableSortOptions;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableStateEntry;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormItemImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.AbstractTextElement;
import org.olat.core.gui.components.form.flexible.impl.elements.AutoCompleteEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.AutoCompleterImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.FormLinkImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.TextElementImpl;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.ajax.autocompletion.ListProvider;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.StringHelper;
import org.olat.core.util.UserSession;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.core.util.prefs.Preferences;


/**
 * 
 * @author Christian Guretzki
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class FlexiTableElementImpl extends FormItemImpl implements FlexiTableElement, FormItemCollection,
	ControllerEventListener, ComponentEventListener {
	
	//settings
	private boolean multiSelect;
	private FlexiTableRendererType rendererType = FlexiTableRendererType.classic;
	private FlexiTableRendererType[] availableRendererType = new FlexiTableRendererType[] {
		FlexiTableRendererType.classic
	};
	
	private String persistentId;
	private boolean customizeColumns = true;
	
	private int rowCount = -1;
	
	private int currentPage;
	private int pageSize;
	private final int defaultPageSize;
	private boolean editMode;
	private boolean exportEnabled;
	private boolean searchEnabled;
	private boolean selectAllEnabled;
	private boolean numOfRowsEnabled = true;
	private boolean showAllRowsEnabled = false;
	private boolean extendedSearchExpanded = false;
	private int columnLabelForDragAndDrop;
	private String emptyTableMessageKey = null;
	
	private VelocityContainer rowRenderer;
	private VelocityContainer detailsRenderer;

	private FormLink customButton, exportButton;
	private FormLink searchButton, extendedSearchButton;
	private FormLink classicTypeButton, customTypeButton;
	private FormLink extendedFilterButton;
	private AbstractTextElement searchFieldEl;
	private ExtendedFilterController extendedFilterCtrl;
	private ExtendedFlexiTableSearchController extendedSearchCtrl;
	
	private final FlexiTableDataModel<?> dataModel;
	private final FlexiTableDataSource<?> dataSource;
	private final FlexiTableComponent component;
	private FlexiTableComponentDelegate componentDelegate;
	private CloseableCalloutWindowController callout;
	private final WindowControl wControl;
	
	private String wrapperSelector;
	private FlexiTableCssDelegate cssDelegate;

	private SortKey[] orderBy;
	private FlexiTableSortOptions sortOptions;
	private List<FlexiTableFilter> filters;
	private List<FlexiTableFilter> extendedFilters;
	private Object selectedObj;
	private boolean allSelectedNeedLoadOfWholeModel = false;
	private Set<Integer> multiSelectedIndex;
	private Set<Integer> detailsIndex;
	private List<String> conditionalQueries;
	private Set<Integer> enabledColumnIndex = new HashSet<Integer>();
	
	private Map<String,FormItem> components = new HashMap<String,FormItem>();
	
	public FlexiTableElementImpl(WindowControl wControl, String name, Translator translator, FlexiTableDataModel<?> tableModel) {
		this(wControl, name, translator, tableModel, -1, true);
	}
	
	public FlexiTableElementImpl(WindowControl wControl, String name, Translator translator,
			FlexiTableDataModel<?> tableModel, int pageSize, boolean loadOnStart) {
		super(name);
		this.wControl = wControl;
		this.dataModel = tableModel;
		this.dataSource = (tableModel instanceof FlexiTableDataSource) ? (FlexiTableDataSource<?>)dataModel : null;
		translator = Util.createPackageTranslator(FlexiTableElementImpl.class, translator.getLocale(), translator);
		component = new FlexiTableComponent(this, translator);
		
		for(int i=dataModel.getTableColumnModel().getColumnCount(); i-->0; ) {
			FlexiColumnModel col = dataModel.getTableColumnModel().getColumnModel(i);
			if(col.isDefaultVisible()) {
				enabledColumnIndex.add(new Integer(col.getColumnIndex()));
			}
		}

		String dispatchId = component.getDispatchID();
		customButton = new FormLinkImpl(dispatchId + "_customButton", "rCustomButton", "", Link.BUTTON + Link.NONTRANSLATED);
		customButton.setTranslator(translator);
		customButton.setIconLeftCSS("o_icon o_icon_customize");
		components.put("rCustomize", customButton);
		
		this.pageSize = pageSize;
		this.defaultPageSize = pageSize;
		if(pageSize > 0) {
			setPage(0);
		}
		
		if(dataSource != null && loadOnStart) {
			//preload it
			dataSource.load(null, null, null, 0, pageSize);
		}
	}

	@Override
	public int getColumnIndexForDragAndDropLabel() {
		return columnLabelForDragAndDrop;
	}

	@Override
	public void setColumnIndexForDragAndDropLabel(int columnLabelForDragAndDrop) {
		this.columnLabelForDragAndDrop = columnLabelForDragAndDrop;
	}

	@Override
	public FlexiTableRendererType getRendererType() {
		return rendererType;
	}

	@Override
	public void setRendererType(FlexiTableRendererType rendererType) {
		// activate active render button
		if(customTypeButton != null) {
			customTypeButton.setActive(FlexiTableRendererType.custom == rendererType);
		}
		if(classicTypeButton != null) {
			classicTypeButton.setActive(FlexiTableRendererType.classic == rendererType);
		}
		// update render type
		this.rendererType = rendererType;
		if(component != null) {
			component.setDirty(true);
		}
	}
	
	public FlexiTableRendererType[] getAvailableRendererTypes() {
		return availableRendererType;
	}

	@Override
	public void setAvailableRendererTypes(FlexiTableRendererType... rendererTypes) {
		this.availableRendererType = rendererTypes;
		if(rendererTypes != null && rendererTypes.length > 1) {
			String dispatchId = component.getDispatchID();
			//custom
			customTypeButton = new FormLinkImpl(dispatchId + "_customRTypeButton", "rCustomRTypeButton", "", Link.BUTTON + Link.NONTRANSLATED);
			customTypeButton.setTranslator(translator);
			customTypeButton.setIconLeftCSS("o_icon o_icon_list o_icon-lg");
			customTypeButton.setActive(FlexiTableRendererType.custom == rendererType);
			components.put("rTypeCustom", customTypeButton);
			//classic tables
			classicTypeButton = new FormLinkImpl(dispatchId + "_classicRTypeButton", "rClassicRTypeButton", "", Link.BUTTON + Link.NONTRANSLATED);
			classicTypeButton.setTranslator(translator);
			classicTypeButton.setIconLeftCSS("o_icon o_icon_table o_icon-lg");
			classicTypeButton.setActive(FlexiTableRendererType.classic == rendererType);
			components.put("rTypeClassic", classicTypeButton);
			
			if(getRootForm() != null) {
				rootFormAvailable(customTypeButton);
				rootFormAvailable(classicTypeButton);
			}
		}
	}

	public FormLink getClassicTypeButton() {
		return classicTypeButton;
	}

	public FormLink getCustomTypeButton() {
		return customTypeButton;
	}

	@Override
	public boolean isMultiSelect() {
		return multiSelect;
	}
	
	@Override
	public void setMultiSelect(boolean multiSelect) {
		this.multiSelect = multiSelect;
	}

	@Override
	public boolean isCustomizeColumns() {
		return customizeColumns;
	}

	@Override
	public void setCustomizeColumns(boolean customizeColumns) {
		this.customizeColumns = customizeColumns;
	}

	public boolean isNumOfRowsEnabled() {
		return numOfRowsEnabled;
	}

	@Override
	public void setNumOfRowsEnabled(boolean enable) {
		numOfRowsEnabled = enable;
	}

	@Override
	public boolean isShowAllRowsEnabled() {
		return showAllRowsEnabled;
	}

	@Override
	public void setShowAllRowsEnabled(boolean showAllRowsEnabled) {
		this.showAllRowsEnabled = showAllRowsEnabled;
	}

	@Override
	public void setAndLoadPersistedPreferences(UserRequest ureq, String id) {
		persistentId = id;
		loadCustomSettings(ureq);
	}

	@Override
	public String getWrapperSelector() {
		return wrapperSelector;
	}

	@Override
	public void setWrapperSelector(String wrapperSelector) {
		this.wrapperSelector = wrapperSelector;
	}

	@Override
	public FlexiTableCssDelegate getCssDelegate() {
		return cssDelegate;
	}

	@Override
	public void setCssDelegate(FlexiTableCssDelegate cssDelegate) {
		this.cssDelegate = cssDelegate;
	}

	@Override
	public FlexiTableComponent getComponent() {
		return component;
	}

	public VelocityContainer getRowRenderer() {
		return rowRenderer;
	}
	
	public VelocityContainer getDetailsRenderer() {
		return detailsRenderer;
	}

	public FlexiTableComponentDelegate getComponentDelegate() {
		return componentDelegate;
	}

	@Override
	public void setRowRenderer(VelocityContainer rowRenderer, FlexiTableComponentDelegate componentDelegate) {
		this.rowRenderer = rowRenderer;
		this.componentDelegate = componentDelegate;
	}
	
	@Override
	public void setDetailsRenderer(VelocityContainer detailsRenderer, FlexiTableComponentDelegate componentDelegate) {
		this.detailsRenderer = detailsRenderer;
		this.componentDelegate = componentDelegate;
	}

	@Override
	public boolean isFilterEnabled() {
		return filters != null && filters.size() > 0;
	}

	@Override
	public List<FlexiTableFilter> getSelectedFilters() {
		List<FlexiTableFilter> selectedFilters = new ArrayList<>(2);
		if(filters != null) {
			for(FlexiTableFilter filter:filters) {
				if(filter.isSelected()) {
					selectedFilters.add(filter);
				}
			}
		}
		return selectedFilters;
	}

	@Override
	public String getSelectedFilterKey() {
		String key = null;
		if(filters != null) {
			for(FlexiTableFilter filter:filters) {
				if(filter.isSelected()) {
					key = filter.getFilter();
				}
			}
		}
		return key;
	}

	@Override
	public void setSelectedFilterKey(String key) {
		if(filters != null) {
			for(FlexiTableFilter filter:filters) {
				boolean selected = (key == null && filter.getFilter() == null)
						|| (key != null && key.equals(filter.getFilter()));
				filter.setSelected(selected);
			}
		}
	}

	@Override
	public String getSelectedFilterValue() {
		String value = null;
		if(filters != null) {
			for(FlexiTableFilter filter:filters) {
				if(filter.isSelected()) {
					value = filter.getLabel();
				}
			}
		}
		return value;
	}
	
	public List<FlexiTableFilter> getFilters() {
		return filters;
	}

	@Override
	public void setFilters(String name, List<FlexiTableFilter> filters) {
		this.filters = new ArrayList<>(filters);
	}
	
	public boolean isSortEnabled() {
		return sortOptions != null && (sortOptions.getSorts().size() > 0 || sortOptions.isFromColumnModel());
	}
	
	public FlexiTableSortOptions getSortOptions() {
		return sortOptions;
	}
	
	public List<FlexiTableSort> getSorts() {
		List<FlexiTableSort> sorts;
		if(sortOptions == null) {
			sorts = Collections.<FlexiTableSort>emptyList();
		} else if(sortOptions.getSorts() != null && sortOptions.getSorts().size() > 0) {
			sorts = sortOptions.getSorts();
		} else if(sortOptions.isFromColumnModel()) {
			FlexiTableColumnModel columnModel = getTableDataModel().getTableColumnModel();
			
			int cols = columnModel.getColumnCount();
			sorts = new ArrayList<>(cols);
			for(int i=0; i<cols; i++) {
				FlexiColumnModel fcm = columnModel.getColumnModel(i);
				if (fcm.isSortable() && fcm.getSortKey() != null) {
					String header;
					if(StringHelper.containsNonWhitespace(fcm.getHeaderLabel())) {
						header = fcm.getHeaderLabel();
					} else {
						header = translator.translate(fcm.getHeaderKey());
					}
					sorts.add(new FlexiTableSort(header, fcm.getSortKey()));
				}
			}
			sortOptions.setSorts(sorts);
		} else {
			sorts = Collections.<FlexiTableSort>emptyList();
		}
		return sorts; 
	}

	@Override
	public void setSortSettings(FlexiTableSortOptions options) {
		this.sortOptions = options;
		if(options.getDefaultOrderBy() != null) {
			SortKey defaultOrder = options.getDefaultOrderBy();
			orderBy = new SortKey[]{ defaultOrder };
			selectSortOption(defaultOrder.getKey(), defaultOrder.isAsc());
		}
	}

	@Override
	public boolean isExportEnabled() {
		return exportEnabled;
	}
	
	@Override
	public void setExportEnabled(boolean enabled) {
		this.exportEnabled = enabled;
		if(exportEnabled) {
			exportButton = null;
			
			String dispatchId = component.getDispatchID();
			exportButton = new FormLinkImpl(dispatchId + "_exportButton", "rExportButton", "", Link.BUTTON | Link.NONTRANSLATED);
			exportButton.setTranslator(translator);
			exportButton.setIconLeftCSS("o_icon o_icon_download");
			components.put("rExport", exportButton);
			rootFormAvailable(exportButton);
		} else {
			exportButton = null;
		}
	}
	
	public FormLink getExportButton() {
		return exportButton;
	}

	@Override
	public boolean isEditMode() {
		return editMode;
	}

	@Override
	public void setEditMode(boolean editMode) {
		if(this.editMode != editMode) {
			this.editMode = editMode;
			component.setDirty(true);
		}
	}

	@Override
	public boolean isSearchEnabled() {
		return searchEnabled;
	}

	@Override
	public void setSearchEnabled(boolean enable) {
		this.searchEnabled = enable;
		if(searchEnabled) {
			String dispatchId = component.getDispatchID();
			searchFieldEl = new TextElementImpl(dispatchId + "_searchField", "search", "");
			searchFieldEl.showLabel(false);
			components.put("rSearch", searchFieldEl);
			searchButton = new FormLinkImpl(dispatchId + "_searchButton", "rSearchButton", "search", Link.BUTTON);
			searchButton.setTranslator(translator);
			searchButton.setIconLeftCSS("o_icon o_icon_search");
			components.put("rSearchB", searchButton);
			rootFormAvailable(searchFieldEl);
			rootFormAvailable(searchButton);
		} else {
			components.remove("rSearch");
			components.remove("rSearchB");
			searchFieldEl = null;
			searchButton = null;
		}
	}
	
	@Override
	public void setSearchEnabled(ListProvider autoCompleteProvider, UserSession usess) {
		searchEnabled = true;

		String dispatchId = component.getDispatchID();
		searchFieldEl = new AutoCompleterImpl(dispatchId + "_searchField", "search");
		searchFieldEl.showLabel(false);
		searchFieldEl.getComponent().addListener(this);
		((AutoCompleterImpl)searchFieldEl).setListProvider(autoCompleteProvider, usess);
		components.put("rSearch", searchFieldEl);
		searchButton = new FormLinkImpl(dispatchId + "_searchButton", "rSearchButton", "search", Link.BUTTON);
		searchButton.setTranslator(translator);
		searchButton.setIconLeftCSS("o_icon o_icon_search");
		components.put("rSearchB", searchButton);
		rootFormAvailable(searchFieldEl);
		rootFormAvailable(searchButton);
	}

	public FormLink getExtendedSearchButton() {
		return extendedSearchButton;
	}
	
	@Override
	public boolean isExtendedSearchExpanded() {
		return extendedSearchExpanded;
	}

	public Component getExtendedSearchComponent() {
		return (extendedSearchCtrl == null) ? null : extendedSearchCtrl.getInitialComponent();
	}
	
	@Override
	public void setExtendedSearch(ExtendedFlexiTableSearchController controller) {
		extendedSearchCtrl = controller;
		if(extendedSearchCtrl != null) {
			extendedSearchCtrl.addControllerListener(this);
			
			String dispatchId = component.getDispatchID();
			extendedSearchButton = new FormLinkImpl(dispatchId + "_extSearchButton", "rExtSearchButton", "extsearch", Link.BUTTON);
			extendedSearchButton.setTranslator(translator);
			extendedSearchButton.setIconLeftCSS("o_icon o_icon_search");
			components.put("rExtSearchB", extendedSearchButton);
			rootFormAvailable(extendedSearchButton);
			extendedSearchButton.setElementCssClass("o_sel_flexi_extendedsearch");

			components.put("rExtSearchCmp", controller.getInitialFormItem());
		}
	}
	
	@Override
	public void setExtendedFilterButton(String label, List<FlexiTableFilter> extendedFilters) {
		if(StringHelper.containsNonWhitespace(label) && extendedFilters != null && extendedFilters.size() > 0) {
			this.extendedFilters = extendedFilters;
			
			String dispatchId = component.getDispatchID();
			extendedFilterButton = new FormLinkImpl(dispatchId + "_extFilterButton", "rExtFilterButton", "extfilter", Link.BUTTON | Link.NONTRANSLATED);
			extendedFilterButton.setI18nKey(label);
			extendedFilterButton.setTranslator(translator);
			extendedFilterButton.setIconLeftCSS("o_icon o_icon_filter");
			components.put("rExtFilterB", extendedFilterButton);
			rootFormAvailable(extendedFilterButton);
			extendedFilterButton.setElementCssClass("o_sel_flexi_extendedsearch");
		} else {
			extendedFilterButton = null;
			extendedFilters = null;
		}
	}
	
	@Override
	public List<FlexiTableFilter> getSelectedExtendedFilters() {
		List<FlexiTableFilter> selectedFilters = new ArrayList<>();
		if(extendedFilters != null && extendedFilters.size() > 0) {
			for(FlexiTableFilter extendedFilter:extendedFilters) {
				if(extendedFilter.isSelected()) {
					selectedFilters.add(extendedFilter);
				}
			}
		}
		return selectedFilters;
	}
	
	public FormLink getExtendedFilterButton() {
		return extendedFilterButton;
	}
	
	@Override
	public boolean isSelectAllEnable() {
		return selectAllEnabled;
	}
	
	@Override
	public void setSelectAllEnable(boolean enable) {
		this.selectAllEnabled = enable;
	}
	
	@Override
	public boolean isDetailsExpended(int row) {
		if(detailsIndex == null) {
			return false;
		}
		return detailsIndex.contains(row);
	}

	@Override
	public void expandDetails(int row) {
		if(detailsIndex == null) {
			detailsIndex = new HashSet<>();
		}
		detailsIndex.add(row);
		component.setDirty(true);
	}

	@Override
	public void collapseDetails(int row) {
		if(detailsIndex != null && detailsIndex.remove(row)) {
			component.setDirty(true);
			if(detailsIndex.isEmpty()) {
				detailsIndex = null;
			}
		}
	}

	@Override
	public void collapseAllDetails() {
		if(detailsIndex != null && detailsIndex.size() > 0) {
			detailsIndex = null;
			component.setDirty(true);
		}
	}

	@Override
	public FlexiTableStateEntry getStateEntry() {
		FlexiTableStateEntry entry = new FlexiTableStateEntry();
		if(searchFieldEl != null && searchFieldEl.isVisible()) {
			entry.setSearchString(searchFieldEl.getValue());
		}
		entry.setExpendedSearch(extendedSearchExpanded);
		return entry;
	}

	@Override
	public void setStateEntry(UserRequest ureq, FlexiTableStateEntry state) {
		if(state.isExpendedSearch()) {
			expandExtendedSearch(ureq);
		}
		if(StringHelper.containsNonWhitespace(state.getSearchString())) {
			quickSearch(ureq, state.getSearchString());
		}
	}

	public String getSearchText() {
		return searchFieldEl == null || !searchFieldEl.isVisible() || !searchFieldEl.isEnabled() ? null : searchFieldEl.getValue();
	}
	
	public List<String> getConditionalQueries() {
		return conditionalQueries;
	}
	
	public SortKey[] getOrderBy() {
		return orderBy;
	}

	public TextElement getSearchElement() {
		return searchFieldEl;
	}
	
	public FormItem getSearchButton() {
		return searchButton;
	}
	
	public FormItem getCustomButton() {
		return customButton;
	}

	public Object getSelectedObj() {
		return selectedObj;
	}

	public void setSelectedObj(Object selectedObj) {
		this.selectedObj = selectedObj;
	}

	@Override
	public int getDefaultPageSize() {
		return defaultPageSize;
	}

	@Override
	public int getPageSize() {
		return pageSize;
	}

	@Override
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	
	@Override
	public int getPage() {
		return currentPage;
	}
	
	@Override
	public void setPage(int page) {
		if(currentPage == page) return;
		if(page < 0) {
			page = 0;
		}
		currentPage = page;
		if(dataSource != null) {
			int firstResult = currentPage * getPageSize();
			int maxResults = getPageSize();
			dataSource.load(getSearchText(), getSelectedFilters(), getConditionalQueries(), firstResult, maxResults, orderBy);
		}
		component.setDirty(true);
	}

	@Override
	public Iterable<FormItem> getFormItems() {
		List<FormItem> items = new ArrayList<>(components.values());
		if(extendedSearchCtrl != null && !extendedSearchExpanded) {
			items.remove(extendedSearchCtrl.getInitialFormItem());
		}
		return items;
	}

	@Override
	public FormItem getFormComponent(String name) {
		return components.get(name);
	}

	public void addFormItem(FormItem item) {
		components.put(item.getName(), item);
	}

	@Override
	public void doDispatchFormRequest(UserRequest ureq) {
		super.doDispatchFormRequest(ureq);
	}
	
	@Override
	protected void dispatchFormRequest(UserRequest ureq) {
		super.dispatchFormRequest(ureq);
	}
	
	/**
	 * @see org.olat.core.gui.components.form.flexible.FormItemImpl#evalFormRequest(org.olat.core.gui.UserRequest)
	 */
	@Override
	public void evalFormRequest(UserRequest ureq) {
		String[] selectedIndexArr = getRootForm().getRequestParameterValues("tb_ms");
		if(selectedIndexArr != null) {
			setMultiSelectIndex(selectedIndexArr);
		}

		Form form = getRootForm();
		String selectedIndex = form.getRequestParameter("rSelect");
		String dispatchuri = form.getRequestParameter("dispatchuri");
		String select = form.getRequestParameter("select");
		String page = form.getRequestParameter("page");
		String sort = form.getRequestParameter("sort");
		String filter = form.getRequestParameter("filter");
		String pagesize = form.getRequestParameter("pagesize");
		String checkbox = form.getRequestParameter("chkbox");
		if("undefined".equals(dispatchuri)) {
			evalSearchRequest(ureq);
		} else if(StringHelper.containsNonWhitespace(checkbox)) {
			toogleSelectIndex(checkbox);
		} else if(StringHelper.containsNonWhitespace(page)) {
			int p = Integer.parseInt(page);
			setPage(p);
		 } else if(StringHelper.containsNonWhitespace(pagesize)) {
			int p;
			if("all".equals(pagesize)) {
				p = -1;
			} else {
				p = Integer.parseInt(pagesize);
			}
			selectPageSize(ureq, p);
		} else if(StringHelper.containsNonWhitespace(sort)) {
			String asc = form.getRequestParameter("asc");
			sort(sort, "asc".equals(asc));
			saveCustomSettings(ureq);
		} else if(StringHelper.containsNonWhitespace(selectedIndex)) {
			int index = selectedIndex.lastIndexOf('-');
			if(index > 0 && index+1 < selectedIndex.length()) {
				String pos = selectedIndex.substring(index+1);
				int selectedPosition = Integer.parseInt(pos);
				selectedObj = dataModel.getObject(selectedPosition);
				doSelect(ureq, selectedPosition);
			}
		} else if(searchButton != null
				&& searchButton.getFormDispatchId().equals(dispatchuri)) {
			evalSearchRequest(ureq);
		} else if(extendedSearchButton != null
				&& extendedSearchButton.getFormDispatchId().equals(dispatchuri)) {
			expandExtendedSearch(ureq);
		} else if(extendedFilterButton != null
				&& extendedFilterButton.getFormDispatchId().equals(dispatchuri)) {
			extendedFilterCallout(ureq);
		} else if(dispatchuri != null && StringHelper.containsNonWhitespace(filter)) {
			doFilter(filter);
		} else if(exportButton != null
				&& exportButton.getFormDispatchId().equals(dispatchuri)) {
			doExport(ureq);
		} else if(dispatchuri != null && select != null && select.equals("checkall")) {
			doSelectAll();
		} else if(dispatchuri != null && select != null && select.equals("uncheckall")) {
			doUnSelectAll();
		} else if(customButton != null
				&& customButton.getFormDispatchId().equals(dispatchuri)) {
			//snap the request
			customizeCallout(ureq);
		} else if(customTypeButton != null
				&& customTypeButton.getFormDispatchId().equals(dispatchuri)) {
			setRendererType(FlexiTableRendererType.custom);
			saveCustomSettings(ureq);
		} else if(classicTypeButton != null
				&& classicTypeButton.getFormDispatchId().equals(dispatchuri)) {
			setRendererType(FlexiTableRendererType.classic);
			saveCustomSettings(ureq);
		} else if(doSelect(ureq)) {
			//do select
		}
	}
	
	private boolean doSelect(UserRequest ureq) {
		boolean select = false;
		FlexiTableColumnModel colModel = dataModel.getTableColumnModel();
		for(int i=colModel.getColumnCount(); i-->0; ) {
			FlexiColumnModel col = colModel.getColumnModel(i);
			if(col.getAction() != null) {
				String selectedRowIndex = getRootForm().getRequestParameter(col.getAction());
				if(StringHelper.containsNonWhitespace(selectedRowIndex)) {
					doSelect(ureq, col.getAction(), Integer.parseInt(selectedRowIndex));
					select = true;
					break;
				}
			}
		}
		return select;
	}
	
	@Override
	public void dispatchEvent(UserRequest ureq, Controller source, Event event) {
		if(source == callout) {
			if(CloseableCalloutWindowController.CLOSE_WINDOW_EVENT == event) {
				//already deactivated
				callout = null;
			}
		} else if(source == extendedSearchCtrl) {
			if(event == Event.CANCELLED_EVENT) {
				collapseExtendedSearch();
			} else if(event == Event.DONE_EVENT) {
				evalExtendedSearch(ureq);
			}
		} else if(source == extendedFilterCtrl) {
			doExtendedFilter(ureq);
			callout.deactivate();
		}
	}
	
	@Override
	public void dispatchEvent(UserRequest ureq, Component source, Event event) {
		if(source instanceof Choice) {
			if(Choice.EVNT_VALIDATION_OK.equals(event)) {
				Choice visibleColsChoice = (Choice)source;
				setCustomizedColumns(ureq, visibleColsChoice);
			} else if(Choice.EVNT_FORM_RESETED.equals(event)) {
				resetCustomizedColumns(ureq);
			}
			if(callout != null) {
				callout.deactivate();
				callout = null;
			}
		} else if(searchFieldEl.getComponent() == source) {
			if(event instanceof AutoCompleteEvent) {
				AutoCompleteEvent ace = (AutoCompleteEvent)event;
				doSearch(ureq, FlexiTableSearchEvent.QUICK_SEARCH_KEY_SELECTION, ace.getKey(), null);
			}
		}
	}
	
	private void selectPageSize(UserRequest ureq, int size) {
		if(callout != null) {
			callout.deactivate();
			callout = null;
		}
		
		setPageSize(size);
		//reset
		rowCount = -1;
		currentPage = 0;
		component.setDirty(true);
		reloadData();
		saveCustomSettings(ureq);
	}
	
	@Override
	public void sort(String sortKey, boolean asc) {
		SortKey key = new SortKey(sortKey, asc);
		orderBy = new SortKey[]{ key };
		if(dataModel instanceof SortableFlexiTableDataModel) {
			((SortableFlexiTableDataModel<?>)dataModel).sort(key);
		} else if(dataSource != null) {
			currentPage = 0;
			dataSource.clear();
			dataSource.load(null, getSelectedFilters(), conditionalQueries, 0, getPageSize(), orderBy);
		}

		selectSortOption(sortKey, asc);
		component.setDirty(true);
	}
	
	private void selectSortOption(String sortKey, boolean asc) {
		if(sortOptions != null) {
			for(FlexiTableSort sort:sortOptions.getSorts()) {
				boolean selected = sort.getSortKey().getKey().equals(sortKey);
				sort.setSelected(selected);
				if(selected) {
					sort.getSortKey().setAsc(asc);
				} else {
					sort.getSortKey().setAsc(false);
				}
			}
		}
	}
	
	private void doFilter(String filterKey) {
		String selectedFilterKey = null;
		FlexiTableFilter selectedFilter = null;
		if(filterKey == null) {
			for(FlexiTableFilter filter:filters) {
				filter.setSelected(false);
			}
		} else {
			for(FlexiTableFilter filter:filters) {
				boolean selected = filter.getFilter().equals(filterKey);
				if(selected) {
					if(filter.isSelected()) {
						filter.setSelected(false);
					} else {
						filter.setSelected(true);
						selectedFilterKey = filterKey;
						selectedFilter = filter;
					}
				} else {
					filter.setSelected(false);
				}
			}
		}
		
		if(dataModel instanceof FilterableFlexiTableModel) {
			rowCount = -1;
			currentPage = 0;
			((FilterableFlexiTableModel)dataModel).filter(selectedFilterKey);
		} else if(dataSource != null) {
			rowCount = -1;
			currentPage = 0;

			List<FlexiTableFilter> selectedFilters = Collections.singletonList(selectedFilter);
			dataSource.clear();
			dataSource.load(null, selectedFilters, null, 0, getPageSize(), orderBy);
		}
		component.setDirty(true);
	}
	
	private void doExtendedFilter(UserRequest ureq) {
		if(dataSource != null) {
			rowCount = -1;
			currentPage = 0;

			List<FlexiTableFilter> selectedFilters = new ArrayList<>(extendedFilters);
			dataSource.clear();
			dataSource.load(null, selectedFilters, null, 0, getPageSize(), orderBy);
		}

		getRootForm().fireFormEvent(ureq, new FlexiTableSearchEvent(FlexiTableSearchEvent.EXTENDED_FILTER, this,
				getSearchText(), getSelectedFilters(), getSelectedExtendedFilters(), getConditionalQueries(), FormEvent.ONCLICK));
	}
	
	private void doExport(UserRequest ureq) {
		// ensure the all rows are loaded to export
		if(dataSource != null) {
			dataSource.load(getSearchText(), getSelectedFilters(), getConditionalQueries(), 0, -1, orderBy);
		}
		
		MediaResource resource;
		if(dataModel instanceof ExportableFlexiTableDataModel) {
			resource = ((ExportableFlexiTableDataModel)dataModel).export(component);
		} else {
			ExportableFlexiTableDataModelDelegate exporter = new ExportableFlexiTableDataModelDelegate();
			resource = exporter.export(component, getTranslator());
		}
		ureq.getDispatchResult().setResultingMediaResource(resource);
	}
	
	@Override
	public void expandExtendedSearch(UserRequest ureq) {
		component.setDirty(true);
		extendedSearchExpanded = true;
		extendedSearchCtrl.setEnabled(true);
		if(searchFieldEl != null) {
			searchFieldEl.setVisible(false);
		}
	}

	private void extendedFilterCallout(UserRequest ureq) {
		extendedFilterCtrl = new ExtendedFilterController(ureq, wControl, extendedFilters);
		extendedFilterCtrl.addControllerListener(this);
		callout = new CloseableCalloutWindowController(ureq, wControl, extendedFilterCtrl.getInitialComponent(),
				extendedFilterButton, "Filter", true, "o_sel_flexi_filter_callout");
		callout.activate();
		callout.addControllerListener(this);
	}
	
	@Override
	public void collapseExtendedSearch() {
		extendedSearchExpanded = false;
		extendedSearchCtrl.setEnabled(false);
		if(searchFieldEl != null) {
			searchFieldEl.setVisible(true);
		}
	}

	protected void customizeCallout(UserRequest ureq) {
		Choice choice = getColumnListAndTheirVisibility();
		callout = new CloseableCalloutWindowController(ureq, wControl, choice,
				customButton, "Customize", true, "o_sel_flexi_custom_callout");
		callout.activate();
		callout.addControllerListener(this);
	}

	@Override
	public boolean isColumnModelVisible(FlexiColumnModel col) {
		return col.isAlwaysVisible() || enabledColumnIndex.contains(col.getColumnIndex());
	}

	@Override
	public void setColumnModelVisible(FlexiColumnModel col, boolean visible) {
		boolean currentlyVisible = enabledColumnIndex.contains(col.getColumnIndex());
		if(currentlyVisible != visible) {
			if(visible) {
				enabledColumnIndex.add(col.getColumnIndex());
			} else {
				enabledColumnIndex.remove(col.getColumnIndex());
			}
		}
	}
	
	protected void setCustomizedColumns(UserRequest ureq, Choice visibleColsChoice) {
		List<Integer> visibleCols = visibleColsChoice.getSelectedRows();
		if(visibleCols.size() > 1) {
			VisibleFlexiColumnsModel model = (VisibleFlexiColumnsModel)visibleColsChoice.getModel();
			for(int i=model.getRowCount(); i-->0; ) {
				FlexiColumnModel col = model.getObject(i);
				if(visibleCols.contains(new Integer(i))) {
					enabledColumnIndex.add(col.getColumnIndex());
				} else {
					enabledColumnIndex.remove(col.getColumnIndex());
				}
			}
		}
		saveCustomSettings(ureq);
		component.setDirty(true);
	}
	
	protected void resetCustomizedColumns(UserRequest ureq) {
		enabledColumnIndex.clear();
		for(int i=dataModel.getTableColumnModel().getColumnCount(); i-->0; ) {
			FlexiColumnModel col = dataModel.getTableColumnModel().getColumnModel(i);
			if(col.isDefaultVisible()) {
				enabledColumnIndex.add(new Integer(col.getColumnIndex()));
			}
		}
		
		if(pageSize > 0) {
			selectPageSize(ureq, defaultPageSize);
		}
		saveCustomSettings(ureq);
		component.setDirty(true);
	} 
	
	private void saveCustomSettings(UserRequest ureq) {
		if(StringHelper.containsNonWhitespace(persistentId)) {
			Preferences prefs = ureq.getUserSession().getGuiPreferences();
			
			boolean sortDirection = false;
			String sortedColKey = null;
			if(orderBy != null && orderBy.length > 0 && orderBy[0] != null) {
				sortDirection = orderBy[0].isAsc();
				String sortKey = orderBy[0].getKey();
				FlexiTableColumnModel colModel = dataModel.getTableColumnModel();
				for(int i=colModel.getColumnCount(); i-->0; ) {
					FlexiColumnModel col = colModel.getColumnModel(i);
					if(col.getSortKey() != null && sortKey.equals(col.getSortKey())) {
						sortedColKey = col.getColumnKey();
					}
				}
				
				if(sortedColKey == null && sortOptions != null && sortOptions.getSorts() != null) {
					for(FlexiTableSort sortOption :sortOptions.getSorts()) {
						if(sortOption.getSortKey().getKey().equals(sortKey)) {
							sortedColKey = sortKey;
						}
					}
				}
			}

			FlexiTablePreferences tablePrefs =
					new FlexiTablePreferences(getPageSize(), sortedColKey, sortDirection,
							convertColumnIndexToKeys(enabledColumnIndex), rendererType);
			prefs.put(FlexiTableElement.class, persistentId, tablePrefs);
			prefs.save();
		}
	}
	
	private void loadCustomSettings(UserRequest ureq) {
		if(StringHelper.containsNonWhitespace(persistentId)) {
			Preferences prefs = ureq.getUserSession().getGuiPreferences();
			FlexiTablePreferences tablePrefs = (FlexiTablePreferences)prefs.get(FlexiTableElement.class, persistentId);
			if(tablePrefs != null) {
				if(tablePrefs.getPageSize() != getDefaultPageSize() && tablePrefs.getPageSize() != 0) {
					setPageSize(tablePrefs.getPageSize());
				}
				
				if(tablePrefs.getEnabledColumnKeys() != null) {
					enabledColumnIndex.clear();
					enabledColumnIndex.addAll(convertColumnKeysToIndex(tablePrefs.getEnabledColumnKeys()));
				}
				
				if(StringHelper.containsNonWhitespace(tablePrefs.getSortedColumnKey())) {
					String sortKey = null;
					String columnKey = tablePrefs.getSortedColumnKey();
					FlexiTableColumnModel colModel = dataModel.getTableColumnModel();
					for(int i=colModel.getColumnCount(); i-->0; ) {
						FlexiColumnModel col = colModel.getColumnModel(i);
						if(columnKey.equals(col.getColumnKey()) && col.isSortable()) {
							sortKey = col.getSortKey();
						}
					}
					if(sortKey == null && sortOptions != null && sortOptions.getSorts() != null) {
						for(FlexiTableSort sortOption :sortOptions.getSorts()) {
							if(sortOption.getSortKey().getKey().equals(columnKey)) {
								sortKey = columnKey;
							}
						}
					}
					
					if(sortKey != null) {
						orderBy = new SortKey[]{ new SortKey(sortKey, tablePrefs.isSortDirection()) };
						selectSortOption(sortKey, tablePrefs.isSortDirection());
					}
				}

				if(tablePrefs.getRendererType() != null) {
					setRendererType(tablePrefs.getRendererType());
				}
			}
		}
	}
	
	private List<Integer> convertColumnKeysToIndex(Collection<String> columnKeys) {
		if(columnKeys == null) return new ArrayList<>(0);
		
		List<Integer> index = new ArrayList<>(columnKeys.size());
		FlexiTableColumnModel colModel = dataModel.getTableColumnModel();
		for(String columnKey:columnKeys) {
			for(int i=colModel.getColumnCount(); i-->0; ) {
				FlexiColumnModel col = colModel.getColumnModel(i);
				if(columnKey.equals(col.getColumnKey())) {
					index.add(new Integer(col.getColumnIndex()));
				}
			}
		}
		return index;
	}
	
	private List<String> convertColumnIndexToKeys(Collection<Integer> columnIndex) {
		List<String> keys = new ArrayList<>();
		FlexiTableColumnModel colModel = dataModel.getTableColumnModel();
		for(Integer columnId:columnIndex) {
			for(int i=colModel.getColumnCount(); i-->0; ) {
				FlexiColumnModel col = colModel.getColumnModel(i);
				if(columnId.intValue() == col.getColumnIndex()) {
					keys.add(col.getColumnKey());
				}
			}
		}
		return keys;
	}
	
	private Choice getColumnListAndTheirVisibility() {
		Choice choice = new Choice("colchoice", getTranslator());
		choice.setModel(new VisibleFlexiColumnsModel(dataModel.getTableColumnModel(), enabledColumnIndex, getTranslator()));
		choice.addListener(this);
		choice.setEscapeHtml(false);
		choice.setCancelKey("cancel");
		choice.setSubmitKey("save");
		choice.setResetKey("reset");
		choice.setElementCssClass("o_table_config");
		return choice;
	}

	protected void evalExtendedSearch(UserRequest ureq) {
		String search = null;
		if(searchFieldEl != null && searchFieldEl.isEnabled() && searchFieldEl.isVisible()) {
			searchFieldEl.evalFormRequest(ureq);
			search = searchFieldEl.getValue();
		}
		List<String> condQueries = extendedSearchCtrl.getConditionalQueries();
		doSearch(ureq, FlexiTableSearchEvent.SEARCH, search, condQueries);
	}

	protected void evalSearchRequest(UserRequest ureq) {
		if(searchFieldEl == null || !searchFieldEl.isEnabled() || !searchFieldEl.isVisible()){
			return;//this a default behavior which can occur without the search configured
		}
		searchFieldEl.evalFormRequest(ureq);
		String search = searchFieldEl.getValue();

		if(StringHelper.containsNonWhitespace(search)) {
			doSearch(ureq, FlexiTableSearchEvent.QUICK_SEARCH, search, null);
		} else {
			doResetSearch(ureq);
		}
	}
	
	@Override
	public String getQuickSearchString() {
		if(searchFieldEl != null && searchFieldEl.isEnabled() && searchFieldEl.isVisible()){
			return searchFieldEl.getValue();
		}
		return null;
	}

	@Override
	public void quickSearch(UserRequest ureq, String search) {
		if(searchFieldEl == null || !searchFieldEl.isEnabled() || !searchFieldEl.isVisible()){
			return;//this a default behavior which can occur without the search configured
		}
		if(StringHelper.containsNonWhitespace(search)) {
			searchFieldEl.setValue(search);
			doSearch(ureq, FlexiTableSearchEvent.QUICK_SEARCH, search, null);
		}
	}
	
	protected void doSelectAll() {
		if(multiSelectedIndex != null) {
			multiSelectedIndex.clear();
		} else {
			multiSelectedIndex = new HashSet<>();
		}
		
		int numOfRows = getRowCount();
		for(int i=0; i<numOfRows;i++) {
			multiSelectedIndex.add(new Integer(i));
		}
		allSelectedNeedLoadOfWholeModel = true;
	}
	
	protected void doUnSelectAll() {
		if(multiSelectedIndex != null) {
			multiSelectedIndex.clear();
		}
	}
	
	protected void doSelect(UserRequest ureq, int index) {
		getRootForm().fireFormEvent(ureq, new SelectionEvent(ROM_SELECT_EVENT, index, this, FormEvent.ONCLICK));
	}
	
	protected void doSelect(UserRequest ureq, String selectAction, int index) {
		getRootForm().fireFormEvent(ureq, new SelectionEvent(selectAction, index, this, FormEvent.ONCLICK));
	}

	
	private void doSearch(UserRequest ureq, String eventCmd, String search, List<String> condQueries) {
		if(condQueries == null || condQueries.isEmpty()) {
			conditionalQueries = null;
		} else {
			conditionalQueries = new ArrayList<String>(condQueries);
		}
		
		if(dataSource != null) {
			currentPage = 0;
			resetInternComponents();
			dataSource.clear();
			dataSource.load(search, getSelectedFilters(), conditionalQueries, 0, getPageSize(), orderBy);
		}
		getRootForm().fireFormEvent(ureq, new FlexiTableSearchEvent(eventCmd, this,
				search, getSelectedFilters(), getSelectedExtendedFilters(), condQueries, FormEvent.ONCLICK));
	}
	
	protected void doResetSearch(UserRequest ureq) {
		conditionalQueries = null;
		currentPage = 0;
		if(dataSource != null) {
			resetInternComponents();
			dataSource.clear();
			dataSource.load(null, null, null, 0, getPageSize());
		} else {
			getRootForm().fireFormEvent(ureq, new FlexiTableSearchEvent(this, FormEvent.ONCLICK));
		}
	}

	@Override
	public Set<Integer> getMultiSelectedIndex() {
		if(allSelectedNeedLoadOfWholeModel && dataSource != null) {
			//ensure the whole data model is loaded
			dataSource.load(getSearchText(), getSelectedFilters(), getConditionalQueries(), 0, -1);
			Set<Integer> allIndex = new HashSet<Integer>();
			for(int i=dataModel.getRowCount(); i-->0; ) {
				allIndex.add(new Integer(i));
			}
			allSelectedNeedLoadOfWholeModel = false;
			return allIndex;
		}
		return multiSelectedIndex == null ? Collections.<Integer>emptySet() : multiSelectedIndex;
	}

	@Override
	public void setMultiSelectedIndex(Set<Integer> set) {
		if(multiSelectedIndex == null) {
			multiSelectedIndex = new HashSet<Integer>();
		}
		multiSelectedIndex.addAll(set);
	}

	@Override
	public boolean isMultiSelectedIndex(int index) {
		return multiSelectedIndex != null && multiSelectedIndex.contains(new Integer(index));
	}
	
	protected void toogleSelectIndex(String selection) {
		if(multiSelectedIndex == null) {
			multiSelectedIndex = new HashSet<Integer>();
		}

		String rowStr;
		int index = selection.lastIndexOf('-');
		if(index > 0 && index+1 < selection.length()) {
			rowStr = selection.substring(index+1);
		} else {
			rowStr = selection;
		}
		
		try {
			Integer row = new Integer(rowStr);
			if(multiSelectedIndex.contains(row)) {
				if(multiSelectedIndex.remove(row) & allSelectedNeedLoadOfWholeModel) {
					allSelectedNeedLoadOfWholeModel = false;
				}
			} else {
				multiSelectedIndex.add(row);
			}	
		} catch (NumberFormatException e) {
			//can happen
		}
	}
	
	protected void setMultiSelectIndex(String[] selections) {
		if(multiSelectedIndex == null) {
			multiSelectedIndex = new HashSet<Integer>();
		}
		//selection format row_{formDispId}-{index}
		if(selections != null && selections.length > 0) {
			int firstIndex = getPageSize() * getPage();
			int lastResult = firstIndex + getPageSize() -1;
			for(int i=firstIndex; i<lastResult; i++) {
				multiSelectedIndex.remove(new Integer(i));
			}

			for(String selection:selections) {	
				int index = selection.lastIndexOf('-');
				if(index > 0 && index+1 < selection.length()) {
					String rowStr = selection.substring(index+1);
					int row = Integer.parseInt(rowStr);
					multiSelectedIndex.add(new Integer(row));
				}
			}
		}
	}

	@Override
	public void validate(List<ValidationStatus> validationResults) {
		if(searchFieldEl != null) searchFieldEl.validate(validationResults);
		if(searchButton != null) searchButton.validate(validationResults);
		if(customButton != null) customButton.validate(validationResults);
		if(extendedSearchButton != null) extendedSearchButton.validate(validationResults);
	}

	@Override
	public void reset() {
		resetInternComponents();
		reloadData();
	}
	
	private void resetInternComponents() {
		rowCount = -1;
		component.setDirty(true);
		multiSelectedIndex = null;
	}
	
	@Override
	public void deselectAll() {
		component.setDirty(true);
		multiSelectedIndex = null;
	}

	@Override
	public void reloadData() {
		if(dataSource != null) {
			dataSource.clear();
			int firstResult = currentPage * getPageSize();
			dataSource.load(getSearchText(), getSelectedFilters(), getConditionalQueries(), firstResult, getPageSize(), orderBy);//reload needed rows
		} else {
			if(dataModel instanceof FilterableFlexiTableModel) {
				if(isFilterEnabled()) {
					String filter = getSelectedFilterKey();
					((FilterableFlexiTableModel)dataModel).filter(filter);
				}
			}
			
			if(dataModel instanceof SortableFlexiTableDataModel) {
				if(orderBy != null && orderBy.length > 0) {
					((SortableFlexiTableDataModel<?>)dataModel).sort(orderBy[0]);
				}
			}
		}

		component.setDirty(true);
	}
	
	/**
	 * Prevent parent to be set as dirty for every request
	 */
	@Override
	public boolean isInlineEditingElement() {
		return true;
	}

	@Override
	protected void rootFormAvailable() {
		rootFormAvailable(searchButton);
		rootFormAvailable(customButton);
		rootFormAvailable(exportButton);
		rootFormAvailable(searchFieldEl);
		rootFormAvailable(extendedSearchButton);
		rootFormAvailable(customTypeButton);
		rootFormAvailable(classicTypeButton);
	}
	
	private final void rootFormAvailable(FormItem item) {
		if(item != null && item.getRootForm() != getRootForm())
			item.setRootForm(getRootForm());
	}

	protected FlexiTableComponent getFormItemComponent() {
		return component;
	}
	
	public int getRowCount() {
		if(rowCount < 0) {
			rowCount = dataModel.getRowCount();
		}
		return rowCount;
	}
	
	public int getFirstRow() {
		if(getPageSize() > 0) {
			return getPage() * getPageSize();
		}
		return 0;
	}
	
	public int getMaxRows() {
		if(pageSize > 0) {
			return pageSize;
		}
		return getRowCount();
	}
	
	public FlexiTableDataModel<?> getTableDataModel() {
		return dataModel;
	}
	
	public FlexiTableDataSource<?> getTableDataSource() {
		return dataSource;
	}

	@Override
	public void setEmtpyTableMessageKey(String i18key) {
		this.emptyTableMessageKey  = i18key;
	}
	@Override
	public String getEmtpyTableMessageKey() {
		return this.emptyTableMessageKey;
	}
}