package gov.ca.water.jdiagram.core.utils;

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import mil.army.usace.hec.metadata.Interval;
import mil.army.usace.hec.metadata.IntervalFactory;

import hec.heclib.dss.CondensedReference;
import hec.heclib.dss.HecDSSUtilities;
import hec.heclib.dss.HecDss;

public class TimeOps
{
	TimeOps() {
		throw new IllegalStateException("TimeOps is a static class and cannot be instantiated");
	}

	public static Vector<CondensedReference> getCondensedReferences(HecDss hecDss)
	{
		HecDSSUtilities dataManager = (HecDSSUtilities)hecDss.getDataManager().dataManager();
		String[] sortedCatalog = dataManager.getCatalog(true, null);
		boolean queryTimes = dataManager.getDssFileVersion() == 7 || sortedCatalog.length < 5000;
		return dataManager.getCondensedCatalog(sortedCatalog, queryTimes);
	}

	public static boolean isLeapYear(int year) {
		if (year % 4 == 0) {
			if (year % 100 != 0) {
				return true;
			} else {
				return year % 400 == 0;
			}
		} else {
			return false;
		}
	}

	public static int numberOfDays(int month, int year) {
		int days;
		if (month != 1 && month != 3 && month != 5 && month != 7 && month != 8 && month != 10 && month != 12) {
			if (month != 4 && month != 6 && month != 9 && month != 11) {
				if (isLeapYear(year)) {
					days = 29;
				} else {
					days = 28;
				}
			} else {
				days = 30;
			}
		} else {
			days = 31;
		}

		return days;
	}

	public static int[] searchYearMonth(int noOfMonths, int startYear, int startMonth) {
		int detYear = noOfMonths / 12;
		int detMonth = noOfMonths % 12;
		int debugMonth = startMonth + detMonth;
		int debugYear = startYear + detYear;
		if (debugMonth > 12) {
			debugMonth -= 12;
			++debugYear;
		}

		return new int[]{debugYear, debugMonth};
	}

	public static int findMonthInBetween(int startYear, int startMonth, int endYear, int endMonth) {
		return (endYear - startYear) * 12 + endMonth - startMonth + 1;
	}

	public static int monthValue(String month) {
		if (month.equalsIgnoreCase("jan")) {
			return 1;
		} else if (month.equalsIgnoreCase("feb")) {
			return 2;
		} else if (month.equalsIgnoreCase("mar")) {
			return 3;
		} else if (month.equalsIgnoreCase("apr")) {
			return 4;
		} else if (month.equalsIgnoreCase("may")) {
			return 5;
		} else if (month.equalsIgnoreCase("jun")) {
			return 6;
		} else if (month.equalsIgnoreCase("jul")) {
			return 7;
		} else if (month.equalsIgnoreCase("aug")) {
			return 8;
		} else if (month.equalsIgnoreCase("sep")) {
			return 9;
		} else if (month.equalsIgnoreCase("oct")) {
			return 10;
		} else {
			return month.equalsIgnoreCase("nov") ? 11 : 12;
		}
	}

	public static String createTimewindow(int year, int month, int day, String timestep) {
		String monthStr = getMonthText(month);
		if (isMonthlyInterval(timestep)) {
			int lastDay = numberOfDays(month, year);
			return lastDay + monthStr + year + " 2300 " + lastDay + monthStr + year + " 2400";
		} else if (day < 10) {
			String dayStr = "0" + day;
			return dayStr + monthStr + year + " 2300 " + dayStr + monthStr + year + " 2400";
		} else {
			return day + monthStr + year + " 2300 " + day + monthStr + year + " 2400";
		}
	}

	public static String createStartTime(String detailDate, String timestep) {
		String[] date = detailDate.split("-");
		return createStartTime(Integer.parseInt(date[2]), Integer.parseInt(date[0]), Integer.parseInt(date[1]), timestep);
	}

	public static String createEndTime(String detailDate, String timestep) {
		String[] date = detailDate.split("-");
		return createEndTime(Integer.parseInt(date[2]), Integer.parseInt(date[0]), Integer.parseInt(date[1]), timestep);
	}

	public static String createStartTime(int year, int month, int day, String timestep) {
		String monthStr = getMonthText(month);
		if (isMonthlyInterval(timestep)) {
			int lastDay = numberOfDays(month, year);
			return lastDay + monthStr + year + " 2300";
		} else if (day < 10) {
			String dayStr = "0" + day;
			return dayStr + monthStr + year + " 2300";
		} else {
			return day + monthStr + year + " 2300";
		}
	}

	public static String createEndTime(int year, int month, int day, String timestep) {
		String monthStr = getMonthText(month);
		if (isMonthlyInterval(timestep)) {
			int lastDay = numberOfDays(month, year);
			return lastDay + monthStr + year + " 2400";
		} else if (day < 10) {
			String dayStr = "0" + day;
			return dayStr + monthStr + year + " 2400";
		} else {
			return day + monthStr + year + " 2400";
		}
	}

	public static String createEndTime1(int year, int month, int day, String timestep) {
		String monthStr = getMonthText(month);
		if (isMonthlyInterval(timestep)) {
			int lastDay = numberOfDays(month, year);
			return lastDay + monthStr + year + " 2400";
		} else if (day < 10) {
			String dayStr = "0" + day;
			return dayStr + monthStr + year + " 0000";
		} else {
			return day + monthStr + year + " 0000";
		}
	}

	public static String getMonthText(int month) {
		switch (month) {
			case 1 -> {
				return "JAN";
			}
			case 2 -> {
				return "FEB";
			}
			case 3 -> {
				return "MAR";
			}
			case 4 -> {
				return "APR";
			}
			case 5 -> {
				return "MAY";
			}
			case 6 -> {
				return "JUN";
			}
			case 7 -> {
				return "JUL";
			}
			case 8 -> {
				return "AUG";
			}
			case 9 -> {
				return "SEP";
			}
			case 10 -> {
				return "OCT";
			}
			case 11 -> {
				return "NOV";
			}
			default -> {
				return "DEC";
			}
		}
	}

	public static int diffInDay(int yearA, int monthA, int dayA, int yearB, int monthB, int dayB) {
		Date dateA = new Date(yearA - 1900, monthA - 1, dayA);
		Date dateB = new Date(yearB - 1900, monthB - 1, dayB);
		Calendar c1 = Calendar.getInstance();
		c1.setTime(dateA);
		Calendar c2 = Calendar.getInstance();
		c2.setTime(dateB);
		return (int) Duration.between(c1.toInstant(), c2.toInstant()).toDays();
	}

	public static String getModMonthText(int month) {
		return month < 10 ? "0" + month : "" + month;
	}

	public static String getModDayhText(int day) {
		return day < 10 ? "0" + day : "" + day;
	}

	public static int getNumberOfTimestep(Date dateA, Date dateB, String timeStep) {
		if (isMonthlyInterval(timeStep)) {
			int monthA = dateA.getMonth();
			int yearA = dateA.getYear();
			int monthB = dateB.getMonth();
			int yearB = dateB.getYear();
			int diff = (yearB - yearA) * 12 + (monthB - monthA) + 1;
			if (diff <= 0) {
				diff = 0;
			}

			return diff;
		} else {
			Calendar c1 = Calendar.getInstance();
			c1.setTime(dateA);
			Calendar c2 = Calendar.getInstance();
			c2.setTime(dateB);
			int diff = (int)Duration.between(c1.toInstant(), c2.toInstant()).toDays() + 1;
			if (diff <= 0) {
				diff = 0;
			}

			return diff;
		}
	}

	public static boolean isMonthlyInterval(String intervalName) {
		return IntervalFactory.findAllDss(IntervalFactory.equalsName(intervalName)).stream().anyMatch(Interval::isMonthly);
	}
}
