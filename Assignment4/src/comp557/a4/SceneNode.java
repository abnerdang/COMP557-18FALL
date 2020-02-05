package comp557.a4;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.vecmath.Matrix4d;

import comp557.a4.IntersectResult;
import comp557.a4.Intersectable;
import comp557.a4.Ray;

/**
 * The scene is constructed from a hierarchy of nodes, where each node
 * contains a transform, a material definition, some amount of geometry, 
 * and some number of children nodes.  Each node has a unique name so that
 * it can be instanced elsewhere in the hierarchy (provided it does not 
 * make loops. 
 * 
 * Note that if the material (inherited from Intersectable) for a scene 
 * node is non-null, it should override the material of any child.
 * 
 */
public class SceneNode extends Intersectable {
	
	/** Static map for accessing scene nodes by name, to perform instancing */
	public static Map<String,SceneNode> nodeMap = new HashMap<String,SceneNode>();
	
    public String name;
   
    /** Matrix transform for this node */
    public Matrix4d M;
    
    /** Inverse matrix transform for this node */
    public Matrix4d Minv;
    
    /** Child nodes */
    public List<Intersectable> children;
    
    /**
     * Default constructor.
     * Note that all nodes must have a unique name, so that they can used as an instance later on.
     */
    public SceneNode() {
    	super();
    	this.name = "";
    	this.M = new Matrix4d();
    	this.Minv = new Matrix4d();
    	this.children = new LinkedList<Intersectable>();
    }
           
    @Override
    public void intersect(Ray ray, IntersectResult result) {

    	// TODO: Objective 7: implement hierarchy with instances
  	
    	Minv.transform(ray.eyePoint);
		Minv.transform(ray.viewDirection);
		
		for(Intersectable child: children){	
			IntersectResult currentResult = new IntersectResult();
			 
			child.intersect(ray, currentResult);
			
			Matrix4d T = new Matrix4d();			
			T.transpose(Minv);
			T.transform(currentResult.n);
			currentResult.n.normalize();
			//We only care about this intersection point if it is the closest from light source
			if(currentResult.t < result.t){
				result.t = currentResult.t;
				result.material = currentResult.material;
				M.transform(currentResult.p);
				result.p.set(currentResult.p);  
				result.n.set(currentResult.n);
				if(this.material != null){
					result.material = this.material;
				}
			}		
		}
		
		
		M.transform(ray.eyePoint);
		M.transform(ray.viewDirection);	
    }
		
    	
    	
    
    
}
