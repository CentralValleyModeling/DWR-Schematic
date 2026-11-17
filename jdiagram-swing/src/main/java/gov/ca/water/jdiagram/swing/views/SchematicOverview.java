package gov.ca.water.jdiagram.swing.views;

import java.awt.Composite;
import javax.swing.JPanel;

import com.mindfusion.diagramming.DiagramView;
import com.mindfusion.diagramming.Overview;

public class SchematicOverview extends JPanel
{
	private transient Composite swingContainer;

	private Overview overview;

	public void setDiagramView(DiagramView diagramView) {
		overview.setDiagramView(diagramView);
		overview.repaint();
		overview.setBounds(diagramView.getBounds());
		overview.setFitAll(true);
	}
}
