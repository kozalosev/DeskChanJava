import info.deskchan.core.PluginManager


def loader = new JythonPluginLoader(getPluginDirPath(), { o -> log(o) })
PluginManager.getInstance().registerPluginLoader(loader)
addCleanupHandler({ -> PluginManager.getInstance().unregisterPluginLoader(loader) })
