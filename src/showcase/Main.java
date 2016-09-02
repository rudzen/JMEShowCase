/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.BasicShadowRenderer;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main extends SimpleApplication implements ActionListener {

    private BulletAppState bulletAppState;
    private VehicleControl player;
    private AudioNode nature, waves, engine;
    private float wheelRadius;
    private float steeringValue = 0f;
    private float accelerationValue = 0f;
    private Node carNode;
    private ChaseCamera chaseCam;
    private AtomicBoolean isResetting;
    private final float TURN_FORCE = 0.5f;
    private final float ACCEL_RATE = 800f;
    private final float BRAKE_FORCE = 50f;
    private HashMap<String, Geometry> geomMap;
    private final String FLOOR = "Floor";
    private final String CAR = "Car";
    private final String WHEEL_FRONT_RIGHT = "WheelFrontRight";
    private final String WHEEL_FRONT_LEFT = "WheelFrontLeft";
    private final String WHEEL_BACK_RIGHT = "WheelBackRight";
    private final String WHEEL_BACK_LEFT = "WheelBackLeft";
    private final String LEFTS = "Lefts";
    private final String RIGHTS = "Rights";
    private final String UPS = "Ups";
    private final String DOWNS = "Downs";
    private final String SPACE = "Space";
    private final String RESET = "Reset";

    // terrain stuff
    private float grassScale = 64;
    private float dirtScale = 16;
    private float rockScale = 128;
    private TerrainQuad terrain;
    private Material matRock;
    
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    private void setupKeys() {
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

        inputManager.addListener(this, LEFTS);
        inputManager.addListener(this, RIGHTS);
        inputManager.addListener(this, UPS);
        inputManager.addListener(this, DOWNS);
        inputManager.addListener(this, SPACE);
        inputManager.addListener(this, RESET);
    }

    private void configureCamera() {
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

    private Spatial loadSky() {
        return SkyFactory.createSky(assetManager,
                assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_west.jpg"),
                assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_east.jpg"),
                assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_north.jpg"),
                assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_south.jpg"),
                assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_up.jpg"),
                assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_down.jpg"));
    }

    private void initAmbience() {
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

    @Override
    public void simpleInitApp() {
        // configure basic fields..
        isResetting = new AtomicBoolean(false);
        geomMap = new HashMap<>();

        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        if (settings.getRenderer().startsWith("LWJGL")) {
            BasicShadowRenderer bsr = new BasicShadowRenderer(assetManager, 512);
            bsr.setDirection(new Vector3f(-0.5f, -0.3f, -0.3f).normalizeLocal());
            viewPort.addProcessor(bsr);
        }
        cam.setFrustumFar(100f);
        //flyCam.setMoveSpeed(10);

        setupKeys();
        createTerrain();
        createPhysicsTestWorld();
        buildPlayer();

        rootNode.attachChild(loadSky());

        configureCamera();

        initAmbience();

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.5f, -1f, -0.3f).normalizeLocal());
        rootNode.addLight(dl);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(0.5f, -0.1f, 0.3f).normalizeLocal());
        rootNode.addLight(dl);
    }

    private Geometry findGeom(Spatial spatial, String name) {
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

    private void buildPlayer() {
        float stiffness = 50.0f;//200=f1 car
        float compValue = 0.1f; //(lower than damp!)
        float dampValue = 0.2f;
        final float mass = 300;

        //Load model and get chassis Geometry
        carNode = (Node) assetManager.loadModel("Models/Ferrari/Car.scene");
        carNode.setShadowMode(ShadowMode.Cast);
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

    @Override
    public void onAction(String binding, boolean value, float tpf) {
        switch (binding) {
            case LEFTS:
                if (value) {
                    steeringValue += TURN_FORCE;
                } else {
                    steeringValue -= TURN_FORCE;
                }
                player.steer(steeringValue);
                break;
            case RIGHTS:
                if (value) {
                    steeringValue -= TURN_FORCE;
                } else {
                    steeringValue += TURN_FORCE;
                }
                player.steer(steeringValue);
                break;
            case UPS:
                if (value) {
                    accelerationValue -= ACCEL_RATE;
                } else {
                    accelerationValue += ACCEL_RATE;
                }
                player.accelerate(accelerationValue);
                player.setCollisionShape(CollisionShapeFactory.createDynamicMeshShape(geomMap.get(CAR)));
                break;
            case DOWNS:
                if (value) {
                    accelerationValue += ACCEL_RATE;
                } else {
                    accelerationValue -= ACCEL_RATE;
                }
                player.accelerate(accelerationValue);
                player.setCollisionShape(CollisionShapeFactory.createDynamicMeshShape(geomMap.get(CAR)));
                break;
            case RESET:
                if (value && !isResetting.get()) {
                    isResetting.set(true);
                    player.setPhysicsLocation(Vector3f.ZERO);
                    player.setPhysicsRotation(new Matrix3f());
                    player.setLinearVelocity(Vector3f.ZERO);
                    player.setAngularVelocity(Vector3f.ZERO);
                    player.resetSuspension();
                    isResetting.set(false);
                } else {
                }
                break;
            case SPACE:
                player.brake(value ? BRAKE_FORCE : 0f);
                break;
        }
    }

    private void createTerrain() {
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

    /**
     * creates a simple physics test world with a floor, an obstacle and some
     * test boxes
     *
     * @param rootNode
     * @param assetManager
     * @param space
     */
    private void createPhysicsTestWorld() {
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

    @Override
    public void simpleUpdate(float tpf) {
        //cam.setLocation(player.getWheel(player.getNumWheels() - 1).getLocation());
        //cam.lookAt(player.getWheel(player.getNumWheels() - 1).getLocation(), Vector3f.UNIT_Y);
    }

    @Override
    public void update() {
        super.update(); //To change body of generated methods, choose Tools | Templates.
    }
}
