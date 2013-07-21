import requests
import json


class Light(object):

  def __init__(self, hub, _id):
    self.hub = hub
    self.id = _id


class HUE(object):

  def __init__(self, hub_ip, username):
    self.hub_ip = hub_ip
    self.username = username

  def _get(self, path, *args, **kwags):
    url = "http://%s/api/%s%s" % (self.hub_ip, self.username, path)
    print url
    response = requests.get(url)
    return response.json()

  def _put(self, path, *args, **kwargs):
    print args[0]
    url = "http://%s/api/%s%s" % (self.hub_ip, self.username, path)
    response = requests.put(url, *args, **kwargs)
    result = response.json()
    print result
    if "error" in result:
      print result
      raise Exception("Call failed")
    return result

  @classmethod
  def get_hubs(cls):
    return [i["internalipaddress"] for i in requests.get("http://www.meethue.com/api/nupnp").json()]

  def get_lights(self):
    return self._get("/lights")

  def flash(self, light_id, hue=0):
    self._put("/lights/%i/state" % (light_id, ), json.dumps({"on": True, "bri": 255, "hue": int(hue), "sat": 255})) 
    self._put("/lights/%i/state" % (light_id, ), json.dumps({"on": False, "bri": 0, "transitiontime": 8})) 
    #self._put("/lights/%i/state" % (light_id, ), json.dumps({"alert": "select", "transitiontime": 10000, "hue": int(hue), "sat": 255})) 

  def pulse_light(self, light_id, hue, fade_time):
    if hue < 1:
      hue = hue * 65535
    self._put("/lights/%i/state" % (light_id, ), json.dumps({"on": True, 
                                                             "bri": 255, 
                                                             "hue": int(hue), 
                                                             "sat": 255,
                                                             "effect": "none",
                                                             "transitiontime": 0}))
    self._put("/lights/%i/state" % (light_id, ), json.dumps({"bri": 0, "transitiontime": int(fade_time * 10)}))


  def turn_off_light(self, light_id):
    self._put("/lights/%i/state" % (light_id, ), json.dumps({ "on": False, "transitiontime": 0 }))
