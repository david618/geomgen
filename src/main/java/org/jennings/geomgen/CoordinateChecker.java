/*
 * CoordinateChecker.java
 *
 * Created on February 29, 2008, 8:31 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jennings.geomgen;

/**
 *
 * @author sysdlj
 */
public class CoordinateChecker {
    
    /** Creates a new instance of CoordinateChecker */
    public CoordinateChecker() {
    }
    
    
    public String getErrorMessage(double dblErrorNum) {
        
        String strErr = "";
        
        int intErrorNum = (int) dblErrorNum;
        
        switch (intErrorNum) {
            case -1001: strErr = "Latitude must be less than 90"; break;
            case -1002: strErr = "Latitude must be greater than -90"; break;
            case -1003: strErr = "Longitude must be less than 360"; break;
            case -1004: strErr = "Longitude must be greater than -180"; break;
            default: strErr = "Unknown Error"; break;
        }
                                      
        return strErr;        
    }
    
    public int isValidLat(double lat) {
        // returns 0 if OK otherwise returns a negative number
        int error = 0;        
        try {            
            if (lat > 90) {
                error = -1001;   // lat to big
            } else if (lat < -90) {
                error = -1002;   // lat to small
            }            
        } catch (Exception e) {
            error = -1005; // Java Exception 
        }        
        return error;
    }

    public int isValidLon(double lon) {
        // returns 0 if OK otherwise returns a negative number
        int error = 0;        
        try {
            if (lon > 360) {
                error = -1003;   // lon to big
            } else if (lon < -180) {
                error = -1004;   // lon to small               
            }            
        } catch (Exception e) {
            error = -1005; // Java Exception 
        }        
        return error;
    }
    
    
}
