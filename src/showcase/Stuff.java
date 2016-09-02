/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package showcase;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Environment;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import java.util.HashMap;
import java.util.Random;

/**
 *
 * @author rudz
 */
public abstract class Stuff extends SimpleApplication {

    protected BulletAppState bulletAppState;
    // constants
    protected final String FLOOR = "Floor";
    protected final String CAR = "Car";
    protected final String WHEEL_FRONT_RIGHT = "WheelFrontRight";
    protected final String WHEEL_FRONT_LEFT = "WheelFrontLeft";
    protected final String WHEEL_BACK_RIGHT = "WheelBackRight";
    protected final String WHEEL_BACK_LEFT = "WheelBackLeft";
    protected final String LEFTS = "Lefts";
    protected final String RIGHTS = "Rights";
    protected final String UPS = "Ups";
    protected final String DOWNS = "Downs";
    protected final String SPACE = "Space";
    protected final String RESET = "Reset";
    // Audio
    protected AudioNode nature, waves, engine;
    protected final float TURN_FORCE = 0.5f;
    protected final float ACCEL_RATE = 800f;
    protected final float BRAKE_FORCE = 50f;
    // terrain stuff
    protected final float grassScale = 64;
    protected final float dirtScale = 16;
    protected final float rockScale = 128;
    protected TerrainQuad terrain;
    protected Material matRock;
    // geometry map
    protected HashMap<String, Geometry> geomMap;
    // player stuff
    protected VehicleControl player;
    private float wheelRadius;
    protected float steeringValue = 0f;
    protected float accelerationValue = 0f;
    private Node carNode;
    private ChaseCamera chaseCam;

    protected Stuff() {
        geomMap = new HashMap<>();
    }

    @Override
    public abstract void simpleInitApp();

        /**
     * creates a simple physics test world with a floor, an obstacle and some
     * test boxes
     *
     * @param rootNode
     * @param assetManager
     * @param space
     */
    protected void createPhysicsTestWorld() {
        AmbientLight light = new AmbientLight();
        light.setColor(ColorRGBA.DarkGray);
        rootNode.addLight(light);

        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/Pond/Pond.jpg"));

        PhysicsSpace space = bulletAppState.getPhysicsSpace();

        //movable boxes
        light = new AmbientLight();
        light.setColor(ColorRGBA.Green);
        Random rnd = new Random();
        Box box = new Box(0.25f, 0.25f, 0.25f);
        for (int i = 0; i < 400; i++) {
            Geometry boxGeometry = new Geometry("Box", box);
            boxGeometry.setMaterial(material);
            boxGeometry.setLocalTranslation(rnd.nextInt(200), 5, (i % 4 <= 2 ? -rnd.nextInt(4) : rnd.nextInt()));
            //boxGeometry.setLocalTranslation(i, 5, -3);

            //RigidBodyControl automatically uses box collision shapes when attached to single geometry with box mesh
            boxGeometry.addControl(new RigidBodyControl(2));
            boxGeometry.addLight(light);
            rootNode.attachChild(boxGeometry);
            space.add(boxGeometry);
        }

        //immovable sphere with mesh collision shape
        Sphere sphere = new Sphere(8, 8, 1);
        Geometry sphereGeometry = new Geometry("Sphere", sphere);

        sphereGeometry.setMaterial(material);
        sphereGeometry.setLocalTranslation(4, -4, 2);
        sphereGeometry.addControl(new RigidBodyControl(new MeshCollisionShape(sphere), 0));
        rootNode.attachChild(sphereGeometry);
        space.add(sphereGeometry);
    }

    protected void configureCamera() {
        //cam.setLocation(player.getPhysicsLocation().mult(20f));
        chaseCam = new ChaseCamera(cam, carNode);
        chaseCam.setSmoothMotion(true);
        chaseCam.setChasingSensitivity(1f);
        chaseCam.setLookAtOffset(new Vector3f(0.0f, 0.0f, 1.0f));
        chaseCam.setMaxVerticalRotation(1f);
        chaseCam.setDefaultDistance(20f);
        chaseCam.setDownRotateOnCloseViewOnly(true);
        chaseCam.setTrailingEnabled(true);
    }

    protected void setupKeys(final ActionListener actionListener) {
        inputManager.addMapping(LEFTS, new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping(LEFTS, new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping(RIGHTS, new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping(RIGHTS, new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping(UPS, new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping(UPS, new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping(DOWNS, new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping(DOWNS, new KeyTrigger(KeyInput.KEY_DOWN));

        inputManager.addMapping(SPACE, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping(RESET, new KeyTrigger(KeyInput.KEY_RETURN));

        inputManager.addListener(actionListener, LEFTS);
        inputManager.addListener(actionListener, RIGHTS);
        inputManager.addListener(actionListener, UPS);
        inputManager.addListener(actionListener, DOWNS);
        inputManager.addListener(actionListener, SPACE);
        inputManager.addListener(actionListener, RESET);
    }

    protected Spatial loadSky() {
        return SkyFactory.createSky(assetManager,
                assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_west.jpg"),
                assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_east.jpg"),
                assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_north.jpg"),
                assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_south.jpg"),
                assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_up.jpg"),
                assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_down.jpg"));
    }

    protected void initAmbience() {
        float[] eax = new float[]{15, 38.0f, 0.300f, -1000, -3300, 0,
            1.49f, 0.54f, 1.00f, -2560, 0.162f, 0.00f, 0.00f,
            0.00f, -229, 0.088f, 0.00f, 0.00f, 0.00f, 0.125f, 1.000f,
            0.250f, 0.000f, -5.0f, 5000.0f, 250.0f, 0.00f, 0x3f};
        Environment env = new Environment(eax);
        audioRenderer.setEnvironment(env);

        waves = new AudioNode(assetManager, "Sound/Environment/Ocean Waves.ogg", false);
        waves.setPositional(true);
        waves.setLocalTranslation(new Vector3f(0, 0, 0));
        waves.setMaxDistance(100);
        waves.setRefDistance(5);
        waves.setVolume(3);

        nature = new AudioNode(assetManager, "Sound/Environment/Nature.ogg", true);
        nature.setPositional(false);
        nature.setVolume(5);

        engine = new AudioNode(assetManager, "Sound/Effects/Gun.wav", false);
        engine.setPositional(true);
        engine.setPitch(2f);
        engine.setVolume(7);
        engine.setLooping(true);
        //engine.setVelocity(new Vector3f(144.0f, 144.0f, 144.0f));
        //engine.play();

        waves.play();
        nature.play();
    }

    protected static Geometry findGeom(Spatial spatial, String name) {
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            Spatial child;
            Geometry result;
            for (int i = 0; i < node.getQuantity(); i++) {
                child = node.getChild(i);
                result = findGeom(child, name);
                if (result != null) {
                    return result;
                }
            }
        } else if (spatial instanceof Geometry && spatial.getName().startsWith(name)) {
            return (Geometry) spatial;
        }
        return null;
    }

    protected void buildPlayer() {
        float stiffness = 50.0f;//200=f1 car
        float compValue = 0.1f; //(lower than damp!)
        float dampValue = 0.2f;
        final float mass = 300;

        //Load model and get chassis Geometry
        carNode = (Node) assetManager.loadModel("Models/Ferrari/Car.scene");
        carNode.setShadowMode(RenderQueue.ShadowMode.Cast);
        if (!geomMap.containsKey(CAR)) {
            geomMap.put(CAR, findGeom(carNode, CAR));
        }

        BoundingBox box = (BoundingBox) geomMap.get(CAR).getModelBound();

        //Create a hull collision shape for the chassis
        CollisionShape carHull = CollisionShapeFactory.createDynamicMeshShape(geomMap.get(CAR));

        //Create a vehicle control
        player = new VehicleControl(carHull, mass);
        carNode.addControl(player);

        //Setting default values for wheels
        player.setSuspensionCompression(compValue * 4.0f * FastMath.sqrt(stiffness));
        player.setSuspensionDamping(dampValue * 5.0f * FastMath.sqrt(stiffness));
        player.setSuspensionStiffness(stiffness);
        player.setMaxSuspensionForce(10000);

        //Create four wheels and add them at their locations
        //note that our fancy car actually goes backwards..
        Vector3f wheelDirection = new Vector3f(0, -1, 0);
        Vector3f wheelAxle = new Vector3f(-1, 0, 0);

        geomMap.put(WHEEL_FRONT_RIGHT, findGeom(carNode, WHEEL_FRONT_RIGHT));
        geomMap.get(WHEEL_FRONT_RIGHT).center();
        box = (BoundingBox) geomMap.get(WHEEL_FRONT_RIGHT).getModelBound();
        wheelRadius = box.getYExtent();
        float back_wheel_h = (wheelRadius * 1.7f) - 1f;
        float front_wheel_h = (wheelRadius * 1.9f) - 1f;
        player.addWheel(geomMap.get(WHEEL_FRONT_RIGHT).getParent(), box.getCenter().add(0, -front_wheel_h, 0), wheelDirection, wheelAxle, 0.2f, wheelRadius, true);

        geomMap.put(WHEEL_FRONT_LEFT, findGeom(carNode, WHEEL_FRONT_LEFT));
        geomMap.get(WHEEL_FRONT_LEFT).center();
        box = (BoundingBox) geomMap.get(WHEEL_FRONT_LEFT).getModelBound();
        player.addWheel(geomMap.get(WHEEL_FRONT_LEFT).getParent(), box.getCenter().add(0, -front_wheel_h, 0), wheelDirection, wheelAxle, 0.2f, wheelRadius, true);

        geomMap.put(WHEEL_BACK_RIGHT, findGeom(carNode, WHEEL_BACK_RIGHT));
        geomMap.get(WHEEL_BACK_RIGHT).center();
        box = (BoundingBox) geomMap.get(WHEEL_BACK_RIGHT).getModelBound();
        player.addWheel(geomMap.get(WHEEL_BACK_RIGHT).getParent(), box.getCenter().add(0, -back_wheel_h, 0), wheelDirection, wheelAxle, 0.2f, wheelRadius, false);

        geomMap.put(WHEEL_BACK_LEFT, findGeom(carNode, WHEEL_BACK_LEFT));
        geomMap.get(WHEEL_BACK_LEFT).center();
        box = (BoundingBox) geomMap.get(WHEEL_BACK_LEFT).getModelBound();
        player.addWheel(geomMap.get(WHEEL_BACK_LEFT).getParent(), box.getCenter().add(0, -back_wheel_h, 0), wheelDirection, wheelAxle, 0.2f, wheelRadius, false);

        player.getWheel(2).setFrictionSlip(4);
        player.getWheel(3).setFrictionSlip(4);

        rootNode.attachChild(carNode);

        bulletAppState.getPhysicsSpace().add(player);
    }

    protected void createTerrain() {
        // First, we load up our textures and the heightmap texture for the terrain

        // TERRAIN TEXTURE material
        matRock = new Material(assetManager, "Common/MatDefs/Terrain/Terrain.j3md");
        matRock.setBoolean("useTriPlanarMapping", false);

        // ALPHA map (for splat textures)
        matRock.setTexture("Alpha", assetManager.loadTexture("Textures/Terrain/splat/alphamap.png"));

        // HEIGHTMAP image (for the terrain heightmap)
        Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/mountains512.png");

        // GRASS texture
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        matRock.setTexture("Tex1", grass);
        matRock.setFloat("Tex1Scale", grassScale);

        // DIRT texture
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(Texture.WrapMode.Repeat);
        matRock.setTexture("Tex2", dirt);
        matRock.setFloat("Tex2Scale", dirtScale);

        // ROCK texture
        Texture rock = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        rock.setWrap(Texture.WrapMode.Repeat);
        matRock.setTexture("Tex3", rock);
        matRock.setFloat("Tex3Scale", rockScale);

        // WIREFRAME material
        /*
         matWire = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
         matWire.getAdditionalRenderState().setWireframe(true);
         matWire.setColor("Color", ColorRGBA.Green);
         */

        // CREATE HEIGHTMAP
        AbstractHeightMap heightmap = null;
        try {
            //heightmap = new HillHeightMap(1025, 1000, 50, 100, (byte) 3);

            heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 1f);
            heightmap.load();

        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
         * Here we create the actual terrain. The tiles will be 65x65, and the total size of the
         * terrain will be 513x513. It uses the heightmap we created to generate the height values.
         */
        /**
         * Optimal terrain patch size is 65 (64x64). The total size is up to
         * you. At 1025 it ran fine for me (200+FPS), however at size=2049, it
         * got really slow. But that is a jump from 2 million to 8 million
         * triangles...
         */
        terrain = new TerrainQuad("terrain", 65, 513, heightmap.getHeightMap());
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        control.setLodCalculator(new DistanceLodCalculator(65, 2.7f)); // patch size, and a multiplier
        terrain.addControl(control);
        terrain.setMaterial(matRock);
        terrain.setLocalTranslation(0, -100, 0);
        terrain.setLocalScale(2f, 0.5f, 2f);
        RigidBodyControl rbc = new RigidBodyControl(0);
        terrain.addControl(rbc);
        bulletAppState.getPhysicsSpace().add(terrain);


        rootNode.attachChild(terrain);

        DirectionalLight light = new DirectionalLight();
        light.setDirection((new Vector3f(-0.5f, -1f, -0.5f)).normalize());
        rootNode.addLight(light);
    }

    private static class ActionListenerImpl implements ActionListener {

        public ActionListenerImpl() {
        }

        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class MovementListener implements ActionListener {

        public MovementListener() {
        }

        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
