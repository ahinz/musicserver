# Sound Server via Clojure

A Clojure app designed to allow coworkers to share music.

## Usage

Listen to music with coworkers and friends! On the computer with
speakers start the server:

```bash
$ lein run server
```

To add clients to the server have each client run something like:

```bash
$ lein run client --port 10100 --file example.txt \
                  --server http://localhost:3131 \
                  --bind <ip address of client>
```

Specify a file of "-" to read the playlist from stdin

The client will ping the server and start playing music. Any number of
clients can connect to the music server. As songs complete each server
is queried in a round-robin fashion for the next song to play.

Right now clients aren't really dynamic; you provide them a "--file"
argument with that contains a bunch of paths to music files (separated
by newlines)

## Frontend

To run the webserver and see info about the currently playing songs
compile the cljs files

```bash
$ lein cljsbuild once
```

And start a webserver
```bash
$ python -m SimpleHTTPServer 8888
```

And go to town!

To install the netty websocket server you'll have to compile it yourself
since it isn't in maven central (yet? Just got to v1.0 on June 7th)

```bash
git clone https://github.com/mrniko/netty-socketio
mvn install
```

## TODO

- Clients that exit wont get removed--- doesn't cause issues but is
  annoying

- Using mplayer to play the music to avoid downloading a ton of java
  junk. Probably should download java junk to avoid the dependency

## License

Copyright © 2013 Adam Hinz

MIT License
