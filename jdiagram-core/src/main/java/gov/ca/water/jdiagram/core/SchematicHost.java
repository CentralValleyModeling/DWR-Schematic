package gov.ca.water.jdiagram.core;

import java.awt.Component;
import java.awt.Composite;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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

	void showTimeSeries(List<String> names);

	void reportError(Throwable t);

	void showWarning(String message);

	/**
	 * Runs the supplied action that opens a specified dialog on the host's UI thread.
	 * Hosts embedded in another application (e.g. Eclipse) should override this to dispatch through
	 * that application's UI mechanism (e.g. {@code Display.asyncExec}). The default runs on the Swing EDT.
	 *
	 * @param showDialog action that creates and shows the dialog
	 */
	default void launchDialog(Runnable showDialog) {
		javax.swing.SwingUtilities.invokeLater(showDialog);
	}

	/**
	 * Gets the parent shell for the supplied component. Should be overridden by
	 * Eclipse workbench.getActiveWorkbenchWindow().getShell() call for Eclipse hosts.
	 * @param component The component for which to get the parent shell
	 * @return Composite parent shell
	 */
	default Composite getShell(Component component) {
		return (Composite) SwingUtilities.getWindowAncestor(component);
	}

	JPanel mtp();

	void setMtpDecimalPlaces(int numberDecimals);

	/**
	 *
	 */
	void showDssView();

	void showDSSPerspective();

	String diff();

	String cfs();
}
