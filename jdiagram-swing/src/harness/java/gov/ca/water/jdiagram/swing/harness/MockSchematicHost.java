package gov.ca.water.jdiagram.swing.harness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.DiagramItem;
import gov.ca.water.jdiagram.core.interfaces.SchematicHost;

import hec.hecmath.HecMath;

/**
 * In-memory {@link SchematicHost} backed by {@link SampleData}. Errors and warnings are recorded so
 * tests can assert on them, and forwarded to an optional event sink (the harness log panel).
 */
public class MockSchematicHost implements SchematicHost
{
	public static final String TW_FILE = "harness_timewindows.txt";
	public static final int STUDY_COUNT = 4;

	private final SampleData sampleData;
	private final List<Throwable> errors = new CopyOnWriteArrayList<>();
	private final List<String> warnings = new CopyOnWriteArrayList<>();
	private final List<String> twSelections = new CopyOnWriteArrayList<>(List.of("Add..."));
	private final Map<Integer, Map<String, String>> unitsCfs = new ConcurrentHashMap<>();
	private final Map<Integer, Map<String, String>> unitsTaf = new ConcurrentHashMap<>();
	private final Map<Integer, List<Map<String, Double>>> longTermCfs = new ConcurrentHashMap<>();
	private final Map<Integer, List<Map<String, Double>>> longTermTaf = new ConcurrentHashMap<>();

	private Consumer<String> eventSink = s -> { };
	private Preferences preferences;
	private volatile int selectedIndex;
	private volatile String selectedDate;
	private List<String> allVariableNames = new ArrayList<>();
	private Diagram clipboardDiagram;
	private List<DiagramItem> clipboardItems;

	public MockSchematicHost(SampleData sampleData, Preferences preferences) {
		this.sampleData = sampleData;
		this.preferences = preferences;
		for (int i = 0; i < STUDY_COUNT; i++) {
			unitsCfs.put(i, new ConcurrentHashMap<>(sampleData.displayUnits(true)));
			unitsTaf.put(i, new ConcurrentHashMap<>(sampleData.displayUnits(false)));
		}
	}

	public void setEventSink(Consumer<String> eventSink) {
		this.eventSink = eventSink == null ? s -> { } : eventSink;
	}

	public List<Throwable> getErrors() {
		return errors;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	@Override
	public Preferences getPreferences() {
		return preferences;
	}

	@Override
	public void setPreferences(Preferences preferences) {
		this.preferences = preferences;
	}

	@Override
	public List<String> getSchematicTwSelections() {
		return twSelections;
	}

	@Override
	public void clearSchematicTwSelections() {
		twSelections.clear();
	}

	@Override
	public Map<String, String> getAllSchematicVariableUnitsCFS(int index) {
		return unitsCfs.computeIfAbsent(index, i -> new ConcurrentHashMap<>());
	}

	@Override
	public Map<String, String> getAllSchematicVariableUnitsTAF(int index) {
		return unitsTaf.computeIfAbsent(index, i -> new ConcurrentHashMap<>());
	}

	/**
	 * Returns a fresh map of fresh {@link HecMath} instances on every call. {@code SchematicPanelBase}
	 * converts units in place on the container it gets back, so handing out copies keeps repeated
	 * refreshes from compounding conversions.
	 */
	@Override
	public Map<String, HecMath> getSchematicVariableData(int index) {
		if (index < 0 || index >= STUDY_COUNT) {
			return new HashMap<>();
		}
		Map<String, HecMath> result = new LinkedHashMap<>();
		for (String name : sampleData.variableNames()) {
			result.put(name, sampleData.series(index, name));
		}
		return result;
	}

	@Override
	public Map<Integer, List<Map<String, Double>>> getLongTermAverageDataCFS() {
		return longTermCfs;
	}

	@Override
	public Map<Integer, List<Map<String, Double>>> getLongTermAverageDataTAF() {
		return longTermTaf;
	}

	/** Clears cached long-term averages, e.g. after changing units or selected studies. */
	public void clearLongTermAverages() {
		longTermCfs.clear();
		longTermTaf.clear();
	}

	@Override
	public String getTwFile() {
		return TW_FILE;
	}

	@Override
	public int getSelectedIndex() {
		return selectedIndex;
	}

	@Override
	public void updateSelectedIndex(int index) {
		this.selectedIndex = index;
	}

	@Override
	public String getSelectedDate() {
		return selectedDate;
	}

	@Override
	public void setSelectedDate(String date) {
		this.selectedDate = date;
		eventSink.accept("Selected date: " + date);
	}

	@Override
	public List<String> getAllSchematicVariableNames() {
		return allVariableNames;
	}

	@Override
	public void setAllSchematicVariableNames(List<String> names) {
		this.allVariableNames = names;
	}

	@Override
	public void reportError(Throwable t) {
		errors.add(t);
		eventSink.accept("ERROR: " + t);
	}

	@Override
	public void showWarning(String message) {
		warnings.add(message);
		eventSink.accept("WARNING: " + message);
	}

	@Override
	public Diagram getDiagram() {
		return clipboardDiagram;
	}

	@Override
	public void setDiagram(Diagram diagram) {
		this.clipboardDiagram = diagram;
	}

	@Override
	public List<DiagramItem> getDiagramItems() {
		return clipboardItems;
	}

	@Override
	public void setDiagramItems(List<DiagramItem> diagramItems) {
		this.clipboardItems = diagramItems;
	}
}
