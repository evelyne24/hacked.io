import time
import colorsys
import threading
import itertools
import contextlib
import random
import Queue
import hueapi

HUES = [
  0, 0.3333, 0.6666, 0.16665, 0.5, 0.8333333
]
HUE_NAMES = ["red", "green", "blue", "yellow", "cyan", "purple"]

PLAYER_NAMES = {
  "047c84c21c2680": "Steve Stagg",
  "04b4d5c21c2680": "Evelina Vrabie"
}


HUB_IP = "192.168.2.76"
USERNAME = "stevestagg"

def num_to_hue(num):
  return HUES[num % len(HUES)] * 65535

def num_to_hue_name(num):
  return HUE_NAMES[num % len(HUES)]

def num_to_hex(num, value=1):
  r,g,b = colorsys.hsv_to_rgb(num_to_hue(num) / 65535.0, 1, value)
  return "#%02x%02x%02x" % (r * 255, g*255, b*255)


class Light(object):

  def __init__(self, model, _id):
    self.model = model
    self.id = _id
    self.flash()
    self.on_time = None
    self.time = self.sleep_time()
    self.color = None
    self._choices = []

  @property
  def choices(self):
    if len(self._choices) == 0:
      self._choices = list(itertools.chain(*[[i]*2 for i in range(len(self.model.players))]))
      random.shuffle(self._choices)
    return self._choices

  def flash(self, hue=0):
    self.model.hue.flash(self.id, hue=hue)

  def sleep_time(self):
    return random.uniform(1, 2)

  def burn_time(self):
    return random.uniform(3, 6)

  @property
  def hue(self):
    if self.color is None:
      return None
    return num_to_hue(self.color)

  @property
  def hex(self):
    if self.color is None:
      return "#fff"
    return num_to_hex(self.color)

  @property
  def alight(self):
    return self.on_time is not None

  @property
  def worth(self):
    if not self.alight:
      return 0
    now = time.time()
    duration = self.time - self.on_time
    age = now - self.on_time
    return (max(0, (duration - age)) / duration) * 100

  def expire(self):
    self.model.hue.turn_off_light(self.id)
    self.on_time = None
    self.color = None
    self.time = time.time() + self.sleep_time()

  def update(self):
    if self.alight:
      if self.worth == 0:
        self.expire()
    else:
      if self.model.game.remaining == 0:
        return
      if time.time() > self.time:
        self.on_time = time.time()
        new_color = self.choices.pop()
        self.color = new_color
        pulse_time = self.burn_time()
        self.time = time.time() + pulse_time
        self.pulse(pulse_time)

  def pulse(self, fade_time):
    self.model.hue.pulse_light(self.id, self.hue, fade_time)


class Player(object):
  
  def __init__(self, uid, id_):
    self.uid = uid
    self.id = id_
    self.score = 0
  
  @property
  def color(self):
    return self.id

  @property
  def color_name(self):
    return num_to_hue_name(self.color)

  @property
  def hue(self):
    return num_to_hue(self.color)

  @property
  def hex(self):
    return num_to_hex(self.color)

  @property
  def name(self):
    fallback = "Unknown player %s" % (self.id, )
    return PLAYER_NAMES.get(self.uid, fallback)


class Model(object):

  def __init__(self, game):
    self.game = game
    self.lights = {}
    self.players = {}
    self._free_bulbs = None

  @property
  def free_bulbs(self):
    if self._free_bulbs is None:
      self._free_bulbs = dict((int(k), v["name"]) for k, v in self.hue.get_lights().iteritems())
    return self._free_bulbs

  @property
  def hue(self):
    return hueapi.HUE(self.game.hub_addr, USERNAME)

  def tag_scan(self, reader_uid, tag_id):
    if tag_id not in self.players:
      player = Player(tag_id, len(self.players))
      self.players[tag_id] = player
      if reader_uid in self.lights:
        self.lights[reader_uid].flash(hue=player.hue)
      return "new", player.hex, player.color_name
    if not self.game.game_running:
      return "error", ""
    light = self.lights[reader_uid]
    player = self.players[tag_id]
    if player.color == light.color:
      player.score += int(light.worth)
      light.expire()
      return "score", str(int(player.score))
    return "error", ""

  def add_reader(self, reader_uid):
    if reader_uid in self.lights:
      return
    light_id = sorted(self.free_bulbs.iteritems())[0][0]
    self.lights[reader_uid] = Light(self, int(light_id))
    del self.free_bulbs[light_id]

  def update(self):
    now = time.time()
    for light in self.lights.values():
      light.update()


class Game(threading.Thread):

  GAME_TIME = 30

  def __init__(self):
    super(Game, self).__init__()
    self.hub_addr = None
    self.daemon = True
    self.start_time = None
    self.lock = threading.RLock()
    self._model = Model(self)

  @property
  def age(self):
    return time.time() - self.start_time

  @property
  def remaining(self):
    return max(self.GAME_TIME - self.age, 0)

  @property
  def game_running(self):
    return self.start_time is not None

  def start_game(self):
    with self.with_model() as model:
      if len(model.players) and len(model.lights):
        self.start_time = time.time()

  @contextlib.contextmanager
  def with_model(self):
    self.lock.acquire()
    try:
      yield self._model
    finally:
      self.lock.release()

  def start_game(self):
    self.start_time = time.time()
    with self.with_model() as model:
      for light in model.lights.values():
        light.time = time.time() + light.sleep_time()

  def run(self):
    while True:
      time.sleep(0.1)
      if self.game_running:
        with self.with_model() as model:
          model.update()
          if not any(l.alight for l in model.lights.values()) and self.remaining == 0:
            self.start_time = None