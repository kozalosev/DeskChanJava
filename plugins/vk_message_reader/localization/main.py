"""A simple localization module.
Author: Leonid Kozarin <kozalo@nekochan.ru>
"""

import os
import re
import codecs
from java.lang import System


class Localization:
    """Singleton class which loads a locale-specific text file and returns localized strings."""

    _instance = None

    def __init__(self):
        path = os.path.dirname(__file__)
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
                    self.strings[matches.group(1)] = matches.group(2)

    @classmethod
    def get_instance(cls):
        """Use this method instead of the constructor!
        :returns: An instance of the class.
        :rtype: Localization
        """

        if cls._instance is None:
            cls._instance = cls()

        return cls._instance

    def __getitem__(self, item):
        """
        :returns: A localized string.
        :rtype: str
        :raises: ValueError
        """

        if item in self.strings:
            return self.strings[item].decode('string_escape')
        else:
            raise ValueError("No localized string: %s!" % item)
