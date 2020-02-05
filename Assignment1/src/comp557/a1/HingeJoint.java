package comp557.a1;

import javax.vecmath.Tuple3d;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.gl2.GLUT;

import mintools.parameters.DoubleParameter;

public class HingeJoint extends DAGNode {
	
	Tuple3d position;
	//DoubleParameter rx;
	DoubleParameter angle, pos;
	Tuple3d axis;
	
	// Constructor	
	public HingeJoint( String name ) {
		super(name);
		dofs.add(angle = new DoubleParameter(name+"agl", 0, -90, 90));
		
		//dofs.add( rx = new DoubleParameter( name+" rx", 0, -180, 180 ) );		
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		
		gl.glPushMatrix();
		if(position != null) {
			gl.glTranslated(position.x, position.y, position.z);
		}
		
		if(axis != null) {
			gl.glRotated(angle.getValue(), axis.x, axis.y, axis.z);
		}
		else {
			gl.glRotated(angle.getValue(), 1, 0, 0);
		}
		super.display(drawable);
		gl.glPopMatrix();
		
		// TODO: Objective 1: implement the FreeJoint display method
		
	}
	
	public void setPosition(Tuple3d t) {
		this.position = t;	
	}
	
	public void setAxis(Tuple3d d) {
		this.axis = d;
	}

	
}
