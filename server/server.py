import os
import contextlib
import json
import colorsys
import random
import threading
import time
import game
import operator
import urllib
import flask
import hueapi

GAMENAME = "H.U.E. Tag"

app = flask.Flask(__name__)
app.game = None

def initgame():
  if app.game is None:
    app.game = game.Game()
    app.game.start() 


@app.route("/hello/<phone>", methods=["GET", "POST"])
def phone_hi(phone):
  initgame()
  with app.game.with_model() as model:
    model.add_reader(phone)
  return flask.jsonify({ "response": "OK"})


@app.route("/tag", methods=["GET", "POST"])
def tag():
  reader =  flask.request.json["device"]
  tag =  flask.request.json["tag"]
  print ">>>", tag
  initgame()
  with app.game.with_model() as model:
    result = model.tag_scan(reader, tag)
  return json.dumps(dict(zip(["type", "data", "name"], result)))


@app.route("/jquery.js")
def send_jq():
  return flask.send_file("static/jquery-1.8.2.min.js", mimetype="application/javascript")

@app.route("/start")
def start_game():
  app.game.start_game()
  return "OK"


@app.route("/pulse/<int:light>")
def pulse(light):
  with app.game.with_model() as model:
    model.pulse_light(light)
  return "OK"


@app.route("/")
def load():
  initgame()
  hubs = hueapi.HUE.get_hubs()
  return flask.render_template("load.html", gamename=GAMENAME, hubs=hubs)


@app.route("/game")
def main():
  initgame()
  if app.game.hub_addr is None:
    return flask.redirect("/")
  app.game.hub_addr
  return flask.render_template("index.html", gamename=GAMENAME)


@app.route("/set_hub/<ip>")
def set_hub(ip):
  initgame()  
  app.game.hub_addr = ip
  return flask.redirect("/game")

@app.route("/remove_bulb/<int:id>")
def remove_bulb(id):
  initgame()
  with app.game.with_model() as model:
    del model.free_bulbs[id]
  return flask.redirect("/game")

@app.route("/update")
def update():
  initgame()
  if app.game.hub_addr is None:
    return flask.jsonify({"fail": True})
  with app.game.with_model() as model:

    data = {
      "players": flask.render_template("players.html", players=sorted(model.players.values(), key=operator.attrgetter("score"), reverse=True)),
      "lights": flask.render_template("lights.html", model=model),
      "free_bulbs": flask.render_template("free_bulbs.html", bulbs=sorted(model.free_bulbs.iteritems())),
      "state": ("%i seconds left" % app.game.remaining) if app.game.game_running else "Waiting",
      "ready": (len(model.lights)>0 and len(model.players)>0 and not app.game.game_running)
    }
  return flask.jsonify(data)


if __name__ == "__main__":
  app.run(debug=True, host='0.0.0.0', port=5000)
