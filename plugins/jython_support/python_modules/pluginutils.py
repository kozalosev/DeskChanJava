"""A set of some useful classes for plugins for DeskChan in Jython.
(c) Leonid Kozarin <kozalo@nekochan.ru>, 2017
"""

import os
import json
import codecs
import re
from java.lang import System


class AbstractMultitonMetaclass(type):
    """This metaclass is responsible for providing the independent `_instances` variable to all descendants of the AbstractMultiton."""

    def __new__(cls, name, bases, dct):
        dct['_instances'] = {}
        return super(cls, cls).__new__(cls, name, bases, dct)


class AbstractMultiton:
    """An abstract implementation of the registry of singletons.
    Gives method get_instance() and a static variable `_instances` to all its descendants.
    """

    __metaclass__ = AbstractMultitonMetaclass

    def __init__(self, bus):
        """
        :param bus: A bus object.
        :type bus: MethodProxy
        """

        assert bus
        self.bus = bus

    @classmethod
    def get_instance(cls, bus, *args, **kwargs):
        """Use this method instead of the constructor! Pass here the same arguments as the constructor takes.
        
        :param bus: A bus object.
        :type bus: MethodProxy
        
        :returns: An instance of a specific class.
        """

        key = bus.getId()

        if key not in cls._instances:
            instance = cls(bus, *args, **kwargs)
            cls._instances[key] = instance
            bus.addCleanupHandler(instance.destroy_instance)

        return cls._instances[key]

    def destroy_instance(self):
        plugin_id = self.bus.getId()
        if plugin_id in self._instances:
            del self._instances[plugin_id]


class Settings(AbstractMultiton):
    """This multiton class is a simple abstraction over a special JSON file for settings storage.
    To access its fields use a common dict-like syntax:
        settings['some_param'].
        settings['another_param'] = "some value"
        settings.save() # Dumps changes on the disk.
    """

    def __init__(self, bus, filename="settings.json"):
        """Constructor. Don't use it directly! Use get_instance() instead!
        
        :param bus: A bus object.
        :type bus: MethodProxy
        
        :param filename: The name of a file to store the settings.
        :type filename: str
        """

        super(Settings, self).__init__(bus)

        self._file = os.path.join(bus.getDataDirPath(), filename)
        self._settings = {}

        if os.path.isfile(self._file):
            with codecs.open(self._file, "r", "utf-8") as f:
                self._settings = json.load(f)

    def __contains__(self, item):
        return item in self._settings

    def __getitem__(self, item):
        """Returns either a stored object or None."""

        if item in self._settings:
            return self._settings[item]
        else:
            return None

    def __setitem__(self, key, value):
        self._settings[key] = value

    def __delitem__(self, key):
        del self._settings[key]

    def __str__(self):
        return "%s: %s" % (self.__class__, self._settings)

    def get(self, item, default=None):
        """Use this method if there is no value and you don't want to get None anyway."""

        value = self[item]
        return value if value is not None else default

    def set(self, key, value, save=True):
        """Use this method if you want to save the change immediately."""

        self[key] = value
        if save:
            self.save()

    def save(self):
        """Dumps the settings to the file on the disk."""

        with codecs.open(self._file, "w", "utf-8") as f:
            json.dump(self._settings, f)


class Localization(AbstractMultiton):
    """This multiton class provides a simple localization mechanism, which loads a locale-specific text file and returns localized strings."""

    def __init__(self, bus, localization_dir):
        """Constructor. Don't use it directly! Use get_instance() instead!
        
        :param bus: A bus object.
        :type bus: MethodProxy
        
        :param localization_dir: A path to the directory where localization files are stored.
        :type localization_dir: str
        """

        super(Localization, self).__init__(bus)

        assert localization_dir
        path = os.path.join(bus.getPluginDirPath(), localization_dir)

        # language = locale.getdefaultlocale()[0]
        # The code above returns a tuple of two None values in Jython. So we have to use a Java equivalent.
        language = "%s_%s" % (System.getProperty("user.language"), System.getProperty("user.country"))

        filename = os.path.join(path, language + ".txt")
        if not os.path.isfile(filename):
            filename = os.path.join(path, language[:2] + ".txt")
            if not os.path.isfile(filename):
                filename = os.path.join(path, "en.txt")
                if not os.path.isfile(filename):
                    raise IOError("No default localization!")

        self.strings = {}
        pattern = re.compile("(\S+)\s*=\s*(.*)")
        with codecs.open(filename, "r", "utf-8") as f:
            for line in f:
                matches = pattern.match(line)
                if matches:
                    self.strings[matches.group(1)] = matches.group(2).rstrip()

    def __getitem__(self, item):
        """
        :returns: A localized string.
        :rtype: str
        :raises: ValueError
        """

        if item in self.strings:
            return self.strings[item].decode("unicode_escape")
        else:
            raise ValueError("No localized string: %s!" % item)
