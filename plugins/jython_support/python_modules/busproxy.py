"""This is a special module for the plugin named jython_support. It provides a bunch of aliases for the bus object \
to let you write less redundant code."""

# Proxy methods

def getId():
    """
    :returns: The id of the currently executing plugin.
    :rtype: str
    """

    return bus.getId()

def sendMessage(tag, data, response_listener = None):
    """Sends a message to other plugins.
    
    :param tag: Your message will receive all plugins subscribed on this tag.
    :type tag: str
    
    :param data: An object with some required data specified by the concrete plugin.
    :type data: dict
    
    :param response_listener: A callback function; note that not every plugin supports this option and will call your function.
    :type response_listener: callable
    """

    if response_listener:
        bus.sendMessage(tag, data, response_listener)
    else:
        bus.sendMessage(tag, data)


def addMessageListener(tag, listener):
    """Use this function to subscribe on any tag.
    
    :param tag: We recommend you use a string in format `your_plugin_id:internal_tag` or `your-plugin-id:internal-tag`.
    :type tag: str
    
    :param listener: Your listener will be called when another plugin sends a message with the specified tag.
    :type listener: callable
    """

    bus.addMessageListener(tag, listener)


def removeMessageListener(tag, listener):
    """Unsubscribe from the tag.
    
    :param tag: The tag that you used in the addMessageListener() function.
    :type tag: str
    
    :param listener: The listener that you used in the addMessageListener() function.
    :type listener: callable
    """

    bus.removeMessageListener(tag, listener)


def addCleanupHandler(handler):
    """Use this function to set a callback, which will be called when the plugin is being unloaded.
    
    :param handler: A callback that should release all resources, stop all timers, and so on.
    :type handler: callable
    """

    bus.addCleanupHandler(handler)


def getPluginDirPath():
    """
    :returns: A path to the directory of the plugin.
    :rtype: str
    """

    return bus.getPluginDirPath()


def getDataDirPath():
    """
    :returns: A path to the special directory where the plugin are allowed to store any data it wants.
    :rtype: str
    """

    return bus.getDataDirPath()


def log(obj):
    """Use this function instead of printing text to stdout!
    :param obj: A string or any object.
    """

    bus.log(obj)


def say(text, character_image = None, timeout = None, priority = None):
    """A shortcut to send a message to the UI plugin to display some message.
    
    :param text: Any object, which will be converted to a string.
    
    :param character_image: The name of a character's sprite (emotion).
    :type character_image: str
    
    :param timeout: The message will be closed after a certain amount of time or if the user clicks on it. If the timeout is 0, only the user can close the message.
    :type timeout: int
    
    :param priority: More important messages are shown first. If the priority is less or equal to zero, the message won't be scheduled at all.
    :type priority: int
    """

    params = {}
    if character_image:
        params['characterImage'] = character_image
    if timeout:
        params['timeout'] = timeout
    if priority:
        params['priority'] = priority

    bus.say(text, params)



# Aliases

def get_id():
    """Alias for getId()."""

    return getId()

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
