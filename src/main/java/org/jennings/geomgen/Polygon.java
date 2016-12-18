/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jennings.geomgen;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author david
 *
 * Future plans...
 *
 * Use a configuration file (json) and outputs random data. field name: format:
 * for str give valid list of characters "abcdefghijklmnopqrstuvwxyz"
 *
 * type [int, dbl, date, str] subtype: rnd min max
 *
 * subtype: enum vals []
 *
 * subtype: default int (any valid random number Java) double (any valid double
 * number Java) string (random string from 1 to 10 chars) date (System time now)
 *
 * int -> Random distribution from min to max double -> Random distribution from
 * min to max string -> List of values, random string a-zA-Z0-9 x numChars, guid
 * date -> Format; now; range min and max
 *
 */
@WebServlet(name = "Polygon", urlPatterns = {"/polygon"})
public class Polygon extends HttpServlet {

    public enum FORMAT {
        GEOJSON, JSONESRI
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        String strNumFeatures = null;
        String strFormat = null;

        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            if (paramName.equalsIgnoreCase("num")) {
                strNumFeatures = request.getParameter(paramName);
            } else if (paramName.equalsIgnoreCase("format")) {
                strFormat = request.getParameter(paramName);
            }
        }

        int numFeatures = 1;

        if (strNumFeatures != null) {
            try {
                numFeatures = Integer.parseInt(strNumFeatures);
            } catch (Exception e) {
                numFeatures = 1;
            }
        }

        FORMAT fmt = FORMAT.GEOJSON;

        try {
            fmt = FORMAT.valueOf(strFormat);
        } catch (Exception e) {
            // Defaults to geojson; probably should just return an error

        }

        try (PrintWriter out = response.getWriter()) {

            JSONObject json = new JSONObject();

            GreatCircle gc = new GreatCircle();

            Random rnd = new Random();

            double lonmax = 179;
            double lonmin = -179;
            double latmax = 85;
            double latmin = -85;

            double maxsize = 0.99;
            double minsize = 0.2;

            JSONArray features = new JSONArray();

            switch (fmt) {
                case JSONESRI:
                    // Each feature will have a set of fields including a geom
                    
                    for (int i = 1; i <= numFeatures; i++) {

                        double rndlon = rnd.nextDouble() * (lonmax - lonmin) + lonmin;
                        double rndlat = rnd.nextDouble() * (latmax - latmin) + latmin;
                        double rndsize = rnd.nextDouble() * (maxsize - minsize) + minsize;

                        JSONObject fields = new JSONObject();

                        System.out.println("");

                        // Regular properies
                        fields.put("fid", i);
                        fields.put("longitude", rndlon);
                        fields.put("latitude", rndlat);
                        fields.put("size", rndsize);
                        fields.put("rndfield1", gc.generateRandomWords(8));
                        fields.put("rndfield2", gc.generateRandomWords(8));
                        fields.put("rndfield3", gc.generateRandomWords(8));
                        fields.put("rndfield4", gc.generateRandomWords(8));

                        // Geom propery
                        GeographicCoordinate center = new GeographicCoordinate(rndlon, rndlat);
                        GeographicCoordinate[] coords = gc.createCircle(center, rndsize, 20);                                                

                        JSONArray exteriorRing = new JSONArray();
                        
                        for (GeographicCoordinate crd: coords) {
                            JSONArray coord = new JSONArray("[" + crd.getLon() + ", " + crd.getLat() + "], ");
                            exteriorRing.put(coord);
                        }
                        
                        JSONArray poly = new JSONArray();
                        poly.put(exteriorRing);                        
                        
                        JSONObject rings = new JSONObject();
                        rings.put("rings", poly);                        
                        
                        fields.put("geometry", rings);
                       
                        JSONObject feature = new JSONObject();
                        feature.put("feature", fields);
                                               
                        features.put(feature);


                    }

                    json.put("features", features);
                    break;
                case GEOJSON:

                    // GeoJSON
                    json.put("type", "FeatureCollection");

                    for (int i = 1; i <= numFeatures; i++) {

                        JSONObject feature = new JSONObject();

                        double rndlon = rnd.nextDouble() * (lonmax - lonmin) + lonmin;
                        double rndlat = rnd.nextDouble() * (latmax - latmin) + latmin;
                        double rndsize = rnd.nextDouble() * (maxsize - minsize) + minsize;

                        JSONObject properties = new JSONObject();

                        properties.put("fid", i);
                        properties.put("longitude", rndlon);
                        properties.put("latitude", rndlat);
                        properties.put("size", rndsize);
                        properties.put("rndfield1", gc.generateRandomWords(8));
                        properties.put("rndfield2", gc.generateRandomWords(8));
                        properties.put("rndfield3", gc.generateRandomWords(8));
                        properties.put("rndfield4", gc.generateRandomWords(8));

                        feature.put("properties", properties);

                        // Create the Geometry Object
                        JSONArray exteriorRing = new JSONArray();

                        GeographicCoordinate center = new GeographicCoordinate(rndlon, rndlat);
                        GeographicCoordinate[] coords = gc.createCircle(center, rndsize, 20);

                        int k = coords.length;

                        while (k > 0) {
                            k--;
                            GeographicCoordinate crd = coords[k];
                            JSONArray coord = new JSONArray("[" + crd.getLon() + ", " + crd.getLat() + "], ");
                            exteriorRing.put(coord);
                        }

                        JSONArray poly = new JSONArray();
                        poly.put(exteriorRing);

                        JSONObject geom = new JSONObject();
                        geom.put("type", "Polygon");
                        geom.put("coordinates", poly);

                        feature.put("geometry", geom);
                        feature.put("type", "Feature");

                        features.put(feature);

                    }

                    json.put("features", features);

                    break;

            }

            out.println(json.toString());

        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
