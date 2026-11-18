package gov.ca.water.jdiagram.core;

import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.google.common.flogger.FluentLogger;
import gov.ca.water.jdiagram.core.interfaces.SchematicDataSource;
import gov.ca.water.jdiagram.core.interfaces.SchematicHost;
import gov.ca.water.jdiagram.core.utils.TimeOps;

import hec.heclib.dss.CondensedReference;
import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecDss;
import hec.heclib.util.HecTime;
import hec.hecmath.HecMath;
import hec.hecmath.HecMathException;
import hec.io.TimeSeriesContainer;

public class SchematicPanelBase
{
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();

	private static final double FACTOR = 1000. * 43560 / (24 * 60 * 60);

	private int baseIndex = 0;

	private final SchematicDataSource dataSource;

	private final SchematicHost host;

	public SchematicPanelBase(SchematicDataSource source, SchematicHost host) {
		this.dataSource = source;
		this.host = host;
	}

	public SchematicHost getHost() {
		return host;
	}

	public SchematicDataSource getDataSource() {
		return dataSource;
	}

	public Map<String, String>[] retrieveUndebug(String date, Map<String, Object> names) {
		List<String> tws = host.getSchematicTwSelections();
		for (int i = 0; i < tws.size() - 1; i++) {
			if (date != null && date.equals(tws.get(i + 1))) {
				if (dataSource.units().equals(dataSource.cfs())) {
					if (dataSource.months().size() < 12) {
						return retrieveLongTermAverageSelectedMonths(date, names, true);
					} else {
						if (!host.getLongTermAverageDataCFS().containsKey(i)) {
							calculateLongTermAverage(i, date, true);
						}
						return retrieveLongTermAverage(i, names, true);
					}
				} else {
					if (dataSource.months().size() < 12){
						return retrieveLongTermAverageSelectedMonths(date, names, false);
					}else{
						if (!host.getLongTermAverageDataTAF().containsKey(i)) {
							calculateLongTermAverage(i, date, false);
						}
						return retrieveLongTermAverage(i, names, false);
					}
				}
			}
		}

		Hashtable<String, String>[] results = new Hashtable[4];

		for (int i = 0; i < 4; ++i) {
			results[i] = new Hashtable<>();
		}

		baseIndex = 5;

		for (int k = 0; k < 4; k++) {
			if (dataSource.getSelectedStudy(k)) {
				if (baseIndex > k){
					baseIndex = k;
				}

				Set<String> variableEnum = names.keySet();
				for (String name : variableEnum) {
					name = name.toLowerCase();
					boolean isStorage = false;
					String value = "";
					if (host.getSchematicVariableData(k).containsKey(name)) {
						try {
							HecMath dataSet = host.getSchematicVariableData(k).get(name);
							if (dataSource.storageNames().contains(name)) {
								isStorage = true;
							}
							TimeSeriesContainer tsc = (TimeSeriesContainer) dataSet.getData();
							if (tsc != null) {
								boolean isTAFSelected = dataSource.units().equalsIgnoreCase("taf");
								tsc = unitsConversion(isStorage, tsc, isTAFSelected, k, name);
								if (dataSource.units().equals("TAF") && tsc.units.equals("TAF") && !getDataSource().isTAFMonthly()) {
									tsc = convertAnnualTAFData(tsc); // CB added constant IV
								}
								HecTime hecStartTime = new HecTime();
								hecStartTime.set(tsc.startTime);
								HecTime ht = new HecTime();

								ht.setDate(date);
								int valueIndex = (ht.year() - hecStartTime.year())
										* 12 + (ht.month() - hecStartTime.month());
								if (valueIndex < 0 || valueIndex > tsc.values.length - 1) {
									value = "N/A";
								} else {
									Double value_temp = tsc.values[valueIndex];
									if (Math.abs(value_temp) > 100000000) {//empty value
										value = "N/A";
									} else {
										value = tsc.values[valueIndex] + " " + tsc.units;
									}
								}

								if (dataSource.mode().equals(dataSource.diff())) {
									if (k > baseIndex) {
										if (results[baseIndex] == null || results[baseIndex].get(name) == null) {
											results[k].put(name, "N/A");
											continue;
										}
										String[] baseFields = results[baseIndex].get(name).split("\\s");
										String[] altFields = value.split("\\s");
										try {
											double baseVal = Double.parseDouble(baseFields[0]);
											double altVal = Double.parseDouble(altFields[0]);
											results[k].put(name, (altVal - baseVal) + " " + baseFields[1]);
										} catch (NumberFormatException nfe) {
											logger.atFiner().withCause(nfe).log("Number format exception encountered");
											results[k].put(name,"N/A");
										}
									} else {
										results[k].put(name, value);
									}
								} else {
									results[k].put(name, value);
								}
							} else{
								results[k].put(name, value);
							}
						} catch (Exception e) {
							logger.atFiner().withCause(e).log("Exception encountered");
							results[k].put(name, value);
						}
					}
				}
			}
		}
		return results;
	}

	public void calculateLongTermAverage(int pi, String date, boolean isCFS) {

		List<Map<String, Double>> termAverage= new ArrayList<>();
		if (isCFS) {
			host.getLongTermAverageDataCFS().put(pi, termAverage);
		} else {
			host.getLongTermAverageDataTAF().put(pi, termAverage);
		}

		int month = -1;
		int year = -1;

		HecTime startDate = new HecTime();
		HecTime endDate = new HecTime();

		String[] split = date.split(" - ");
		startDate.setDate(split[0]);
		startDate.setTime("0100");

		endDate.setDate(split[1]);
		endDate.setTime("2400");
		month = endDate.month() + 1;
		year = endDate.year();
		if (month == 13) {
			year = startDate.year() + 1;
		}
		String monthName = TimeOps.getMonthText(month);
		endDate.setDate(monthName + year);
		endDate.add(-1440);

		HecTime ht = new HecTime();
		HecTime hecStartTime = new HecTime();
		HecTime hecEndTime = new HecTime();

		for (int i = 0; i < 4; i++){
			Map<String, Double> altAverage = new HashMap<>();
			termAverage.add(altAverage);

			Map<String, HecMath> altSchematicVariableData = host.getSchematicVariableData(i);
			if (altSchematicVariableData != null){
				Set<String> keys = altSchematicVariableData.keySet();
				Iterator<String> it = keys.iterator();
				while (it.hasNext()) {
					String name = it.next();
					HecMath dataSet = altSchematicVariableData.get(name);
					boolean isStorage = false;
					try {
						TimeSeriesContainer tsc = (TimeSeriesContainer) dataSet.getData();
						if (dataSource.storageNames().contains(name)) {
							isStorage = true;
						}
						boolean isTAFSelected = dataSource.units().equalsIgnoreCase("taf");
						tsc = unitsConversion(isStorage, tsc, isTAFSelected, i, name);
						ht.set(startDate);
						hecStartTime.set(tsc.startTime);
						hecEndTime.set(tsc.endTime);
						int count = 0;
						double sum = 0.0;
						boolean inRange = false;
						while (ht.compareTimes(hecEndTime) <= 0 && ht.compareTimes(endDate) <= 0){
							if (ht.compareTimes(hecStartTime) >= 0){
								int valueIndex = (ht.year() - hecStartTime
										.year())
										* 12
										+ (ht.month() - hecStartTime
										.month());
								sum += tsc.values[valueIndex];
								count++;
								inRange = true;
							}
							month = ht.month() + 1;
							if (month == 13) {
								month = 1;
								year = ht.year() + 1;
							} else {
								year = ht.year();
							}
							ht.setDate(TimeOps.getMonthText(month)
									+ year);
						}
						if (inRange) {
							altAverage.put(name, sum/count);
						}
					} catch (HecMathException e) {
						logger.atSevere().log("Error calculating long term average for %s", name);
					}
				}
			}
		}
	}

	public List<Map<String, Double>> calculateLongTermAverageSelectedMonths(String date, Map<String, Object> names, boolean isCFS) {
		List<Map<String, Double>> termAverage= new ArrayList<>();

		int month = -1;
		int year = -1;

		HecTime startDate = new HecTime();
		HecTime endDate = new HecTime();

		String[] split = date.split(" - ");
		startDate.setDate(split[0]);
		startDate.setTime("0100");

		endDate.setDate(split[1]);
		endDate.setTime("2400");
		month = endDate.month() + 1;
		year = endDate.year();
		if (month == 13) {
			year = startDate.year() + 1;
		}
		String monthName = TimeOps.getMonthText(month);
		endDate.setDate(monthName + year);
		endDate.add(-1440);

		HecTime ht = new HecTime();
		HecTime hecStartTime = new HecTime();
		HecTime hecEndTime = new HecTime();

		for (int i = 0; i < 4; i++){
			Map<String, Double> altAverage = new HashMap<>();
			termAverage.add(altAverage);

			Map<String, HecMath> altSchematicVariableData = host.getSchematicVariableData(i);
			Set<String> keys = names.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()){
				String name = it.next().toLowerCase();
				if (altSchematicVariableData.containsKey(name)){
					HecMath dataSet=altSchematicVariableData.get(name);
					boolean isStorage = false;
					try {
						TimeSeriesContainer tsc = (TimeSeriesContainer) dataSet.getData();
						if (dataSource.storageNames().contains(name)) {
							isStorage = true;
						}
						boolean isTAFSelected = dataSource.units().equalsIgnoreCase("taf");
						tsc = unitsConversion(isStorage, tsc, isTAFSelected, i, name);
						ht.set(startDate);
						hecStartTime.set(tsc.startTime);
						hecEndTime.set(tsc.endTime);
						int count = 0;
						double sum = 0.0;
						boolean inRange = false;
						while (ht.compareTimes(hecEndTime) <= 0 && ht.compareTimes(endDate) <= 0) {
							if (ht.compareTimes(hecStartTime) >= 0 && dataSource.months().contains(Month.of(ht.month()))) {
								int valueIndex = (ht.year() - hecStartTime
										.year())
										* 12
										+ (ht.month() - hecStartTime
										.month());
								sum += tsc.values[valueIndex];
								count++;
								inRange = true;
							}
							month = ht.month() + 1;
							if (month == 13) {
								month = 1;
								year = ht.year() + 1;
							} else {
								year = ht.year();
							}
							ht.setDate(TimeOps.getMonthText(month)
									+ year);
						}
						if (inRange) {
							altAverage.put(name, sum/count);
						}
					} catch (HecMathException e) {
						logger.atSevere().log("Error calculating long term average for " + name, e);
					}
				}
			}
		}

		return termAverage;
	}

	public Map<String, String>[] retrieveLongTermAverage(int index, Map<String, Object> names, boolean isCFS) {

		Map<String, String>[] results = new Hashtable[4];

		for (int i = 0; i < 4; ++i) {
			results[i] = new Hashtable<>();
		}

		int baseIndex = 5;

		List<Map<String, Double>> termAverage;
		if (isCFS){
			termAverage = host.getLongTermAverageDataCFS().get(index);
		}else{
			termAverage = host.getLongTermAverageDataTAF().get(index);
		}

		for (int k = 0; k < 4; k++) {
			if (dataSource.getSelectedStudy(k)) {
				if (baseIndex > k){
					baseIndex = k;
				}
				Set<String> variableEnum = names.keySet();
				for (String name : variableEnum) {
					name = name.toLowerCase();
					String value = "";
					Map<String, Double> altAverage = termAverage.get(k);
					if (altAverage.containsKey(name)) {
						double data = altAverage.get(name);
						String units;
						if (isCFS) {
							units = host.getAllSchematicVariableUnitsCFS(k).get(name);
						} else {
							units = host.getAllSchematicVariableUnitsTAF(k).get(name);
						}
						if (data > 100000000) {
							value = "N/A";
						} else {
							value = data + " " + units;
							if (dataSource.units().equals("TAF") && !dataSource.isTAFMonthly()){
								value = data * 12 + " " + units;
							}
						}
						if (dataSource.mode().equals(dataSource.diff())) {
							if (k > baseIndex) {
								if (results[baseIndex] == null || results[baseIndex].get(name) == null) {
									results[k].put(name, "N/A");
									continue;
								}
								String[] baseFields = results[baseIndex].get(name)
										.split("\\s");
								String[] altFields = value.split("\\s");
								try {
									double baseVal = Double.parseDouble(baseFields[0]);
									double altVal = Double.parseDouble(altFields[0]);
									if (dataSource.units().equals("TAF") && !dataSource.isTAFMonthly()) {
										baseVal = baseVal * 12;
										altVal = altVal * 12;
									}
									results[k].put(name, (altVal - baseVal) + " " + baseFields[1]);
								} catch (NumberFormatException nfe) {
									results[k].put(name,"N/A");
								}
							} else {
								results[k].put(name, value);
							}
						} else {
							results[k].put(name, value);
						}
					} else{
						value = "";
						results[k].put(name, value);
					}
				}
			}
		}
		return results;
	}

	public Map<String, String>[] retrieveLongTermAverageSelectedMonths(String date, Map<String, Object> names, boolean isCFS) {

		Map<String, String>[] results = new Hashtable[4];

		for (int i = 0; i < 4; ++i) {
			results[i] = new Hashtable<>();
		}

		int baseIndex = 5;

		List<Map<String, Double>> termAverage = calculateLongTermAverageSelectedMonths(date, names, isCFS);

		for (int k = 0; k < 4; k++) {
			if (dataSource.getSelectedStudy(k)){
				if (baseIndex > k){
					baseIndex = k;
				}
				Map<String, Double> altAverage = termAverage.get(k);
				Set<String> variableEnum = names.keySet();
				for (String name : variableEnum) {
					name = name.toLowerCase();
					String value = "";
					if (altAverage.containsKey(name)) {
						double data = altAverage.get(name);
						String units;
						if (isCFS) {
							units = host.getAllSchematicVariableUnitsCFS(k).get(name);
						} else {
							units = host.getAllSchematicVariableUnitsTAF(k).get(name);
						}
						if (data > 100000000) {
							value = "N/A";
						} else {
							value = data + " " + units;
							if (dataSource.units().equals("TAF") && !dataSource.isTAFMonthly()){
								value = data * 12 + " " + units;
							}
						}
						if (dataSource.mode().equals(dataSource.diff())) {
							if (k > baseIndex) {
								if (results[baseIndex] == null || results[baseIndex].get(name) == null) {
									results[k].put(name, "N/A");
									continue;
								}
								String[] baseFields = results[baseIndex].get(name)
										.split("\\s");
								String[] altFields = value.split("\\s");
								try {
									double baseVal = Double.parseDouble(baseFields[0]);
									double altVal = Double.parseDouble(altFields[0]);
									if (dataSource.units().equals("TAF") && !dataSource.isTAFMonthly()) {
										baseVal = baseVal * 12;
										altVal = altVal * 12;
									}
									results[k].put(name, (altVal - baseVal) + " " + baseFields[1]);
								} catch (NumberFormatException nfe) {
									logger.atFiner().withCause(nfe).log("Number format exception encountered");
									results[k].put(name, "N/A");
								}
							} else {
								results[k].put(name, value);
							}
						} else {
							results[k].put(name, value);
						}
					} else {
						value = "";
						results[k].put(name, value);
					}
				}
			}
		}
		return results;
	}

	public Map<String, String>[] retrieveDebug(String date, Map<String, Object> names) {
		Map<String, String>[] results = new Hashtable[5];

		for (int i = 0; i < 5; ++i) {
			results[i] = new Hashtable<>();
		}

		baseIndex = 0;

		Set<String> variableEnum = names.keySet();
		for (int k = 0; k < 4; k++) {
			if (dataSource.getSelectedStudy(k)){

				for (String name : variableEnum) {
					name = name.toLowerCase();
					boolean isStorage = false;
					String value = "";
					if (host.getSchematicVariableData(k).containsKey(name)){
						try {
							HecMath dataSet = host.getSchematicVariableData(k).get(name);
							if (dataSet != null) {
								if (dataSource.storageNames().contains(name)) {
									isStorage = true;
								}
								TimeSeriesContainer tsc = (TimeSeriesContainer) dataSet.getData();
								boolean isTAFSelected = dataSource.units().equalsIgnoreCase("taf");
								tsc = unitsConversion(isStorage, tsc, isTAFSelected, k, name);
								if (dataSource.units().equals("TAF") && tsc.units.equals("TAF") && !dataSource.isTAFMonthly()) {
									tsc = convertAnnualTAFData(tsc); // CB added constant IV
								}
								HecTime hecStartTime = new HecTime();
								hecStartTime.set(tsc.startTime);
								HecTime ht = new HecTime();

								ht.setDate(date);
								int valueIndex = (ht.year() - hecStartTime.year())
										* 12 + (ht.month() - hecStartTime.month());
								if (valueIndex < 0 || valueIndex > tsc.values.length - 1) {
									value = "N/A";
								} else {
									Double value_temp = tsc.values[valueIndex];
									if (Math.abs(value_temp) > 100000000) {//empty value
										value = "N/A";
									} else {
										value = tsc.values[valueIndex] + " " + tsc.units;
									}
								}

								if (dataSource.mode().equals(dataSource.diff())) {
									if (k > baseIndex) {
										if (results[baseIndex] == null || results[baseIndex].get(name) == null) {
											results[k].put(name, "N/A");
											continue;
										}
										String[] baseFields = results[baseIndex].get(name).split("\\s");
										String[] altFields = value.split("\\s");
										try {
											double baseVal = Double.parseDouble(baseFields[0]);
											double altVal = Double.parseDouble(altFields[0]);
											results[k].put(name, (altVal - baseVal) + " " + baseFields[1]);
										} catch (NumberFormatException nfe) {
											logger.atFiner().withCause(nfe).log("Number format error encountered");
											results[k].put(name,"N/A");
										}
									} else {
										results[k].put(name, value);
									}
								} else {
									results[k].put(name, value);
								}
							} else {
								results[k].put(name, value);
							}
						} catch (Exception e) {
							logger.atSevere().withCause(e).log("Error encountered");
							host.reportError(e);
						}
					}
				}
			}
		}
		return results;
	}

	public TimeSeriesContainer unitsConversion(boolean isStorage, TimeSeriesContainer dataSet, boolean force, int index, String name) {

		if ((dataSource.units().equals("TAF") || force || isStorage)
				&& dataSet.units.equals("CFS")) {
			dataSet = adjustMonthlyData(dataSet, true); // CB added constant IV FACTOR
			// so do not need to pass a
			// factor
			dataSet.units = "TAF";
			host.getAllSchematicVariableUnitsTAF(index).put(name, "TAF");

		} else if (dataSource.units().equals("CFS") && dataSet.units.equals("TAF")
				&& !force && !isStorage) { // CB added section
			dataSet = adjustMonthlyData(dataSet, false); // CB added constant IV
			// FACTOR so do not
			// need to pass a
			// factor
			dataSet.units = "CFS";
			host.getAllSchematicVariableUnitsCFS(index).put(name, "CFS");
		}
		return dataSet;
	}

	public TimeSeriesContainer adjustMonthlyData(TimeSeriesContainer tsc, boolean isCFStoTAF) {
		HecTime ht = null;
		try {
			double[] nvalues = new double[tsc.values.length];
			int[] times = tsc.times;
			int ndays = 0;
			ht = new HecTime();
			for (int i = 0; i < times.length; i++) {
				ht.set(times[i]);
				// FIX: subtract 1 min
				ht.add(-1);
				ndays = ht.day();
				/*
				 * HecTime BUG: sometimes ht.day() returns a day later than *
				 * should???? I wasted a lot of time on this!!
				 *
				 * returns ht.day() == 1, ht.hour() == 0 AS OPPOSED TO ht.day()
				 * == 28,30,31 (depending on month), ht.hour() == 24
				 */

				// if (DEBUG) System.out.println(ht.year()+" "+ht.month()+
				// " "+ht.day()+ht.hour()+" "+ht.minute()+" "+ht.second()+" "+times[i]+"
				// "+values[i]);
				// CB nvalues[i] = tsc.values[i] * ndays * factor;
				if (isCFStoTAF) {
					nvalues[i] = tsc.values[i] * ndays / FACTOR;
				} else {
					nvalues[i] = tsc.values[i] / ndays * FACTOR;
				}
			}
			tsc.values = nvalues;
		} catch (Exception e) {
			logger.atFine().withCause(e).log("Error encountered in adjustMonthlyData method");
		}
		return tsc;
	}

	public TimeSeriesContainer convertAnnualTAFData(TimeSeriesContainer tsc) {
		HecTime ht;
		try {
			double[] nvalues = new double[tsc.values.length];
			int[] times = tsc.times;
			ht = new HecTime();
			for (int i = 0; i < times.length; i++) {
				ht.set(times[i]);
				ht.add(-1);
				nvalues[i] = tsc.values[i] * 12;
			}
			tsc.values = nvalues;
		} catch (Exception e) {
			logger.atFine().withCause(e).log("Error encountered in convertAnnualTAFData method");
		}
		return tsc;
	}

	public static Map<String, String> generatePathnameMap(HecDss file){
		Map<String, String> pathnameMap = new HashMap<> ();
		Vector<CondensedReference> v = TimeOps.getCondensedReferences(file);
		for (int i = 0; i < v.size(); i++){
			CondensedReference cr = v.get(i);
			String pathname = cr.getNominalPathname();
			DSSPathname dssPathname = new DSSPathname(pathname);
			String partB = dssPathname.bPart();
			pathnameMap.put(partB, pathname);
		}
		return pathnameMap;
	}

	public String truncateAfterDecimal(String value, int i,
			boolean displayUnits) {
		if (value == null) {
			return null;
		}
		String[] fields = value.split("\\s");
		if (fields.length < 2) {
			return value;
		}
		return String.format("%." + i + "f %s", Double.parseDouble(fields[0]),
				displayUnits ? fields[1] : "");
	}

	public String getToolTip(int index, int studyId, boolean isDiff) {
		if (index == 0) {
			if (studyId == baseIndex) {
				return "Alt " + studyId;
			} else if (studyId > baseIndex) {
				if (isDiff) {
					return "Alt " + studyId + " - Alt " + baseIndex;
				} else {
					return "Alt " + studyId;
				}
			} else {
				return "";
			}
		} else {
			switch (studyId) {
				case 0:
					return "Debug";
				case 1:
					return isDiff ? "Alt 1 - Debug" : "Alt 1";
				case 2:
					return isDiff ? "Alt 2 - Debug" : "Alt 2";
				case 3:
					return isDiff ? "Alt 3 - Debug" : "Alt 3";
				case 4:
					return isDiff ? "Alt 4 - Debug" : "Alt 4";
			}
			return "";
		}
	}

	public void loadAllSchematicVariableData(){
		// TODO: update once schematic data source is available
	}
}
