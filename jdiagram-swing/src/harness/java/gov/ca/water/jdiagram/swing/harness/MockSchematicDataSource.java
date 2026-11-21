package gov.ca.water.jdiagram.swing.harness;

import java.io.File;
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

import gov.ca.water.jdiagram.core.SchematicDataSource;

import hec.hecmath.HecMath;

/**
 * In-memory {@link SchematicDataSource} for the UI harness. All state is mutable so the harness
 * menus and tests can flip units, compare mode, selected studies, etc.
 */
public class MockSchematicDataSource implements SchematicDataSource
{
	public static final String SHAPE_LIB_PATH = "d:/temp/shapes.xml";
	public static final String TW_FILE = "harness_timewindows.txt";
	public static final int STUDY_COUNT = 4;

	private final SampleData sampleData;
	private final List<Throwable> errors = new CopyOnWriteArrayList<>();
	private final List<String> warnings = new CopyOnWriteArrayList<>();
	private final Map<Integer, Map<String, String>> unitsCfs = new ConcurrentHashMap<>();
	private final Map<Integer, Map<String, String>> unitsTaf = new ConcurrentHashMap<>();
	private final Map<Integer, List<Map<String, Double>>> longTermCfs = new ConcurrentHashMap<>();
	private final Map<Integer, List<Map<String, Double>>> longTermTaf = new ConcurrentHashMap<>();

	private Consumer<String> eventSink = s -> { };
	private Preferences preferences;

	private final File dataDir;

	private boolean debugging;
	private Object target;

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
	public Set<String> storageNames() {
		return SampleData.STORAGE_NAMES;
	}

	@Override
	public Optional<HecMath> series(int alt, String name) {
		return Optional.ofNullable(this.getSchematicVariableData(alt).get(name.toLowerCase()));
	}

	@Override
	public Object target() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
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
	public void reportError(Throwable t) {
		errors.add(t);
		eventSink.accept("ERROR: " + t);
	}

	@Override
	public void showWarning(String message) {
		warnings.add(message);
		eventSink.accept("WARNING: " + message);
	}
}
