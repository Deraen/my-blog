# My blog

WIP

- Uses [boot][boot-clj]
- Uses [Cryogen][cryogen]
- Advantages to using Boot instead of Leiningen:
  - Easy to use Cljs
  - Live reloading
  - User can select LESS, SCSS or other general tasks which need not know
  anything about blog generator

## Notes

- I had to copy some of `cryogen.compiler` ns
  - I changed the find-\* and read-\* fns to take fileset as param and they now
  find the files they are interested using boot fileset api
  - compile-\* fns now take tmpdir as parameter and write the result files there

## TODO

- Now every file is recompiled when ever one file is changed
  - Keeping the state between file changes and only loading the changed files
  should make recompilatin faster
- Cryogen stuff should run in pod
- Cryogen-boot should be a separate project

[boot-clj]: http://boot-clj.com/
[cryogen]: http://cryogenweb.org/
