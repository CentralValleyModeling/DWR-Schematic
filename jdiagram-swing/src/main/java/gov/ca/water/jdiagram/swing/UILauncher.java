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

	JPanel mtp();

	void setMtpDecimalPlaces(int numberDecimals);

	void showDssView();

	void showDSSPerspective();

	void launchFontDialog();

	String launchFileDialog();

	void launchViewPart(SchematicOverview overview, DiagramView view, JPanel container);

	JToolBar getToolBar();
}
