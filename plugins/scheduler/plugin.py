from threading import Timer
from operator import itemgetter

from busproxy import *
from pluginutils import Localization, Settings

from constants import *
from functions import *


timer = None
sound = None
l10n = Localization.get_instance("localization")

opts = Settings.get_instance()
if not opts['events']:
    opts['events'] = []


def timer_action(messages, sound_path=None):
    for message in messages:
        say(message)

    global sound
    if not sound or not (sound == sound_path):
        sound = try_get_sound(sound_path)
    if sound:
        sound.play()

    update_state()

def update_timer():
    stop_timer()

    if len(opts['events']) == 0:
        return

    # We're going to allow our users to create several events on the same date and time.
    # Let's consider the first met sound as a sound of an entire notification.
    events = opts['events']
    closest_timestamp = min(events, key=itemgetter("timestamp"))['timestamp']
    closest_events = [x for x in events if x['timestamp'] == closest_timestamp]
    event_datetime, event_sound = None, None
    messages = []
    for event in closest_events:
        if not event_datetime:
            event_datetime = timestamp_to_datetime(event['timestamp'])
        if not event_sound:
            event_sound = event['sound'] if "sound" in event else None
        messages.append(event['message'])

    delta = diff_seconds(event_datetime)
    if delta > 0:
        global timer
        timer = Timer(delta, lambda: timer_action(messages, event_sound))
        timer.start()

def stop_timer(flush_events=True, event_cleaning=True):
    global timer
    if timer:
        timer.cancel()
        timer = None
    if event_cleaning:
        delete_expired_events(flush_events)

def add_event(data):
    if not data[TAG_MESSAGE]:
        send_message("gui:show-notification", {'text': l10n['no_message']})
        return
    if not data[TAG_DATE]:
        send_message("gui:show-notification", {'text': l10n['no_date']})
        return

    datetime = build_datetime(data[TAG_DATE], data[TAG_HOUR], data[TAG_MINUTE])
    if not datetime.isAfter(now()):
        send_message("gui:show-notification", {'text': l10n['attempt_to_add_event_in_past']})
        return

    new_event = {
        'message': data[TAG_MESSAGE],
        'timestamp': datetime.atZone(get_zone()).toEpochSecond()
    }

    if data[TAG_SOUND_FILE]:
        filepath = data[TAG_SOUND_FILE]
        new_event['sound'] = filepath
        opts['last_sound_file'] = filepath

    opts['events'].append(new_event)
    opts['events'] = sorted(opts['events'], key=itemgetter('timestamp'))
    opts.save()

    update_state()

def rebuild_options_menu(datetime):
    send_message("gui:setup-options-tab", {'name': 'Scheduler', 'msgTag': TAG_SAVE_OPTIONS, 'controls': [
        {
            'type': 'ListBox', 'id': TAG_LIST, 'label': l10n['label_events'],
            'values': [event['message'] for event in opts['events']]
        },
        {
            'type': 'DatePicker', 'id': TAG_DATE, 'label': l10n['label_date'],
            'format': DATE_FORMAT, 'value': datetime_to_string(datetime)
        },
        {
            'type': 'Spinner', 'id': TAG_HOUR, 'label': l10n['label_hour'],
            'value': datetime.getHour(), 'min': 0, 'max': 24, 'step': 1
        },
        {
            'type': 'Spinner', 'id': TAG_MINUTE, 'label': l10n['label_minute'],
            'value': datetime.getMinute(), 'min': 0, 'max': 59, 'step': 1
        },
        { 'type': 'TextField', 'id': TAG_MESSAGE, 'label': l10n['label_message'] },
        {
            'type': 'FileField', 'id': TAG_SOUND_FILE, 'label': l10n['label_sound'],
            'value': opts['last_sound_file']
        }
    ]})

def update_state():
    update_timer()
    rebuild_options_menu(now())


add_message_listener(TAG_SAVE_OPTIONS, lambda sender, tag, data: add_event(data))
# We can't flush options to the disk because it will be already inaccessible when the handler is running.
add_cleanup_handler(lambda: stop_timer(event_cleaning=False))
update_state()
