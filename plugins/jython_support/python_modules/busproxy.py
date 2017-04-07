"""This is a special module for the plugin named jython_support. It provides a bunch of aliases for the bus object \
to let you write less redundant code."""

# Proxy methods

def sendMessage(tag, data, response_listener = None):
    """Sends a message to other plugins.
    
    Arguments:
    tag -- your message will receive all plugins subscribed on this tag.
    data -- an object with some required data specified by the concrete plugin.
    response_listener -- callback function; note that not every plugin supports this option and will call your function.
    """

    if response_listener is not None:
        bus.sendMessage(tag, data, response_listener)
    else:
        bus.sendMessage(tag, data)


def addMessageListener(tag, listener):
    """Use this function to subscribe on any tag. Your listener will be called when another plugin sends a message \
    with the same tag."""

    bus.addMessageListener(tag, listener)


def removeMessageListener(tag, listener):
    """Unsubscribe from the tag."""

    bus.removeMessageListener(tag, listener)


def addCleanupHandler(handler):
    """Use this function to set a callback which will be called when the plugin is being unloaded."""

    bus.addCleanupHandler(handler)


def getPluginDirPath():
    """Returns a path to the directory of the plugin."""

    return bus.getPluginDirPath()


def getDataDirPath():
    """Returns a path to the special directory where the plugin are allowed to store any data it wants."""

    return bus.getDataDirPath()


def log(obj):
    bus.log(obj)


def say(message):
    """A shortcut to send a message to the UI plugin to display some message.
    message -- any object which will be converted to a string.
    """

    bus.say(message)



# Aliases

def send_message(tag, data, response_listener = None):
    """Alias for sendMessage()."""

    sendMessage(tag, data, response_listener)


def add_message_listener(tag, listener):
    """Alias for addMessageListener()."""

    addMessageListener(tag, listener)


def remove_message_listener(tag, listener):
    """Alias for removeMessageListener()."""

    removeMessageListener(tag, listener)


def add_cleanup_handler(handler):
    """Alias for addCleanupHandler()."""

    addCleanupHandler(handler)


def get_plugin_dir_path():
    """Alias for getPluginDirPath()."""

    return getPluginDirPath()


def get_data_dir_path():
    """Alias for getDataDirPath()."""

    return getDataDirPath()
