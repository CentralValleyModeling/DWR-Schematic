package gov.ca.water.jdiagram.core;

import java.time.Month;
import java.util.Optional;
import java.util.Set;

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

	String cfs();
}
