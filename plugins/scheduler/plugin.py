from threading import Timer
from operator import itemgetter

from busproxy import *
from pluginutils import Localization, Settings

from java.time import LocalDateTime, LocalDate, LocalTime, ZoneId, Instant
from java.time.temporal import ChronoUnit


PLUGIN_TAG = "timer_demo"
TAG_SAVE_OPTIONS = "%s:save-options" % PLUGIN_TAG
TAG_MESSAGE = "%s:message" % PLUGIN_TAG
TAG_DATE = "date"
TAG_HOUR = "hour"
TAG_MINUTE = "minute"
TAG_LIST = "notification-list"

DATE_FORMAT = "d.M.y"


timer = None
opts = Settings.get_instance()

def update_timer(data = None):
    def action(data):
        say(data)
        update_timer()

    stop_timer()
    zone = ZoneId.systemDefault()

    if data:
        date = LocalDate.parse(data[TAG_DATE], DATE_FORMAT)
        time = LocalTime.of(data[TAG_HOUR], data[TAG_MINUTE])
        datetime = LocalDateTime(date, time)

        if "events" not in opts:
            opts['events'] = []

        opts['events'].append({
            'message': data[TAG_MESSAGE],
            'timestamp': datetime.atZone(zone).toEpochSecond()
        })
        opts.save()

    events = sorted(opts['events'], key=itemgetter('timestamp'))
    closest_event = events[0]
    event_instant = Instant.ofEpochSecond(closest_event)
    event_datetime = LocalDate.of(event_instant, zone)
    event_message = closest_event['message']

    now = LocalDateTime.now()
    delta = ChronoUnit.MILLIS.between(now, event_datetime)

    timer = Timer(delta, lambda: action(event_message))
    timer.start()

def stop_timer():
    global timer
    if timer:
        timer.cancel()
        timer = None

# I use Java's LocalTime instead of Python's datetime module to don't care about different time format strings.
now = LocalTime.now()

bus.sendMessage("gui:add-options-tab", {'name': 'Timer Demo', 'msgTag': TAG_SAVE_OPTIONS, 'controls': [
    {
        'type': 'ListBox', 'id': TAG_LIST, 'label': 'List of all scheduled notifications',
        'values': opts.get("events", default=[])
    },
    {
        'type': 'DatePicker', 'id': TAG_DATE, 'label': 'Date',
        'format': DATE_FORMAT
    },
    {
        'type': 'Spinner', 'id': TAG_HOUR, 'label': 'Hour',
        'value': now.getHour(), 'min': 0, 'max': 24, 'step': 1
    },
    {
        'type': 'Spinner', 'id': TAG_MINUTE, 'label': 'Minute',
        'value': now.getMinute(), 'min': 0, 'max': 59, 'step': 1
    },
    {
        'type': 'TextField', 'id': TAG_MESSAGE, 'label': 'Message',
        'value': 'Water in your kettle is boiling!'
    }
]})

bus.addMessageListener(TAG_SAVE_OPTIONS, lambda sender, tag, data: update_timer(data))
bus.addCleanupHandler(stop_timer)
