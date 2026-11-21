package gov.ca.water.jdiagram.swing.views;

import java.awt.BorderLayout;
import javax.swing.JPanel;

import com.mindfusion.diagramming.DiagramView;
import com.mindfusion.diagramming.Overview;

public final class SchematicOverview extends JPanel
{
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = SchematicOverview.class.getCanonicalName();

	private final transient Overview overview;

	/**
	 * The constructor.
	 */
	public SchematicOverview() {
		super(new BorderLayout());
		overview = new Overview();
		overview.setFitAll(true);
		add(overview, BorderLayout.CENTER);
	}

	public Overview getOverview() {
		return overview;
	}

	public void setDiagramView(DiagramView diagramView) {
		overview.setDiagramView(diagramView);
		overview.setFitAll(true);
		overview.repaint();
	}
}
