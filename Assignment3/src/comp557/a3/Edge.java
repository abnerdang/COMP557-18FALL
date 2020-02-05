package comp557.a3;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;

/**
 * A class to store information concerning mesh simplificaiton
 * that is common to a pair of half edges.  Speicifically, 
 * the error metric, optimal vertex location on collapse, 
 * and the error.
 * @author Bo Dang
 */
public class Edge implements Comparable<Edge> {
	
	/** One of the two half edges */
	HalfEdge he;
	
	/** Optimal vertex location on collapse */
	Vector4d v = new Vector4d();
	
	/** Error metric for this edge */
	Matrix4d Q = new Matrix4d();
	
	/** The error involved in performing the collapse of this edge */
	double error;
	
	private Vertex getHalfVertex() {
		Vertex v = new Vertex();
		Vertex v1 = he.head;
//		Vertex v2 = he.twin.head;
		Vertex v2 = he.next.next.head;
		v.p.x = (v1.p.x+v2.p.x)/2;
		v.p.y = (v1.p.y+v2.p.y)/2;
		v.p.z = (v1.p.z+v2.p.z)/2;
		return v;
	}
	
	private Matrix4d getQreg() {
		Vertex midV = getHalfVertex();
		Matrix4d Qreg = new Matrix4d();
		Qreg.m00 = 1;
		Qreg.m11 = 1;
		Qreg.m22 = 1;
		Qreg.m03 = -midV.p.x;
		Qreg.m13 = -midV.p.y;
		Qreg.m23 = -midV.p.z;
		Qreg.m33 = (midV.p.x*midV.p.x)+(midV.p.y*midV.p.y)+(midV.p.z*midV.p.z);
		Qreg.m30 = -midV.p.x;
		Qreg.m31 = -midV.p.y;
		Qreg.m32 = -midV.p.z;
		return Qreg;
	}
	
	//set function public, so call function outside of this class when collapsing happens
	public void recomputeQ(double regWeight) {
		Matrix4d newQ = new Matrix4d();
		Matrix4d Qreg = getQreg();
		Qreg.mul(regWeight);
		Vertex v1 = he.head;
//		Vertex v2 = he.twin.head;
		Vertex v2 = he.next.next.head;
		newQ.add(v1.Q);
		newQ.add(v2.Q);
		newQ.add(Qreg);
		this.Q = newQ;
		return;	
	}
	//Call this function outside the class when edge collapsing happens
	public void recomputeOptimalPos() {
		double bx, by, bz;
		Vector4d pos = new Vector4d();
		Matrix3d A = new Matrix3d();
		A.m00 = Q.m00;
		A.m01 = Q.m01;
		A.m02 = Q.m02;
		A.m10 = Q.m10;
		A.m11 = Q.m11;
		A.m12 = Q.m12;
		A.m20 = Q.m20;
		A.m21 = Q.m21;
		A.m22 = Q.m22;
		A.invert();
		bx = Q.m03;
		by = Q.m13;
		bz = Q.m23;
		pos.x = -(A.m00*bx+A.m01*by+A.m02*bz);
		pos.y = -(A.m10*bx+A.m11*by+A.m12*bz);
		pos.z = -(A.m20*bx+A.m21*by+A.m22*bz);
		pos.w = 1;
		v = pos;
	}
	//Call this function outside the class when edge collapsing happens
	public void recomputeError() {
		double tempX = v.x*Q.m00+v.y*Q.m10+v.z*Q.m20+v.w*Q.m30;
		double tempY = v.x*Q.m01+v.y*Q.m11+v.z*Q.m21+v.w*Q.m31;
		double tempZ = v.x*Q.m02+v.y*Q.m12+v.z*Q.m22+v.w*Q.m32;
		double tempW = v.x*Q.m03+v.y*Q.m13+v.z*Q.m23+v.w*Q.m33;
		error = tempX*v.x+tempY*v.y+tempZ*v.z+tempW*v.w;
	}
	
	@Override
	public int compareTo(Edge o) {
		if (error < o.error ) return -1;
		if (error > o.error ) return 1;
		return 0;
	}
}
