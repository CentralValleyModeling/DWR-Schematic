package gov.ca.water.jdiagram.swing;

import java.awt.Component;
import java.awt.Window;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.mindfusion.diagramming.DiagramView;
import gov.ca.water.jdiagram.swing.views.SchematicOverview;

/**
 * Interface for launching UI components from the host application.
 */
public interface UILauncher
{
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
	 * Gets the parent window for the supplied component. Hosts that embed the schematic in a
	 * non-Swing shell may override this to return a suitable owner window.
	 * @param component The component for which to get the parent window
	 * @return the owning window, or null if the component is not displayed in a window
	 */
	default Window getShell(Component component) {
		return component == null ? null : SwingUtilities.getWindowAncestor(component);
	}

	/**
	 * Retrieves the MonthlyTablePanel, defined in WRIMS.
	 * Formerly DssPluginCore.mtp
	 * @return The MonthlyTablePanel.
	 */
	JPanel mtp();

	/**
	 * Set the number of decimal places used when representing flow values in the MonthlyTablePanel.
	 * @param numberDecimals The number of decimal places to be set.
	 */
	void setMtpDecimalPlaces(int numberDecimals);

	/**
	 * Shows the DSSMonthlyView, DSSPlotView, and DSSTableView
	 * Formerly implemented in Schematicbase.createPartControl()
	 */
	void showDssView();

	/**
	 * Open DssCatalogView perspective.
	 * Formerly implemented in Schematicbase.createPartControl().openDSSPerspective()
	 */
	void showDSSPerspective();

	/**
	 * Launches the font dialog. Formerly launched Eclipse FontDialog as SchematicBase fontAction.
	 */
	void launchFontDialog();

	/**
	 * Launches the file dialog. Formerly launched Eclipse FileDialog as SchematicBase openSchematicAction
	 * @return The absolute path to the first selected file, or null if none selected.
	 */
	String launchFileDialog();

	/**
	 * Launches the schematic overview panel. Formerly implemented in Schematicbase.createPartControl().
	 * @param overview The schematic overview panel.
	 * @param view The diagram view to assign to the overview panel.
	 * @param container The container panel.
	 */
	void launchViewPart(SchematicOverview overview, DiagramView view, JPanel container);

	/**
	 * Retrieves the toolbar for the schematic.
	 * Formerly implemented in SchematicEditor.contributeToEditorActionBars()
	 * @return The toolbar. Formerly returned the Eclipse IToolBarManager.
	 */
	JToolBar getToolBar();
}
