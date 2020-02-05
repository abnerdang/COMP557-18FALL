package comp557.a3;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;

/**
 * Simple vertex class
 */
public class Vertex {
	
	/** position of this vertex */
    public Point3d p = new Point3d();
    
    /** Error metric, v^T Q v gives sum of distances squared to the planes of all faces adjacent to this vertex */
    public Matrix4d Q = new Matrix4d();
    
    public void recomputeQi(HalfEdge he) {
    	//if(he.head.p != this.p) return;
    	
    	Matrix4d newQi = new Matrix4d();
    	HalfEdge loop = he;
    	do {
    		loop.leftFace.recomputeMatrixKp();
    		newQi.add(loop.leftFace.K);
    		loop = loop.next.twin;
    	}while(loop != he && loop != null);
    	
    	//For checking boundary
    	if(loop == null) {
    		loop = he.twin;
    		do {
    			if(loop == null) break;
        		loop.leftFace.recomputeMatrixKp();
        		newQi.add(loop.leftFace.K);
        		loop = loop.prev().twin;
        	}while(loop != he.twin && loop != null);
    	}
    	
    	Q = newQi;
    	return;
    }
    
    public void recomputeQiWhenCollpase(HalfEdge he) {
    	Matrix4d newQi = new Matrix4d();
    	newQi.add(he.head.Q);
//    	newQi.add(he.twin.head.Q);
    	newQi.add(he.next.next.head.Q);
    	Q = newQi;
    }
    
    
}
