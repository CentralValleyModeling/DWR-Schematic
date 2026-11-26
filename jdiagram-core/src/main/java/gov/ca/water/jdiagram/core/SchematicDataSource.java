package gov.ca.water.jdiagram.core;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;

import hec.hecmath.HecMath;

/**
 * Data source interface for schematic diagram.
 */
public interface SchematicDataSource
{
	/**
	 * Whether debugging is enabled. Formerly DebugCorePlugin.isDebugging
	 * @return True if debugging is enabled, false otherwise.
	 */
	boolean isDebugging();

	/**
	 * Returns the working data directory. Formerly DebugCorePlugin.dataDir
	 * @return The data directory.
	 */
	String getDataDir();

	/**
	 * Retrieve the names of the storage locations. Formerly DssPluginCore.allStorageNames
	 * @return The set of storage names.
	 */
	Set<String> storageNames();

	/**
	 * Retrieve schematic series data.
	 * @param alt
	 * @param name
	 * @return The optional HecMath object containing the series data.
	 */
	Optional<HecMath> series(int alt, String name);

	/**
	 * The debug target. Formerly DebugCorePlugin.target.
	 * Allows for implementations that provide specific target implementations.
	 * @return The object representation of the debug target. Formerly WPPDebugTarget type.
	 */
	Object target();

	/**
	 * Retrieves the path to the shape library output directory used for saving files to the library.
	 * Formerly hard-coded value
	 * @return The path to the shape library used for schematic data representation.
	 */
	String shapeLibPath();

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
	 * Retrieves the data for a schematic variable.
	 * Formerly DssPluginCore.allSchematicVariableData
	 * @param index The index of the schematic variable.
	 * @return The data for the schematic variable.
	 */
	Map<String, HecMath> getSchematicVariableData(int index);

	/**
	 * Retrieves the time window file path, used to store added time windows between sessions.
	 * @return The time window file.
	 */
	String getTwFile();

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
}
