"""A simple abstraction over a special JSON file for settings storage.
Author: Leonid Kozarin <kozalo@nekochan.ru>
"""

import os
import json
import codecs


class Settings:
    """Singleton class which loads either a file or an empty dict.
    To access its fields use a common dict-like syntax:
        settings['some_param'].
        settings['another_param'] = "some value"
        settings.save() # Dumps changes on the disk.
    """

    _instance = None

    def __init__(self, path):
        """Constructor.
        :param path: A path to the settings file.
        :type path: str
        """

        self._file = path
        self._settings = {}

        if os.path.isfile(path):
            with codecs.open(path, "r", "utf-8") as f:
                self._settings = json.load(f)

    def __getitem__(self, item):
        if item in self._settings:
            return self._settings[item]
        else:
            return None

    def __setitem__(self, key, value):
        self._settings[key] = value

    def set(self, key, value, save=True):
        """Use this method if you want to save the change immediately."""

        self[key] = value
        if save:
            self.save()

    def save(self):
        """Dumps the settings to the file on the disk."""

        with codecs.open(self._file, "w", "utf-8") as f:
            json.dump(self._settings, f)

    @classmethod
    def get_instance(cls, path=None):
        """Use this method instead of the constructor!
        :param path: A path to the file. Should be omit if the settings have been already initialized.
        :type path: str
        :returns: An instance of the class.
        :rtype: Settings
        """

        if cls._instance is None:
            assert path is not None
            cls._instance = cls(path)

        return cls._instance
