package gov.ca.water.jdiagram.swing.panel;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import com.google.common.flogger.FluentLogger;
import com.mindfusion.diagramming.Behavior;
import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.DiagramAdapter;
import com.mindfusion.diagramming.DiagramItem;
import com.mindfusion.diagramming.DiagramView;
import com.mindfusion.diagramming.DrawNodeEvent;
import com.mindfusion.diagramming.HandlesStyle;
import com.mindfusion.diagramming.LinkEvent;
import com.mindfusion.diagramming.LinkValidationEvent;
import com.mindfusion.diagramming.NodeEvent;
import com.mindfusion.diagramming.NodeValidationEvent;
import com.mindfusion.diagramming.ShapeNode;
import com.mindfusion.drawing.Pen;
import gov.ca.water.jdiagram.swing.utils.ImageUtil;
import gov.ca.water.jdiagram.swing.views.SchematicEditor;
import gov.ca.water.jdiagram.swing.views.SchematicPanel;

public class LegendPanel extends JPanel {
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	private final transient Diagram legendDiagram;
	private final transient DiagramView legendDiagramView;
	private AbstractAction copyAction;
	private JPopupMenu popupMenu;
	private transient DiagramItem clickedItem;

	public LegendPanel(final SchematicPanel viewer) {
		legendDiagram = new Diagram();
		try {
			legendDiagram.loadFromXml(getClass().getResourceAsStream(
					"legend.xml"));
		} catch (Exception e1) {
			logger.atFine().withCause(e1).log("Failed to load legend diagram");
		}
		legendDiagramView = new DiagramView(legendDiagram);

		if (viewer instanceof SchematicEditor) {
			legendDiagramView.setBehavior(Behavior.Modify);
		} else {
			legendDiagramView.setBehavior(Behavior.DoNothing);
		}
		legendDiagramView.setZoomFactor(40);

		this.add(legendDiagramView);

		if (viewer instanceof SchematicEditor editor) {
			Icon copyIcon = ImageUtil.createImageIcon("images/copy.png");
			copyAction = new AbstractAction("Copy", copyIcon) {

				@Override
				public void actionPerformed(ActionEvent evt) {
					editor.onCopy(legendDiagram);
				}
			};
			legendDiagramView.getInputMap().put(
					KeyStroke.getKeyStroke("ctrl typed c"), copyAction);

			JMenuItem copyMenuItem = new JMenuItem(copyAction);

			popupMenu = new JPopupMenu("Edit Popup");
			popupMenu.setName("editPopup");
			popupMenu.add(copyMenuItem);

		}

		legendDiagram.addDiagramListener(new DiagramAdapter() {
			@Override
			public void viewportChanged() {
			}

			@Override
			public void linkModifying(LinkValidationEvent arg0) {
				arg0.cancelDrag();
			}

			@Override
			public void nodeModifying(NodeValidationEvent arg0) {
				arg0.cancelDrag();
			}

			@Override
			public void nodeSelected(NodeEvent e) {
				e.getNode().setHandlesStyle(HandlesStyle.HatchFrame);
			}

			@Override
			public void linkSelected(LinkEvent e) {
				e.getLink().setSelected(true);
			}

			@Override
			public void nodeClicked(NodeEvent e) {
				if (e.getNode() instanceof ShapeNode) {
					boxClicked(e);
				}
			}

			@Override
			public void linkClicked(LinkEvent e) {
				arrowClicked(e);
			}

			@Override
			/**
			 * Raised when a diagram node must be custom drawn.
			 */
			public void drawNode(DrawNodeEvent e) {
				Rectangle2D rect = e.getBounds();
				rect.setFrame(rect.getMinX() + 9, rect.getMinY() + 9, rect
						.getWidth() - 16, rect.getHeight() - 16);

				Graphics2D g = e.getGraphics();
				Pen pen = e.getNode().getPen().clone();
				pen.setWidth(5);
				pen.applyTo(g);
				g.draw(rect);
			}
		});
	}

	public void boxClicked(NodeEvent e) {
		if (e.getMouseButton() == MouseEvent.BUTTON3) {
			e.getNode().setLocked(false);

			Point mousePos = getMousePosition();
			clickedItem = e.getNode();
			if (popupMenu != null) {
				popupMenu.show(this, mousePos.x, mousePos.y);
			}
		}
	}

	public void arrowClicked(LinkEvent e) {
		if(e.getMouseButton() == MouseEvent.BUTTON3)
		{
			e.getLink().setLocked(false);

			Point mousePos = getMousePosition();

			clickedItem = e.getLink();
			if(popupMenu != null)
			{
				popupMenu.show(this, mousePos.x, mousePos.y);
			}
		}
	}
}
