package comp557.a4;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

public class Quadrics extends Intersectable{
	
	//The function of quadrics: x^2/a^2+y^2/b^2 = 2*z
	public double a = 1;
	
	public double b = 1;
	
	public Quadrics() {
		super();
	}
	
	public Quadrics(double a, double b, Material material) {
		super();
		this.a = a;
		this.b = b;
		this.material = material;
	}
	
	@Override
	public void intersect(Ray ray, IntersectResult result) {
		double A, B, C, t;
		Point3d p = new Point3d();
		Vector3d d = new Vector3d();
		p.set(ray.eyePoint);
		d.set(ray.viewDirection);
		A = d.x*d.x/(a*a)+d.y*d.y/(b*b);
		B = 2.0*p.x*d.x/(a*a) + 2.0*p.y*d.y/(b*b) - 2.0*d.z;
		C = p.x*p.x/(a*a) + p.y*p.y/(b*b) - 2.0*p.z;
		double inner = B*B - 4*A*C;
		if(inner >= 0) {
			double t1 = (-B+Math.sqrt(inner))/(2.0*A);
			double t2 = (-B-Math.sqrt(inner))/(2.0*A);
			t = Math.min(t1, t2);
			//if(t2<0) t = t1;

			if(t < result.t && t > 0) {
				result.material = this.material;
				result.t = t;
				result.p.x = ray.eyePoint.x + t*d.x;
	    		result.p.y = ray.eyePoint.y + t*d.y;
	    		result.p.z = ray.eyePoint.z + t*d.z;
	    		
	    		//Calculate the normal of the surface
	     		Vector3d n = new Vector3d(2.0*result.p.x/(a*a), 2.0*result.p.y/(b*b), -2.0);
	     		n.normalize();
	     		result.n.set(n);
	    		
			}
		}
	}
	
}
