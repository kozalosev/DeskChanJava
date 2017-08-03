package info.deskchan.core;

import info.deskchan.core_utils.CoreUtilsKt;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PluginManager {
	
	private static final PluginManager instance = new PluginManager();
	private final Map<String, PluginEntity> plugins = new HashMap<>();
	private final Map<String, Set<MessageListener>> messageListeners = new HashMap<>();
	private final List<PluginLoader> loaders = new ArrayList<>();
	private final Set<String> blacklistedPlugins = new HashSet<>();
	private String[] args;
	private static OutputStream logStream = null;

	private static boolean debugBuild = false;
	private static Path corePath = null;
	private static Path pluginsDirPath = null;
	private static Path dataDirPath = null;
	private static Path rootDirPath = null;
	
	/* Singleton */
	
	private PluginManager() {
	}
	
	public static PluginManager getInstance() {
		return instance;
	}
	
	/* Core initialization */
	
	void initialize(String[] args) {
		this.args = args;
		try {
			logStream = Files.newOutputStream(getDataDirPath().resolve("DeskChan.log"));
		} catch (IOException e) {
			log(e);
		}
		CoreInfo.printInfo();
		tryLoadPluginByClass(CorePlugin.class);
		loadPluginsBlacklist();
	}
	
	public String[] getArgs() {
		return args;
	}

	public static boolean isDebugBuild() {
		return debugBuild;
	}
	
	/* Plugin initialization and unloading */
	
	public boolean initializePlugin(String id, Plugin plugin, PluginManifest manifest, PluginConfig config) {
		if (!plugins.containsKey(id)) {
			PluginProxyInterface pluginProxy = new PluginProxy(plugin);
			if (blacklistedPlugins.contains(id)) {
				plugins.put(id, new PluginEntity(pluginProxy, id, manifest, config));
				return false;
			}
			if (CoreUtilsKt.resolveDependencies(pluginProxy, manifest) && pluginProxy.initialize(id)) {
				plugins.put(id, new PluginEntity(pluginProxy, id, manifest, config));
				log("Registered plugin: " + id);
				sendMessage("core", "core-events:plugin-load", id);
				return true;
			}
		}
		return false;
	}

	public boolean initializePlugin(String id, Plugin plugin, PluginManifest manifest) {
		return initializePlugin(id, plugin, manifest, null);
	}

	public boolean initializePlugin(String id, Plugin plugin, PluginConfig config) {
		return initializePlugin(id, plugin, null, config);
	}

	public boolean initializePlugin(String id, Plugin plugin) {
		return initializePlugin(id, plugin, null, null);
	}
	
	void unregisterPlugin(PluginProxyInterface pluginProxy) {
		plugins.remove(pluginProxy.getId());
		log("Unregistered plugin: " + pluginProxy.getId());
		sendMessage("core", "core-events:plugin-unload", pluginProxy.getId());
	}
	
	public boolean unloadPlugin(String name) {
		PluginProxyInterface plugin = plugins.getOrDefault(name, null);
		if (plugin != null) {
			plugin.unload();
			return true;
		}
		return false;
	}
	
	/* Message bus */
	
	void registerMessageListener(String tag, MessageListener listener) {
		Set<MessageListener> listeners = messageListeners.getOrDefault(tag, null);
		if (listeners == null) {
			listeners = new HashSet<>();
			messageListeners.put(tag, listeners);
		}
		if (tag.equals("core-events:plugin-load")) {
			for (String id : plugins.keySet()) {
				listener.handleMessage("core", "core-events:plugin-load", id);
			}
		}
		listeners.add(listener);
	}
	
	void unregisterMessageListener(String tag, MessageListener listener) {
		Set<MessageListener> listeners = messageListeners.getOrDefault(tag, null);
		if (listeners != null) {
			listeners.remove(listener);
			if (listeners.size() == 0) {
				messageListeners.remove(tag);
			}
		}
	}
	int getMessageListenersCount(String tag) {
		Set<MessageListener> listeners = messageListeners.getOrDefault(tag, null);
		if (listeners != null) {
			return listeners.size();
		}
		return 0;
	}
	void sendMessage(String sender, String tag, Object data) {
		Set<MessageListener> listeners = messageListeners.getOrDefault(tag, null);
		if (listeners != null) {
			for (MessageListener listener : listeners) {
				try {
					listener.handleMessage(sender, tag, data);
				} catch (Exception e){
					log("Error while calling "+tag+", called by "+sender);
					log(e);
				}
			}
		}
	}

	/* Plugin loaders */

	public synchronized void registerPluginLoader(PluginLoader loader) {
		loaders.add(loader);
	}

	public synchronized void unregisterPluginLoader(PluginLoader loader) {
		loaders.remove(loader);
	}

	public boolean loadPluginByClass(Class cls) throws Throwable {
		Object plugin = cls.newInstance();
		if (plugin instanceof Plugin) {
			String packageName = cls.getPackage().getName();
			if (packageName.startsWith("info.deskchan.")) {
				packageName = packageName.substring("info.deskchan.".length());
			}
			return initializePlugin(packageName, (Plugin) plugin);
		}
		return false;
	}

	public boolean tryLoadPluginByClass(Class cls) {
		try {
			return loadPluginByClass(cls);
		} catch (Throwable e) {
			return false;
		}
	}

	public boolean loadPluginByClassName(String className) throws Throwable {
		Class cls = getClass().getClassLoader().loadClass(className);
		return loadPluginByClass(cls);
	}

	public boolean tryLoadPluginByClassName(String className) {
		try {
			return loadPluginByClassName(className);
		} catch (Throwable e) {
			return false;
		}
	}

	public boolean loadPluginByPackageName(String packageName) throws Throwable {
		return loadPluginByClassName(packageName + ".Main");
	}

	public boolean tryLoadPluginByPackageName(String packageName) {
		try {
			return loadPluginByPackageName(packageName);
		} catch (Throwable e) {
			log(e);
			return false;
		}
	}
	
	public synchronized boolean loadPluginByPath(Path path) throws Throwable {
		// TODO: get rid of manifest double reading
		if (Files.isDirectory(path)) {
			Path manifestPath = path.resolve("manifest.json");
			if (Files.isReadable(manifestPath)) {
				try (final InputStream manifestInputStream = Files.newInputStream(manifestPath)) {
					final String manifestStr = IOUtils.toString(manifestInputStream, "UTF-8");
					manifestInputStream.close();
					final JSONObject manifest = new JSONObject(manifestStr);
					if (manifest.has("deps") || manifest.has("dependencies")) {
						final List<Object> dependencies = new ArrayList<>();
						if (manifest.has("deps")) {
							dependencies.addAll(manifest.getJSONArray("deps").toList());
						}
						if (manifest.has("dependencies")) {
							dependencies.addAll(manifest.getJSONArray("dependencies").toList());
						}
						for (Object dep : dependencies) {
							if (dep instanceof String) {
								String depID = dep.toString();
								if (!tryLoadPluginByName(depID)) {
									throw new Exception("Failed to load dependency " + depID +
											" of plugin " + path.toString());
								}
							}
						}
					}
				} catch (IOException | JSONException e) {
					e.printStackTrace();
				}
			}
		}
		for (PluginLoader loader : loaders) {
			if (loader.matchPath(path)) {
				loader.loadByPath(path);
				return true;
			}
		}
		throw new Exception("Could not match loader for plugin " + path.toString());
	}
	
	public boolean tryLoadPluginByPath(Path path) {
		try {
			return loadPluginByPath(path);
		} catch (Throwable e) {
			log(e);
		}
		return false;
	}
	
	public boolean loadPluginByName(String name) throws Throwable {
		// 1. Tries to find an already loaded plugin with the same name.
		if (plugins.containsKey(name)) {
			return true;
		}

		// 2. If the plugin can be found in the plugins directory, it's loaded.
		Path path = getDefaultPluginDirPath(name);
		if (path.toFile().exists()) {
			return loadPluginByPath(path);
		}

		// 3. Tries to find an already loaded plugin with a similar name.
		if (plugins.values().stream().anyMatch(pluginEntity -> pluginEntity.isNameMatched(name))) {
			return true;
		}

		// 4. If any plugin can be found in the plugins directory without respect to their extensions, the first one will be loaded.
		File[] files = getPluginsDirPath().toFile().listFiles((file, s) -> FilenameUtils.removeExtension(s).equals(name));
		if (files != null) {
			if (files.length > 1) {
				log("Too many plugins with similar names (" + name + ")!");
			}
			return loadPluginByPath(files[0].toPath());
		}

		// 5. Otherwise, the plugin cannot be loaded by name.
		return false;
	}
	
	public boolean tryLoadPluginByName(String name) {
		try {
			return loadPluginByName(name);
		} catch (Throwable e) {
			log(e);
		}
		return false;
	}
	
	/* Application finalization */
	
	void unloadPlugins() {
		List<PluginProxyInterface> pluginsToUnload = new ArrayList<>();
		for (Map.Entry<String, PluginEntity> entry : plugins.entrySet()) {
			if (!entry.getKey().equals("core")) {
				pluginsToUnload.add(entry.getValue());
			}
		}
		for (PluginProxyInterface plugin : pluginsToUnload) {
			plugin.unload();
		}
		pluginsToUnload.clear();
		plugins.get("core").unload();
		savePluginsBlacklist();
		if (logStream != null) {
			try {
				logStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			logStream = null;
		}
	}
	
	/* Plugins blacklist */
	
	public List<String> getBlacklistedPlugins() {
		return new ArrayList<>(blacklistedPlugins);
	}
	
	public void addPluginToBlacklist(String name) {
		if (!name.equals("core")) {
			blacklistedPlugins.add(name);
			unloadPlugin(name);
		}
	}
	
	public void removePluginFromBlacklist(String name) {
		blacklistedPlugins.remove(name);
	}
	
	private void loadPluginsBlacklist() {
		try {
			BufferedReader reader = Files.newBufferedReader(
					getPluginDataDirPath("core").resolve("blacklisted-plugins.txt"),
					Charset.forName("UTF-8")
			);
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.length() == 0) {
					continue;
				}
				blacklistedPlugins.add(line);
			}
			reader.close();
		} catch (IOException e) {
			blacklistedPlugins.add("random_phrases");
		}
	}
	
	private void savePluginsBlacklist() {
		try {
			BufferedWriter writer = Files.newBufferedWriter(
					getPluginDataDirPath("core").resolve("blacklisted-plugins.txt"),
					Charset.forName("UTF-8")
			);
			for (String id : blacklistedPlugins) {
				writer.write(id);
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			log(e);
		}
	}
	
	/* Plugins and data directories */
	
	public static Path getCorePath() {
		if (corePath == null) {
			try {
				corePath = Paths.get(PluginManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			} catch (URISyntaxException e) {
				corePath = Paths.get(PluginManager.class.getProtectionDomain().getCodeSource().getLocation().getFile());
			}
			debugBuild = Files.isDirectory(corePath);
		}
		return corePath;
	}

	public static Path getPluginsDirPath() {
		if(pluginsDirPath != null) {
			return pluginsDirPath;
		}
		Path path = getRootDirPath();
		if (debugBuild) {
			path = path.resolve("build");
		}
		path = path.resolve("plugins");
		pluginsDirPath = path.normalize();
		return pluginsDirPath;
	}

	public static Path getDefaultPluginDirPath(String name) {
		Path possiblePath = getPluginsDirPath().resolve(name);
		while (!Files.isDirectory(possiblePath)) {
			possiblePath = possiblePath.getParent();
		}
		return possiblePath;
	}
	
	public Path getPluginDirPath(String name) {
		if (plugins.containsKey(name)) {
			return plugins.get(name).getConfig().getDirectory();
		}
		return getDefaultPluginDirPath(name);
	}

	public static Path getDataDirPath() {
		if(dataDirPath != null) {
			return dataDirPath;
		}
		Path path = getRootDirPath();
		if (debugBuild) {
			path = path.resolve("build");
		}
		path = path.resolve("data");
		if (!Files.isDirectory(path)) {
			path.toFile().mkdir();
			log("Created directory: " + path);
		}
		dataDirPath = path.normalize();
		return dataDirPath;
	}

	public static Path getRootDirPath() {
		if(rootDirPath != null) {
			return rootDirPath;
		}
		Path corePath = getCorePath();
		Path path;
		if (debugBuild) {
			path = corePath.resolve("../../../");
		} else {
			path = corePath.getParent().resolve("../");
		}
		rootDirPath = path.normalize();
		return rootDirPath;
	}

	public static Path getPluginDataDirPath(String id) {
		final Path baseDir = getDataDirPath();
		final Path dataDir = baseDir.resolve(id);
		if (!Files.isDirectory(dataDir)) {
			dataDir.toFile().mkdirs();
			log("Created directory: " + dataDir.toString());
		}
		return dataDir;
	}

	/* Manifest getter */

	public PluginManifest getManifest(String name) {
		if (!plugins.containsKey(name)) {
			return null;
		}
		return plugins.get(name).getManifest();
	}
	
	/* Logging */
	
	static void log(String id, String message) {
		String text = id + ": " + message;
		System.err.println(text);
		if (logStream != null) {
			try {
				logStream.write((text + "\n").getBytes("UTF-8"));
			} catch (IOException e) {
				logStream = null;
				log(e);
			}
		}
	}
	
	static void log(String id, Throwable e) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		e.printStackTrace(printWriter);
		String[] lines = stringWriter.toString().split("\n");
		for (String line : lines) {
			log(id, line);
		}
	}
	
	static void log(String message) {
		log("core", message);
	}
	
	static void log(Throwable e) {
		log("core", e);
	}
	
}
