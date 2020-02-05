package comp557.a4;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * A simple sphere class.
 */
public class Sphere extends Intersectable {
    
	/** Radius of the sphere. */
	public double radius = 1;
    
	/** Location of the sphere center. */
	public Point3d center = new Point3d( 0, 0, 0 );
    
    /**
     * Default constructor
     */
    public Sphere() {
    	super();
    }
    
    /**
     * Creates a sphere with the request radius and center. 
     * 
     * @param radius
     * @param center
     * @param material
     */
    public Sphere( double radius, Point3d center, Material material ) {
    	super();
    	this.radius = radius;
    	this.center = center;
    	this.material = material;
    }
    
    @Override
    public void intersect( Ray ray, IntersectResult result ) {
    	Vector3d ec = new Vector3d();
    	ec.sub(ray.eyePoint, center);
    	Vector3d d = ray.viewDirection;
    	double dec = d.dot(ec);
    	double inner = dec*dec-d.dot(d)*(ec.dot(ec)-radius*radius);
    	if(inner >= 0 ) {
    		double t = (-d.dot(ec)-Math.sqrt(inner))/d.dot(d);
    		if(t<0) return;
    		if(t < result.t) {
    		result.t = t;
    		Point3d p = new Point3d();
    		p.x = ray.eyePoint.x + t*d.x;
    		p.y = ray.eyePoint.y + t*d.y;
    		p.z = ray.eyePoint.z + t*d.z;
    		Vector3d n = new Vector3d();
    		n.sub(p, center);
    		n.normalize();
    		result.n = n;
    		result.p = p;
    		result.material = this.material;
    		}
    	}
    	
    	//compute normal etc.
    	
        // TODO: Objective 2: intersection of ray with sphere
    	
    }
    
}
