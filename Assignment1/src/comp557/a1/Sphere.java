package comp557.a1;

import javax.vecmath.Tuple3d;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.gl2.GLUT;
import mintools.parameters.DoubleParameter;

public class Sphere extends DAGNode {

	Tuple3d center;
	Tuple3d scale;
	Tuple3d color;
	// Constructor	
	public Sphere( String name ) {
		super(name);
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		
		
		GLUT sphere = new GLUT();
		
		gl.glPushMatrix();
		
		if(center!=null) {
			gl.glTranslated(center.x, center.y, center.z);
		}
		
		if(scale != null) {
			gl.glScaled(scale.x, scale.y, scale.z);
		}
		
		if(color != null) {
			gl.glColor3d(color.x, color.y, color.z);
		}
		
		sphere.glutSolidSphere(1, 100, 100);
		super.display(drawable);
		gl.glPopMatrix();
		
		
		
	}
	
	public void setCentre(Tuple3d t) {
		this.center = t;
	}
	
	public void setScale(Tuple3d t) {
		this.scale = t;
	}
	
	public void setColor(Tuple3d t) {
		this.color = t;
	}
	

	
}
