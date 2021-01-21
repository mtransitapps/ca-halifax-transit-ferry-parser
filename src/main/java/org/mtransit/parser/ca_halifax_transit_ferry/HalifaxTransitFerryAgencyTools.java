package org.mtransit.parser.ca_halifax_transit_ferry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.StringUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mtransit.parser.StringUtils.EMPTY;

// http://www.halifax.ca/opendata/
// http://www.halifax.ca/opendata/transit.php
// http://gtfs.halifax.ca/static/google_transit.zip
public class HalifaxTransitFerryAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-halifax-transit-ferry-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new HalifaxTransitFerryAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating Halifax Transit ferry data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating Halifax Transit ferry data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_FERRY;
	}

	private static final long RID_ALD = 100_001L;
	private static final long RID_WS = 100_002L;

	private static final String FER_D = "FerD";
	private static final String FER_W = "FerW";

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		if (FER_D.equals(gRoute.getRouteShortName())) {
			return RID_ALD;
		} else if (FER_W.equals(gRoute.getRouteShortName())) {
			return RID_WS;
		}
		throw new MTLog.Fatal("Unexpected route ID  for %s %s!", gRoute);
	}

	private static final String RLN_ALD = "Alderney";
	private static final String RLN_WS = "Woodside";

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		if (FER_D.equals(gRoute.getRouteShortName())) {
			return RLN_ALD;
		} else if (FER_W.equals(gRoute.getRouteShortName())) {
			return RLN_WS;
		}
		throw new MTLog.Fatal("Unexpected route long name for %s!", gRoute);
	}

	private static final String RTS_ALD = "ALD";
	private static final String RTS_WS = "WS";

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		if (FER_D.equals(gRoute.getRouteShortName())) {
			return RTS_ALD;
		} else if (FER_W.equals(gRoute.getRouteShortName())) {
			return RTS_WS;
		}
		throw new MTLog.Fatal("Unexpected route short name for %s!", gRoute);
	}

	private static final String AGENCY_COLOR = "FDB714"; // YELLOW (PDF map)

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String AGENCY_COLOR_BLUE = "08215C"; // BLUE (MetroLink SVG from Wikipedia)
	private static final String DEFAULT_ROUTE_COLOR = AGENCY_COLOR_BLUE;

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			return DEFAULT_ROUTE_COLOR;
		}
		return super.getRouteColor(gRoute);
	}

	private static final String WOODSIDE = "Woodside";
	private static final String DARTMOUTH = "Dartmouth";
	private static final String HALIFAX = "Halifax";

	private static final HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		//noinspection deprecation
		map2.put(RID_ALD, new RouteTripSpec(RID_ALD, //
				0, MTrip.HEADSIGN_TYPE_STRING, DARTMOUTH, //
				1, MTrip.HEADSIGN_TYPE_STRING, HALIFAX) //
				.addTripSort(0, //
						Arrays.asList(
								"1073",
								"1074"
						)) //
				.addTripSort(1, //
						Arrays.asList(
								"1074",
								"1073"
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(RID_WS, new RouteTripSpec(RID_WS, //
				0, MTrip.HEADSIGN_TYPE_STRING, HALIFAX, //
				1, MTrip.HEADSIGN_TYPE_STRING, WOODSIDE) //
				.addTripSort(0, //
						Arrays.asList(
								"1075",
								"1073"
						)) //
				.addTripSort(1, //
						Arrays.asList(
								"1073",
								"1075"
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2, @NotNull MTripStop ts1, @NotNull MTripStop ts2, @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@NotNull
	@Override
	public ArrayList<MTrip> splitTrip(@NotNull MRoute mRoute, @Nullable GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@NotNull
	@Override
	public Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull ArrayList<MTrip> splitTrips, @NotNull GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		throw new MTLog.Fatal("Unexpected trip %s!", gTrip);
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern FERRY_STOP = Pattern.compile("(ferry stop - )", Pattern.CASE_INSENSITIVE);
	private static final Pattern ENDS_WITH_NUMBER = Pattern.compile("( \\([\\d]+\\)$)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = FERRY_STOP.matcher(gStopName).replaceAll(EMPTY);
		gStopName = ENDS_WITH_NUMBER.matcher(gStopName).replaceAll(EMPTY);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(@NotNull GStop gStop) {
		//noinspection deprecation
		final String stopId = gStop.getStopId();
		if (Utils.isDigitsOnly(stopId)) {
			return Integer.parseInt(stopId);
		}
		Matcher matcher = DIGITS.matcher(stopId);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group());
		}
		throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		//noinspection deprecation
		final String stopId = gStop.getStopId();
		if (Utils.isDigitsOnly(stopId)) {
			return stopId; // using stop ID as stop code ("GoTime" number)
		}
		Matcher matcher = DIGITS.matcher(stopId);
		if (matcher.find()) {
			return matcher.group();
		}
		throw new MTLog.Fatal("Unexpected stop code for %s!", gStop);
	}
}
