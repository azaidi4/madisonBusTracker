import org.joda.time.*;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.model.*;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

import java.io.File;
import java.io.IOException;
import java.util.*;

final class travel {
    LinkedHashSet<Stop> closetoSrc;
    LinkedHashSet<Stop> closetoDest;
    List<Route> possible_routes;
    static int currTime;

    private final static String FEED_PATH = "gtfs-feed.zip";
    private static GtfsDaoImpl store;


    private travel(LinkedHashSet<Stop> from, LinkedHashSet<Stop> to, int time){
        closetoSrc = from;
        closetoDest = to;
        currTime = time;
        possible_routes = getRoutes(closetoSrc, closetoDest);
    }

    public static travel createInstance(String src, String dest) throws InterruptedException, ApiException, IOException {
        GtfsReader reader = new GtfsReader();
        reader.setInputLocation(new File(FEED_PATH));
        store = new GtfsDaoImpl();
        reader.setEntityStore(store);
        reader.run();

        LatLng from = parseToCoords(src);
        LatLng to = parseToCoords(dest);

        MutableDateTime now = MutableDateTime.now();
        DateTime fromMidnight = now.toDateTime().toDateMidnight().toDateTime();
        Duration dFromMidnight = new Duration(fromMidnight, now);
        int currTime = (int) dFromMidnight.getStandardSeconds();

        return new travel(closestStops(from), closestStops(to), currTime);
    }
    /**
     * Gets the nearest stop from Location.
     */
    private static LinkedHashSet<Stop> closestStops(final LatLng myCoord) {
        class DistanceComparator implements Comparator<Stop> {
            public int compare(Stop o1, Stop o2) {
                double d1 = distance(myCoord, new LatLng(o1.getLat(), o2.getLon()));
                double d2 = distance(myCoord, new LatLng(o2.getLat(), o2.getLon()));
                return Double.compare(d1, d2);
            }
        }
        List<Stop> closestStops = new ArrayList<Stop>();
        for (Stop stop : store.getAllStops()){
            LatLng stopCoord = new LatLng(stop.getLat(), stop.getLon());
            if (distance(myCoord, stopCoord) < 0.3){
                closestStops.add(stop);
            }
        }

        Collections.sort(closestStops, new DistanceComparator());
        return new LinkedHashSet<Stop>(closestStops);
    }

    private static LatLng parseToCoords(String str) throws InterruptedException, ApiException, IOException {
        String key = "";    //Insert API Key here
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(key)
                .build();

        GeocodingResult[] results = GeocodingApi.geocode(context, str).await();
        System.out.println("Success!");
        return results[0].geometry.location;
    }

    public static double distance(LatLng src, LatLng dst){

        final int R = 6371; // Radius of the earth (km)
        Double lat1 = src.lat;
        Double lon1 = src.lng;
        Double lat2 = dst.lat;
        Double lon2 = dst.lng;
        Double latDistance = toRad(lat2-lat1);
        Double lonDistance = toRad(lon2-lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }

    private static Double toRad(Double value) {
        return value * Math.PI / 180;
    }

    /**
     * Traversal: stop{stopID} -> stop_times{tripID} -> trips{routeID}
     */
    private static List<Route> getRoutes(LinkedHashSet<Stop> closestToSrc, LinkedHashSet<Stop> closestToDest) {
        LinkedHashMap<Route, Set<Stop>> allRoutesfromSrc = getAllRoutes(closestToSrc);
        LinkedHashMap<Route, Set<Stop>> allRoutesToDest = getAllRoutes(closestToDest);

        for (Route route : allRoutesfromSrc.keySet()) {
            if (allRoutesToDest.containsKey(route)) {
                System.out.println("Route " + route.getShortName() + " is in both stops");
                Set<Stop> srcPath = allRoutesfromSrc.get(route);
                Set<Stop> destPath = allRoutesToDest.get(route);
            }
        }
        return null;
    }
    private static LinkedHashMap<Route, Set<Stop>> getAllRoutes(LinkedHashSet<Stop> stops) {
        LinkedHashMap<Route, Set<Stop>> routes =
                new LinkedHashMap<Route, Set<Stop>>();

        for (Route r : store.getAllRoutes()){
            AgencyAndId ID = r.getId();
            Set<Stop> tempStops= new HashSet<Stop>();
            for (StopTime s : store.getAllStopTimes()){
                if (s.getArrivalTime() > currTime
                        && stops.contains(s.getStop())
                        && ID.equals(s.getTrip().getRoute().getId())){
                    tempStops.add(s.getStop());
                    routes.put(r, tempStops);
                }
            }
        }
        return routes;
    }
}

