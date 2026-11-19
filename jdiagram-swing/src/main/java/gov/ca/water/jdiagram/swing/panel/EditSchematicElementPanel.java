package gov.ca.water.jdiagram.swing.panel;

import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.google.common.flogger.FluentLogger;
import com.mindfusion.diagramming.DiagramItem;
import com.mindfusion.diagramming.DiagramLink;
import com.mindfusion.diagramming.Shape;
import com.mindfusion.diagramming.ShapeNode;

public class EditSchematicElementPanel extends JPanel {
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();

	private final JLabel colorLabel;
	private final JLabel shapeLabel;
	private final JComboBox<String> shapeBox;
	private final JComboBox<Color> colorBox;

	public EditSchematicElementPanel() {
		setLayout(new GridLayout(2,2));
		colorLabel = new JLabel("Color: ");
		add(colorLabel);
		colorBox = new JComboBox();
		add(colorBox);
		colorBox.addItem(Color.red);
		colorBox.addItem(Color.blue);
		colorBox.addItem(Color.green);
		colorBox.addItem(Color.magenta);
		shapeLabel = new JLabel("Shape: ");
		add(shapeLabel);
		shapeBox = new JComboBox();
		add(shapeBox);
		shapeBox.addItem("Rectangle");
		shapeBox.addItem("Circle");
		shapeBox.addItem("Triangle");
	}

	public void setDiagramItem(DiagramItem item) {
		if (item instanceof ShapeNode shapeNode) {
			shapeBox.setVisible(true);
			shapeLabel.setVisible(true);
			Shape shape = shapeNode.getShape();
			setShapeSelected(shape);
			Color color = shapeNode.getPen().getColor();
			setColorSelected(color);
		} else if (item instanceof DiagramLink link){
			shapeBox.setVisible(false);
			shapeLabel.setVisible(false);
			Color color = link.getPen().getColor();
			setColorSelected(color);
		}
	}

	private void setColorSelected(Color color) {
		colorBox.setSelectedItem(color);
	}

	private void setShapeSelected(Shape shape) {
		String id = shape.getId();
		logger.atFinest().log("Shape id: " + id);
		String shapeName = "Rectangle";
		if (id.equals("Alternative")) {
			shapeName.equals("Triangle");
		} else if (id.equals("Ellipse")) {
			shapeName.equals("Circle");
		}
		shapeBox.setSelectedItem(shapeName);
	}

	public String getShape() {
		String shapeName = (String) shapeBox.getSelectedItem();
		String shapeId = shapeName;
		if (shapeName.equals("Triangle")) {
			shapeId = "Alternative";
		} else if (shapeName.equals("Circle")) {
			shapeId = "Ellipse";
		}
		return shapeId;
	}

	public Color getColor(){
		return (Color) colorBox.getSelectedItem();
	}
}
