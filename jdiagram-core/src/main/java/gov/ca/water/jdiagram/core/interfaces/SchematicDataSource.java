package gov.ca.water.jdiagram.core.interfaces;

import java.time.Month;
import java.util.Optional;
import java.util.Set;

import com.mindfusion.pdf.PageSizesEnum;

import hec.hecmath.HecMath;

public interface SchematicDataSource
{
	boolean[] getSelectedStudies();

	boolean getSelectedStudy(int index);

	boolean isDebugging();

	String getDataDir();

	String units();

	Set<String> storageNames();

	Optional<HecMath> series(int alt, String name);

	String mode();

	Set<Month> months();

	Object target();

	String diff();

	boolean isTAFMonthly();

	void setTAFMonthly(boolean isTAFMonthly);

	String cfs();

	boolean zoomToRect();

	void setZoomToRect(boolean zoomToRect);

	boolean showMagnifier();

	void setShowMagnifier(boolean showMagnifier);

	PageSizesEnum pageSize();

	void setPageSize(PageSizesEnum pageSize);
}
