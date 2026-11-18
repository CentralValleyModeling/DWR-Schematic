package gov.ca.water.jdiagram.swing.harness;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.DiagramLink;
import com.mindfusion.diagramming.Shape;
import com.mindfusion.diagramming.ShapeNode;
import com.mindfusion.diagramming.XmlException;
import com.mindfusion.drawing.SolidBrush;

import hec.heclib.util.HecTime;
import hec.hecmath.HecMath;
import hec.hecmath.HecMathException;
import hec.hecmath.TimeSeriesMath;
import hec.io.TimeSeriesContainer;

/**
 * Deterministic synthetic data and a small sample schematic for the harness.
 * <p>
 * Series are monthly, end-of-month stamped, OCT1921 through SEP2015 (the CalSim period of record).
 * Flow variables are in CFS, storage variables in TAF, so with the default "CFS" display units no
 * unit conversion takes place and values are easy to predict in tests (see {@link #expectedValue}).
 */
public final class SampleData
{
	public static final int START_YEAR = 1921;
	public static final int START_MONTH = 10;
	public static final int MONTH_COUNT = 94 * 12;
	public static final int MONTHLY_INTERVAL_MINUTES = 43200;

	/** Storage variables (TAF). Keys are lower case, as SchematicPanelBase lower-cases node names. */
	public static final Set<String> STORAGE_NAMES = Set.of("s_shsta", "s_orovl", "s_folsm");

	/** Flow variables (CFS). */
	public static final List<String> FLOW_NAMES = List.of("c_kswck", "c_fthrbl", "c_nimbus", "c_sac041", "d_banks", "d_jones");

	/** Node text that is deliberately not backed by any data series. */
	public static final String UNMAPPED_NODE = "NO_DATA";

	private final Map<String, TimeSeriesContainer> templates = new LinkedHashMap<>();

	public SampleData() {
		for (String name : STORAGE_NAMES.stream().sorted().toList()) {
			templates.put(name, buildTemplate(name));
		}
		for (String name : FLOW_NAMES) {
			templates.put(name, buildTemplate(name));
		}
	}

	public Set<String> variableNames() {
		return Collections.unmodifiableSet(templates.keySet());
	}

	public static boolean isStorage(String name) {
		return STORAGE_NAMES.contains(name.toLowerCase());
	}

	/** Units of the raw series. */
	public static String nativeUnits(String name) {
		return isStorage(name) ? MockSchematicDataSource.TAF : MockSchematicDataSource.CFS;
	}

	/**
	 * Units a value will be displayed in for the given display mode: storage is always TAF, flows follow the
	 * selection.
	 */
	public Map<String, String> displayUnits(boolean cfsSelected) {
		Map<String, String> units = new LinkedHashMap<>();
		for (String name : templates.keySet()) {
			units.put(name, isStorage(name) || !cfsSelected ? MockSchematicDataSource.TAF : MockSchematicDataSource.CFS);
		}
		return units;
	}

	/**
	 * Raw value of {@code name} for study {@code alt} at {@code monthOffset} months after OCT1921.
	 */
	public static double expectedValue(String name, int alt, int monthOffset) {
		String key = name.toLowerCase();
		double base = isStorage(key) ? 2000 + Math.floorMod(key.hashCode(), 2500) : 500 + Math.floorMod(key.hashCode(), 9500);
		double seasonal = 1 + 0.4 * Math.sin(2 * Math.PI * monthOffset / 12.0);
		double trend = 1 + 0.0001 * monthOffset;
		double altFactor = 1 + 0.1 * alt;
		return Math.round(base * seasonal * trend * altFactor * 100) / 100.0;
	}

	/**
	 * @return a new {@link HecMath} wrapping a copy of the series, or null if the name is unknown
	 */
	public HecMath series(int alt, String name) {
		TimeSeriesContainer template = templates.get(name.toLowerCase());
		if (template == null) {
			return null;
		}
		TimeSeriesContainer tsc = copy(template);
		double[] values = new double[tsc.numberValues];
		for (int i = 0; i < values.length; i++) {
			values[i] = expectedValue(name, alt, i);
		}
		tsc.values = values;
		tsc.fullName = "/HARNESS/" + name.toUpperCase() + "/" + (isStorage(name) ? "STORAGE" : "FLOW") + "//1MON/ALT" + alt + "/";
		try {
			return new TimeSeriesMath(tsc);
		} catch (HecMathException e) {
			throw new IllegalStateException("Unable to wrap sample series " + name, e);
		}
	}

	private static TimeSeriesContainer buildTemplate(String name) {
		HecTime time = new HecTime("31Oct" + START_YEAR, "2400");
		int[] times = new int[MONTH_COUNT];
		for (int i = 0; i < MONTH_COUNT; i++) {
			times[i] = time.value();
			time.increment(1, MONTHLY_INTERVAL_MINUTES);
		}
		TimeSeriesContainer tsc = new TimeSeriesContainer();
		tsc.times = times;
		tsc.values = new double[MONTH_COUNT];
		tsc.numberValues = MONTH_COUNT;
		tsc.startTime = times[0];
		tsc.endTime = times[MONTH_COUNT - 1];
		tsc.interval = MONTHLY_INTERVAL_MINUTES;
		tsc.units = nativeUnits(name);
		tsc.type = isStorage(name) ? "INST-VAL" : "PER-AVER";
		return tsc;
	}

	private static TimeSeriesContainer copy(TimeSeriesContainer src) {
		TimeSeriesContainer tsc = new TimeSeriesContainer();
		tsc.times = src.times.clone();
		tsc.values = src.values.clone();
		tsc.numberValues = src.numberValues;
		tsc.startTime = src.startTime;
		tsc.endTime = src.endTime;
		tsc.interval = src.interval;
		tsc.units = src.units;
		tsc.type = src.type;
		return tsc;
	}

	/**
	 * Clears {@code diagram} and draws a small Sacramento-valley style schematic: three reservoirs feeding
	 * river reaches that end at two export pumps, plus one node with no data and one transparent label.
	 */
	public static void populate(Diagram diagram) {
		diagram.clearAll();

		ShapeNode shasta = reservoir(diagram, "S_SHSTA", 20, 10);
		ShapeNode oroville = reservoir(diagram, "S_OROVL", 110, 10);
		ShapeNode folsom = reservoir(diagram, "S_FOLSM", 200, 10);

		ShapeNode keswick = channel(diagram, "C_KSWCK", 20, 60);
		ShapeNode feather = channel(diagram, "C_FTHRBL", 110, 60);
		ShapeNode nimbus = channel(diagram, "C_NIMBUS", 200, 60);
		ShapeNode sac = channel(diagram, "C_SAC041", 110, 110);
		ShapeNode banks = channel(diagram, "D_BANKS", 60, 160);
		ShapeNode jones = channel(diagram, "D_JONES", 160, 160);
		ShapeNode unmapped = channel(diagram, UNMAPPED_NODE, 250, 110);
		unmapped.setBrush(new SolidBrush(new Color(230, 230, 230)));

		link(diagram, shasta, keswick);
		link(diagram, oroville, feather);
		link(diagram, folsom, nimbus);
		link(diagram, keswick, sac);
		link(diagram, feather, sac);
		link(diagram, nimbus, sac);
		link(diagram, sac, banks);
		link(diagram, sac, jones);

		ShapeNode title = diagram.getFactory().createShapeNode(20, -15, 220, 12);
		title.setText("jdiagram UI harness - sample schematic");
		title.setTransparent(true);
		title.setLocked(true);

		diagram.resizeToFitItems(10);
	}

	/** Writes the sample schematic to {@code file} as MindFusion XML. */
	public static void writeSampleXml(File file) throws IOException, XmlException {
		Diagram diagram = new Diagram();
		populate(diagram);
		diagram.saveToXml(file.getAbsolutePath());
	}

	private static ShapeNode reservoir(Diagram diagram, String text, double x, double y) {
		ShapeNode node = diagram.getFactory().createShapeNode(x, y, 50, 28);
		node.setShape(Shape.fromId("Ellipse"));
		node.setText(text);
		node.setBrush(new SolidBrush(new Color(173, 216, 230)));
		return node;
	}

	private static ShapeNode channel(Diagram diagram, String text, double x, double y) {
		ShapeNode node = diagram.getFactory().createShapeNode(x, y, 50, 28);
		node.setText(text);
		node.setBrush(new SolidBrush(new Color(255, 250, 205)));
		return node;
	}

	private static DiagramLink link(Diagram diagram, ShapeNode from, ShapeNode to) {
		return diagram.getFactory().createDiagramLink(from, to);
	}
}
