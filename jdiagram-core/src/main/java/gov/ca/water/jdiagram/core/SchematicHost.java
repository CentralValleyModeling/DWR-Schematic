package gov.ca.water.jdiagram.core;

import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import hec.hecmath.HecMath;

public interface SchematicHost
{
	String taf = "TAF";
	String cfs = "CFS";
	String comp = "comp";
	String diff = "diff";
	int WATERYEAR = 0;
	int CALENDAR_YEAR = 1;
	int FEDERAL_CONTRACT_YEAR = 2;
	String DEFAULT_TIME_WINDOW = "All";
	String twFile = "twSchematic.prf";
	String selDate = "";
	int selIndex = -1;
	boolean showMagnifier = false;
	boolean zoomToRect = false;
	boolean isTAFMonthly = true;

	Preferences getPreferences();

	List<String> getSchematicTwSelections();

	String getUnits();

	List<Integer> getMonths();

	String getMode();

	Map<String, String> getAllSchematicVariableUnitsCFS(int index);

	Map<String, String> getAllSchematicVariableUnitsTAF(int index);

	List<String> getAllStorageNames();

	Map<String, HecMath> getSchematicVariableData(int index);

	Map<Integer, List<Map<String, Double>>> getLongTermAverageDataCFS();

	Map<Integer, List<Map<String, Double>>> getLongTermAverageDataTAF();
}
