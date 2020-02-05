package comp557.a2;

import javax.vecmath.Point2d;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.jogamp.opengl.DebugGL2;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;

import mintools.parameters.BooleanParameter;
import mintools.parameters.DoubleParameter;
import mintools.parameters.IntParameter;
import mintools.swing.ControlFrame;
import mintools.swing.VerticalFlowPanel;
import mintools.viewer.FlatMatrix4d;
import mintools.viewer.Interactor;
import mintools.viewer.TrackBallCamera;

/**
 * Assignment 2 - depth of field blur, and anaglyphys
 * 
 * For additional information, see the following paper, which covers
 * more on quality rendering, but does not cover anaglyphs.
 * 
 * The Accumulation Buffer: Hardware Support for High-Quality Rendering
 * Paul Haeberli and Kurt Akeley
 * SIGGRAPH 1990
 * 
 * http://http.developer.nvidia.com/GPUGems/gpugems_ch23.html
 * GPU Gems [2007] has a slightly more recent survey of techniques.
 *
 * @author Dang Bo
 */
public class A2App implements GLEventListener, Interactor {

	
	private String name = "Comp 557 Assignment 2 - Dang Bo";
	
    /** Viewing mode as specified in the assignment */
    int viewingMode = 1;
        
    /** eye Z position in world coordinates */
    private DoubleParameter eyeZPosition = new DoubleParameter( "eye z", 0.5, 0.25, 3 ); 
    /** near plane Z position in world coordinates */
    private DoubleParameter nearZPosition = new DoubleParameter( "near z", 0.25, -0.2, 0.5 ); 
    /** far plane Z position in world coordinates */
    private DoubleParameter farZPosition  = new DoubleParameter( "far z", -0.5, -2, -0.25 ); 
    /** focal plane Z position in world coordinates */
    private DoubleParameter focalPlaneZPosition = new DoubleParameter( "focal z", 0, -1.5, 0.4 );     

    /** Samples for drawing depth of field blur */    
    private IntParameter samples = new IntParameter( "samples", 5, 1, 100 );   
    
    /** 
     * Aperture size for drawing depth of field blur
     * In the human eye, pupil diameter ranges between approximately 2 and 8 mm
     */
    private DoubleParameter aperture = new DoubleParameter( "aperture size", 0.003, 0, 0.01 );
    
    /** x eye offsets for testing (see objective 4) */         
    private DoubleParameter eyeXOffset = new DoubleParameter("eye offset in x", 0.0, -0.3, 0.3);
    /** y eye offsets for testing (see objective 4) */
    private DoubleParameter eyeYOffset = new DoubleParameter("eye offset in y", 0.0, -0.3, 0.3);
    
    private BooleanParameter drawCenterEyeFrustum = new BooleanParameter( "draw center eye frustum", true );    
    
    private BooleanParameter drawEyeFrustums = new BooleanParameter( "draw left and right eye frustums", true );
    
	/**
	 * The eye disparity should be constant, but can be adjusted to test the
	 * creation of left and right eye frustums or likewise, can be adjusted for
	 * your own eyes!! Note that 63 mm is a good inter occular distance for the
	 * average human, but you may likewise want to lower this to reduce the
	 * depth effect (images may be hard to fuse with cheap 3D colour filter
	 * glasses). Setting the disparity negative should help you check if you
	 * have your left and right eyes reversed!
	 */
    private DoubleParameter eyeDisparity = new DoubleParameter("eye disparity", 0.063, -0.1, 0.1 );

    private GLUT glut = new GLUT();
    
    private Scene scene = new Scene();

    /**
     * Launches the application
     * @param args
     */
    public static void main(String[] args) {
        new A2App();
    }
    
    GLCanvas glCanvas;
    
    /** Main trackball for viewing the world and the two eye frustums */
    TrackBallCamera tbc = new TrackBallCamera();
    /** Second trackball for rotating the scene */
    TrackBallCamera tbc2 = new TrackBallCamera();
    
    /**
     * Creates the application
     */
    public A2App() {      
        Dimension controlSize = new Dimension(640, 640);
        Dimension size = new Dimension(640, 480);
        ControlFrame controlFrame = new ControlFrame("Controls");
        controlFrame.add("Camera", tbc.getControls());
        controlFrame.add("Scene TrackBall", tbc2.getControls());
        controlFrame.add("Scene", getControls());
        controlFrame.setSelectedTab("Scene");
        controlFrame.setSize(controlSize.width, controlSize.height);
        controlFrame.setLocation(size.width + 20, 0);
        controlFrame.setVisible(true);    
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities glc = new GLCapabilities(glp);
        glCanvas = new GLCanvas( glc );
        glCanvas.setSize( size.width, size.height );
        glCanvas.setIgnoreRepaint( true );
        glCanvas.addGLEventListener( this );
        glCanvas.requestFocus();
        FPSAnimator animator = new FPSAnimator( glCanvas, 60 );
        animator.start();        
        tbc.attach( glCanvas );
        tbc2.attach( glCanvas );
        // initially disable second trackball, and improve default parameters given our intended use
        tbc2.enable(false);
        tbc2.setFocalDistance( 0 );
        tbc2.panRate.setValue(5e-5);
        tbc2.advanceRate.setValue(0.005);
        this.attach( glCanvas );        
        JFrame frame = new JFrame( name );
        frame.getContentPane().setLayout( new BorderLayout() );
        frame.getContentPane().add( glCanvas, BorderLayout.CENTER );
        frame.setLocation(0,0);        
        frame.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing( WindowEvent e ) {
                System.exit(0);
            }
        });
        frame.pack();
        frame.setVisible( true );        
    }
    
    @Override
    public void dispose(GLAutoDrawable drawable) {
    	// nothing to do
    }
        
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        // do nothing
    }
    
    @Override
    public void attach(Component component) {
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_7) {
                    viewingMode = e.getKeyCode() - KeyEvent.VK_1 + 1;
                }
                // only use the tbc trackball camera when in view mode 1 to see the world from
                // first person view, while leave it disabled and use tbc2 ONLY FOR ROTATION when
                // viewing in all other modes
                if ( viewingMode == 1 ) {
                	tbc.enable(true);
                	tbc2.enable(false);
	            } else {
                	tbc.enable(false);
                	tbc2.enable(true);
	            }
            }
        });
    }
    
    /**
     * @return a control panel
     */
    public JPanel getControls() {     
        VerticalFlowPanel vfp = new VerticalFlowPanel();
        
        VerticalFlowPanel vfp2 = new VerticalFlowPanel();
        vfp2.setBorder(new TitledBorder("Z Positions in WORLD") );
        vfp2.add( eyeZPosition.getSliderControls(false));        
        vfp2.add( nearZPosition.getSliderControls(false));
        vfp2.add( farZPosition.getSliderControls(false));        
        vfp2.add( focalPlaneZPosition.getSliderControls(false));     
        vfp.add( vfp2.getPanel() );
        
        vfp.add ( drawCenterEyeFrustum.getControls() );
        vfp.add ( drawEyeFrustums.getControls() );        
        vfp.add( eyeXOffset.getSliderControls(false ) );
        vfp.add( eyeYOffset.getSliderControls(false ) );        
        vfp.add ( aperture.getSliderControls(false) );
        vfp.add ( samples.getSliderControls() );        
        vfp.add( eyeDisparity.getSliderControls(false) );
        VerticalFlowPanel vfp3 = new VerticalFlowPanel();
        vfp3.setBorder( new TitledBorder("Scene size and position" ));
        vfp3.add( scene.getControls() );
        vfp.add( vfp3.getPanel() );        
        return vfp.getPanel();
    }
             
    public void init( GLAutoDrawable drawable ) {
    	drawable.setGL( new DebugGL2( drawable.getGL().getGL2() ) );
        GL2 gl = drawable.getGL().getGL2();
        gl.glShadeModel(GL2.GL_SMOOTH);             // Enable Smooth Shading
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);    // Black Background
        gl.glClearDepth(1.0f);                      // Depth Buffer Setup
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glEnable(GL2.GL_NORMALIZE );
        gl.glEnable(GL.GL_DEPTH_TEST);              // Enables Depth Testing
        gl.glDepthFunc(GL.GL_LEQUAL);               // The Type Of Depth Testing To Do 
        gl.glLineWidth( 2 );                        // slightly fatter lines by default!
    }   

	
    // TODO: Objective 1 - adjust for your screen resolution and dimension to something reasonable.
	double screenWidthPixels = 2560;
	double screenWidthMeters = 0.2864;       // 2560 by 1600 at 227 pixels per inch
	double metersPerPixel = screenWidthMeters / screenWidthPixels;
	
	
	//FastPoisson
	FastPoissonDisk fpd = new FastPoissonDisk();
	
    @Override
    public void display(GLAutoDrawable drawable) {        
        GL2 gl = drawable.getGL().getGL2();
        double width = drawable.getSurfaceWidth()*metersPerPixel;
    	double height = drawable.getSurfaceHeight()*metersPerPixel;
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);            

        
        //draw screen window rectangle
        gl.glDisable(gl.GL_LIGHTING);
        gl.glColor3f(1.0f, 1.0f, 0.0f);
        gl.glBegin(gl.GL_LINE_LOOP);
        gl.glVertex3d(-width/2, height/2, 0.0d);
        gl.glVertex3d(width/2, height/2, 0.0d);
        gl.glVertex3d(width/2, -height/2, 0.0d);
        gl.glVertex3d(-width/2, -height/2, 0.0d);
        gl.glEnd();
        gl.glEnable(gl.GL_LIGHTING);
        
        //draw central eye
        gl.glPushMatrix();
        gl.glTranslated(eyeXOffset.getValue(), eyeYOffset.getValue(), eyeZPosition.getValue());
        gl.glDisable(gl.GL_LIGHTING);
     	gl.glColor3f(1.0f, 1.0f, 1.0f);
        GLUT eye = new GLUT();
        eye.glutSolidSphere(0.0125, 100, 100);
        gl.glEnable(gl.GL_LIGHTING);
        gl.glPopMatrix();
        
        //draw left eye
        gl.glPushMatrix();
        gl.glTranslated(-0.5*eyeDisparity.getValue(), 0.0d, eyeZPosition.getValue());
        gl.glDisable(gl.GL_LIGHTING);
     	gl.glColor3f(1.0f, 0.0f, 0.0f);
        GLUT leftEye = new GLUT();
        leftEye.glutSolidSphere(0.0125, 100, 100);
        gl.glEnable(gl.GL_LIGHTING);
        gl.glPopMatrix();
        
        //draw right eye
        gl.glPushMatrix();
        gl.glTranslated(0.5*eyeDisparity.getValue(), 0.0d, eyeZPosition.getValue());
        gl.glDisable(gl.GL_LIGHTING);
     	gl.glColor3f(0.0f, 1.0f, 1.0f);
        GLUT rightEye = new GLUT();
        rightEye.glutSolidSphere(0.0125, 100, 100);
        gl.glEnable(gl.GL_LIGHTING);
        gl.glPopMatrix();
        
        
        //near plane parameters
        double nearLeft = -width/2 * (eyeZPosition.getValue()-nearZPosition.getValue())/eyeZPosition.getValue();
    	double nearTop = height/2 * (eyeZPosition.getValue()-nearZPosition.getValue())/eyeZPosition.getValue();
    	double nearRight = -nearLeft;
    	double nearBottom = -nearTop;
    	
    	
    	//draw focal plane
    	double focalLeft = -width/2 * (eyeZPosition.getValue()-focalPlaneZPosition.getValue())/eyeZPosition.getValue();
    	double focalTop = height/2 * (eyeZPosition.getValue()-focalPlaneZPosition.getValue())/eyeZPosition.getValue();
    	double focalRight = -focalLeft;
    	double focalBottom = -focalTop;
    	gl.glMatrixMode(GL2.GL_MODELVIEW);
    	gl.glPushMatrix();
    	gl.glDisable(gl.GL_LIGHTING);
    	gl.glColor3f(0.5f, 0.5f, 0.5f);
    	gl.glTranslated(0.0d, 0.0d, focalPlaneZPosition.getValue());
        gl.glBegin(gl.GL_LINE_LOOP);
        gl.glVertex3d(focalLeft, focalBottom, 0.0d);
        gl.glVertex3d(focalLeft, focalTop, 0.0d);
        gl.glVertex3d(focalRight, focalTop, 0.0d);
        gl.glVertex3d(focalRight, focalBottom, 0.0d);
        gl.glEnd();
        gl.glEnable(gl.GL_LIGHTING);
        gl.glPopMatrix();
        
        //draw focal plane for left eye
        double focalLeftL =(-eyeDisparity.getValue()/2)+(-width/2+eyeDisparity.getValue()/2)
           		*(eyeZPosition.getValue()-focalPlaneZPosition.getValue())/(eyeZPosition.getValue()- 0.0d); //why relative coodinates?
        double focalRightL = (-eyeDisparity.getValue()/2)+(width/2+eyeDisparity.getValue()/2)
           		*(eyeZPosition.getValue()-focalPlaneZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
        double focalBottomL =(-height/2)
           		*(eyeZPosition.getValue()-focalPlaneZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
        double focalTopL = (height/2)
           		*(eyeZPosition.getValue()-focalPlaneZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
     	gl.glPushMatrix();
     	gl.glDisable(gl.GL_LIGHTING);
     	gl.glColor3f(1.0f, 0.0f, 0.0f);
     	gl.glTranslated(0.0d, 0.0d, focalPlaneZPosition.getValue());
        gl.glBegin(gl.GL_LINE_LOOP);
        gl.glVertex3d(focalLeftL, focalBottomL, 0.0d);
        gl.glVertex3d(focalLeftL, focalTopL, 0.0d);
        gl.glVertex3d(focalRightL, focalTopL, 0.0d);
        gl.glVertex3d(focalRightL, focalBottomL, 0.0d);
        gl.glEnd();
        gl.glEnable(gl.GL_LIGHTING);
        gl.glPopMatrix();
        
        //draw focal plane for right eye
        double focalLeftR =(eyeDisparity.getValue()/2)+(-width/2-eyeDisparity.getValue()/2)
        		*(eyeZPosition.getValue()-focalPlaneZPosition.getValue())/(eyeZPosition.getValue()- 0.0d); //why relative coodinates?
        double focalRightR = (eyeDisparity.getValue()/2)+(width/2-eyeDisparity.getValue()/2)
        		*(eyeZPosition.getValue()-focalPlaneZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
        double focalBottomR =(-height/2)
        		*(eyeZPosition.getValue()-focalPlaneZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
        double focalTopR = (height/2)
        		*(eyeZPosition.getValue()-focalPlaneZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glDisable(gl.GL_LIGHTING);
        gl.glColor3f(0.0f, 1.0f, 1.0f);
        gl.glTranslated(0.0d, 0.0d, focalPlaneZPosition.getValue());
        gl.glBegin(gl.GL_LINE_LOOP);
        gl.glVertex3d(focalLeftR, focalBottomL, 0.0d);
        gl.glVertex3d(focalLeftR, focalTopL, 0.0d);
        gl.glVertex3d(focalRightR, focalTopL, 0.0d);
        gl.glVertex3d(focalRightR, focalBottomL, 0.0d);
        gl.glEnd();
        gl.glEnable(gl.GL_LIGHTING);
        gl.glPopMatrix();
        
        
        //initialize matrix P
        FlatMatrix4d P = new FlatMatrix4d();
		FlatMatrix4d Pinv = new FlatMatrix4d();

        if ( viewingMode == 1 ) {
        	// We will use a trackball camera, but also apply an 
        	// arbitrary scale to make the scene and frustums a bit easier to see
        	// (note the extra scale could have been part of the initializaiton of
        	// the tbc track ball camera, but this is eaiser)
            tbc.prepareForDisplay(drawable);
            gl.glScaled(15,15,15);        
            
            gl.glPushMatrix();
            tbc2.applyViewTransformation(drawable); // only the view transformation
            scene.display( drawable );
            gl.glPopMatrix();
            
             
    		
            if(drawCenterEyeFrustum.getValue()) {
            	//near plane parameters under offset conditions
                double nearLeft2 =(focalLeft-eyeXOffset.getValue())
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue()); //why relative coodinates?
                double nearRight2 = (focalRight-eyeXOffset.getValue())
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue());
                double nearBottom2 = (focalBottom-eyeYOffset.getValue())
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue());
                double nearTop2 = (focalTop-eyeYOffset.getValue())
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue());
            	
            	//Get frustum matrix
            	gl.glMatrixMode(gl.GL_PROJECTION);
            	gl.glGetDoublev( GL2.GL_PROJECTION_MATRIX, P.asArray(), 0 );
            	gl.glPushMatrix();
            	gl.glLoadIdentity();
            	gl.glFrustum(nearLeft2, nearRight2, nearBottom2, nearTop2, eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
            	//gl.glFrustum(nearLeft, nearRight, nearBottom, nearTop, eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
            	gl.glGetDoublev( GL2.GL_PROJECTION_MATRIX, P.asArray(), 0 );
            	P.reconstitute();
            	Pinv.getBackingMatrix().invert(P.getBackingMatrix());
            	gl.glPopMatrix();
            	
            	//Draw frustum
            	gl.glMatrixMode(GL2.GL_MODELVIEW);
            	gl.glPushMatrix();
            	gl.glDisable(gl.GL_LIGHTING);
            	gl.glTranslated(eyeXOffset.getValue(), eyeYOffset.getValue(), eyeZPosition.getValue());
            	gl.glMultMatrixd(Pinv.asArray(), 0);
            	gl.glColor3f(1f, 1f, 1f);
            	glut.glutWireCube(2f);  //why 2f?
            	gl.glDisable(gl.GL_LIGHTING);
            	gl.glPopMatrix();
            	 // TODO: Objective 2 - draw camera frustum if drawCenterEyeFrustum is true
            	
            }
            
           if(drawEyeFrustums.getValue()) {
        	 
        	   //left eye frustum 
            	 //near plane parameters under offset conditions
                 double nearLeft6L =(-width/2+eyeDisparity.getValue()/2)
                   		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d); //why relative coodinates?
                 double nearRight6L = (width/2+eyeDisparity.getValue()/2)
                   		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
                 double nearBottom6L =(-height/2)
                   		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
                 double nearTop6L = (height/2)
                   		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
               	
               	//Get frustum matrix
               	gl.glMatrixMode(gl.GL_PROJECTION);
               	gl.glGetDoublev( GL2.GL_PROJECTION_MATRIX, P.asArray(), 0 );
               	gl.glPushMatrix();
               	gl.glLoadIdentity();
               	gl.glFrustum(nearLeft6L, nearRight6L, nearBottom6L, nearTop6L, 
               			eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
               	//gl.glFrustum(nearLeft, nearRight, nearBottom, nearTop, eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
               	gl.glGetDoublev( GL2.GL_PROJECTION_MATRIX, P.asArray(), 0 );
               	P.reconstitute();
               	Pinv.getBackingMatrix().invert(P.getBackingMatrix());
               	gl.glPopMatrix();
               	
               	//Draw frustum
               	gl.glMatrixMode(GL2.GL_MODELVIEW);
               	gl.glPushMatrix();
               	gl.glTranslated(-0.5*eyeDisparity.getValue(), 0, eyeZPosition.getValue()); // no 1/2!!!!!
               	gl.glMultMatrixd(Pinv.asArray(), 0);
               	gl.glDisable(gl.GL_LIGHTING);
               	gl.glColor3f(1.0f, 0.0f, 0.0f);
               	glut.glutWireCube(2f);  //why 2f?
      
               	gl.glEnable(gl.GL_LIGHTING);
               	gl.glPopMatrix();
               	
               	
              //right eye frustum 
           	 //near plane parameters under offset conditions
                double nearLeft6R =(width/2-eyeDisparity.getValue()/2)
                  		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d); //why relative coodinates?
                double nearRight6R = (-width/2-eyeDisparity.getValue()/2)
                  		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
                double nearBottom6R =(-height/2)
                  		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
                double nearTop6R = (height/2)
                  		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
              	
              	//Get frustum matrix
              	gl.glMatrixMode(gl.GL_PROJECTION);
              	gl.glGetDoublev( GL2.GL_PROJECTION_MATRIX, P.asArray(), 0 );
              	gl.glPushMatrix();
              	gl.glLoadIdentity();
              	gl.glFrustum(nearLeft6R, nearRight6R, nearBottom6R, nearTop6R, 
              			eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
              	//gl.glFrustum(nearLeft, nearRight, nearBottom, nearTop, eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
              	gl.glGetDoublev( GL2.GL_PROJECTION_MATRIX, P.asArray(), 0 );
              	P.reconstitute();
              	Pinv.getBackingMatrix().invert(P.getBackingMatrix());
              	gl.glPopMatrix();
              	
              	//Draw frustum
              	gl.glMatrixMode(GL2.GL_MODELVIEW);
              	gl.glPushMatrix();
              	gl.glTranslated(0.5*eyeDisparity.getValue(), 0, eyeZPosition.getValue()); // no 1/2!!!!!
              	gl.glMultMatrixd(Pinv.asArray(), 0);
              	gl.glDisable(gl.GL_LIGHTING);
              	gl.glColor3f(0.0f, 0.0f, 1.0f);
              	glut.glutWireCube(2f);  //why 2f?
              	gl.glEnable(gl.GL_LIGHTING);
              	gl.glPopMatrix();
               	
        	  
           
           }
            
            // TODO: Objective 6 - draw left and right eye frustums if drawEyeFrustums is true
            
        } else if ( viewingMode == 2 ) {
        	GLU glu = new GLU();
        	gl.glMatrixMode(gl.GL_PROJECTION);
        	gl.glLoadIdentity();
        	gl.glFrustum(nearLeft, nearRight, nearBottom, nearTop, eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
        	gl.glMatrixMode(gl.GL_MODELVIEW);
        	gl.glPushMatrix();
        	gl.glLoadIdentity();
        	glu.gluLookAt(0, 0, eyeZPosition.getValue(), 0, 0, -1, 0, 1, 0);
        	scene.display(drawable);
        	gl.glPopMatrix();
        	
        	
        } else if ( viewingMode == 3 ) { 
        	//get the number of sample
        	int numSamples = samples.getValue();
        	gl.glClear(gl.GL_ACCUM_BUFFER_BIT);
        	Accum accum = new Accum();
        	accum.glAccumLoadZero(drawable);
        	
        	for(int i = 0; i< numSamples; i++) {
        		//get eyeOffset
        		Point2d eyeOffset = new Point2d();
        		fpd.get(eyeOffset, i, numSamples);
        		eyeOffset.scale(aperture.getValue());
        		
        		//frustum projection
        		double nearLeft3 =(focalLeft-eyeOffset.x)
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue()); //why relative coodinates?
                double nearRight3 = (focalRight-eyeOffset.x)
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue());
                double nearBottom3 = (focalBottom-eyeOffset.y)
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue());
                double nearTop3 = (focalTop-eyeOffset.y)
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue());
        		GLU glu = new GLU();
            	gl.glMatrixMode(gl.GL_PROJECTION);
            	gl.glLoadIdentity();
            	gl.glFrustum(nearLeft3, nearRight3, nearBottom3, nearTop3, eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
            	gl.glMatrixMode(gl.GL_MODELVIEW);
            	gl.glPushMatrix();
            	gl.glLoadIdentity();
            	glu.gluLookAt(eyeOffset.x, eyeOffset.y, eyeZPosition.getValue(), 0, 0, focalPlaneZPosition.getValue(), 0, 1, 0);
            	scene.display(drawable);
            	accum.glAccum(drawable, 1f/numSamples);
            	gl.glPopMatrix();
        	}
        	accum.glAccumReturn(drawable);
        	
        	
        	
        	
        	// TODO: Objective 5 - draw center eye with depth of field blur
            
        } else if ( viewingMode == 4 ) {
        	//near plane parameters under offset conditions
            double nearLeft4L =(-width/2+eyeDisparity.getValue()/2)
              		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d); //why relative coodinates?
            double nearRight4L = (width/2+eyeDisparity.getValue()/2)
              		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
            double nearBottom4L =(-height/2)
              		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
            double nearTop4L = (height/2)
              		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
          	
          	//Get frustum matrix
          	gl.glMatrixMode(gl.GL_PROJECTION);
          	gl.glLoadIdentity();
          	gl.glFrustum(nearLeft4L, nearRight4L, nearBottom4L, nearTop4L, 
          			eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
          	//gl.glFrustum(nearLeft, nearRight, nearBottom, nearTop, eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
          	gl.glMatrixMode(gl.GL_MODELVIEW);
        	gl.glPushMatrix();
        	gl.glLoadIdentity();
        	GLU glu = new GLU();
        	glu.gluLookAt(-eyeDisparity.getValue()/2, 0.0, eyeZPosition.getValue(), -eyeDisparity.getValue()/2, 0, -1, 0, 1, 0);
        	
       
        	scene.display(drawable);
        	gl.glPopMatrix();
            
          	
            // TODO: Objective 6 - draw the left eye view
        	
        } else if ( viewingMode == 5 ) {  
            
        	double nearLeft5R =(-width/2-eyeDisparity.getValue()/2)
              		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d); //why relative coodinates?
            double nearRight5R = (width/2-eyeDisparity.getValue()/2)
              		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
            double nearBottom5R =(-height/2)
              		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
            double nearTop5R = (height/2)
              		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
          	//Get frustum matrix
          	gl.glMatrixMode(gl.GL_PROJECTION);
          	gl.glLoadIdentity();
          	gl.glFrustum(nearLeft5R, nearRight5R, nearBottom5R, nearTop5R, 
          			eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());		
          	gl.glMatrixMode(gl.GL_MODELVIEW);
        	gl.glPushMatrix();
        	gl.glLoadIdentity();
        	GLU glu = new GLU();
        	glu.gluLookAt(eyeDisparity.getValue()/2, 0.0, eyeZPosition.getValue(), eyeDisparity.getValue()/2, 0, -1, 0, 1, 0);
        	
        	
        	scene.display(drawable);
        	gl.glPopMatrix();
        	
        	// TODO: Objective 6 - draw the right eye view
        	                               
        } else if ( viewingMode == 6 ) { 
        	 double nearLeft4L =(-width/2+eyeDisparity.getValue()/2)
               		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d); //why relative coodinates?
             double nearRight4L = (width/2+eyeDisparity.getValue()/2)
               		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
             double nearBottom4L =(-height/2)
               		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
             double nearTop4L = (height/2)
               		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
           	
           	//Get frustum matrix
           	gl.glMatrixMode(gl.GL_PROJECTION);
           	gl.glLoadIdentity();
           	gl.glFrustum(nearLeft4L, nearRight4L, nearBottom4L, nearTop4L, 
           			eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
           	//gl.glFrustum(nearLeft, nearRight, nearBottom, nearTop, eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
           	gl.glMatrixMode(gl.GL_MODELVIEW);
         	gl.glPushMatrix();
         	gl.glLoadIdentity();
         	GLU glu = new GLU();
         	glu.gluLookAt(-eyeDisparity.getValue()/2, 0.0, eyeZPosition.getValue(), -eyeDisparity.getValue()/2, 0, -1, 0, 1, 0);
         	
         	gl.glColorMask( true, false, false, true );
         	gl. glClear( gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT );
         	scene.display(drawable);
         	gl.glPopMatrix();
         	
         	double nearLeft5R =(-width/2-eyeDisparity.getValue()/2)
              		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d); //why relative coodinates?
            double nearRight5R = (width/2-eyeDisparity.getValue()/2)
              		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
            double nearBottom5R =(-height/2)
              		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
            double nearTop5R = (height/2)
              		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()- 0.0d);
          	
          	gl.glMatrixMode(gl.GL_PROJECTION);
          	gl.glLoadIdentity();
          	gl.glFrustum(nearLeft5R, nearRight5R, nearBottom5R, nearTop5R, 
          	eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());		
          	gl.glMatrixMode(gl.GL_MODELVIEW);
        	gl.glPushMatrix();
        	gl.glLoadIdentity();
        	GLU glu2 = new GLU();
        	glu2.gluLookAt(eyeDisparity.getValue()/2, 0.0, eyeZPosition.getValue(), eyeDisparity.getValue()/2, 0, -1, 0, 1, 0);
        	
        	gl.glColorMask( false, true, true, true );
        	gl. glClear( gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT );
        	scene.display(drawable);
        	gl.glPopMatrix();
        	
        	gl.glColorMask(true, true, true, false);
        	// TODO: Objective 7 - draw the anaglyph view using glColouMask
        	
        } else if ( viewingMode == 7 ) {   
         
            //prepare for draw images 
            int numSamples = samples.getValue();
          	gl.glClear(gl.GL_ACCUM_BUFFER_BIT);
          	Accum accum = new Accum();
          	accum.glAccumLoadZero(drawable);
          	//get eyeOffset
      		Point2d eyeOffset = new Point2d();
 
          	//left eye images
      		gl.glColorMask( true, false, false, true );
         	gl. glClear( gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT );
          	for(int i = 0; i< numSamples; i++) {
          		fpd.get(eyeOffset, i, numSamples);
          		eyeOffset.scale(aperture.getValue());
          		//frustum projection
          		double nearLeft7L =(focalLeftL+eyeDisparity.getValue()/2-eyeOffset.x)
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue()); //why relative coodinates?
                double nearRight7L = (focalRightL+eyeDisparity.getValue()/2-eyeOffset.x)
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue());
                double nearBottom7L = (focalBottomL-eyeOffset.y)
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue());
                double nearTop7L = (focalTopL-eyeOffset.y)
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue());
          		GLU glu = new GLU();
              	gl.glMatrixMode(gl.GL_PROJECTION);
              	gl.glLoadIdentity();
              	gl.glFrustum(nearLeft7L, nearRight7L, nearBottom7L, nearTop7L, eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
              	gl.glMatrixMode(gl.GL_MODELVIEW);
              	gl.glPushMatrix();
              	gl.glLoadIdentity();
              	glu.gluLookAt((eyeOffset.x)-eyeDisparity.getValue()/2, eyeOffset.y, eyeZPosition.getValue(), (eyeOffset.x)-eyeDisparity.getValue()/2, eyeOffset.y, focalPlaneZPosition.getValue(), 0, 1, 0);
              	
              	scene.display(drawable);
              	accum.glAccum(drawable, 1f/(2f*numSamples));
              	gl.glPopMatrix();
          	}
          	
          	//right eye images
          	gl.glColorMask( false, true, true, true );
        	gl. glClear( gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT );
          	for(int i = 0; i< numSamples; i++) {
          		fpd.get(eyeOffset, i, numSamples);
          		eyeOffset.scale(aperture.getValue());
          		double nearLeft7R =(focalLeftR-eyeDisparity.getValue()/2-eyeOffset.x)
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue()); //why relative coodinates?
                double nearRight7R = (focalRightR-eyeDisparity.getValue()/2-eyeOffset.x)
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue());
                double nearBottom7R = (focalBottomR-eyeOffset.y)
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue());
                double nearTop7R = (focalTopR-eyeOffset.y)
                		*(eyeZPosition.getValue()-nearZPosition.getValue())/(eyeZPosition.getValue()-focalPlaneZPosition.getValue());
          		GLU glu = new GLU();
              	gl.glMatrixMode(gl.GL_PROJECTION);
              	gl.glLoadIdentity();
              	gl.glFrustum(nearLeft7R, nearRight7R, nearBottom7R, nearTop7R, eyeZPosition.getValue()-nearZPosition.getValue(), eyeZPosition.getValue()-farZPosition.getValue());	
              	gl.glMatrixMode(gl.GL_MODELVIEW);
              	gl.glPushMatrix();
              	gl.glLoadIdentity();
              	glu.gluLookAt((eyeOffset.x)+eyeDisparity.getValue()/2, eyeOffset.y, eyeZPosition.getValue(), (eyeOffset.x)+eyeDisparity.getValue()/2, eyeOffset.y, focalPlaneZPosition.getValue(), 0, 1, 0);
              	
              	scene.display(drawable);
              	accum.glAccum(drawable, 1f/(2f*numSamples));
              	gl.glPopMatrix();
          	}
          	accum.glAccumReturn(drawable);
          	
          	gl.glColorMask(true, true, true, false);
        	
        	// TODO: Bonus Ojbective 8 - draw the anaglyph view with depth of field blur
        	
        }        
    }
    
}
