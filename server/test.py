import sys
import hueapi
import time


def main():
  hue = hueapi.HUE("192.168.2.76", "stevestagg")
  hue.flash(3, 0)


if __name__ == "__main__":
  sys.exit(main())