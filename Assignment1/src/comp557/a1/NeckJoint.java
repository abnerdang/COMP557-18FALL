package comp557.a1;

import javax.vecmath.Tuple3d;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.gl2.GLUT;

import mintools.parameters.DoubleParameter;

public class NeckJoint extends DAGNode {
	
	Tuple3d position;
	//DoubleParameter rx;
	DoubleParameter angle, pos;
	Tuple3d axis;
	
	// Constructor	
	public NeckJoint( String name ) {
		super(name);
		dofs.add(angle = new DoubleParameter(name+"agl", 0, -40, 40));
		
		//dofs.add( rx = new DoubleParameter( name+" rx", 0, -180, 180 ) );		
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		
		gl.glPushMatrix();
		gl.glTranslated(position.x, position.y, position.z);
		gl.glRotated(angle.getValue(), axis.x, axis.y, axis.z);
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

