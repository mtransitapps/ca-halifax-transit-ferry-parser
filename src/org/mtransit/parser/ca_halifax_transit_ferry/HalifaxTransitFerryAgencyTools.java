package org.mtransit.parser.ca_halifax_transit_ferry;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// http://www.halifax.ca/opendata/
// http://www.halifax.ca/metrotransit/googletransitfeed/google_transit.zip
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
		System.out.printf("Generating Halifax Transit ferry data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("Generating Halifax Transit ferry data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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
		if (FER_D.equals(gRoute.route_short_name)) {
			return RID_ALD;
		} else if (FER_W.equals(gRoute.route_short_name)) {
			return RID_WS;
		}
		System.out.println("Unexpected route ID " + gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final String RLN_ALD = "Alderney";
	private static final String RLN_WS = "Woodside";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (FER_D.equals(gRoute.route_short_name)) {
			return RLN_ALD;
		} else if (FER_W.equals(gRoute.route_short_name)) {
			return RLN_WS;
		}
		System.out.println("Unexpected route long name " + gRoute);
		System.exit(-1);
		return null;
	}

	private static final String RTS_ALD = "ALD";
	private static final String RTS_WS = "WS";

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (FER_D.equals(gRoute.route_short_name)) {
			return RTS_ALD;
		} else if (FER_W.equals(gRoute.route_short_name)) {
			return RTS_WS;
		}
		System.out.println("Unexpected route short name " + gRoute);
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
		return DEFAULT_ROUTE_COLOR;
	}

	private static final String WOODSIDE = "Woodside";
	private static final String DARTMOUTH = "Dartmouth";
	private static final String HALIFAX = "Halifax";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (mRoute.id == RID_ALD) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(DARTMOUTH, gTrip.direction_id);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignString(HALIFAX, gTrip.direction_id);
				return;
			}
		} else if (mRoute.id == RID_WS) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(HALIFAX, gTrip.direction_id);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignString(WOODSIDE, gTrip.direction_id);
				return;
			}
		}
		System.out.println("Unexpected trip " + gTrip);
		System.exit(-1);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		return MSpec.cleanLabel(tripHeadsign);
	}

	private static final Pattern FERRY_STOP = Pattern.compile("(ferry stop - )", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = FERRY_STOP.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = MSpec.cleanNumbers(gStopName);
		return MSpec.cleanLabel(gStopName);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(GStop gStop) {
		if (Utils.isDigitsOnly(gStop.stop_id)) {
			return Integer.parseInt(gStop.stop_id);
		}
		Matcher matcher = DIGITS.matcher(gStop.stop_id);
		matcher.find();
		return Integer.parseInt(matcher.group());
	}

	@Override
	public String getStopCode(GStop gStop) {
		if (Utils.isDigitsOnly(gStop.stop_id)) {
			return gStop.stop_id; // using stop ID as stop code ("GoTime" number)
		}
		Matcher matcher = DIGITS.matcher(gStop.stop_id);
		matcher.find();
		return matcher.group();
	}
}
