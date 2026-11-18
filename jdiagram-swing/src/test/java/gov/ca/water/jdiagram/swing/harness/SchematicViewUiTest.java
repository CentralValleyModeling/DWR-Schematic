package gov.ca.water.jdiagram.swing.harness;

import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JToolBar;

import com.mindfusion.diagramming.Behavior;
import com.mindfusion.diagramming.DiagramView;
import com.mindfusion.diagramming.ShapeNode;
import gov.ca.water.jdiagram.swing.toolbars.SearchText;
import gov.ca.water.jdiagram.swing.views.SchematicPanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.assertNoUncaught;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.flushEdt;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.format;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.node;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.onEdt;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.runOnEdt;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.valueText;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.waitUntil;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Drives the read-only {@code SchematicView} through {@link HarnessFrame}. Run with
 * {@code gradlew :jdiagram-swing:uiTest}; skipped automatically on headless machines.
 * <p>
 * {@link SchematicEditorUiTest} re-runs every test here against {@code SchematicEditor} by overriding
 * {@link #editor()}, then adds editor-only checks.
 */
@Tag("ui")
class SchematicViewUiTest
{
	private static final String FIRST_MONTH = "OCT 1921";
	private static final String SECOND_MONTH = "NOV 1921";

	@TempDir
	Path tempDir;

	private final List<Throwable> uncaught = Collections.synchronizedList(new ArrayList<>());
	private Thread.UncaughtExceptionHandler previousHandler;
	Preferences prefs;
	HarnessFrame harness;

	@BeforeEach
	void setUp() throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless(), "UI tests need a display");
		previousHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> uncaught.add(e));
		prefs = Preferences.userRoot().node("gov/ca/water/jdiagram/harness-test/" + UUID.randomUUID());
		harness = onEdt(() -> {
			HarnessFrame f = HarnessFrame.create(editor(), tempDir.toFile(), prefs);
			f.setVisible(true);
			return f;
		});
		waitUntil("harness window showing", () -> harness.isShowing() && harness.getSchematic().getDiagramView().isShowing());
		flushEdt();
	}

	boolean editor() {
		return false;
	}

	@AfterEach
	void tearDown() throws Exception {
		if (harness != null) {
			// Dispose every window, including ownerless dialogs the library may have opened
			runOnEdt(() -> {
				for (Window window : Window.getWindows()) {
					window.dispose();
				}
			});
		}
		if (prefs != null) {
			HarnessFrame.removePreferences(prefs);
		}
		Thread.setDefaultUncaughtExceptionHandler(previousHandler);
		if (harness != null) {
			assertNoUncaught(uncaught);
			assertTrue(harness.getHost().getErrors().isEmpty(), () -> "Host reported errors: " + harness.getHost().getErrors());
		}
	}

	SchematicPanel schematic() {
		return harness.getSchematic();
	}

	JToolBar toolBar() {
		return harness.getPresenter().getToolBar();
	}

	AbstractButton button(String actionName) {
		return HarnessSupport.findToolbarButton(toolBar(), actionName)
				.orElseThrow(() -> new AssertionError("No toolbar action " + actionName + " in "
						+ HarnessSupport.toolbarActionNames(toolBar())));
	}

	void click(String actionName) throws Exception {
		runOnEdt(() -> button(actionName).doClick());
	}

	// ---------------------------------------------------------------- startup / layout

	@Test
	void toolbarContainsViewerActions() throws Exception {
		List<String> names = onEdt(() -> HarnessSupport.toolbarActionNames(toolBar()));
		assertTrue(names.containsAll(List.of("Open", "Save As", "Zoom In", "Zoom Out", "Zoom 100",
				"Zoom to Rectangular", "Magnifier", "Font", "Decimal", "TAF Type", "Backward", "Forward")), names::toString);
		assertTrue(onEdt(() -> HarnessSupport.find(toolBar(), SearchText.class, null).isPresent()), "search control on toolbar");
	}

	@Test
	void overviewIsCreatedAndHandedToPresenter() {
		assertNotNull(harness.getPresenter().getOverview(), "SchematicPanel should pass a non-null overview to launchViewPart");
		assertSame(schematic().getOverview(), harness.getPresenter().getOverview());
		assertSame(schematic().getDiagramView(), harness.getPresenter().getDiagramView());
		assertNotNull(schematic().getOverview().getOverview());
	}

	@Test
	void sampleDiagramIsLoaded() throws Exception {
		int nodes = onEdt(() -> schematic().getDiagram().getNodes().size());
		assertTrue(nodes >= 10, "expected sample nodes, got " + nodes);
		if (!editor()) {
			List<String> names = harness.getHost().getAllSchematicVariableNames();
			assertTrue(names.contains("C_SAC041"), () -> "collectAllSchematicVariables gave " + names);
		}
	}

	// ---------------------------------------------------------------- date selection / values

	@Test
	void dateComboListsTimeWindowsThenMonthsWithoutOpeningDialog() throws Exception {
		JComboBox<String> dates = onEdt(() -> schematic().getDateCombo().getDateList());
		assertEquals("Add...", onEdt(() -> dates.getItemAt(0)));
		assertEquals(HarnessFrame.SAMPLE_TIME_WINDOW, onEdt(() -> dates.getItemAt(1)));
		assertEquals(FIRST_MONTH, onEdt(() -> dates.getItemAt(2)));
		flushEdt();
		assertEquals(0, harness.getPresenter().countEvents("launchDialog"),
				"populating the combo must not open the Add Time Window dialog");
	}

	@Test
	void selectingMonthWritesValuesForEachSelectedStudy() throws Exception {
		runOnEdt(() -> harness.selectDate(FIRST_MONTH));
		assertEquals(FIRST_MONTH, harness.getHost().getSelectedDate());

		ShapeNode sac = onEdt(() -> node(schematic().getDiagram(), "C_SAC041"));
		String alt0 = format(SampleData.expectedValue("c_sac041", 0, 0), 0, "CFS");
		String alt1 = format(SampleData.expectedValue("c_sac041", 1, 0), 0, "CFS");
		waitUntil("alt 0 value " + alt0 + " on C_SAC041", () -> valueText(sac, 0).filter(alt0::equals).isPresent());
		waitUntil("alt 1 value " + alt1 + " on C_SAC041", () -> valueText(sac, 1).filter(alt1::equals).isPresent());

		ShapeNode shasta = onEdt(() -> node(schematic().getDiagram(), "S_SHSTA"));
		String storage = format(SampleData.expectedValue("s_shsta", 0, 0), 0, "TAF");
		waitUntil("storage value " + storage + " on S_SHSTA", () -> valueText(shasta, 0).filter(storage::equals).isPresent());
	}

	@Test
	void diffModeShowsAltMinusBase() throws Exception {
		harness.getDataSource().setMode(MockSchematicDataSource.DIFF);
		runOnEdt(() -> harness.selectDate(FIRST_MONTH));

		ShapeNode banks = onEdt(() -> node(schematic().getDiagram(), "D_BANKS"));
		double diff = SampleData.expectedValue("d_banks", 1, 0) - SampleData.expectedValue("d_banks", 0, 0);
		String expected = format(diff, 0, "CFS");
		waitUntil("diff value " + expected, () -> valueText(banks, 1).filter(expected::equals).isPresent());
	}

	@Test
	void decimalPlacesPreferenceIsApplied() throws Exception {
		prefs.putInt("decimalPlaces", 2);
		runOnEdt(() -> harness.selectDate(FIRST_MONTH));
		ShapeNode jones = onEdt(() -> node(schematic().getDiagram(), "D_JONES"));
		String expected = format(SampleData.expectedValue("d_jones", 0, 0), 2, "CFS");
		waitUntil("2-decimal value " + expected, () -> valueText(jones, 0).filter(expected::equals).isPresent());
	}

	@Test
	void selectingTimeWindowShowsLongTermAverage() throws Exception {
		runOnEdt(() -> harness.selectDate(HarnessFrame.SAMPLE_TIME_WINDOW));
		ShapeNode sac = onEdt(() -> node(schematic().getDiagram(), "C_SAC041"));
		waitUntil("long-term average on C_SAC041", () -> valueText(sac, 0).filter(s -> s.endsWith("CFS")).isPresent());

		double shown = Double.parseDouble(onEdt(() -> valueText(sac, 0).orElseThrow()).split("\\s")[0]);
		double mean = 0;
		for (int i = 0; i < SampleData.MONTH_COUNT; i++) {
			mean += SampleData.expectedValue("c_sac041", 0, i);
		}
		mean /= SampleData.MONTH_COUNT;
		assertEquals(mean, shown, mean * 0.01, "long-term average should be close to the period mean");
	}

	@Test
	void forwardAndBackwardStepThroughMonths() throws Exception {
		runOnEdt(() -> harness.selectDate(FIRST_MONTH));
		click("Forward");
		assertEquals(SECOND_MONTH, onEdt(() -> schematic().getDateCombo().getDateList().getSelectedItem()));
		assertEquals(SECOND_MONTH, harness.getHost().getSelectedDate());
		click("Backward");
		assertEquals(FIRST_MONTH, onEdt(() -> schematic().getDateCombo().getDateList().getSelectedItem()));
	}

	// ---------------------------------------------------------------- zoom / view tools

	@Test
	void zoomInAndOutChangeZoomFactor() throws Exception {
		DiagramView view = schematic().getDiagramView();
		float start = onEdt(view::getZoomFactor);
		click("Zoom In");
		float zoomedIn = onEdt(view::getZoomFactor);
		assertTrue(zoomedIn > start, "zoom in: " + start + " -> " + zoomedIn);
		click("Zoom Out");
		click("Zoom Out");
		float zoomedOut = onEdt(view::getZoomFactor);
		assertTrue(zoomedOut < zoomedIn, "zoom out: " + zoomedIn + " -> " + zoomedOut);
		click("Zoom 100");
	}

	@Test
	void zoomToRectangleTogglesCustomBehavior() throws Exception {
		click("Zoom to Rectangular");
		assertTrue(harness.getDataSource().zoomToRect());
		assertEquals(Behavior.Custom, onEdt(() -> schematic().getDiagramView().getBehavior()));
		click("Zoom to Rectangular");
		assertFalse(harness.getDataSource().zoomToRect());
		assertNotEquals(Behavior.Custom, onEdt(() -> schematic().getDiagramView().getBehavior()));
	}

	@Test
	void magnifierToggles() throws Exception {
		click("Magnifier");
		assertTrue(harness.getDataSource().showMagnifier());
		click("Magnifier");
		assertFalse(harness.getDataSource().showMagnifier());
	}

	@Test
	void tafTypeTogglesMonthlyFlagAndPreference() throws Exception {
		boolean before = harness.getDataSource().isTAFMonthly();
		click("TAF Type");
		waitUntil("TAF monthly flag flipped", () -> harness.getDataSource().isTAFMonthly() != before);
		assertEquals(!before, prefs.getBoolean("ismonthlytaf", before));
	}

	@Test
	void decimalButtonOpensDialog() throws Exception {
		click("Decimal");
		// DecimalDialog is a JDialog, so it is not in Frame.getFrames()
		waitUntil("Set Decimal Places dialog", () -> {
			for (Window w : Window.getWindows()) {
				if (w.isShowing() && w instanceof Dialog d && "Set Decimal Places".equals(d.getTitle())) {
					return true;
				}
			}
			return false;
		});
		assertEquals(1, harness.getPresenter().countEvents("launchDialog"));
	}

	// ---------------------------------------------------------------- search / selection

	@Test
	void searchFindsNodesCaseInsensitively() throws Exception {
		assertTrue(onEdt(() -> schematic().findInView("banks")));
		assertFalse(onEdt(() -> schematic().findInView("not-a-node")));
		runOnEdt(() -> {
			SearchText search = schematic().getSearchText();
			search.getTextField().setText("jones");
			search.getSearchButton().doClick();
		});
		flushEdt();
	}

	@Test
	void selectionPopulatesPropertiesModel() throws Exception {
		ShapeNode sac = onEdt(() -> node(schematic().getDiagram(), "C_SAC041"));
		schematic().getSelectionProvider().setSelection(List.of(sac));
		waitUntil("properties rows", () -> harness.getPropertiesModel().getRowCount() == 3);
		assertEquals("C_SAC041", onEdt(() -> harness.getPropertiesModel().getValueAt(0, 1)));
		String incoming = (String) onEdt(() -> harness.getPropertiesModel().getValueAt(1, 1));
		assertTrue(incoming.contains("C_KSWCK") && incoming.contains("C_FTHRBL") && incoming.contains("C_NIMBUS"), incoming);
		String outgoing = (String) onEdt(() -> harness.getPropertiesModel().getValueAt(2, 1));
		assertTrue(outgoing.contains("D_BANKS") && outgoing.contains("D_JONES"), outgoing);
	}

	// ---------------------------------------------------------------- open / save

	@Test
	void saveAsXmlThenOpenRoundTrips() throws Exception {
		File xml = tempDir.resolve("roundtrip.xml").toFile();
		int nodes = onEdt(() -> schematic().getDiagram().getNodes().size());

		harness.getPresenter().queueFileDialogResult(xml.getAbsolutePath());
		click("Save As");
		assertTrue(xml.isFile() && xml.length() > 0, "Save As should write " + xml);

		runOnEdt(() -> schematic().getDiagram().clearAll());
		assertEquals(0, (int) onEdt(() -> schematic().getDiagram().getNodes().size()));

		harness.getPresenter().queueFileDialogResult(xml.getAbsolutePath());
		click("Open");
		waitUntil("diagram reloaded", () -> schematic().getDiagram().getNodes().size() == nodes);
	}

	@Test
	void saveAsSvgExports() throws Exception {
		File svg = tempDir.resolve("export.svg").toFile();
		harness.getPresenter().queueFileDialogResult(svg.getAbsolutePath());
		click("Save As");
		assertTrue(svg.isFile(), "SVG export should write " + svg);
		String content = Files.readString(svg.toPath(), StandardCharsets.UTF_8);
		assertTrue(content.contains("<svg"), "not an SVG document");
	}

	@Test
	void cancelledFileDialogDoesNothing() throws Exception {
		int nodes = onEdt(() -> schematic().getDiagram().getNodes().size());
		harness.getPresenter().queueFileDialogResult(HarnessPresenter.NO_FILE);
		click("Open");
		flushEdt();
		assertEquals(nodes, (int) onEdt(() -> schematic().getDiagram().getNodes().size()));
	}
}
