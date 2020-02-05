package comp557.a4;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Class for a plane at y=0.
 * 
 * This surface can have two materials.  If both are defined, a 1x1 tile checker 
 * board pattern should be generated on the plane using the two materials.
 */
public class Plane extends Intersectable {
    
	/** The second material, if non-null is used to produce a checker board pattern. */
	Material material2;
	
	/** The plane normal is the y direction */
	public static final Vector3d n = new Vector3d( 0, 1, 0 );
    
    /**
     * Default constructor
     */
    public Plane() {
    	super();
    }

        
    @Override
    public void intersect( Ray ray, IntersectResult result ) {
    	
    	double t = -ray.eyePoint.y/ray.viewDirection.y;
    	if(t < 0) return;
    	if(t < result.t) {
    		Point3d p = new Point3d();
    		p.x = ray.eyePoint.x + t*ray.viewDirection.x;
    		p.z = ray.eyePoint.z + t*ray.viewDirection.z;
    		p.y = 0;
    		result.t = t;
    		result.n.set(n);
    		result.p.set(p);
    		
    		if(material2 == null) {
    			result.material = material;
    		}
    		else {
    			int x;
    			int z;
    			if(p.x>=0&&p.z>=0 || p.x<0&&p.z<0) {
    				x = (int)p.x;
    				z = (int)p.z;
    				int sum = x+z;
    				if(sum%2 == 0) {
    					result.material = material;
    				}
    				else {
    					result.material = material2;
    				}
    			}
    			else {
    				x = (int)p.x;
    				z = (int)p.z;
    				int sum = x+z;
    				if(sum%2 == 0) {
    					result.material = material2;
    				}
    				else {
    					result.material = material;
    				}
    			}
    			
    		}
    		
    		
    	}
        // TODO: Objective 4: intersection of ray with plane
    	
    }
    
}
