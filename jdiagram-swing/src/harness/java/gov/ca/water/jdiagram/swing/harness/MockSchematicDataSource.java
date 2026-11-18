package gov.ca.water.jdiagram.swing.harness;

import java.io.File;
import java.time.Month;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import com.mindfusion.pdf.PageSizesEnum;
import gov.ca.water.jdiagram.core.interfaces.SchematicDataSource;

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

	private final boolean[] selectedStudies = {true, true, false, false};
	private final File dataDir;
	private final MockSchematicHost host;

	private boolean debugging;
	private String units = CFS;
	private String mode = BASE;
	private Set<Month> months = EnumSet.allOf(Month.class);
	private Object target;
	private boolean tafMonthly = true;
	private boolean zoomToRect;
	private boolean showMagnifier;
	private PageSizesEnum pageSize = PageSizesEnum.Letter;

	public MockSchematicDataSource(File dataDir, MockSchematicHost host) {
		this.dataDir = dataDir;
		this.host = host;
	}

	@Override
	public boolean[] getSelectedStudies() {
		return selectedStudies;
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
		return Optional.ofNullable(host.getSchematicVariableData(alt).get(name.toLowerCase()));
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
}
