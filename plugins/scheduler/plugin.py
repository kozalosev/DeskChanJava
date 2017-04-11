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

clean_expired_events()


def timer_action(msg):
    say(msg)
    update_timer()

def update_timer(data=None):
    stop_timer()

    clean_expired_events(save=False)

    if data:
        if data[TAG_ACTION] == ACTION_ADD:
            if not data[TAG_MESSAGE]:
                say("No message!")
                return
            if not data[TAG_DATE]:
                say("No date!")
                return

            datetime = datetime_builder(data[TAG_DATE], data[TAG_HOUR], data[TAG_MINUTE])
            if not datetime.isAfter(now()):
                say("I don't have a time machine, senpai!")
                return

            opts['events'].append({
                'message': data[TAG_MESSAGE],
                'timestamp': datetime.atZone(get_zone()).toEpochSecond()
            })
        elif data[TAG_ACTION] == ACTION_DELETE:
            if not data[TAG_LIST]:
                say("You should have selected something!")
                return

            del opts['events'][data[TAG_LIST]]
        else:
            raise NotImplementedError("Unexpected action!")

        opts['events'] = sorted(opts['events'], key=itemgetter('timestamp'))
        opts.save()

    closest_event = opts['events'][0]
    event_datetime = timestamp_to_datetime(closest_event['timestamp'])
    event_message = closest_event['message']

    delta = diff_seconds(event_datetime)
    log("[delta: %i]: %s" % (delta, event_message))

    if delta > 0:
        timer = Timer(delta, lambda: timer_action(event_message))
        timer.start()

    if data:
        say("OK! I'll remind you about that!")
        # I'm not able to update the list due to the fact that API is still pretty poor.

def stop_timer():
    global timer
    if timer:
        timer.cancel()
        timer = None


def build_options_menu(datetime):
    send_message("gui:add-options-tab", {'name': 'Scheduler', 'msgTag': TAG_SAVE_OPTIONS, 'controls': [
        {
            'type': 'ComboBox', 'id': TAG_ACTION, 'label': 'Action',
            'values': ['Add', 'Delete'], 'value': 0
        },
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
build_options_menu(now())
