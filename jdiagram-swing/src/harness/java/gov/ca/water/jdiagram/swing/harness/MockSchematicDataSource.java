package gov.ca.water.jdiagram.swing.harness;

import java.io.File;
import java.time.Month;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.DiagramItem;
import com.mindfusion.pdf.PageSizesEnum;
import gov.ca.water.jdiagram.core.SchematicDataSource;

import hec.hecmath.HecMath;

/**
 * In-memory {@link SchematicDataSource} for the UI harness. All state is mutable so the harness
 * menus and tests can flip units, compare mode, selected studies, etc.
 */
public class MockSchematicDataSource implements SchematicDataSource
{
	public static final String CFS = "CFS";
	public static final String TAF = "TAF";
	public static final String BASE = "Base";
	public static final String DIFF = "Diff";
	public static final String SHAPE_LIB_PATH = "d:/temp/shapes.xml";
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

	private final boolean[] selectedStudies = {true, true, false, false};
	private final File dataDir;

	private boolean debugging;
	private String units = CFS;
	private String mode = BASE;
	private Set<Month> months = EnumSet.allOf(Month.class);
	private Object target;
	private boolean tafMonthly = true;
	private boolean zoomToRect;
	private boolean showMagnifier;
	private PageSizesEnum pageSize = PageSizesEnum.Letter;

	public MockSchematicDataSource(File dataDir, SampleData sampleData, Preferences preferences) {
		this.dataDir = dataDir;
		this.sampleData = sampleData;
		this.preferences = preferences;
		for (int i = 0; i < STUDY_COUNT; i++) {
			unitsCfs.put(i, new ConcurrentHashMap<>(sampleData.displayUnits(true)));
			unitsTaf.put(i, new ConcurrentHashMap<>(sampleData.displayUnits(false)));
		}
	}

	@Override
	public boolean getSelectedStudy(int index) {
		return index >= 0 && index < selectedStudies.length && selectedStudies[index];
	}

	public void setSelectedStudy(int index, boolean selected) {
		selectedStudies[index] = selected;
	}

	@Override
	public boolean isDebugging() {
		return debugging;
	}

	public void setDebugging(boolean debugging) {
		this.debugging = debugging;
	}

	@Override
	public String getDataDir() {
		return dataDir.getAbsolutePath();
	}

	@Override
	public String units() {
		return units;
	}

	public void setUnits(String units) {
		this.units = units;
	}

	@Override
	public Set<String> storageNames() {
		return SampleData.STORAGE_NAMES;
	}

	@Override
	public Optional<HecMath> series(int alt, String name) {
		return Optional.ofNullable(this.getSchematicVariableData(alt).get(name.toLowerCase()));
	}

	@Override
	public String mode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	@Override
	public Set<Month> months() {
		return months;
	}

	public void setMonths(Set<Month> months) {
		this.months = months;
	}

	@Override
	public Object target() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	@Override
	public String diff() {
		return DIFF;
	}

	@Override
	public boolean isTAFMonthly() {
		return tafMonthly;
	}

	@Override
	public void setTAFMonthly(boolean isTAFMonthly) {
		this.tafMonthly = isTAFMonthly;
	}

	@Override
	public String cfs() {
		return CFS;
	}

	@Override
	public boolean zoomToRect() {
		return zoomToRect;
	}

	@Override
	public void setZoomToRect(boolean zoomToRect) {
		this.zoomToRect = zoomToRect;
	}

	@Override
	public boolean showMagnifier() {
		return showMagnifier;
	}

	@Override
	public void setShowMagnifier(boolean showMagnifier) {
		this.showMagnifier = showMagnifier;
	}

	@Override
	public PageSizesEnum pageSize() {
		return pageSize;
	}

	@Override
	public void setPageSize(PageSizesEnum pageSize) {
		this.pageSize = pageSize;
	}

	@Override
	public String shapeLibPath() {
		return SHAPE_LIB_PATH;
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
