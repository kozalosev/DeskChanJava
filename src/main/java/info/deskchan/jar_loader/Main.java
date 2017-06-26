package info.deskchan.jar_loader;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginLoader;
import info.deskchan.core.PluginManager;
import info.deskchan.core.PluginProxy;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.Manifest;


public class Main implements Plugin, PluginLoader {

    private PluginProxy pluginProxy;

    @Override
    public boolean initialize(PluginProxy pluginProxy) {
        this.pluginProxy = pluginProxy;
        PluginManager.getInstance().registerPluginLoader(this);
        return true;
    }

    @Override
    public void unload() {
        PluginManager.getInstance().unregisterPluginLoader(this);
    }

    @Override
    public boolean matchPath(Path path) {
        return path.getFileName().toString().endsWith(".jar");
    }

    @Override
    public void loadByPath(Path path) throws Throwable {
        String id = path.getFileName().toString();
        Manifest manifest = new JarFile(path.toFile()).getManifest();
        String mainClass = manifest.getMainAttributes().getValue("Main-Class");
        String className = (mainClass != null) ? mainClass : "Main";

        URL[] urls = new URL[1];
        urls[0] = path.toUri().toURL();
        ClassLoader loader = new URLClassLoader(urls, getClass().getClassLoader());
        Class cls;
        try {
            cls = Class.forName(className, true, loader);
        } catch (ClassNotFoundException e) {
            pluginProxy.log(String.format("Unable to load plugin \"%s\"! Couldn't find class \"%s\".", id, className));
            return;
        }
        Plugin plugin = (Plugin) cls.newInstance();
        PluginManager.getInstance().initializePlugin(id, plugin);
    }

}
