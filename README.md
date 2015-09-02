[![Build Status](https://travis-ci.org/beardandcode/users.svg)](https://travis-ci.org/beardandcode/users)

A library to manage and manipulate users for use with a webapp

## Try

Find your way to the directory where you checked out this project and execute the following:

```
$ lein repl

user=> (start-webapp!)  ;; starts the example webapp on a random port
Listening on http://localhost:53677/

user=> (open-webapp!)   ;; only works on OSX as it uses /usr/bin/open
                        ;; on linux point your browser at the url printed
                        ;; after running start-webapp!
```

