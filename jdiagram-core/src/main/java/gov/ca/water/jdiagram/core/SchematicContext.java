package gov.ca.water.jdiagram.core;

import java.time.Month;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.DiagramItem;
import com.mindfusion.pdf.PageSizesEnum;

public final class SchematicContext {
	private final boolean[] selectedStudies = {false, false, false, false, false, false, false, false};
	private static final List<DiagramItem> DIAGRAM_ITEMS = new ArrayList<>();
	private static final List<String> ALL_SCHEMATIC_VARIABLE_NAMES = new ArrayList<>();
	private static final Set<Month> months = EnumSet.allOf(Month.class);
	private static Diagram diagram;
	private final SchematicDataSource dataSource;
	private final Map<Integer, List<Map<String, Double>>> longTermCfs = new ConcurrentHashMap<>();
	private final Map<Integer, List<Map<String, Double>>> longTermTaf = new ConcurrentHashMap<>();
	private final Map<Integer, Map<String, String>> unitsCfs = new ConcurrentHashMap<>();
	private final Map<Integer, Map<String, String>> unitsTaf = new ConcurrentHashMap<>();
	private final List<String> twSelections = new CopyOnWriteArrayList<>(List.of("Add..."));
	private String units = CFS;
	private String mode = BASE;
	private String selectedDateOption;
	private int selectedIndex;
	private boolean tafMonthly = true;
	private boolean zoomToRect;
	private boolean showMagnifier;
	private PageSizesEnum pageSize = PageSizesEnum.Letter;

	public static final String DIFF = "diff";
	public static final String CFS = "cfs";
	public static final String TAF = "taf";
	public static final String BASE = "Base";

	public SchematicContext(SchematicDataSource dataSource) {
		this.dataSource = dataSource;
	}

	public SchematicDataSource getDataSource() {
		return dataSource;
	}

	/**
	 * Returns the selected study. Formerly DebugCorePlugin.selectedStudies
	 * @param index The index of the selected study.
	 * @return True if the study is selected, false otherwise.
	 */
	public boolean getSelectedStudy(int index) {
		return selectedStudies[index];
	}

	public void setSelectedStudy(int index, boolean selected) {
		selectedStudies[index] = selected;
	}

	/**
	 * Set the list of copied diagram items. Formerly SchematicPluginCore.copiedItems
	 * @param diagramItems The list of copied diagram items to be set.
	 */
	public void setDiagramItems(List<DiagramItem> diagramItems) {
		DIAGRAM_ITEMS.clear();
		DIAGRAM_ITEMS.addAll(diagramItems);
	}

	/**
	 * Retrieve copied diagram items. Formerly SchematicPluginCore.copiedItems
	 * @return A list of copied diagram items.
	 */
	public List<DiagramItem> getDiagramItems() {
		return DIAGRAM_ITEMS;
	}

	/**
	 * Retrieve the copied diagram object. Formerly SchematicPluginCore.copyDiagram
	 * @return The diagram object.
	 */
	public static Diagram getDiagram() {
		return diagram;
	}

	/**
	 * Sets the copied diagram object. Formerly SchematicPluginCore.copyDiagram
	 * @param copiedDiagram The diagram object to be set.
	 */
	public static void setDiagram(Diagram copiedDiagram) {
		diagram = copiedDiagram;
	}

	/**
	 * Retrieves the names of all schematic variables. Formerly DssPluginCore.allSchematicVariableNames
	 * @return The list of schematic variable names.
	 */
	public List<String> getAllSchematicVariableNames() {
		return ALL_SCHEMATIC_VARIABLE_NAMES;
	}

	/**
	 * Updates the names of all schematic variables. Formerly DssPluginCore.allSchematicVariableNames
	 * @param names The list of schematic variable names.
	 */
	public void setAllSchematicVariableNames(List<String> names) {
		ALL_SCHEMATIC_VARIABLE_NAMES.clear();
		ALL_SCHEMATIC_VARIABLE_NAMES.addAll(names);
	}

	/**
	 * Retrieves the date of the currently selected time window. Formerly SchematicPluginCore.selDate
	 * @return The string representation of the date of the selected time window.
	 */
	public String getSelectedDateOption() {
		return selectedDateOption;
	}

	/**
	 * Updates the date of the currently selected time window. Formerly SchematicPluginCore.selDate
	 * @param date The string representation of the date of the selected time window.
	 */
	public void setSelectedDateOption(String date) {
		selectedDateOption = date;
	}

	/**
	 * Retrieves the index of the currently selected time window. Formerly SchematicPluginCore.selIndex
	 * @return The index of the selected time window.
	 */
	public int getSelectedIndex() {
		return selectedIndex;
	}

	/**
	 * Updates the index of the currently selected time window. Formerly SchematicPluginCore.selIndex
	 * @param index The index of the selected time window.
	 */
	public void updateSelectedIndex(int index) {
		selectedIndex = index;
	}

	/**
	 * Retrieve the mode. Formerly DssPluginCore.mode
	 * @return The string representation of the mode.
	 */
	public String mode() {
		return mode;
	}

	/**
	 * Set the mode. Formerly DssPluginCore.mode
	 */
	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * Retrieves long-term average data in CFS. Formerly DssPluginCore.longTermAverageDataCFS
	 * @return The long-term average data, keyed to the time window index.
	 */
	public Map<Integer, List<Map<String, Double>>> getLongTermAverageDataCFS() {
		return longTermCfs;
	}

	/**
	 * Retrieves long-term average data in TAF. Formerly DssPluginCore.longTermAverageDataTAF
	 * @return The long-term average data, keyed to the time window index.
	 */
	public Map<Integer, List<Map<String, Double>>> getLongTermAverageDataTAF() {
		return longTermTaf;
	}

	/**
	 * Retrieve a list of months in the year. Formerly DssPluginCore.months
	 * @return The set of months in the year. Formerly the integer representation of the months.
	 */
	public Set<Month> months() {
		return months;
	}

	/**
	 * Retrieves the string representation of all schematic variable units in CFS (cubic feet/second).
	 * Formerly DssPluginCore._schematicVariableUnitsCFS
	 * @param index The index of the schematic variable.
	 * @return The string representation of all schematic variable units in CFS.
	 */
	public Map<String, String> getAllSchematicVariableUnitsCFS(int index) {
		return unitsCfs.computeIfAbsent(index, i -> new ConcurrentHashMap<>());
	}

	/**
	 * Retrieves the string representation of all schematic variable units in TAF (thousand acre-feet).
	 * Formerly DssPluginCore._schematicVariableUnitsTAF
	 * @param index The index of the schematic variable.
	 * @return The string representation of all schematic variable units in TAF.
	 */
	public Map<String, String> getAllSchematicVariableUnitsTAF(int index) {
		return unitsTaf.computeIfAbsent(index, i -> new ConcurrentHashMap<>());
	}

	/**
	 * Retrieves the list of selected time windows for the schematic diagram.
	 * Formerly DssPluginCore._schematicTwSelections
	 * @return The list of selected time windows in string format.
	 */
	public List<String> getSchematicTwSelections() {
		return twSelections;
	}

	/**
	 * Clears the list of selected time windows for the schematic diagram.
	 */
	public void clearSchematicTwSelections() {
		twSelections.clear();
	}

	/**
	 * Retrieves the page size used for schematic data representation. Formerly SchematicPluginCore.pageSize
	 * @return The page size used for schematic data representation (A4, letter, etc.).
	 */
	public PageSizesEnum pageSize() {
		return pageSize;
	}

	/**
	 * Set the page size used for schematic data representation. Formerly SchematicPluginCore.pageSize
	 * @param pageSize The page size used for schematic data representation (A4, letter, etc.).
	 */
	public void setPageSize(PageSizesEnum pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * Retrieve the boolean representation of the TAF monthly flag. Formerly SchematicPluginCore.isTAFMonthly
	 * @return The boolean representation of the TAF monthly flag.
	 */
	public boolean isTAFMonthly() {
		return tafMonthly;
	}

	/**
	 * Set the boolean representation of the TAF monthly flag. Formerly DssPluginCore.isTAFMonthly
	 * @param isTAFMonthly The boolean representation of the TAF monthly flag.
	 */
	public void setTAFMonthly(boolean isTAFMonthly) {
		this.tafMonthly = isTAFMonthly;
	}

	/**
	 * A boolean flag used to toggle the rectangular zoom feature. Formerly SchematicPluginCore.zoomToRect
	 * @return The boolean value identifying whether to use rectangular zoom behavior.
	 */
	public boolean zoomToRect() {
		return zoomToRect;
	}

	/**
	 * Set the boolean representation of the rectangular zoom feature. Formerly SchematicPluginCore.zoomToRect
	 * @param zoomToRect The boolean value identifying whether to use rectangular zoom behavior.
	 */
	public void setZoomToRect(boolean zoomToRect) {
		this.zoomToRect = zoomToRect;
	}

	/**
	 * A boolean flag used to toggle the magnifier feature. Formerly SchematicPluginCore.showMagnifier
	 * @return The boolean value identifying whether to use magnifier behavior.
	 */
	public boolean showMagnifier() {
		return showMagnifier;
	}

	/**
	 * Set the boolean representation of the magnifier feature. Formerly SchematicPluginCore.showMagnifier
	 * @param showMagnifier The boolean value identifying whether to use magnifier behavior.
	 */
	public void setShowMagnifier(boolean showMagnifier) {
		this.showMagnifier = showMagnifier;
	}

	/**
	 * Retrieve units to use for data representation. Formerly DssPluginCore.units
	 * @return The string representation of the units.
	 */
	public String units() {
		return units;
	}

	/**
	 * Set the units to use for data representation. Formerly DssPluginCore.units
	 */
	public void setUnits(String units) {
		this.units = units;
	}
}
