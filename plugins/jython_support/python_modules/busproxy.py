"""This is a special module for the plugin named jython_support. It provides a bunch of aliases for the bus object \
to let you write less redundant code."""

def inject(bus, dct, *methods):
    """Call this function to inject bus functions into a dictionary.
    
    :param bus: A bus object.
    :type bus: MethodProxy
    
    :param dct: Most likely you should pass either globals() or locals() here. The functions will be injected into a specified dictionary.
    :type dct: dict
    
    :param methods: Names of the methods you want to inject. Don't pass anything to import all functions.
    :type methods: str
    """

    assert bus, "The bus is not passed!"
    assert type(dct) == dict, "dct is not a dict!"

    def py2ja(pythonic_name):
        """String function that is used to covert pythonic_names to javaOnes."""

        import re

        pattern = "_[a-z]"
        repl = lambda x: x.group(0)[1].upper()
        java_name = re.sub(pattern, repl, pythonic_name)

        return java_name


    if len(methods) == 0:
        methods = [
            'getId', 'get_id',
            'sendMessage', 'send_message',
            'addMessageListener', 'add_message_listener',
            'removeMessageListener', 'remove_message_listener',
            'addCleanupHandler', 'add_cleanup_handler',
            'getPluginDirPath', 'get_plugin_dir_path',
            'getDataDirPath', 'get_data_dir_path',
            'getRootDirPath', 'get_root_dir_path',
            'log', 'say'
        ]

    for method in methods:
        java_name = py2ja(method)
        if java_name in dir(bus):
            dct[method] = bus.__getattribute__(java_name)
        else:
            raise ValueError("Unknown bus method: %s!" % method)
