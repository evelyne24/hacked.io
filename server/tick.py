import urllib
import time
import sys


def main():
  while True:
    try:
      urllib.urlopen("http://127.0.0.1:5000/tick")
    except Exception, e:
      print e
    time.sleep(4)


if __name__ == "__main__":
  sys.exit(main())