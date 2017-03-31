import os
import json
import codecs


class Settings:
    _instance = None

    def __init__(self, path):
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
        self[key] = value
        if save:
            self.save()

    def save(self):
        with codecs.open(self._file, "w", "utf-8") as f:
            json.dump(self._settings, f)

    @classmethod
    def get_instance(cls, path=None):
        if cls._instance is None:
            assert path is not None
            cls._instance = cls(path)

        return cls._instance
