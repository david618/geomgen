/**
 * Great Circle
 */
package org.jennings.geomgen;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.io.FileWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Random;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author jenningd
 */
public class GreatCircle {

    private static final Logger log = Logger.getLogger(GreatCircle.class);

    private final double D2R = Math.PI / 180.0;
    private final double R2D = 180.0 / Math.PI;

    private final static DecimalFormat df8 = new DecimalFormat("###0.00000000");
    private final static DecimalFormat df5 = new DecimalFormat("###0.00000");
    private final static DecimalFormat df3 = new DecimalFormat("###0.000");

    /**
     *
     * @param coord1 First Coordinate
     * @param coord2 Second Coordinate
     * @return Distance (km) and Bearing (-180 to 180 from North)
     */
    public DistanceBearing getDistanceBearing(GeographicCoordinate coord1, GeographicCoordinate coord2) {

        double lon1 = coord1.getLon();
        double lat1 = coord1.getLon();

        double lon2 = coord2.getLon();
        double lat2 = coord2.getLon();

        DistanceBearing distB = new DistanceBearing();

        double gcDist = 0.0;
        double bearing = 0.0;

        try {

            double lon1R = lon1 * D2R;
            double lat1R = lat1 * D2R;
            double lon2R = lon2 * D2R;
            double lat2R = lat2 * D2R;

            /*
            Functions are a little whacky around the north and south pole.
            The only valid bearing from north pole is -180.
            I wouldn't trust the program for points near the poles.
             */
            if (lat1 - 90.0 < 0.00001) {
                // very close to north pole distance R * theta            
                double l = 90.0 - lat2;
                gcDist = Earth.Radius * l * D2R / 1000.0;
                // let bearing in lon2
                bearing = lon2;
            } else if (lat1 + 90 < 0.00001) {
                // very close to south pole distance R * theta
                double l = lat2 + 90.0;
                gcDist = Earth.Radius * l * D2R / 1000.0;
                bearing = lon2;

            } else {

                // law of Cosines
                double lambda = Math.abs(lon2R - lon1R);

                double x1 = Math.cos(lat2R) * Math.sin(lambda);

                double x2 = Math.cos(lat1R) * Math.sin(lat2R)
                        - Math.sin(lat1R) * Math.cos(lat2R) * Math.cos(lambda);

                double x3 = Math.sin(lat1R) * Math.sin(lat2R)
                        + Math.cos(lat1R) * Math.cos(lat2R) * Math.cos(lambda);

                double x4 = Math.sqrt(x1 * x1 + x2 * x2);

                double sigma = Math.atan2(x4, x3);

                gcDist = sigma * Earth.Radius / 1000.0;

                double y1 = Math.sin(lon2R - lon1R) * Math.cos(lat2R);

                double y2 = Math.cos(lat1R) * Math.sin(lat2R)
                        - Math.sin(lat1R) * Math.cos(lat2R) * Math.cos(lon2R - lon1R);

                double y3 = Math.atan2(y1, y2);

                bearing = (y3 * R2D) % 360;

                // Conver to value from -180 to 180
                bearing = bearing > 180.0 ? bearing - 360.0 : bearing;

            }
        } catch (Exception e) {
            gcDist = -1;
            bearing = 0;
        }

        distB = new DistanceBearing(gcDist, bearing);

        return distB;
    }

    public GeographicCoordinate getNewCoordPair(GeographicCoordinate coord1, DistanceBearing distB) {

        double lat1 = coord1.getLat();
        double lon1 = coord1.getLon();
        double lat2 = 0.0;
        double lon2 = 0.0;

        boolean bln360 = false;

        try {

            // Allow for lon values 180 to 360 (adjust them to -180 to 0)
            double lonDD = lon1;
            if (lonDD > 180.0 && lonDD <= 360) {
                lonDD = lonDD - 360;
                lon1 = lonDD;
                bln360 = true;
            }

            double alpha;
            double l;
            double k;
            double gamma;
            double phi;
            double theta;
            double hdng2;

            double hdng = distB.getBearing();

            if (hdng < 0) {
                hdng = hdng + 360;
            }

            // Round the input            
            BigDecimal bd = new BigDecimal(hdng);
            bd = bd.setScale(6, BigDecimal.ROUND_HALF_UP);
            hdng = bd.doubleValue();

            double dist = distB.getDistance() * 1000;

            if (lat1 == 90 || lat1 == -90) {
                // hdng doesn't make a lot of since at the poles assume this is just the lon
                lon2 = hdng;
                alpha = dist / Earth.Radius;
                if (lat1 == 90) {
                    lat2 = 90 - alpha * R2D;
                } else {
                    lat2 = -90 + alpha * R2D;
                }

            } else if (hdng == 0 || hdng == 360) {
                // going due north within some rounded number
                alpha = dist / Earth.Radius;
                lat2 = lat1 + alpha * R2D;
                lon2 = lon1;
            } else if (hdng == 180) {
                // going due south witin some rounded number
                alpha = dist / Earth.Radius;
                lat2 = lat1 - alpha * R2D;
                lon2 = lon1;
            } else if (hdng == 90) {
                lat2 = lat1;
                l = 90 - lat1;
                alpha = dist / Earth.Radius / Math.sin(l * D2R);
                //phi = Math.asin(Math.sin(alpha)/ Math.sin(l*D2R));                 
                lon2 = lon1 + alpha * R2D;
            } else if (hdng == 270) {
                lat2 = lat1;
                l = 90 - lat1;
                alpha = dist / Earth.Radius / Math.sin(l * D2R);
                //phi = Math.asin(Math.sin(alpha)/ Math.sin(l*D2R));                       
                lon2 = lon1 - alpha * R2D;
            } else if (hdng > 0 && hdng < 180) {
                l = 90 - lat1;
                alpha = dist / Earth.Radius;
                k = Math.acos(Math.cos(alpha) * Math.cos(l * D2R)
                        + Math.sin(alpha) * Math.sin(l * D2R) * Math.cos(hdng * D2R));
                lat2 = 90 - k * R2D;
                //phi = Math.asin(Math.sin(hdng*D2R) * Math.sin(alpha)/ Math.sin(k)); 
                phi = Math.acos((Math.cos(alpha) - Math.cos(k) * Math.cos(l * D2R))
                        / (Math.sin(k) * Math.sin(l * D2R)));
                lon2 = lon1 + phi * R2D;
                theta = Math.sin(phi) * Math.sin(l * D2R) / Math.sin(alpha);
                hdng2 = 180 - theta * R2D;
            } else if (hdng > 180 && hdng < 360) {
                gamma = 360 - hdng;
                l = 90 - lat1;
                alpha = dist / Earth.Radius;
                k = Math.acos(Math.cos(alpha) * Math.cos(l * D2R)
                        + Math.sin(alpha) * Math.sin(l * D2R) * Math.cos(gamma * D2R));
                lat2 = 90 - k * R2D;
                //phi = Math.asin(Math.sin(gamma*D2R) * Math.sin(alpha)/ Math.sin(k));                       
                phi = Math.acos((Math.cos(alpha) - Math.cos(k) * Math.cos(l * D2R))
                        / (Math.sin(k) * Math.sin(l * D2R)));
                lon2 = lon1 - phi * R2D;
                theta = Math.sin(phi) * Math.sin(l * D2R) / Math.sin(alpha);
                hdng2 = 180 - theta * R2D;
            }

            int decimalPlaces = 12;
            bd = new BigDecimal(lat2);
            bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP);
            lat2 = bd.doubleValue();

            bd = new BigDecimal(lon2);
            bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP);
            lon2 = bd.doubleValue();

            if (lat2 > 90) {
                lat2 = 180 - lat2;
                lon2 = (lon2 + 180) % 360;
            }

            if (lon2 > 180) {
                lon2 = lon2 - 360;
            }

            if (lat2 < -90) {
                lat2 = 180 - lat2;
                lon2 = (lon2 + 180) % 360;
            }
            if (lon2 < -180) {
                lon2 = lon2 + 360;
            }

            // adjust the lon back to 360 scale if input was like that
            if (bln360) {
                if (lon2 < 0) {
                    lon2 = lon2 + 360;
                }
            }

        } catch (Exception e) {
            lon2 = -1000;
            lat2 = -1000;
        }

        GeographicCoordinate nc = new GeographicCoordinate(lon2, lat2);

        return nc;
    }

    public GeographicCoordinate[] createCircle(GeographicCoordinate center, Double radius, Integer numPoints) {

        GeographicCoordinate[] coords = new GeographicCoordinate[numPoints];

        try {
            if (numPoints == null) {
                // if null default to 20 points
                numPoints = 20;
            }

            if (radius == null) {
                // default to 50 meters or 0.050 km
                radius = 0.050;
            }

            double d = 360.0 / (numPoints - 1);
            int i = 0;
            GeographicCoordinate nc1 = new GeographicCoordinate();
            while (i < numPoints - 1) {
                double theta = i * d - 180.0;

                DistanceBearing distb = new DistanceBearing(radius, theta);

                GeographicCoordinate nc = getNewCoordPair(center, distb);

                coords[i] = nc;

                i++;
                if (i == 1) {
                    nc1 = nc;
                }
            }

            // last point same as first
            coords[i] = nc1;

        } catch (Exception e) {
            coords = null;
        }

        return coords;
    }

    public JSONObject createCirle(double clon, double clat, double radius, Integer numPoints, boolean esriGeom) {

        JSONObject geom = new JSONObject();

        if (numPoints == null) {
            numPoints = 20;
        }

        if (esriGeom) {

            double d = 360.0 / (numPoints - 1);
            int i = 0;
            GeographicCoordinate nc1 = new GeographicCoordinate();
            JSONArray exteriorRing = new JSONArray();
            while (i < numPoints - 1) {
                double v = i * d - 180.0;
                i++;

                GeographicCoordinate coord1 = new GeographicCoordinate(clon, clat);
                DistanceBearing distb = new DistanceBearing(radius, v);

                GeographicCoordinate nc = getNewCoordPair(coord1, distb);

                if (i == 1) {
                    nc1 = nc;
                }
                JSONArray coord = new JSONArray("[" + nc.getLon() + ", " + nc.getLat() + "], ");

                exteriorRing.put(coord);

            }

            JSONArray coord = new JSONArray("[" + nc1.getLon() + ", " + nc1.getLat() + "], ");
            exteriorRing.put(coord);

            JSONArray poly = new JSONArray();
            poly.put(exteriorRing);

            geom.put("rings", poly);

        } else {

            double d = 360.0 / (numPoints - 1);
            int i = 0;

            GeographicCoordinate nc1 = new GeographicCoordinate();
            JSONArray exteriorRing = new JSONArray();
            while (i < numPoints - 1) {
                double v = 180.0 - i * d;  // counterclockwise
                i++;

                GeographicCoordinate coord1 = new GeographicCoordinate(clon, clat);
                DistanceBearing distb = new DistanceBearing(radius, v);

                GeographicCoordinate nc = getNewCoordPair(coord1, distb);
                if (i == 1) {
                    nc1 = nc;
                }
                JSONArray coord = new JSONArray("[" + nc.getLon() + ", " + nc.getLat() + "], ");

                exteriorRing.put(coord);

            }

            JSONArray coord = new JSONArray("[" + nc1.getLon() + ", " + nc1.getLat() + "], ");
            exteriorRing.put(coord);

            JSONArray poly = new JSONArray();
            poly.put(exteriorRing);

            geom.put("coordinates", poly);

        }

        return geom;

    }

    public String generateRandomWords(int numchars) {
        Random random = new Random();
        char[] word = new char[numchars];
        for (int j = 0; j < word.length; j++) {
            word[j] = (char) ('a' + random.nextInt(26));
        }
        return new String(word);
    }

    public static void main(String[] args) {
        GreatCircle gc = new GreatCircle();

        GeographicCoordinate center = new GeographicCoordinate(0, 0);

        GeographicCoordinate[] coords = gc.createCircle(center, 120.0, 39);

        System.out.println(coords.length);

        int i = 0;
        
        for (GeographicCoordinate coord : coords) {            
            System.out.println(i + ":" + coord);
            i++;
        }
        
        System.out.println();
        i = coords.length;
        
        while (i > 0) {
            i--;
            System.out.println(i + ":" + coords[i]);
        }
        
       

    }

}
