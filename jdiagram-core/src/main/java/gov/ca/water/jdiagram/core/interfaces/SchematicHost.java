package gov.ca.water.jdiagram.core.interfaces;

import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.DiagramItem;

import hec.hecmath.HecMath;

public interface SchematicHost
{
	Preferences getPreferences();

	void setPreferences(Preferences preferences);

	List<String> getSchematicTwSelections();

	void clearSchematicTwSelections();

	Map<String, String> getAllSchematicVariableUnitsCFS(int index);

	Map<String, String> getAllSchematicVariableUnitsTAF(int index);

	Map<String, HecMath> getSchematicVariableData(int index);

	Map<Integer, List<Map<String, Double>>> getLongTermAverageDataCFS();

	Map<Integer, List<Map<String, Double>>> getLongTermAverageDataTAF();

	String getTwFile();

	int getSelectedIndex();

	void updateSelectedIndex(int index);

	String getSelectedDate();

	void setSelectedDate(String date);

	List<String> getAllSchematicVariableNames();

	void setAllSchematicVariableNames(List<String> names);

	void reportError(Throwable t);

	void showWarning(String message);

	Diagram getDiagram();

	void setDiagram(Diagram diagram);

	List<DiagramItem> getDiagramItems();

	void setDiagramItems(List<DiagramItem> diagramItems);
}
