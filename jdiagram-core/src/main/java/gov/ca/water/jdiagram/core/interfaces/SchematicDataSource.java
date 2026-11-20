package gov.ca.water.jdiagram.core.interfaces;

import java.time.Month;
import java.util.Optional;
import java.util.Set;

import com.mindfusion.pdf.PageSizesEnum;

import hec.hecmath.HecMath;

/**
 * Data source interface for schematic diagram.
 */
public interface SchematicDataSource
{
	/**
	 * Returns the selected study. Formerly DebugCorePlugin.selectedStudies
	 * @param index The index of the selected study.
	 * @return True if the study is selected, false otherwise.
	 */
	boolean getSelectedStudy(int index);

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
	 * Retrieve units to use for data representation. Formerly DssPluginCore.units
	 * @return The string representation of the units.
	 */
	String units();

	/**
	 * Retrieve the names of the storage locations. Formerly DssPluginCore.allStorageNames
	 * @return The set of storage names.
	 */
	Set<String> storageNames();


	Optional<HecMath> series(int alt, String name);

	/**
	 * Retrieve the mode. Formerly DssPluginCore.mode
	 * @return The string representation of the mode.
	 */
	String mode();

	/**
	 * Retrieve a list of months in the year. Formerly DssPluginCore.months
	 * @return The set of months in the year. Formerly the integer representation of the months.
	 */
	Set<Month> months();

	/**
	 * The debug target. Formerly DebugCorePlugin.target.
	 * Allows for implementations that provide specific target implementations.
	 * @return The object representation of the debug target. Formerly WPPDebugTarget type.
	 */
	Object target();

	/**
	 * Retrieve the diff mode. Formerly DssPluginCore.diff
	 * @return The string representation of the diff mode.
	 */
	String diff();

	/**
	 * Retrieve the boolean representation of the TAF monthly flag. Formerly SchematicPluginCore.isTAFMonthly
	 * @return The boolean representation of the TAF monthly flag.
	 */
	boolean isTAFMonthly();

	/**
	 * Set the boolean representation of the TAF monthly flag. Formerly DssPluginCore.isTAFMonthly
	 * @param isTAFMonthly The boolean representation of the TAF monthly flag.
	 */
	void setTAFMonthly(boolean isTAFMonthly);

	/**
	 * Retrieves the cubic feet per second unit label or identifier used for schematic data representation.
	 * Formerly DssPluginCore.cfs
	 * @return The string representation of the CFS unit.
	 */
	String cfs();

	/**
	 * A boolean flag used to toggle the rectangular zoom feature. Formerly SchematicPluginCore.zoomToRect
	 * @return The boolean value identifying whether to use rectangular zoom behavior.
	 */
	boolean zoomToRect();

	/**
	 * Set the boolean representation of the rectangular zoom feature. Formerly SchematicPluginCore.zoomToRect
	 * @param zoomToRect The boolean value identifying whether to use rectangular zoom behavior.
	 */
	void setZoomToRect(boolean zoomToRect);

	/**
	 * A boolean flag used to toggle the magnifier feature. Formerly SchematicPluginCore.showMagnifier
	 * @return The boolean value identifying whether to use magnifier behavior.
	 */
	boolean showMagnifier();

	/**
	 * Set the boolean representation of the magnifier feature. Formerly SchematicPluginCore.showMagnifier
	 * @param showMagnifier The boolean value identifying whether to use magnifier behavior.
	 */
	void setShowMagnifier(boolean showMagnifier);

	/**
	 * Retrieves the page size used for schematic data representation. Formerly SchematicPluginCore.pageSize
	 * @return The page size used for schematic data representation (A4, letter, etc.).
	 */
	PageSizesEnum pageSize();

	/**
	 * Set the page size used for schematic data representation. Formerly SchematicPluginCore.pageSize
	 * @param pageSize The page size used for schematic data representation (A4, letter, etc.).
	 */
	void setPageSize(PageSizesEnum pageSize);

	/**
	 * Retrieves the path to the shape library output directory used for saving files to the library.
	 * Formerly hard-coded value
	 * @return The path to the shape library used for schematic data representation.
	 */
	String shapeLibPath();
}
