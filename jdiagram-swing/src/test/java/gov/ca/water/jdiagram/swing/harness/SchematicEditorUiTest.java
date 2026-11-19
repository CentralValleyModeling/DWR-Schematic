package gov.ca.water.jdiagram.swing.harness;

import java.util.List;

import com.mindfusion.diagramming.Behavior;
import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.ShapeNode;
import gov.ca.water.jdiagram.swing.views.SchematicEditor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.flushEdt;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.node;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.onEdt;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.runOnEdt;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.waitUntil;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs every {@link SchematicViewUiTest} check against {@link SchematicEditor}, plus editor-only behaviour:
 * extra toolbar actions, edit mode setup, copy, align, and delete/undo.
 * <p>
 * Import/Export Attributes are not exercised: they open a {@code JFileChooser} directly rather than going
 * through {@code DataPresenter.launchFileDialog()}, so they cannot be scripted without blocking.
 */
@Tag("ui")
class SchematicEditorUiTest extends SchematicViewUiTest
{
	@Override
	boolean editor() {
		return true;
	}

	SchematicEditor editorPanel() {
		return (SchematicEditor) schematic();
	}

	@Test
	void harnessCreatesEditor() {
		assertInstanceOf(SchematicEditor.class, schematic());
		assertTrue(harness.isEditor());
	}

	@Test
	void toolbarAddsEditorActionsAndRemovesDateCombo() throws Exception {
		List<String> names = onEdt(() -> HarnessSupport.toolbarActionNames(toolBar()));
		assertTrue(names.containsAll(List.of("Import Attributes", "Export Attributes", "Undo", "Redo", "Copy", "Paste",
				"Align Vertical", "Align Horizontal")), names::toString);
		boolean comboOnToolbar = onEdt(() -> schematic().getDateCombo().getDateList().getParent() == toolBar());
		assertFalse(comboOnToolbar, "SchematicEditor should remove the date combo from the toolbar");
	}

	@Test
	void editorIsConfiguredForEditing() throws Exception {
		assertEquals(Behavior.Modify, onEdt(() -> schematic().getDiagramView().getBehavior()));
		assertTrue(onEdt(() -> schematic().getDiagramView().getAllowInplaceEdit()));
		Diagram diagram = schematic().getDiagram();
		assertTrue(onEdt(diagram::getAlignToGrid), "load() should enable align-to-grid");
		assertTrue(onEdt(() -> diagram.getUndoManager().getUndoEnabled()), "load() should enable undo");
		assertFalse(onEdt(() -> button("Undo").isEnabled()), "Undo starts disabled");
		assertFalse(onEdt(() -> button("Redo").isEnabled()), "Redo starts disabled");
	}

	@Test
	void copyStoresSelectionOnHost() throws Exception {
		runOnEdt(() -> {
			schematic().getDiagram().getSelection().clear();
			node(schematic().getDiagram(), "S_SHSTA").setSelected(true);
		});
		click("Copy");
		flushEdt();
		assertEquals(1, harness.getCore().getDiagramItems().size(), "one node copied");
		assertInstanceOf(ShapeNode.class, harness.getCore().getDiagramItems().get(0));
		assertSame(schematic().getDiagram(), harness.getCore().getDiagram());
	}

	@Test
	void alignVerticalLinesUpNodeCenters() throws Exception {
		runOnEdt(() -> {
			Diagram d = schematic().getDiagram();
			d.getSelection().clear();
			node(d, "C_KSWCK").setSelected(true);
			node(d, "C_SAC041").setSelected(true);
		});
		click("Align Vertical");
		double topX = onEdt(() -> node(schematic().getDiagram(), "C_KSWCK").getBounds().getCenterX());
		double bottomX = onEdt(() -> node(schematic().getDiagram(), "C_SAC041").getBounds().getCenterX());
		assertEquals(topX, bottomX, 1.0, "vertical align should match the X centers");
	}

	@Test
	void deleteThenUndoRestoresNode() throws Exception {
		int before = onEdt(() -> schematic().getDiagram().getNodes().size());
		runOnEdt(() -> {
			Diagram d = schematic().getDiagram();
			d.getSelection().clear();
			node(d, "D_JONES").setSelected(true);
			editorPanel().onDelete();
		});
		assertEquals(before - 1, (int) onEdt(() -> schematic().getDiagram().getNodes().size()));

		waitUntil("Undo enabled after delete", () -> button("Undo").isEnabled());
		click("Undo");
		waitUntil("node restored by undo", () -> schematic().getDiagram().getNodes().size() == before);
		assertEquals("D_JONES", onEdt(() -> node(schematic().getDiagram(), "D_JONES").getTextToEdit()));
	}
}
