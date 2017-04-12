# I use Java's libraries instead of Python's datetime module to don't care about different time format strings.
from java.time import LocalDateTime, LocalDate, LocalTime, ZoneId, Instant
from java.time.format import DateTimeFormatter
from java.time.temporal import ChronoUnit
from javafx.scene.media import Media, MediaPlayer, MediaException
from java.io import File

from constants import DATE_FORMAT
from pluginutils import Settings
from busproxy import log


def build_datetime(date_str, hour, minute):
    formatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
    date = LocalDate.parse(date_str, formatter)
    time = LocalTime.of(hour, minute)
    datetime = LocalDateTime.of(date, time)
    return datetime

def timestamp_to_datetime(timestamp):
    zone = get_zone()
    instant = Instant.ofEpochSecond(timestamp)
    datetime = LocalDateTime.ofInstant(instant, zone)
    return datetime

def datetime_to_string(datetime):
    formatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
    string = datetime.format(formatter)
    return string

def get_zone():
    return ZoneId.systemDefault()

def now():
    return LocalDateTime.now()

def diff_seconds(datetime):
    return ChronoUnit.SECONDS.between(now(), datetime)

def delete_expired_events(save=True):
    opts = Settings.get_instance()
    opts_modified = False
    for event in opts['events']:
        datetime = timestamp_to_datetime(event['timestamp'])
        if diff_seconds(datetime) <= 0:
            opts['events'].remove(event)
            opts_modified = True
    if opts_modified and save:
        opts.save()


class Sound:
    def __init__(self, filepath):
        file = File(filepath)
        media = Media(file.toURI().toString())
        self._player = MediaPlayer(media)
        self._filepath = filepath

    def __eq__(self, another):
        if isinstance(another, self.__class__):
            return self._filepath == another._filepath
        else:
            return self._filepath == another

    def play(self):
        self._player.play()

def try_get_sound(filepath):
    sound = None
    if filepath:
        try:
            sound = Sound(filepath)
        except MediaException as err:
            log(err.toString())
    return sound
