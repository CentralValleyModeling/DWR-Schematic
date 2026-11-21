package gov.ca.water.jdiagram.swing.harness;

import java.awt.Color;
import java.awt.Container;
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
import java.util.Set;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import com.mindfusion.diagramming.Behavior;
import com.mindfusion.diagramming.DiagramView;
import com.mindfusion.diagramming.ShapeNode;
import gov.ca.water.jdiagram.core.SchematicContext;
import gov.ca.water.jdiagram.swing.toolbars.SearchText;
import gov.ca.water.jdiagram.swing.views.SchematicPanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.assertNoUncaught;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.awaitValueRefreshes;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.findAll;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.flushEdt;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.showingDialog;
import static gov.ca.water.jdiagram.swing.harness.UiTestSupport.valueNode;
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
		// Values are only written to visible nodes; make sure the whole sample is in view before any test runs
		runOnEdt(harness::fitDiagramToView);
		flushEdt();
		Set<String> visible = onEdt(harness::visibleNodeNames);
		assertTrue(visible.containsAll(List.of("S_SHSTA", "C_SAC041", "D_BANKS", "D_JONES")),
				() -> "Sample nodes outside the visible area after fit; visible = " + visible);
	}

	/** Waits for the value label on {@code node} for {@code studyId}; on timeout reports what was shown. */
	void waitForValue(ShapeNode node, int studyId, String expected) throws Exception {
		waitUntil("value " + expected + " on " + node.getTextToEdit() + " (study " + studyId + ")",
				() -> valueText(node, studyId).filter(expected::equals).isPresent(),
				() -> "shown = " + valueText(node, studyId).orElse("<no label>") + ", visible nodes = "
						+ harness.visibleNodeNames());
	}

	boolean editor() {
		return false;
	}

	@AfterEach
	void tearDown() throws Exception {
		if (harness != null) {
			// Let background value refreshes finish before the preference node they read is removed
			awaitValueRefreshes();
			// Dispose every window, including any dialogs the library opened
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
			assertTrue(harness.getDataSource().getErrors().isEmpty(), () -> "Host reported errors: " + harness.getDataSource().getErrors());
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

	// startup / layout

	@Test
	void toolbarContainsViewerActions() throws Exception {
		List<String> names = onEdt(() -> HarnessSupport.toolbarActionNames(toolBar()));
		assertTrue(names.containsAll(List.of("Open", "Save As", "Zoom In", "Zoom Out", "Zoom 100",
				"Zoom to Rectangular", "Magnifier", "Font", "Decimal", "TAF Type", "Backward", "Forward")), names::toString);
		assertTrue(onEdt(() -> HarnessSupport.find(toolBar(), SearchText.class, null).isPresent()), "search control on toolbar");
	}

	@Test
	void schematicContainsNoHeavyweightComponents() throws Exception {
		// A heavyweight AWT component under the diagram leaves grey boxes where lightweight popups (the date
		// combo list, editor context menus) were drawn over it
		List<String> heavyweight = onEdt(() -> findAll(harness.getContentPane(), java.awt.Component.class).stream()
				.filter(c -> !c.isLightweight())
				.map(c -> c.getClass().getName())
				.toList());
		assertTrue(heavyweight.isEmpty(), () -> "heavyweight components in the schematic: " + heavyweight);
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
			List<String> names = harness.getContext().getAllSchematicVariableNames();
			assertTrue(names.contains("C_SAC041"), () -> "collectAllSchematicVariables gave " + names);
		}
	}

	// date selection / values

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
		assertEquals(FIRST_MONTH, harness.getContext().getSelectedDateOption());

		ShapeNode sac = onEdt(() -> node(schematic().getDiagram(), "C_SAC041"));
		String alt0 = format(SampleData.expectedValue("c_sac041", 0, 0), 0, "CFS");
		String alt1 = format(SampleData.expectedValue("c_sac041", 1, 0), 0, "CFS");
		waitForValue(sac, 0, alt0);
		waitForValue(sac, 1, alt1);

		ShapeNode shasta = onEdt(() -> node(schematic().getDiagram(), "S_SHSTA"));
		String storage = format(SampleData.expectedValue("s_shsta", 0, 0), 0, "TAF");
		waitForValue(shasta, 0, storage);
	}

	@Test
	void deselectingStudyClearsItsValues() throws Exception {
		runOnEdt(() -> harness.selectDate(FIRST_MONTH));
		ShapeNode sac = onEdt(() -> node(schematic().getDiagram(), "C_SAC041"));
		String alt0 = format(SampleData.expectedValue("c_sac041", 0, 0), 0, "CFS");
		waitForValue(sac, 0, alt0);
		waitForValue(sac, 1, format(SampleData.expectedValue("c_sac041", 1, 0), 0, "CFS"));

		// Untick "Alt 1" in the harness Controls tab, as a user would
		runOnEdt(() -> findAll(harness.getContentPane(), JCheckBox.class).stream()
				.filter(b -> "Alt 1".equals(b.getText()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No Alt 1 checkbox"))
				.doClick());
		assertFalse(harness.getContext().getSelectedStudy(1));

		waitForValue(sac, 1, "");
		waitForValue(sac, 0, alt0);
	}

	@Test
	void diffModeShowsAltMinusBase() throws Exception {
		harness.getContext().setMode(SchematicContext.DIFF);
		runOnEdt(() -> harness.selectDate(FIRST_MONTH));

		ShapeNode banks = onEdt(() -> node(schematic().getDiagram(), "D_BANKS"));
		double diff = SampleData.expectedValue("d_banks", 1, 0) - SampleData.expectedValue("d_banks", 0, 0);
		String expected = format(diff, 0, "CFS");
		waitForValue(banks, 1, expected);

		// Diff labels are red; the parent node's own pen must not be recoloured with them
		Color altColor = onEdt(() -> valueNode(banks, 1).getPen().getColor());
		assertEquals(Color.red, altColor);
		Color parentColor = onEdt(() -> banks.getEffectivePen().getColor());
		assertNotEquals(Color.red, parentColor, "value label shares its pen with the parent node");
	}

	@Test
	void decimalPlacesPreferenceIsApplied() throws Exception {
		prefs.putInt("decimalPlaces", 2);
		runOnEdt(() -> harness.selectDate(FIRST_MONTH));
		ShapeNode jones = onEdt(() -> node(schematic().getDiagram(), "D_JONES"));
		String expected = format(SampleData.expectedValue("d_jones", 0, 0), 2, "CFS");
		waitForValue(jones, 0, expected);
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
		assertEquals(SECOND_MONTH, harness.getContext().getSelectedDateOption());
		click("Backward");
		assertEquals(FIRST_MONTH, onEdt(() -> schematic().getDateCombo().getDateList().getSelectedItem()));
	}

	// zoom / view tools

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
		assertTrue(harness.getContext().zoomToRect());
		assertEquals(Behavior.Custom, onEdt(() -> schematic().getDiagramView().getBehavior()));
		click("Zoom to Rectangular");
		assertFalse(harness.getContext().zoomToRect());
		assertNotEquals(Behavior.Custom, onEdt(() -> schematic().getDiagramView().getBehavior()));
	}

	@Test
	void magnifierToggles() throws Exception {
		click("Magnifier");
		assertTrue(harness.getContext().showMagnifier());
		click("Magnifier");
		assertFalse(harness.getContext().showMagnifier());
	}

	@Test
	void tafTypeTogglesMonthlyFlagAndPreference() throws Exception {
		boolean before = harness.getContext().isTAFMonthly();
		click("TAF Type");
		waitUntil("TAF monthly flag flipped", () -> harness.getContext().isTAFMonthly() != before);
		assertEquals(!before, prefs.getBoolean("ismonthlytaf", before));
	}

	@Test
	void decimalButtonOpensDialog() throws Exception {
		click("Decimal");
		waitUntil("Set Decimal Places dialog", () -> showingDialog("Set Decimal Places").isPresent());
		Dialog dialog = onEdt(() -> showingDialog("Set Decimal Places").orElseThrow());
		assertSame(harness, dialog.getOwner(), "dialog should be owned by the schematic's window");
		assertTrue(dialog.isModal());
		assertEquals(1, harness.getPresenter().countEvents("launchDialog"));
	}

	@Test
	void decimalDialogOkStoresPreference() throws Exception {
		click("Decimal");
		waitUntil("Set Decimal Places dialog", () -> showingDialog("Set Decimal Places").isPresent());
		runOnEdt(() -> {
			Dialog dialog = showingDialog("Set Decimal Places").orElseThrow();
			findAll(dialog, JTextField.class).get(0).setText("3");
			button(dialog, "OK").doClick();
		});
		waitUntil("dialog closed", () -> showingDialog("Set Decimal Places").isEmpty());
		assertEquals(3, prefs.getInt("decimalPlaces", -1));
	}

	@Test
	void addTimeWindowDialogAddsNewWindow() throws Exception {
		runOnEdt(() -> harness.selectDate("Add..."));
		waitUntil("Add Time Window dialog", () -> showingDialog("Add Time Window").isPresent());
		runOnEdt(() -> {
			Dialog dialog = showingDialog("Add Time Window").orElseThrow();
			List<JTextField> years = findAll(dialog, JTextField.class);
			years.get(0).setText("1922");
			years.get(1).setText("2003");
			button(dialog, "OK").doClick();
		});
		String added = "OCT1922 - SEP2003";
		waitUntil("dialog closed", () -> showingDialog("Add Time Window").isEmpty());
		assertTrue(harness.getContext().getSchematicTwSelections().contains(added),
				() -> "time windows: " + harness.getContext().getSchematicTwSelections());
		assertEquals(added, onEdt(() -> schematic().getDateCombo().getDateList().getSelectedItem()));
		String file = Files.readString(tempDir.resolve(MockSchematicDataSource.TW_FILE), StandardCharsets.UTF_8);
		assertTrue(file.contains(added), "time-window file should contain the new window: " + file);
	}

	@Test
	void saveAsPdfOpensOptionsDialog() throws Exception {
		File pdf = tempDir.resolve("export.pdf").toFile();
		harness.getPresenter().queueFileDialogResult(pdf.getAbsolutePath());
		click("Save As");
		waitUntil("PDF Options dialog", () -> showingDialog("PDF Options").isPresent());
		runOnEdt(() -> button(showingDialog("PDF Options").orElseThrow(), "OK").doClick());
		waitUntil("PDF written", () -> pdf.isFile() && pdf.length() > 0);
	}

	static AbstractButton button(Container root, String text) {
		return findAll(root, AbstractButton.class).stream()
				.filter(b -> text.equals(b.getText()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No button " + text));
	}

	// search / selection

	@Test
	void searchFindsNodesCaseInsensitively() throws Exception {
		assertTrue(onEdt(() -> schematic().findInView("banks")));
		assertFalse(onEdt(() -> schematic().findInView("not-a-node")));
	}

	@Test
	void searchSelectsMatchAndUpdatesProperties() throws Exception {
		search("jones");
		ShapeNode jones = onEdt(() -> node(schematic().getDiagram(), "D_JONES"));
		assertTrue(onEdt(jones::getSelected), "match should be selected");
		assertEquals(1, (int) onEdt(() -> schematic().getDiagram().getSelection().getNodes().size()));
		waitUntil("properties show the match", () -> "D_JONES".equals(harness.getPropertiesModel().getValueAt(0, 1)));
		assertTrue(onEdt(() -> schematic().getSearchText().isLastSearchFound()));
	}

	@Test
	void searchScrollsOffscreenMatchIntoView() throws Exception {
		// Zoom in until D_BANKS (bottom row) is off screen
		for (int i = 0; i < 6 && onEdt(harness::visibleNodeNames).contains("D_BANKS"); i++) {
			click("Zoom In");
			flushEdt();
		}
		assertFalse(onEdt(harness::visibleNodeNames).contains("D_BANKS"), "precondition: D_BANKS off screen");
		DiagramView view = schematic().getDiagramView();
		float zoomBefore = onEdt(view::getZoomFactor);
		String before = onEdt(() -> geometry("D_BANKS"));

		search("banks");
		waitUntil("D_BANKS scrolled into view", () -> harness.visibleNodeNames().contains("D_BANKS"),
				() -> "before search: " + before + "; after: " + geometry("D_BANKS"));
		assertEquals(zoomBefore, onEdt(view::getZoomFactor), 0.01f, "search should scroll, not change zoom");
	}

	/** Zoom, visible document rect, node bounds and visible nodes, for failure messages. Call on the EDT. */
	String geometry(String nodeText) {
		DiagramView view = schematic().getDiagramView();
		return "zoom=" + view.getZoomFactor()
				+ " visibleRect=" + view.deviceToDoc(view.getVisibleRect())
				+ " " + nodeText + "=" + node(schematic().getDiagram(), nodeText).getBounds()
				+ " diagramBounds=" + schematic().getDiagram().getBounds()
				+ " visibleNodes=" + harness.visibleNodeNames();
	}

	@Test
	void repeatedSearchCyclesThroughMatchesAndSkipsLabels() throws Exception {
		// Put value labels on the diagram; their text ("1234 CFS") must never be a search hit
		runOnEdt(() -> harness.selectDate(FIRST_MONTH));
		waitForValue(onEdt(() -> node(schematic().getDiagram(), "C_SAC041")), 0,
				format(SampleData.expectedValue("c_sac041", 0, 0), 0, "CFS"));

		Set<String> hits = new java.util.TreeSet<>();
		for (int i = 0; i < 8; i++) {
			search("s_");
			hits.add(onEdt(() -> {
				var selected = schematic().getDiagram().getSelection().getNodes();
				return selected.isEmpty() ? "<none>" : ((ShapeNode) selected.get(0)).getTextToEdit();
			}));
		}
		assertEquals(Set.of("S_SHSTA", "S_OROVL", "S_FOLSM"), hits, "should cycle through the three reservoirs");

		search("cfs");
		assertFalse(onEdt(() -> schematic().getSearchText().isLastSearchFound()), "value labels must not match");
	}

	@Test
	void unmatchedSearchMarksFieldUntilEdited() throws Exception {
		search("not-a-node");
		SearchText searchText = schematic().getSearchText();
		assertFalse(onEdt(searchText::isLastSearchFound));
		assertEquals(SearchText.NOT_FOUND, onEdt(() -> searchText.getTextField().getBackground()));

		runOnEdt(() -> searchText.getTextField().setText("banks"));
		assertNotEquals(SearchText.NOT_FOUND, onEdt(() -> searchText.getTextField().getBackground()));
	}

	/** Types {@code query} into the toolbar search field and presses Enter, as a user would. */
	void search(String query) throws Exception {
		runOnEdt(() -> {
			JTextField field = schematic().getSearchText().getTextField();
			field.setText(query);
			field.postActionEvent();
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

	// open / save

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
