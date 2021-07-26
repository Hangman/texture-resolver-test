package com.bug.mgsx;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

import net.mgsx.gltf.loaders.gltf.GLTFAssetLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.EnvironmentUtil;

public class Start extends ApplicationAdapter {
    private static final String BLUE_ASTEROID_PATH  = "asteroid1_blue.gltf";
    private static final String GREEN_ASTEROID_PATH = "asteroid1_green.gltf";

    private float                    time;
    private AssetManager             scatman;
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private Cubemap diffuseCubemap;
    private Cubemap specularCubemap;

    private SceneManager      sceneManager;
    private PerspectiveCamera camera;
    private Texture           brdfLUT;

    private SceneAsset blueAsteroidSceneAsset;
    private SceneAsset greenAsteroidSceneAsset;


    @Override
    public void create() {
        // SETUP CAMERA
        this.camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.camera.near = 1f;
        this.camera.far = 10f;

        // LOAD TEXTURES
        this.brdfLUT = new Texture("brdfLUT.png");
        this.diffuseCubemap = EnvironmentUtil.createCubemap(new InternalFileHandleResolver(), "diffuse_", "_0.png", EnvironmentUtil.FACE_NAMES_FULL);
        this.specularCubemap = EnvironmentUtil.createCubemap(new InternalFileHandleResolver(), "specular_", "_", ".png", 10, EnvironmentUtil.FACE_NAMES_FULL);

        // CREATE SHADER PROVIDER & CONFIG
        final PBRShaderProvider shaderProvider = PBRShaderProvider.createDefault(0);
        final DepthShaderProvider depthShaderProvider = PBRShaderProvider.createDefaultDepth(0);
        shaderProvider.config.numDirectionalLights = 2;
        shaderProvider.config.numPointLights = 0;
        shaderProvider.config.numSpotLights = 0;
        shaderProvider.config.numBones = 0;

        // SETUP SCENEMANAGER
        this.sceneManager = new SceneManager(shaderProvider, depthShaderProvider);
        this.sceneManager.setCamera(this.camera);
        this.sceneManager.setAmbientLight(0.3f);
        this.sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, this.brdfLUT));
        this.sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(this.diffuseCubemap));
        this.sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(this.specularCubemap));
        this.sceneManager.environment.add(new DirectionalLightEx().set(new Color(1f, 1f, 1f, 1f), new Vector3(2f, -1f, 0.5f), 1f));
        this.sceneManager.environment.add(new DirectionalLightEx().set(new Color(1f, 1f, 1f, 1f), new Vector3(-2f, -1f, -0.5f), 1f));

        // SETUP ASSET MANAGER
        this.scatman = new AssetManager();
        final FileHandleResolver resolver = new InternalFileHandleResolver();
        this.scatman.setLoader(SceneAsset.class, ".gltf", new GLTFAssetLoader(resolver));

        // LOAD ASSETS
        this.scatman.load(Start.BLUE_ASTEROID_PATH, SceneAsset.class);
        this.scatman.load(Start.GREEN_ASTEROID_PATH, SceneAsset.class);
        this.scatman.finishLoading();

        // ADD ASSETS TO SCENEMANAGER
        this.blueAsteroidSceneAsset = this.scatman.get(Start.BLUE_ASTEROID_PATH, SceneAsset.class);
        final Scene blueAsteroidScene = new Scene(this.blueAsteroidSceneAsset.scene);
        blueAsteroidScene.modelInstance.transform.translate(-1f, 0f, 0f);
        this.sceneManager.addScene(blueAsteroidScene);
        this.greenAsteroidSceneAsset = this.scatman.get(Start.GREEN_ASTEROID_PATH, SceneAsset.class);
        final Scene greenAsteroidScene = new Scene(this.greenAsteroidSceneAsset.scene);
        greenAsteroidScene.modelInstance.transform.translate(1f, 0f, 0f);
        this.sceneManager.addScene(greenAsteroidScene);

        // UNLOAD GREEN ASTEROID AFTER 5 SECONDS
        this.executorService.schedule(() -> {
            Gdx.app.postRunnable(() -> {
                this.sceneManager.removeScene(greenAsteroidScene);
                this.greenAsteroidSceneAsset = null; // is disposed by the asset manager
                this.scatman.unload(Start.GREEN_ASTEROID_PATH);
            });
        }, 5, TimeUnit.SECONDS);
    }


    @Override
    public void render() {
        final float deltaTime = Gdx.graphics.getDeltaTime();
        this.time += deltaTime;

        // ANIMATE CAMERA
        this.camera.position.setFromSpherical(MathUtils.PI / 4f, this.time * 0.3f).scl(5f);
        this.camera.up.set(Vector3.Y);
        this.camera.lookAt(Vector3.Zero);
        this.camera.update();

        // RENDER
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        this.sceneManager.update(deltaTime);
        this.sceneManager.render();
    }


    @Override
    public void resize(int width, int height) {
        this.sceneManager.updateViewport(width, height);
    }


    @Override
    public void dispose() {
        this.executorService.shutdownNow();
        this.sceneManager.dispose();
        this.diffuseCubemap.dispose();
        this.specularCubemap.dispose();
        this.brdfLUT.dispose();
        if (this.blueAsteroidSceneAsset != null) {
            this.blueAsteroidSceneAsset.dispose();
        }
        if (this.greenAsteroidSceneAsset != null) {
            this.greenAsteroidSceneAsset.dispose();
        }
    }

}