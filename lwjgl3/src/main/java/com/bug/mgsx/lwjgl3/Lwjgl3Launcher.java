package com.bug.mgsx.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.bug.mgsx.Start;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3Launcher.createApplication();
    }


    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Start(), Lwjgl3Launcher.getDefaultConfiguration());
    }


    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        final Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("textureresolvertest");
        configuration.setWindowedMode(1024, 768);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}