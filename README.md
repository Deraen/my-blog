# My blog

WIP

- Uses [boot][boot-clj]
- Uses [Cryogen][cryogen]
- Advantages to using Boot instead of Leiningen:
  - Easy to use Cljs
  - Live reloading
  - User can select LESS, SCSS or other general tasks which need not know
  anything about blog generator

## TODO

- Now every file is recompiled when ever one file is changed
  - Keeping the state between file changes and only loading the changed files
  should make recompilatin faster

[boot-clj]: http://boot-clj.com/
[cryogen]: http://cryogenweb.org/
