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

import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.controls.ActionListener;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.shadow.BasicShadowRenderer;

public class Main extends Stuff {

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
    private ActionListener movementActionListener;

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        if (settings.getRenderer().startsWith("LWJGL")) {
            BasicShadowRenderer bsr = new BasicShadowRenderer(assetManager, 512);
            bsr.setDirection(new Vector3f(-0.5f, -0.3f, -0.3f).normalizeLocal());
            viewPort.addProcessor(bsr);
        }
        cam.setFrustumFar(100f);
        //flyCam.setMoveSpeed(10);

        movementActionListener = new MovementListener();

        setupKeys(movementActionListener);
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

    @Override
    public void simpleUpdate(float tpf) {
        //cam.setLocation(player.getWheel(player.getNumWheels() - 1).getLocation());
        //cam.lookAt(player.getWheel(player.getNumWheels() - 1).getLocation(), Vector3f.UNIT_Y);
    }

    @Override
    public void update() {
        super.update(); //To change body of generated methods, choose Tools | Templates.
    }

    private class MovementListener implements ActionListener {

        public MovementListener() {
        }

        @Override
        public void onAction(String binding, boolean isPressed, float tpf) {
            switch (binding) {
                case LEFTS:
                    if (isPressed) {
                        steeringValue += TURN_FORCE;
                    } else {
                        steeringValue -= TURN_FORCE;
                    }
                    player.steer(steeringValue);
                    break;
                case RIGHTS:
                    if (isPressed) {
                        steeringValue -= TURN_FORCE;
                    } else {
                        steeringValue += TURN_FORCE;
                    }
                    player.steer(steeringValue);
                    break;
                case UPS:
                    if (isPressed) {
                        accelerationValue -= ACCEL_RATE;
                    } else {
                        accelerationValue += ACCEL_RATE;
                    }
                    player.accelerate(accelerationValue);
                    player.setCollisionShape(CollisionShapeFactory.createDynamicMeshShape(geomMap.get(CAR)));
                    break;
                case DOWNS:
                    if (isPressed) {
                        accelerationValue += ACCEL_RATE;
                    } else {
                        accelerationValue -= ACCEL_RATE;
                    }
                    player.accelerate(accelerationValue);
                    player.setCollisionShape(CollisionShapeFactory.createDynamicMeshShape(geomMap.get(CAR)));
                    break;
                case RESET:
                    if (isPressed) {
                        player.setPhysicsLocation(Vector3f.ZERO);
                        player.setPhysicsRotation(new Matrix3f());
                        player.setLinearVelocity(Vector3f.ZERO);
                        player.setAngularVelocity(Vector3f.ZERO);
                        player.resetSuspension();
                    }
                    break;
                case SPACE:
                    player.brake(isPressed ? BRAKE_FORCE : 0f);
                    break;
            }
        }
    }
}
