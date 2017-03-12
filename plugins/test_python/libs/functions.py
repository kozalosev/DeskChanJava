from constants import TAG_PLUGIN

def build_tag(tag_action):
    """Builds and returns a string with the following format:
    TAG_PLUGIN:tag_action"""
    return "%s:%s" % (TAG_PLUGIN, tag_action)
