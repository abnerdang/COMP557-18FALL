package comp557.a3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 * Half edge data structure.
 * Maintains a list of faces (i.e., one half edge of each) to allow
 * for easy display of geometry.
 */
public class HEDS {

    /** List of faces */
    Set<Face> faces = new HashSet<Face>();
    PriorityQueue<Edge> edges = new PriorityQueue<Edge>();
    
    /**
     * Constructs an empty mesh (used when building a mesh with subdivision)
     */
    public HEDS() {
        // do nothing
    }
        
    /**
     * Builds a half edge data structure from the polygon soup   
     * @param soup
     */
    
    /*===================================================
     * Create half edge data structure for a given polygon soup
     ====================================================*/
    public HEDS( PolygonSoup soup, double regWeight) {
    	
    	Map<String,HalfEdge> halfEdges = new TreeMap<String,HalfEdge>();
        halfEdges.clear();
        faces.clear();
        int numFace = soup.faceList.size();
        
        //For each face, construct the 3(number of half edges per face) half edges.
        
        for(int i = 0; i<numFace; i++) {
        	int numEdge = soup.faceList.get(i).length;
        	List<HalfEdge> heArray = new ArrayList<>();
        	 
        	for(int j = 0; j<numEdge; j++) {
        		 int tailIndex = soup.faceList.get(i)[(j%numEdge)]; //tailIndex
        		 int headIndex = soup.faceList.get(i)[(j+1)%numEdge]; //headIndex
        		 
        		 HalfEdge he = new HalfEdge();    
        		 String direction = tailIndex+","+headIndex;
        		 he.head = soup.vertexList.get(headIndex);
        		 halfEdges.put(direction, he);
        		 heArray.add(he);
        	 }
        	 
        	 //update HalfEdge.next
        	 for(int j = 0; j<numEdge; j++) {
        		 heArray.get(j).next = heArray.get((j+1)%numEdge);
        	 }
        	 
        	 Face leftFace = new Face(heArray.get(0)); //initialize Face class, each face with no null member
        	 faces.add(leftFace); //!!!!!!!!!!!!!!!!!!!!
        }
        
        //Get the corresponding twin of the current half  edge
        for(String direction: halfEdges.keySet()) {
        	int midIndex = direction.indexOf(",");
        	String invDir = direction.substring(midIndex+1)+","+direction.substring(0, midIndex);
        	halfEdges.get(direction).twin = halfEdges.get(invDir);
        }
        
        //initialize vertex matrix
        for(Face face: faces) {
        	HalfEdge he = face.he;
        	do {
        		//vertex initialization
        		he.head.recomputeQi(he);	
        		he = he.next;
        	}while(he != face.he);
        }
        
        //initialize matrix of edge
        for(Face face: faces) {
        	HalfEdge he = face.he;
        	do {
        		//Edge initialization
        		if(he.twin != null && he.twin.e != null) {
        			he.e = he.twin.e;
        		}
        		else {
        			Edge e = new Edge();
        			e.he = he;
        			e.recomputeQ(regWeight); //?????????????????????
        			e.recomputeOptimalPos();
        			e.recomputeError();
        			//System.out.println(e.error);
        			he.e = e;
        			edges.add(e);
        		}
        		he = he.next;
        	}while(he != face.he); 	
        }
       
//        while(!edges.isEmpty()) {
//        	System.out.println(edges.remove().error);
//       }
        
        // TODO: Objective 5: fill your priority queue on load
        
    }

    /**
     * You might want to use this to match up half edges... 
     */
    
    
    
    public Vertex optimalVertex(Vector4d optimalPos) {
    	Vertex optimalV = new Vertex();
    	optimalV.p.x = optimalPos.x;
    	optimalV.p.y = optimalPos.y;
    	optimalV.p.z = optimalPos.z;
    	return optimalV;
    }
    
    
    
    public void meshSimplification(double regWeight) {   	
    	//Choose a best edge from priority queue which could be collapsed.
    	if(!redoListHalfEdge.isEmpty()) {
    		System.out.println("Note: the redolist is not empty, REDO first!");
    		return;
    	}
    	
    	Stack<Edge> tempEdges = new Stack<Edge>();
    	if(edges.isEmpty()) return;
    	Edge eTemp = edges.remove();
    	do {
    		if(ifCollapse(eTemp.he)) {
    			while(!tempEdges.isEmpty()) {
    				edges.add(tempEdges.pop());
    			}
    			break;
    		}
    		else {
    			if(!edges.isEmpty()) {
    				tempEdges.push(eTemp);
    				eTemp = edges.remove();
    			}
    			else {
    				return;
    			}
    		}
    	}while(true);
    	
    	undoList.add(eTemp.he);
    	Vertex optimalV = optimalVertex(eTemp.v);
    	
    	if(!isBoundaryVertex(eTemp.he)) {
    		//Remove all edges adjacent to two deleted faces from priorityQueue.
    		HalfEdge loop1 = eTemp.he.next;
			HalfEdge loop2 = eTemp.he.twin.next;
			do {
				edges.remove(loop1.e);
				loop1 = loop1.next;
				System.out.println("test 1");
			}while(loop1 != eTemp.he);
			do {
				edges.remove(loop2.e);
				loop2 = loop2.next;
				System.out.println("test 2");
			}while(loop2 != eTemp.he.twin);
    	}
    	//Boundary vertex situation
    	else {
    		HalfEdge loop = eTemp.he.next;
    		do {
    			edges.remove(loop.e);
    			loop = loop.next;
    			System.out.println("test 3");
    		}while(loop != eTemp.he);
    	}
    	
    	
    	//
    	edgeCollapse(eTemp.he, optimalV, regWeight);

    }
    
    /*================================================
     *  Edge collapsing and topological problem checking
     =================================================*/
    private void boundaryEdgeCollapse(HalfEdge he, Vertex v, double regWeight) {
    	HalfEdge a = he.next.twin;
    	HalfEdge b = he.prev().twin;
    	a.twin = b;
    	b.twin = a;
    	this.faces.remove(he.leftFace);
    	HalfEdge loop = a;
    	do {
    		loop.head = v;
    		loop.leftFace.recomputeNormal();
    		loop.leftFace.recomputeMatrixKp();
    		loop = loop.next.twin;
    	}while(loop != null);
    	
    	loop = b.prev();
    	do {
    		loop.head = v;
    		loop.leftFace.recomputeNormal();
    		loop.leftFace.recomputeMatrixKp();
    		loop = loop.twin.prev();
    	}while(loop != null);
    	
    	Edge newEdge = new Edge();
    	newEdge.he = a;
    	a.e = newEdge;
    	a.twin.e = a.e;
    	newEdge.recomputeQ(regWeight);
    	newEdge.recomputeOptimalPos();
    	newEdge.recomputeError();
    	edges.add(newEdge);
    	
    	loop = a;
    	do {
    		loop.e.recomputeQ(regWeight);
    		loop.e.recomputeOptimalPos();
    		loop.e.recomputeError();
    		loop = loop.next.twin;
    	}while(loop != null);
    	
    	loop = b;
    	do {
    		loop.e.recomputeQ(regWeight);
    		loop.e.recomputeOptimalPos();
    		loop.e.recomputeError();
    		loop = loop.prev().twin;
    	}while(loop != null);
    		
    }
    public void edgeCollapse(HalfEdge he, Vertex v, double regWeight) {
    	if(!ifCollapse(he)) 
    		return;
    	
    	v.recomputeQiWhenCollpase(he);
    	
    	if(isBoundaryVertex(he)) {
    		boundaryEdgeCollapse(he, v, regWeight);
    		return;
    	}
    	
    	HalfEdge a = he.next.twin;
    	HalfEdge b = he.prev().twin;
    	HalfEdge c = he.twin.next.twin;
    	HalfEdge d = he.twin.prev().twin;
    	if(a != null) a.twin = b;
    	if(b != null) b.twin = a;
    	if(c != null) c.twin = d;
    	if(d != null) d.twin = c;
    	this.faces.remove(he.leftFace);
    	this.faces.remove(he.twin.leftFace);
    	HalfEdge loop = new HalfEdge();
    	HalfEdge selectedHE = new HalfEdge();
    	if(a != null) selectedHE = a;
    	else selectedHE = b.prev();
    	loop = selectedHE;
    	do {
    		loop.head = v;
    		loop.leftFace.recomputeNormal();
    		loop.leftFace.recomputeMatrixKp();
    		loop = loop.next.twin;
    	} while(loop != selectedHE && loop != null);
    	
    	if(loop == null) {
    		loop = selectedHE.twin;
    		if(loop == null) return;
    		do {
    			loop.prev().head = v;
    			loop = loop.prev().twin;
    		}while(loop != selectedHE.twin && loop != null);
    	}
    	
    	//Vertex Matrix update
    	//v.recomputeQi(a);
    	
    	
    	//Add two new edges to edges priority queue and update corresponding 4 half edges
    	Edge newEdge1 = new Edge();
    	Edge newEdge2 = new Edge();
    	if( a!= null) {
    		newEdge1.he = a;
    		a.e = newEdge1;
    		if(a.twin != null) a.twin.e = a.e;
    	}
    	else{
    		newEdge1.he = b;
    		b.e = newEdge1;
    	}
    	
    	if(c != null) {
    		newEdge2.he = c;
    		c.e = newEdge2;
    		if(c.twin != null) c.twin.e = c.e;
    	}
    	else{
    		newEdge2.he = d;
    		d.e = newEdge2;
    	}


    	newEdge1.recomputeQ(regWeight);
    	newEdge1.recomputeOptimalPos();
    	newEdge1.recomputeError();
    	newEdge2.recomputeQ(regWeight);
    	newEdge2.recomputeOptimalPos();
    	newEdge2.recomputeError();
    	edges.add(newEdge1);
    	edges.add(newEdge2);
    	
    	//update all edges adjacent to new vertex
    	HalfEdge loop2 = newEdge1.he;
    	if(loop2.head != v) loop2 = loop2.prev();
    	loop = loop2;
    	do {
    		loop2.e.recomputeQ(regWeight);
    		loop2.e.recomputeOptimalPos();
    		loop2.e.recomputeError();
    		loop2 = loop2.next.twin;
    		//System.out.println("");
    	}while(loop2 != loop && loop2 != null);
    	
    	//Boundary edge case
    	if(loop2 == null) {
    		loop2 = loop.twin;
    		if(loop2 == null) return;
    		do {
    			loop2.e.recomputeQ(regWeight);
    			loop2.e.recomputeOptimalPos();
    			loop2.e.recomputeError();
    			loop2 = loop2.prev().twin;
    			//System.out.println("");
    		}while(loop2 != null && loop2 != loop.twin);
    		return;
    	}
    	
    	
    }
    
    
    //Get the 1-ring of any vertex. (he.head = currentHE)
    private Set<Vertex> adjVertex(HalfEdge he){
    	Set<Vertex> adjV = new HashSet<Vertex>();
    	HalfEdge loop = new HalfEdge();
    	loop = he;
    	do {
    		adjV.add(loop.next.head);
    		loop = loop.next.twin;
    	}while(loop != he && loop != null);
    	if(loop == null) {
    		loop = he.twin;
    		if(loop == null) {
    			adjV.add(he.prev().head);
    			return adjV;
    		}
    		do {
    			adjV.add(loop.head);
    			loop = loop.prev().twin;
    		}while(loop != he.twin && loop != null);
    	}
    	return adjV;
    }
    
    
    //whether we can collapse current half edge
    private boolean ifCollapse(HalfEdge he) {
    	if(faces.size() <= 4)
    		return false;
    	Set<Vertex> headAdj = new HashSet<Vertex>();
    	Set<Vertex> tailAdj = new HashSet<Vertex>();
    	headAdj = adjVertex(he);
    	tailAdj = adjVertex(he.prev());
    	int counter = 0;
    	for(Vertex v: headAdj) {
    		if(tailAdj.contains(v)) 
    			counter++;
    	}
    	if(counter>2) { 
    		return false;
    	}
    	//No boundary version
//    	else 
//    		return true;
    	//Boundary version
    	else {
    		if(isBoundaryVertex(he)) {
    			if(he.next == null) return true; //Boundary Edge!
    			return false;
    		}
    		return true;
    	}
    }
    
    private boolean isBoundaryVertex(HalfEdge h) {
    	HalfEdge loop = h;
    	do {
    		loop = loop.next.twin;
    		System.out.println("test 4");
    	}while(loop != null && loop != h);
    	if(loop == h) return false;
    	
    	loop = h.prev();
    	do {
    		if(loop.twin == null) return true;
    		loop = loop.twin.prev();
    		System.out.println("test 5");
    	}while(loop.twin != null && loop != h.prev());
    	if(loop == h.prev()) return false;
    	
    	return true;	
    }
    
    /**
	 * Need to know both verts before the collapse, but this information is actually 
	 * already stored within the excized portion of the half edge data structure.
	 * Thus, we only need to have a half edge (the collapsed half edge) to undo
	 */
	LinkedList<HalfEdge> undoList = new LinkedList<>();
	/**
	 * To redo an undone collapse, we must know which edge to collapse.  We should
	 * likewise reuse the Vertex that was created for the collapse.
	 */
	LinkedList<HalfEdge> redoListHalfEdge = new LinkedList<>();
	LinkedList<Vertex> redoListVertex = new LinkedList<>();

    public void undoCollapse() {
    	if ( undoList.isEmpty() ) return; // ignore the request
   
    	HalfEdge he = undoList.removeLast();
    	Vertex v1 = he.head;
    	Vertex v2 = he.twin.head;
    	//Redo list update
    	redoListVertex.add(he.next.twin.head);
    	redoListHalfEdge.add(he);
    	
    	//Undo
    	HalfEdge a = he.next.twin;
    	HalfEdge b = he.prev().twin;
    	HalfEdge c = he.twin.next.twin;
    	HalfEdge d = he.twin.prev().twin;
    	a.twin = he.next;
    	b.twin = he.prev();
    	c.twin = he.twin.next;
    	d.twin = he.twin.prev();
    	HalfEdge loop = a;
    	do {
    		loop.head = v1;
    		loop.leftFace.recomputeNormal();
    		loop = loop.next.twin;
    	}while(loop != a);
    	
    	loop = he.twin;
    	do {
    		loop.head = v2;
    		loop.leftFace.recomputeNormal();
    		loop = loop.next.twin;
    	}while(loop != he.twin);
    	he.leftFace.recomputeNormal();
    	he.twin.leftFace.recomputeNormal();
    	faces.add(he.leftFace);
    	faces.add(he.twin.leftFace);
    }
    
    public void redoCollapse() {
    	if ( redoListHalfEdge.isEmpty() ) {
    		System.out.println("The redo list is empty!");
    		return; // ignore the request
    	}
    	
    	HalfEdge he = redoListHalfEdge.removeLast();
    	Vertex v = redoListVertex.removeLast();
    	
    	undoList.add( he );  // put this on the undo list so we can undo this collapse again
    	
    	faces.remove(he.leftFace);
    	faces.remove(he.twin.leftFace);
    	HalfEdge a = he.next.twin;
    	HalfEdge b = he.prev().twin;
    	HalfEdge c = he.twin.next.twin;
    	HalfEdge d = he.twin.prev().twin;
    	a.twin = b;
    	b.twin = a;
    	d.twin = c;
    	c.twin = d;
    	HalfEdge loop = a;
    	do {
    		loop.head = v;
    		loop.leftFace.recomputeNormal();
    		loop = loop.next.twin;
    	}while(loop != a); 	
    }
      
    /**
     * Draws the half edge data structure by drawing each of its faces.
     * Per vertex normals are used to draw the smooth surface when available,
     * otherwise a face normal is computed. 
     * @param drawable
     */
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // we do not assume triangular faces here        
        Point3d p;
        Vector3d n;        
        for ( Face face : faces ) {
            HalfEdge he = face.he;
            gl.glBegin( GL2.GL_POLYGON );
            n = he.leftFace.n;
            gl.glNormal3d( n.x, n.y, n.z );
            HalfEdge e = he;
            do {
                p = e.head.p;
                gl.glVertex3d( p.x, p.y, p.z );
                e = e.next;
            } while ( e != he );
            gl.glEnd();
        }
    }

}