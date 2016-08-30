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
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
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
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main extends SimpleApplication implements ActionListener {

    private BulletAppState bulletAppState;
    private VehicleControl player;
    private VehicleWheel fr, fl, br, bl;
    private Node node_fr, node_fl, node_br, node_bl;
    private float wheelRadius;
    private float steeringValue = 0;
    private float accelerationValue = 0;
    private Node carNode;
    private ChaseCamera chaseCam;
    private AtomicBoolean isResetting;
    private final float TURN_FORCE = 0.5f;
    private final float ACCEL_RATE = 800f;
    private final float BRAKE_FORCE = 50f;
    private HashMap<String, Geometry> wheelMap;
    private final String WHEEL_FRONT_RIGHT = "WheelFrontRight";
    private final String WHEEL_FRONT_LEFT = "WheelFrontLeft";
    private final String WHEEL_BACK_RIGHT = "WheelBackRight";
    private final String WHEEL_BACK_LEFT = "WheelBackLeft";

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    private void setupKeys() {
        inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_DOWN));

        inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));

        inputManager.addListener(this, "Lefts");
        inputManager.addListener(this, "Rights");
        inputManager.addListener(this, "Ups");
        inputManager.addListener(this, "Downs");
        inputManager.addListener(this, "Space");
        inputManager.addListener(this, "Reset");
    }

    private void configureCamera() {
        cam.setLocation(player.getPhysicsLocation().mult(20f));
        chaseCam = new ChaseCamera(cam, carNode);
        chaseCam.setSmoothMotion(true);
        chaseCam.setChasingSensitivity(2);
    }

    @Override
    public void simpleInitApp() {
        // configure basic fields..
        isResetting = new AtomicBoolean(false);
        wheelMap = new HashMap<>(4);

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
        createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
        buildPlayer();

        configureCamera();

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.5f, -1f, -0.3f).normalizeLocal());
        rootNode.addLight(dl);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(0.5f, -0.1f, 0.3f).normalizeLocal());
        rootNode.addLight(dl);
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
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
        float stiffness = 60.0f;//200=f1 car
        float compValue = 0.5f; //(lower than damp!)
        float dampValue = 0.6f;
        final float mass = 300;

        //Load model and get chassis Geometry
        carNode = (Node) assetManager.loadModel("Models/Ferrari/Car.scene");
        carNode.setShadowMode(ShadowMode.Cast);
        Geometry chasis = findGeom(carNode, "Car");
        BoundingBox box = (BoundingBox) chasis.getModelBound();

        //Create a hull collision shape for the chassis
        CollisionShape carHull = CollisionShapeFactory.createDynamicMeshShape(chasis);

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

        wheelMap.put(WHEEL_FRONT_RIGHT, findGeom(carNode, WHEEL_FRONT_RIGHT));
        wheelMap.get(WHEEL_FRONT_RIGHT).center();
        box = (BoundingBox) wheelMap.get(WHEEL_FRONT_RIGHT).getModelBound();
        wheelRadius = box.getYExtent();
        float back_wheel_h = (wheelRadius * 1.7f) - 1f;
        float front_wheel_h = (wheelRadius * 1.9f) - 1f;
        player.addWheel(wheelMap.get(WHEEL_FRONT_RIGHT).getParent(), box.getCenter().add(0, -front_wheel_h, 0), wheelDirection, wheelAxle, 0.2f, wheelRadius, true);

        wheelMap.put(WHEEL_FRONT_LEFT, findGeom(carNode, WHEEL_FRONT_LEFT));
        wheelMap.get(WHEEL_FRONT_LEFT).center();
        box = (BoundingBox) wheelMap.get(WHEEL_FRONT_LEFT).getModelBound();
        player.addWheel(wheelMap.get(WHEEL_FRONT_LEFT).getParent(), box.getCenter().add(0, -front_wheel_h, 0), wheelDirection, wheelAxle, 0.2f, wheelRadius, true);

        wheelMap.put(WHEEL_BACK_RIGHT, findGeom(carNode, WHEEL_BACK_RIGHT));
        wheelMap.get(WHEEL_BACK_RIGHT).center();
        box = (BoundingBox) wheelMap.get(WHEEL_BACK_RIGHT).getModelBound();
        player.addWheel(wheelMap.get(WHEEL_BACK_RIGHT).getParent(), box.getCenter().add(0, -back_wheel_h, 0), wheelDirection, wheelAxle, 0.2f, wheelRadius, false);

        wheelMap.put(WHEEL_BACK_LEFT, findGeom(carNode, WHEEL_BACK_LEFT));
        wheelMap.get(WHEEL_BACK_LEFT).center();
        box = (BoundingBox) wheelMap.get(WHEEL_BACK_LEFT).getModelBound();
        player.addWheel(wheelMap.get(WHEEL_BACK_LEFT).getParent(), box.getCenter().add(0, -back_wheel_h, 0), wheelDirection, wheelAxle, 0.2f, wheelRadius, false);

        player.getWheel(2).setFrictionSlip(4);
        player.getWheel(3).setFrictionSlip(4);

        rootNode.attachChild(carNode);

        getPhysicsSpace().add(player);
    }

    @Override
    public void onAction(String binding, boolean value, float tpf) {
        switch (binding) {
            case "Lefts":
                if (value) {
                    steeringValue += TURN_FORCE;
                } else {
                    steeringValue -= TURN_FORCE;
                }
                player.steer(steeringValue);
                break;
            case "Rights":
                if (value) {
                    steeringValue -= TURN_FORCE;
                } else {
                    steeringValue += TURN_FORCE;
                }
                player.steer(steeringValue);
                break;
            case "Ups":
                if (value) {
                    accelerationValue -= ACCEL_RATE;
                } else {
                    accelerationValue += ACCEL_RATE;
                }
                player.accelerate(accelerationValue);
                player.setCollisionShape(CollisionShapeFactory.createDynamicMeshShape(findGeom(carNode, "Car")));
                break;
            case "Downs":
                if (value) {
                    accelerationValue += ACCEL_RATE;
                } else {
                    accelerationValue -= ACCEL_RATE;
                }
                player.accelerate(accelerationValue);
                player.setCollisionShape(CollisionShapeFactory.createDynamicMeshShape(findGeom(carNode, "Car")));
                break;
            case "Reset":
                if (value && !isResetting.get()) {
                    isResetting.set(true);
                    System.out.println("Reset");
                    player.setPhysicsLocation(Vector3f.ZERO);
                    player.setPhysicsRotation(new Matrix3f());
                    player.setLinearVelocity(Vector3f.ZERO);
                    player.setAngularVelocity(Vector3f.ZERO);
                    player.resetSuspension();
                    isResetting.set(false);
                } else {
                }
                break;
            case "Space":
                player.brake(value ? BRAKE_FORCE : 0f);
                break;
        }
    }

    /**
     * creates a simple physics test world with a floor, an obstacle and some
     * test boxes
     *
     * @param rootNode
     * @param assetManager
     * @param space
     */
    public static void createPhysicsTestWorld(Node rootNode, AssetManager assetManager, PhysicsSpace space) {
        AmbientLight light = new AmbientLight();
        light.setColor(ColorRGBA.DarkGray);
        rootNode.addLight(light);

        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/Pond/Pond.jpg"));

        Box floorBox = new Box(240, 0.25f, 240);
        Geometry floorGeometry = new Geometry("Floor", floorBox);
        floorGeometry.setMaterial(material);
        floorGeometry.setLocalTranslation(0, -5, 0);
//        Plane plane = new Plane();
//        plane.setOriginNormal(new Vector3f(0, 0.25f, 0), Vector3f.UNIT_Y);
//        floorGeometry.addControl(new RigidBodyControl(new PlaneCollisionShape(plane), 0));
        floorGeometry.addControl(new RigidBodyControl(0));


        rootNode.attachChild(floorGeometry);
        space.add(floorGeometry);

        //material.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));

        //movable boxes
        light = new AmbientLight();
        light.setColor(ColorRGBA.Pink);
        for (int i = 0; i < 12; i++) {
            Box box = new Box(0.25f, 0.25f, 0.25f);
            Geometry boxGeometry = new Geometry("Box", box);
            boxGeometry.setMaterial(material);
            boxGeometry.setLocalTranslation(i, 5, -3);
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
