package gov.ca.water.jdiagram.core.interfaces;

import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.DiagramItem;

import hec.hecmath.HecMath;

/**
 * Core interface for schematic diagram.
 */
public interface SchematicCore
{
	/**
	 * Retrieves the preferences used for schematic diagram. Formerly DssPluginCore._preferences
	 * @return The preferences used for schematic diagram.
	 */
	Preferences getPreferences();

	/**
	 * Sets the preferences used for schematic diagram. Formerly DssPluginCore._preferences
	 * @param preferences The preferences used for schematic diagram.
	 */
	void setPreferences(Preferences preferences);

	/**
	 * Retrieves the list of selected time windows for the schematic diagram.
	 * Formerly DssPluginCore._schematicTwSelections
	 * @return The list of selected time windows in string format.
	 */
	List<String> getSchematicTwSelections();

	/**
	 * Clears the list of selected time windows for the schematic diagram.
	 */
	void clearSchematicTwSelections();

	/**
	 * Retrieves the string representation of all schematic variable units in CFS (cubic feet/second).
	 * Formerly DssPluginCore._schematicVariableUnitsCFS
	 * @param index The index of the schematic variable.
	 * @return The string representation of all schematic variable units in CFS.
	 */
	Map<String, String> getAllSchematicVariableUnitsCFS(int index);

	/**
	 * Retrieves the string representation of all schematic variable units in TAF (thousand acre-feet).
	 * Formerly DssPluginCore._schematicVariableUnitsTAF
	 * @param index The index of the schematic variable.
	 * @return The string representation of all schematic variable units in TAF.
	 */
	Map<String, String> getAllSchematicVariableUnitsTAF(int index);

	/**
	 * Retrieves the data for a schematic variable.
	 * Formerly DssPluginCore.allSchematicVariableData
	 * @param index The index of the schematic variable.
	 * @return The data for the schematic variable.
	 */
	Map<String, HecMath> getSchematicVariableData(int index);

	/**
	 * Retrieves long-term average data in CFS. Formerly DssPluginCore.longTermAverageDataCFS
	 * @return The long-term average data, keyed to the time window index.
	 */
	Map<Integer, List<Map<String, Double>>> getLongTermAverageDataCFS();

	/**
	 * Retrieves long-term average data in TAF. Formerly DssPluginCore.longTermAverageDataTAF
	 * @return The long-term average data, keyed to the time window index.
	 */
	Map<Integer, List<Map<String, Double>>> getLongTermAverageDataTAF();

	/**
	 * Retrieves the time window file path, used to store added time windows between sessions.
	 * @return The time window file.
	 */
	String getTwFile();

	/**
	 * Retrieves the index of the currently selected time window. Formerly SchematicPluginCore.selIndex
	 * @return The index of the selected time window.
	 */
	int getSelectedIndex();

	/**
	 * Updates the index of the currently selected time window. Formerly SchematicPluginCore.selIndex
	 * @param index The index of the selected time window.
	 */
	void updateSelectedIndex(int index);

	/**
	 * Retrieves the date of the currently selected time window. Formerly SchematicPluginCore.selDate
	 * @return The string representation of the date of the selected time window.
	 */
	String getSelectedDate();

	/**
	 * Updates the date of the currently selected time window. Formerly SchematicPluginCore.selDate
	 * @param date The string representation of the date of the selected time window.
	 */
	void setSelectedDate(String date);

	/**
	 * Retrieves the names of all schematic variables. Formerly DssPluginCore.allSchematicVariableNames
	 * @return The list of schematic variable names.
	 */
	List<String> getAllSchematicVariableNames();

	/**
	 * Updates the names of all schematic variables. Formerly DssPluginCore.allSchematicVariableNames
	 * @param names The list of schematic variable names.
	 */
	void setAllSchematicVariableNames(List<String> names);

	/**
	 * Reports an error that occurred during the execution of the schematic core.
	 * Formerly handled by WPPException
	 * @param t The throwable object representing the error.
	 */
	void reportError(Throwable t);

	/**
	 * Displays a warning message to the user with the specified text.
	 * Formerly handled by MessageBox usage in AddTimeWindowDialog
	 * @param message The warning message to be displayed.
	 */
	void showWarning(String message);

	/**
	 * Retrieve the copied diagram object. Formerly SchematicPluginCore.copyDiagram
	 * @return The diagram object.
	 */
	Diagram getDiagram();

	/**
	 * Sets the copied diagram object. Formerly SchematicPluginCore.copyDiagram
	 * @param diagram The diagram object to be set.
	 */
	void setDiagram(Diagram diagram);

	/**
	 * Retrieve copied diagram items. Formerly SchematicPluginCore.copiedItems
	 * @return A list of copied diagram items.
	 */
	List<DiagramItem> getDiagramItems();

	/**
	 * Set the list of copied diagram items. Formerly SchematicPluginCore.copiedItems
	 * @param diagramItems The list of copied diagram items to be set.
	 */
	void setDiagramItems(List<DiagramItem> diagramItems);
}
