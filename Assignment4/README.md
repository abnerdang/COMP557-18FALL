
Only the bonus marks listed below:
 
1. Soft shadows 
Implemented in Scene.getColor(). For each light source, we generate a jitterLight with slightly different light.from. the light.from is 
jitterLight.from = light.from + delta1*vectorA + delta2*vectorB (0<delta<1).

2. Mirror Reflection (Verify MirrorReflectionPlane.xml)
Implemented in the function getColor(). Firstly we should check whether there is mirror attribute in material attribute list. 
Once the answer is yes, we then create the mirror reflection ray. Find the the nearest intersection between mirror ray and objects. Then get the corresponding	color.


3. Multi-threading
Creat RenderThread.class in the Scene.java. We split the screen pixel to 4 screen rectangle equally. So we create 4 RenderThread instances. In this way we can accelerate our rendering process.

4. Quadrics (Verify Quadrics.xml)
1) Create class Quadrics.java. Implement function intersect(). The function of Quadrics is x^2/a^2+y^2/b^2 = 2*z
2) Implement function createQuadrics().
3) Create a novel scene Quadrics.xml.

5. Depth of field blur (Check BoxStacksDepthFieldBlur.xml)
1) Add the focalLengh Attribute in Camera.java.
2) Implement function generateRay() in Scene.java, Which has the same name  but one more argument eyeOffset.
3) Implement function getEyeOffset() in Camera.java.
4) Modify createCamera() in Parser.java to add focalLength to camera.

 