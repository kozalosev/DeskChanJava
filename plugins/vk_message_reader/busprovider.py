from pluginutils import Localization, Settings


class BusProvider:
    """A simple storage to pass the bus through all other modules."""

    _bus = None

    @classmethod
    def init(cls, bus):
        """Call this method at the start of the plugin!"""
        cls._bus = bus

    @classmethod
    def get_localization(cls, *args, **kwargs):
        """:returns: Localization"""
        return Localization.get_instance(cls._bus, *args, **kwargs)

    @classmethod
    def get_settings(cls, *args, **kwargs):
        """:returns: Settings"""
        return Settings.get_instance(cls._bus, *args, **kwargs)
