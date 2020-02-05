package comp557.a4;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import javax.vecmath.Color4f;

/**
 * Simple scene loader based on XML file format.
 */
public class Scene {
    
    /** List of surfaces in the scene */
    public List<Intersectable> surfaceList = new ArrayList<Intersectable>();
	
	/** All scene lights */
	public Map<String,Light> lights = new HashMap<String,Light>();

    /** Contains information about how to render the scene */
    public Render render;
    
    /** The ambient light colour */
    public Color3f ambient = new Color3f();
 

    /** 
     * Default constructor.
     */
    public Scene() {
    	this.render = new Render();
    }
    
    /**
     * renders the scene
     */
    public void render(boolean showPanel) {
        Camera cam = render.camera; 
        int w = cam.imageSize.width;
        int h = cam.imageSize.height;
        render.init(w, h, showPanel);
        
        RenderThread t1 = new RenderThread(this, cam, w, h, 1);
        RenderThread t2 = new RenderThread(this, cam, w, h, 2);
        RenderThread t3 = new RenderThread(this, cam, w, h, 3);
        RenderThread t4 = new RenderThread(this, cam, w, h, 4);
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        
        render.save();
        render.waitDone();
        
    }

 
    
    
    /**
     * Generate a ray through pixel (i,j).
     * 
     * @param i The pixel row.
     * @param j The pixel column.
     * @param offset The offset from the center of the pixel, in the range [-0.5,+0.5] for each coordinate. 
     * @param cam The camera.
     * @param ray Contains the generated ray.
     */
    //ray ends at result
    public Color4f getColor(Ray ray, IntersectResult result) {
    	Color4f c = new Color4f();
    	if(result.t == Double.POSITIVE_INFINITY) {
    		c.x = render.bgcolor.x;
    		c.y = render.bgcolor.y;
    		c.z = render.bgcolor.z;
    		return c;
    	}
    	
    	//Ambient color
    	c.x = ambient.x * result.material.diffuse.x;
    	c.y = ambient.y * result.material.diffuse.y;
    	c.z = ambient.z * result.material.diffuse.z;
    	
    	//multi light resources contribute to result point
    	for(Light light: lights.values()) {
    		Ray shadowRay = new Ray();
    		IntersectResult shadowResult = new IntersectResult();
    		
    		int rowNum = 5;
    		int columnNum = 5; //the light plane is separated to rowNum*columnNum  sub plane.
    		Color4f lightAccumColor = new Color4f();
    		for(int i = 0; i < rowNum; i++) {
        		for(int j = 0; j < columnNum; j++) {
        			Light jitterLight = new Light();
            		jitterLight.color.set(light.color);
            		jitterLight.from.set(light.from);
            		jitterLight.power = light.power;
            		Vector3d a = new Vector3d(2, 2, 0);
            		Vector3d b = new Vector3d(0, 2, 2);
            		a.scale((double)i/(double)rowNum); //delta
            		b.scale((double)j/(double)columnNum);
            		//jitterLight.from = light.from + epsilon1*a + epsilon2*b
            		jitterLight.from.x = jitterLight.from.x+a.x+b.x;
            		jitterLight.from.y = jitterLight.from.y+a.y+b.y;
            		jitterLight.from.z = jitterLight.from.z+a.z+b.z;
            		
            		
            		if(!inShadow(result, surfaceList, jitterLight, shadowResult, shadowRay)) {
        	    		//Lambertian shader
        	    		Color3f ld = new Color3f();
        	    		Vector3d l = new Vector3d();
        	    		
        	    		//Area light according to FCG p340
        	    		l.sub(jitterLight.from, result.p);
        	    		l.normalize();
        	    		double max = Math.max(0.0, l.dot(result.n));
        	    		ld.x = (float)(light.color.x * light.power * result.material.diffuse.x * max);
        	    		ld.y = (float)(light.color.y * light.power * result.material.diffuse.y * max);
        	    		ld.z = (float)(light.color.z * light.power * result.material.diffuse.z * max);
        	    		
        	    		//Blinn-Phone shader
        	    		Color3f ls = new Color3f();
        	    		Vector3d h = new Vector3d();
        	    		Vector3d v = new Vector3d();
        	    		v.sub(ray.eyePoint, result.p);
        	    		v.normalize();
        	    		h.add(l, v);
        	    		h.normalize();
        	    		max = Math.max(0.0, h.dot(result.n));
        	    		ls.x = (float)( light.color.x * light.power * result.material.specular.x * Math.pow(max, result.material.shinyness));
        	    		ls.y = (float)( light.color.y * light.power * result.material.specular.y * Math.pow(max, result.material.shinyness));
        	    		ls.z = (float)( light.color.z * light.power * result.material.specular.z * Math.pow(max, result.material.shinyness));
        	    		
        	    		//update c
        	    		lightAccumColor.x += ld.x+ls.x;
        	    		lightAccumColor.y += ld.y+ls.y;
        	    		lightAccumColor.z += ld.z+ls.z;
            		}
            		
            		if(result.material.mirror != null) {
            			//Firstly calculate reflection ray
            			Ray mirrorRay = new Ray();
            			IntersectResult mirrorResult = new IntersectResult();
            			mirrorRay.eyePoint.set(result.p); //Firstly origin is intersection point
            			mirrorRay.viewDirection.set(ray.viewDirection); //initialize to d temply, finally to r = d-2(dn)n textbook 103
            			
            			Vector3d d = new Vector3d();
            			Vector3d n = new Vector3d();
            			d.set(ray.viewDirection);
            			n.set(result.n);
            			n.scale(-2.0*d.dot(n)); // n = -2(dn)n
            			mirrorRay.viewDirection.add(n); //finally
            			
            			Vector3d epsilon = new Vector3d();
            			epsilon.set(mirrorRay.viewDirection);
            			epsilon.scale(1e-8);
            			mirrorRay.eyePoint.add(epsilon); //add a very small offset in the mirror ray direction to avoid t = 0 
            			
            			for(Intersectable surface: surfaceList) {
            				surface.intersect(mirrorRay, mirrorResult); //see the intersection of mirror ray with the scene
            			}
            			Color4f mirrorColor = getColor(mirrorRay, mirrorResult);
            			lightAccumColor.x += result.material.mirror.x*mirrorColor.x;
            			lightAccumColor.y += result.material.mirror.y*mirrorColor.y;
            			lightAccumColor.z += result.material.mirror.z*mirrorColor.z;
            		}
        		}
    		}
    		lightAccumColor.scale(1/(float)(rowNum*columnNum));
    		c.add(lightAccumColor);

    		
    		
    	}
    	
    	return c;
    }

	public static void generateRay(final int i, final int j, final double[] offset, final Camera cam, Ray ray) {
		// i: column  j: row
		double width = cam.imageSize.getWidth();
		double height = cam.imageSize.getHeight();
		Point3d eyePoint = new Point3d();
		eyePoint.set(cam.from);
		ray.eyePoint.set(eyePoint);
		
		double dToImage = cam.from.distance(cam.to); //distance from camera to image plane
		double focalLength = cam.focalLength; //Distance from camera to focal plane
		
		//For pixel (j, i), we calculate it's camera coordinates (u, v) first.
		double aspectRatio = width / height;
		double top = Math.tan(Math.toRadians(cam.fovy)/2.0) * dToImage;
		
		double left = -aspectRatio*top;
		double right = aspectRatio*top;
		double bot = -top;
		double u = left + (right - left) * (j+0.5+offset[0]) / width;
		double v = bot + (top - bot) * (i+0.5+offset[1]) / height; 
		
		//Then transfer the camera coordinates (u, v) to world coordinates (x, y, z).
		Vector3d wCam = new Vector3d();
		wCam.sub(cam.from, cam.to);
		wCam.normalize(); 
		Vector3d uCam = new Vector3d();
		uCam.cross(cam.up, wCam);
		uCam.normalize();
		Vector3d vCam = new Vector3d();
		vCam.cross(uCam, wCam); //Why not w cross u?
		vCam.normalize();
		uCam.scale(u);
		vCam.scale(v);
		wCam.scale(dToImage);
		
		Vector3d s = new Vector3d(cam.from);
		s.add(uCam);
		s.add(vCam);
		s.sub(wCam);
		s.sub(cam.from);
		s.normalize();
		ray.viewDirection.set(s);
	}
	
	public static void generateRay(final int i, final int j, final double[] pixelOffset, Point3d eyeOffset, final Camera cam, Ray ray) {
	//Calculate direction and origin of ray for pixel (j, i) when there is a focal length attribute in camera.
	// i: column  j: row
		
		//Calculate eyePoint and assign it to ray.eyePoint
		double width = cam.imageSize.getWidth();
		double height = cam.imageSize.getHeight();
		Point3d eyePoint = new Point3d();
		eyePoint.set(cam.from);
		eyePoint.add(eyeOffset); //small eyeOffset to simulate the focal lens
		ray.eyePoint.set(eyePoint);
		
		double dToImage = cam.from.distance(cam.to); //distance from camera to image plane
		double focalLength = cam.focalLength; //Distance from camera to focal plane
		
		//For pixel (j, i), we calculate it's camera coordinates (u, v) first.
		double aspectRatio = width / height;
		double top = Math.tan(Math.toRadians(cam.fovy)/2.0) * dToImage;
		
		double left = -aspectRatio*top;
		double right = aspectRatio*top;
		double bot = -top;
		double u = left + (right - left) * (j+0.5+pixelOffset[0]) / width;
		double v = bot + (top - bot) * (i+0.5+pixelOffset[1]) / height; 
		
		//Then transfer the camera coordinates (u, v) to world coordinates (x, y, z).
		Vector3d wCam = new Vector3d();
		wCam.sub(cam.from, cam.to);
		wCam.normalize(); 
		Vector3d uCam = new Vector3d();
		uCam.cross(cam.up, wCam);
		uCam.normalize();
		Vector3d vCam = new Vector3d();
		vCam.cross(uCam, wCam); //Why not w cross u?
		vCam.normalize();
		uCam.scale(u);
		vCam.scale(v);
		wCam.scale(-dToImage);
		double xPixel = cam.from.x + uCam.x + vCam.x + wCam.x;
		double yPixel = cam.from.y + uCam.y + vCam.y + wCam.y;
		double zPixel = cam.from.z + uCam.z + vCam.z + wCam.z; //world coordinates (x, y, z) of pixel (j, i)
		
		//Find the corresponding focal plane world coordinates.
		double xFocal, yFocal, zFocal;
		double tmpRatio = focalLength / dToImage;
		xFocal = (xPixel-cam.from.x) * tmpRatio + cam.from.x;
		yFocal = (yPixel-cam.from.y) * tmpRatio + cam.from.y;
		zFocal = (zPixel-cam.from.z) * tmpRatio + cam.from.z; //world coordinates of corresponding focal point
		
		//Calculate direction of ray
		Vector3d dirToFocal = new Vector3d(xFocal, yFocal, zFocal);
		dirToFocal.sub(ray.eyePoint); //dirToFocal = focalPointCoordinates - eyePoint
		dirToFocal.normalize();
		ray.viewDirection.set(dirToFocal);	
	}

	/**
	 * Shoot a shadow ray in the scene and get the result.
	 * 
	 * @param result Intersection result from raytracing. 
	 * @param light The light to check for visibility.
	 * @param root The scene node.
	 * @param shadowResult Contains the result of a shadow ray test.
	 * @param shadowRay Contains the shadow ray used to test for visibility.
	 * 
	 * @return True if a point is in shadow, false otherwise. 
	 */
	public static boolean inShadow(final IntersectResult result, final List<Intersectable> surfaces, final Light light, IntersectResult shadowResult, Ray shadowRay) {
		

		shadowRay.viewDirection.x = light.from.x - result.p.x;
		shadowRay.viewDirection.y = light.from.y - result.p.y;
		shadowRay.viewDirection.z = light.from.z - result.p.z;
		
		Point3d eyePoint = new Point3d();
		Vector3d d = new Vector3d();
		d.set(shadowRay.viewDirection);
		d.scale(0.000001);
		eyePoint.set(result.p);
		eyePoint.add(d);
		shadowRay.eyePoint.set(eyePoint);
		
		for(Intersectable surface: surfaces) {
			surface.intersect(shadowRay, shadowResult);
		}
		
		if(shadowResult.t>0 && shadowResult.t < 1) {
			return true;
		}
		return false;

	}    
	
	

	class RenderThread extends Thread{
		int left; //first rendering column position 
		int right; // last rendering column position
		int top; //first rendering row position
		int bot; //last rendering row postion
		Scene scene;
		Camera cam;
		int num;
		
		//We use 4 threads to render our scene. Each thread is indexed by num. Each thread has its own unique left, right, top and bot.
		RenderThread(Scene scene, Camera cam, int w, int h, int num){
			this.scene = scene;
			this.cam = cam;
			this.num = num;
			if(this.num == 1) {
				this.left = 0;
				this.right = w/2;
				this.top = 0;
				this.bot = h/2;
			}
			if(this.num == 2) {
				this.left = w/2;
				this.right = w;
				this.top = 0;
				this.bot = h/2;
			}
			if(this.num == 3) {
				this.left = 0;
				this.right = w/2;
				this.top = h/2;
				this.bot = h;
			}
			if(this.num == 4) {
				this.left = w/2;
				this.right = w;
				this.top = h/2;
				this.bot = h;
			}
			
		}
		
		//rendering function.
		public void run() {
			for ( int i = top; i < bot && !render.isDone(); i++ ) {
	            for ( int j = left; j < right && !render.isDone(); j++ ) {
	            	
	            	Color4f c = new Color4f();
	            	//If there is a focal length attribute in .xml (which means that cam.focalLength>0), we use different eye positions.
	            	if(cam.focalLength > 0) {
	            		Color4f EyeAccumColor = new Color4f();
	            		int eyeSamplesNum = 10;
	            		for (int e = 0; e<eyeSamplesNum; e++) {
	            			Point3d currentEyeOffset = cam.getEyeOffset(); //Get the current eye offset
	    	            	
	            			//For each pixel, we separate it into subpixels. 
	            			// We set a PixelAccumColor to store each subpixel color and finally scale it.
	            			Color4f PixelAccumColor = new Color4f();
	            			int numColumn = (int) Math.sqrt(render.samples);  //Pixel column number. Ideally samples is a square number.
	    	            	int numRow = render.samples/numColumn; //Separated pixel row number.
	    	            	int extra = render.samples - numColumn*numRow; // If render.samples is not a square number.
	    	            	//m: the number of rows
	    	            	for(int m = 0; m < numRow; m++) {
	    	            		int currentColumn = numColumn;
	    	            		if(m == 0) currentColumn += extra; //We add the remainder to the first row as extra columns.
	    	            		//n: the number of columns.
	    	            		for(int n = 0; n < numColumn; n++) {
	    	            			double distanceColumn = 1.0/(currentColumn+1);
	    	            			double distanceRow = 1.0/(numRow+1);
	    	            			double currentX;
	    	            			double currentY;
	    	            			//If there is a render.jitter attribute, we need to add jitter to each subpixel center.
	    	            			//If not, we use subpixel center directly.
	    	            			if(render.jitter) { 
	    	            				currentX = -0.5+(n+Math.random()) * distanceColumn;
	    	            				currentY = -0.5+(m+Math.random()) * distanceRow;
	    	            			}
	    	            			else {
	    	            				currentX = -0.5+(n+0.5) * distanceColumn;
	    	            				currentY = -0.5+(m+0.5) * distanceRow;
	    	            			}           			
	    	            			double[] currentOffset = {currentX, currentY};
	    	            			Ray currentRay = new Ray();
	    	            			generateRay(i, j, currentOffset, currentEyeOffset, cam, currentRay);
	    	            			
	    	            			//Find the intersection between current ray and scene.
	    	            			IntersectResult currentResult = new IntersectResult();
	    	            			for(Intersectable surface: surfaceList) {
	    	            				surface.intersect(currentRay, currentResult);
	    	            			}
	    	            			Color4f currentColor = getColor(currentRay, currentResult);
	    	            			PixelAccumColor.add(currentColor);
	    	            		}
	    	            	}
	    	            	PixelAccumColor.scale(1/(float)render.samples);
	    	            	EyeAccumColor.add(PixelAccumColor);
	            		}
	            		EyeAccumColor.scale(1/(float)eyeSamplesNum);
	            		c.set(EyeAccumColor);	
	            	}
	            	
	            	//If there is no cam.focalLength in .xml(which means cam.focalLength < 0)
	            	else {
	            		//For each pixel, we separate it into many subpixels.
		            	ArrayList<Ray> rays = new ArrayList<Ray>(); //store the sample rays per pixel
		            	int numColumn = (int) Math.sqrt(render.samples);
		            	int numRow = render.samples/numColumn;
		            	int extra = render.samples - numColumn*numRow;
		            	//m: the number of rows
		            	for(int m = 0; m < numRow; m++) {
		            		int currentColumn = numColumn;
		            		if(m == 0) currentColumn += extra;
		            		//n: the number of columns.
		            		for(int n = 0; n < numColumn; n++) {
		            			double distanceColumn = 1.0/(currentColumn+1);
		            			double distanceRow = 1.0/(numRow+1);
		            			double currentX;
		            			double currentY;
		            			if(render.jitter) {
		            				currentX = -0.5+(n+Math.random()) * distanceColumn;
		            				currentY = -0.5+(m+Math.random()) * distanceRow;
		            			}
		            			else {
		            				currentX = -0.5+(n+0.5) * distanceColumn;
		            				currentY = -0.5+(m+0.5) * distanceRow;
		            			}           			
		            			double[] currentOffset = {currentX, currentY};
		            			Ray currentRay = new Ray();
		            			generateRay(i, j, currentOffset, cam, currentRay);
		            			rays.add(currentRay);
		            		}
		            	}
		            	
		            	for(Ray ray: rays) {
		            		IntersectResult result = new IntersectResult();
		            		for(Intersectable surface: surfaceList) {
		            			surface.intersect(ray, result);
		            			result.n.normalize();
		            		}
		                	Color4f tempColor = new Color4f();
		            		tempColor = getColor(ray, result);
		            		c.add(tempColor);
		            	}
		            	float colorScale = 1/(float)render.samples; //Average the accumulated colors
		            	c.scale(colorScale);
	            	}

	            	//Show color
	            	int r = (int)(255*c.x);
	                int g = (int)(255*c.y);
	                int b = (int)(255*c.z);
	                
	                if(r > 255){
						r = 255;
					}
					if(g > 255){
						g = 255;
					}
					if(b > 255){
						b = 255;
					}     
	                int a = 255;
	                int argb = (a<<24 | r<<16 | g<<8 | b);    
	                // update the render image
	                render.setPixel(j, i, argb);
	            }
	        }
	        
	        // save the final render image
	        render.save();
	        
	        // wait for render viewer to close
	        render.waitDone();
		}
	}
	
}

	

