import info.deskchan.core.PluginManager

def loader = new JavaScriptPluginLoader({ o -> log(o) })
PluginManager.getInstance().registerPluginLoader(loader)
addCleanupHandler({ -> PluginManager.getInstance().unregisterPluginLoader(loader) })
