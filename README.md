# org.photonsphere/fulcro.ui.bulma

A fulcro.ui.bootstrap3 inspired Clojure library for Bulma, to be used with Fulcro.

## Rationale

A purposely 'thin' Clojure library for the lean CSS-only, [Flexbox](https://en.wikipedia.org/wiki/CSS_Flex_Box_Layout) based, [Bulma](https://github.com/jgthms/bulma) framework. Work in progress. Intention is to *not* implement all Bulma components and elements. For example rendering and adding ClojureScript to a Bulma button is relatively straightforward. More complex Bulma components, such as a Navbar and a Dropdown, will be supported by this library.

## Currently implemented Bulma components

* Navbar
* Dropdown

## Usage (via Clojars)

The library is on Clojars: [org.photonsphere/fulcro.ui.bulma](https://clojars.org/org.photonsphere/fulcro.ui.bulma). In your project.clj use the following dependency:

`[org.photonsphere/fulcro.ui.bulma "0.1.1"]`

Also see the file [docs/intro.md](https://github.com/maridonkers/fulcro-bulma/blob/master/docs/intro.md).

## Copyright and License

[Fulcro](https://github.com/fulcrologic/fulcro) is:

Copyright (c) 2018, Fulcrologic, LLC
The MIT License (MIT)

[Fulcro-Bulma](https://github.com/maridonkers/fulcro-bulma) is:

Copyright (c) 2018, Mari Donkers (photonsphere.org; Twitter: @maridonkers | Google+: +MariDonkers | GitHub: maridonkers)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
