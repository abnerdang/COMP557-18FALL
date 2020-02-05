package comp557.a4;

import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

public class Mesh extends Intersectable {
	
	/** Static map storing all meshes by name */
	public static Map<String,Mesh> meshMap = new HashMap<String,Mesh>();
	
	/**  Name for this mesh, to allow re-use of a polygon soup across Mesh objects */
	public String name = "";
	
	/**
	 * The polygon soup.
	 */
	public PolygonSoup soup;

	public Mesh() {
		super();
		this.soup = null;
	}			
		
	@Override
	public void intersect(Ray ray, IntersectResult result) {
		
		for(int[] face: soup.faceList) {
			Vector3d a = new Vector3d(soup.vertexList.get(face[0]).p);
			Vector3d b = new Vector3d(soup.vertexList.get(face[1]).p);
			Vector3d c = new Vector3d(soup.vertexList.get(face[2]).p);
			Vector3d ab = new Vector3d(); //from b to a 
			Vector3d bc = new Vector3d();
			Vector3d ca = new Vector3d();
			Vector3d normal = new Vector3d();
			bc.sub(c, b);
			ab.sub(b, a);
			ca.sub(a, c);
			normal.cross(ab, bc);
			normal.normalize();
			
			//intersection of ray and plane
			Vector3d temp = new Vector3d();
			Vector3d temp2 = new Vector3d(ray.viewDirection);
			temp.sub(a, ray.eyePoint);
			double t = temp.dot(normal)/temp2.dot(normal);
			
			Point3d x = new Point3d(ray.viewDirection);
			x.scale(t);
			x.add(ray.eyePoint);
			Vector3d ax = new Vector3d();
			Vector3d bx = new Vector3d();
			Vector3d cx = new Vector3d();
			ax.sub(x, a);
			bx.sub(x, b);
			cx.sub(x, c);
			ax.cross(ab, ax);
			bx.cross(bc, bx);
			cx.cross(ca, cx);
			if(ax.dot(normal)>0 && bx.dot(normal)>0 && cx.dot(normal)>0 && t>0 && t<result.t) {
				result.n = normal;
				result.p = x;
				result.t = t;
				result.material = material;
			}
			
		}
		

		
	}

}
