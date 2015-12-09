[![Build Status](https://travis-ci.org/beardandcode/users.svg)](https://travis-ci.org/beardandcode/users)

A library to manage and manipulate users for use with a webapp.

## Dependencies

  - PhantomJS 1.9.8 - Sadly Selenium doesn't yet support PhantomJS 2+, [PhantomJS 1.9.8 can be found here](https://bitbucket.org/ariya/phantomjs/downloads)

## Try

Find your way to the directory where you checked out this project and execute the following:

```
$ lein repl

user=> (go)  ;; starts the example webapp on a random port
...
...
... c.beardandcode.components.web-server - Started web server on http://127.0.0.1:8080

user=> (open!)  ;; only works on OSX

```

If you are not on OS X, open the url found in your logs it will likely be different to the one above.

