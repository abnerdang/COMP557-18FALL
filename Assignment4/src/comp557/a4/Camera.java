package comp557.a4;

import java.awt.Dimension;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Simple camera object, which could be extended to handle a variety of 
 * different camera settings (e.g., aperature size, lens, shutter)
 */
public class Camera {
	
	/** Camera name */
    public String name = "camera";

    /** The eye position */
    public Point3d from = new Point3d(0,0,10);
    
    /** The "look at" position */
    public Point3d to = new Point3d(0,0,0);
    
    /** Up direction, default is y up */
    public Vector3d up = new Vector3d(0,1,0);
    
    /** Vertical field of view (in degrees), default is 45 degrees */
    public double fovy = 45.0;
    
    
    /** Focal length */
    public double focalLength = -1; //Initially focalLength is set to a negative number. If the focalLength is negative, we don't need to consider it.
    
    
    /** The rendered image size */
    public Dimension imageSize = new Dimension(640,480);

    /**
     * Default constructor
     */
    public Camera() {
    	// do nothing
    }
    
    public Point3d getEyeOffset() {
    	Vector3d n = new Vector3d(); //Plane normal
    	n.sub(this.from, this.to); //We generate eyes in a plane which is perpendicular to the cam.to-cam.from
    	//A*dx+B*dy+C*dz = 0(A, B, C is the x, y, z of normal respectively)
    	double dx = 0.5*(Math.random()-0.5);
    	double dy = 0.5 *(Math.random()-0.5);
    	double dz = n.x*dx + n.y*dy/(-n.z);
    	return new Point3d(dx, dy, dz);
    }
}

