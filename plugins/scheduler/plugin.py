from threading import Timer
from operator import itemgetter

from busproxy import *
from pluginutils import Localization, Settings

from constants import *
from functions import *


timer = None

opts = Settings.get_instance()
if not opts['events']:
    opts['events'] = []


def timer_action(msg):
    say(msg)
    update_timer()

def update_timer(data=None):
    # If we came here from the options menu,
    # we don't need to removed expired events immediately since we'll do it later manually.
    stop_timer(not data)

    if data:
        if not data[TAG_MESSAGE]:
            say("No message!")
            return
        if not data[TAG_DATE]:
            say("No date!")
            return

        datetime = build_datetime(data[TAG_DATE], data[TAG_HOUR], data[TAG_MINUTE])
        if not datetime.isAfter(now()):
            say("I don't have a time machine, senpai!")
            return

        opts['events'].append({
            'message': data[TAG_MESSAGE],
            'timestamp': datetime.atZone(get_zone()).toEpochSecond()
        })

        opts['events'] = sorted(opts['events'], key=itemgetter('timestamp'))
        opts.save()

    if len(opts['events']) == 0:
        return

    closest_event = opts['events'][0]
    event_datetime = timestamp_to_datetime(closest_event['timestamp'])
    event_message = closest_event['message']

    delta = diff_seconds(event_datetime)
    if delta > 0:
        global timer
        timer = Timer(delta, lambda: timer_action(event_message))
        timer.start()

    if data:
        say("OK! I'll remind you about that!")
        # I'm not able to update the list due to the fact that API is still pretty poor.

def stop_timer(flush_events=True):
    global timer
    if timer:
        timer.cancel()
        timer = None
    clean_expired_events(flush_events)


def build_options_menu(datetime):
    send_message("gui:add-options-tab", {'name': 'Scheduler', 'msgTag': TAG_SAVE_OPTIONS, 'controls': [
        {
            'type': 'ListBox', 'id': TAG_LIST, 'label': 'List of all scheduled notifications',
            'values': [event['message'] for event in opts['events']]
        },
        {
            'type': 'DatePicker', 'id': TAG_DATE, 'label': 'Date',
            'format': DATE_FORMAT, 'value': datetime_to_string(datetime)
        },
        {
            'type': 'Spinner', 'id': TAG_HOUR, 'label': 'Hour',
            'value': datetime.getHour(), 'min': 0, 'max': 24, 'step': 1
        },
        {
            'type': 'Spinner', 'id': TAG_MINUTE, 'label': 'Minute',
            'value': datetime.getMinute(), 'min': 0, 'max': 59, 'step': 1
        },
        { 'type': 'TextField', 'id': TAG_MESSAGE, 'label': 'Message' }
    ]})

add_message_listener(TAG_SAVE_OPTIONS, lambda sender, tag, data: update_timer(data))
add_cleanup_handler(stop_timer)
update_timer()
build_options_menu(now())
