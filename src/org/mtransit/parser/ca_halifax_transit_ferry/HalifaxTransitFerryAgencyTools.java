package org.mtransit.parser.ca_halifax_transit_ferry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTripStop;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

// http://www.halifax.ca/opendata/
// http://www.halifax.ca/opendata/transit.php
// http://gtfs.halifax.ca/static/google_transit.zip
public class HalifaxTransitFerryAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-halifax-transit-ferry-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new HalifaxTransitFerryAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating Halifax Transit ferry data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating Halifax Transit ferry data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_FERRY;
	}

	private static final long RID_ALD = 100001l;
	private static final long RID_WS = 100002l;

	private static final String FER_D = "FerD";
	private static final String FER_W = "FerW";

	@Override
	public long getRouteId(GRoute gRoute) {
		if (FER_D.equals(gRoute.getRouteShortName())) {
			return RID_ALD;
		} else if (FER_W.equals(gRoute.getRouteShortName())) {
			return RID_WS;
		}
		System.out.printf("\nUnexpected route ID  for %s %s!\n", gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final String RLN_ALD = "Alderney";
	private static final String RLN_WS = "Woodside";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (FER_D.equals(gRoute.getRouteShortName())) {
			return RLN_ALD;
		} else if (FER_W.equals(gRoute.getRouteShortName())) {
			return RLN_WS;
		}
		System.out.printf("\nUnexpected route long name for %s!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static final String RTS_ALD = "ALD";
	private static final String RTS_WS = "WS";

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (FER_D.equals(gRoute.getRouteShortName())) {
			return RTS_ALD;
		} else if (FER_W.equals(gRoute.getRouteShortName())) {
			return RTS_WS;
		}
		System.out.printf("\nUnexpected route short name for %s!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static final String AGENCY_COLOR = "FDB714"; // YELLOW (PDF map)

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String AGENCY_COLOR_BLUE = "08215C"; // BLUE (MetroLink SVG from Wikipedia)
	private static final String DEFAULT_ROUTE_COLOR = AGENCY_COLOR_BLUE;

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			return DEFAULT_ROUTE_COLOR;
		}
		return super.getRouteColor(gRoute);
	}

	private static final String WOODSIDE = "Woodside";
	private static final String DARTMOUTH = "Dartmouth";
	private static final String HALIFAX = "Halifax";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(RID_ALD, new RouteTripSpec(RID_ALD, //
				0, MTrip.HEADSIGN_TYPE_STRING, DARTMOUTH, //
				1, MTrip.HEADSIGN_TYPE_STRING, HALIFAX) //
				.addTripSort(0, //
						Arrays.asList(new String[] { "1073", "1074" })) //
				.addTripSort(1, //
						Arrays.asList(new String[] { "1074", "1073" })) //
				.compileBothTripSort());
		map2.put(RID_WS, new RouteTripSpec(RID_WS, //
				0, MTrip.HEADSIGN_TYPE_STRING, HALIFAX, //
				1, MTrip.HEADSIGN_TYPE_STRING, WOODSIDE) //
				.addTripSort(0, //
						Arrays.asList(new String[] { "1075", "1073" })) //
				.addTripSort(1, //
						Arrays.asList(new String[] { "1073", "1075" })) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		System.out.printf("\nUnexpected trip %s!\n", gTrip);
		System.exit(-1);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern FERRY_STOP = Pattern.compile("(ferry stop - )", Pattern.CASE_INSENSITIVE);
	private static final Pattern ENDS_WITH_NUMBER = Pattern.compile("( \\([\\d]+\\)$)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = FERRY_STOP.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = ENDS_WITH_NUMBER.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(GStop gStop) {
		if (Utils.isDigitsOnly(gStop.getStopId())) {
			return Integer.parseInt(gStop.getStopId());
		}
		Matcher matcher = DIGITS.matcher(gStop.getStopId());
		if (matcher.find()) {
			return Integer.parseInt(matcher.group());
		}
		System.out.printf("\nUnexpected stop ID for %s!\n", gStop);
		System.exit(-1);
		return -1;
	}

	@Override
	public String getStopCode(GStop gStop) {
		if (Utils.isDigitsOnly(gStop.getStopId())) {
			return gStop.getStopId(); // using stop ID as stop code ("GoTime" number)
		}
		Matcher matcher = DIGITS.matcher(gStop.getStopId());
		if (matcher.find()) {
			return matcher.group();
		}
		System.out.printf("\nUnexpected stop code for %s!\n", gStop);
		System.exit(-1);
		return null;
	}
}
