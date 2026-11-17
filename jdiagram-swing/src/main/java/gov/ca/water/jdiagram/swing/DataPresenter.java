package gov.ca.water.jdiagram.swing;

import java.awt.Component;
import java.awt.Composite;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.mindfusion.diagramming.DiagramView;
import gov.ca.water.jdiagram.swing.views.SchematicOverview;

public interface DataPresenter
{
	void showTimeSeries(List<String> names);

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

	void showDssView();

	void showDSSPerspective();

	void launchFontDialog();

	String launchFileDialog();

	void launchViewPart(SchematicOverview overview, DiagramView view, JPanel container);

	JToolBar getToolBar();
}
