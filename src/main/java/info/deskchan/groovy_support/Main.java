package info.deskchan.groovy_support;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import info.deskchan.core.*;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;

import static info.deskchan.core_utils.CoreUtilsKt.parsePluginManifest;

public class Main implements Plugin, PluginLoader {
	
	@Override
	public boolean initialize(PluginProxyInterface pluginProxy) {
		PluginManager.getInstance().registerPluginLoader(this);
		return true;
	}
	
	@Override
	public void unload() {
		PluginManager.getInstance().unregisterPluginLoader(this);
	}
	
	@Override
	public boolean matchPath(Path path) {
		if (Files.isDirectory(path)) {
			return Files.isReadable(path.resolve("plugin.groovy"));
		} else {
			return path.getFileName().toString().endsWith(".groovy");
		}
	}
	
	@Override
	public void loadByPath(Path path) throws Throwable {
		String id = path.getFileName().toString();
		PluginManifest manifest = null;
		if (Files.isDirectory(path)) {
			manifest = parsePluginManifest(id, path.resolve("manifest.json"));
			path = path.resolve("plugin.groovy");
		}
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		compilerConfiguration.setSourceEncoding("UTF-8");
		compilerConfiguration.setScriptBaseClass("info.deskchan.groovy_support.GroovyPlugin");
		compilerConfiguration.setClasspath(path.getParent().toString());
		GroovyShell groovyShell = new GroovyShell(compilerConfiguration);
		Script script = groovyShell.parse(path.toFile());
		GroovyPlugin plugin = (GroovyPlugin) script;
		plugin.setPluginDirPath(path.getParent());
		PluginManager.getInstance().initializePlugin(id, plugin, manifest);
	}
	
}
