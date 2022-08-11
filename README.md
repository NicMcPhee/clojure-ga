# clojure-ga

An implementation of a simple genetic algorithm (GA) in Clojure,
mostly done so I can run some timing comparisons between Clojure
and Rust.

## Timing for constructing a population

All timings are computing using
[the Criterium library](https://github.com/hugoduncan/criterium).
I used calls like:

```clojure
(with-progress-reporting (bench (doall (make-population 1000 128)) :verbose))
```

i.e., making a population of a thousand individuals each with
length 128.

:warning: The `doall` is critical; without it nothing actually
gets constructed thanks to lazy evaluation. The serial timing
without `doall` was about 20ns, where it's over 40ms with the
`doall` inserted.

### Serial (no parallelism)

With `make-population` implemented with no parallelism, i.e.,

```clojure
(defn make-population
  [pop-size num-bits]
  (repeatedly
   pop-size
   #(make-individual num-bits)))
```

we get an evaluation time of just over 43ms:

```text
(with-progress-reporting (bench (doall (make-population 1000 128)) :verbose))
Warming up for JIT optimisations 10000000000 ...
Estimating execution count ...
Sampling ...
Final GC...
Checking GC...
Finding outliers ...
Bootstrapping ...
Checking outlier significance
aarch64 Mac OS X 12.5 8 cpu(s)
OpenJDK 64-Bit Server VM 18.0.2+0
Runtime arguments: -Dfile.encoding=UTF-8 -XX:-OmitStackTraceInFastThrow -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dclojure.compile.path=/Users/mcphee/Documents/Year_of_Programming/clojure-ga/target/default/classes -Dclojure-ga.version=0.1.0-SNAPSHOT -Dclojure.debug=false
Evaluation count : 1440 in 60 samples of 24 calls.
      Execution time sample mean : 43.466522 ms
             Execution time mean : 43.502910 ms
Execution time sample std-deviation : 1.453606 ms
    Execution time std-deviation : 1.516402 ms
   Execution time lower quantile : 42.605081 ms ( 2.5%)
   Execution time upper quantile : 46.805365 ms (97.5%)
                   Overhead used : 9.735649 ns

Found 7 outliers in 60 samples (11.6667 %)
 low-severe  4 (6.6667 %)
 low-mild  3 (5.0000 %)
 Variance from outliers : 22.1664 % Variance is moderately inflated by outliers
```

### Parallel using `pmap`

Using `pmap` to parallelise the population creation as follows:

```clojure
(defn pmap-make-population
  [pop-size num-bits]
  (pmap (fn [_] (make-individual num-bits))
        (range pop-size)))
```

We got almost exactly the same timing, i.e., just under 45ms. This
is super weird, and I'm confused. The Activity Monitor made it clear
that we were using all the cores (where the serial was just using
one), but the timing didn't improve at all.

Maybe the cost of parallelizing the construction of 1K individuals
is just too high relative to the cost of constructing them, and we
don't win here. That's interesting, though, given that the Rust
code saw a substantial speedup when we added the parallelism.

```text
(with-progress-reporting (bench (doall (pmap-make-population 1000 128)) :verbose))
Warming up for JIT optimisations 10000000000 ...
  compilation occurred before 53 iterations
  compilation occurred before 105 iterations
  compilation occurred before 261 iterations
Estimating execution count ...
Sampling ...
Final GC...
Checking GC...
Finding outliers ...
Bootstrapping ...
Checking outlier significance
aarch64 Mac OS X 12.5 8 cpu(s)
OpenJDK 64-Bit Server VM 18.0.2+0
Runtime arguments: -Dfile.encoding=UTF-8 -XX:-OmitStackTraceInFastThrow -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dclojure.compile.path=/Users/mcphee/Documents/Year_of_Programming/clojure-ga/target/default/classes -Dclojure-ga.version=0.1.0-SNAPSHOT -Dclojure.debug=false
Evaluation count : 1560 in 60 samples of 26 calls.
      Execution time sample mean : 44.894916 ms
             Execution time mean : 44.879725 ms
Execution time sample std-deviation : 2.368709 ms
    Execution time std-deviation : 2.400475 ms
   Execution time lower quantile : 39.249901 ms ( 2.5%)
   Execution time upper quantile : 48.462834 ms (97.5%)
                   Overhead used : 9.735649 ns
```

### Comparison with Rust

Rust is (for this part) substantially faster. The serial version
of Rust took 2.4ms, which is nearly 20x faster than either the
serial or parallel version of Clojure. The parallel Rust version
was only 638 µs, which is nearly 70x faster than either of the
Clojure versions.

It's entirely possible, however, that we're not yet doing "enough"
for the parallelism to really pay off in Clojure. Certainly the
"fitness calculation" (counting the number of ones) is super cheap
here, and in a fairly unrealistic way. I should probably add a more
complex fitness calculation like HIFF to see how that affects the
timing results (if at all).

## Constructing a population with HIFF fitness function

When I changed things to use the HIFF fitness function (or some
approximation of that taken from memory) instead of
just `count_ones`, things slow down quite a bit.

The serial time is nearly 300 ms, so over six times slower than
when just using `count_ones`.

The parallel time is about 90 ms, or roughly twice what it was
with `count_ones`, and about 1/3 the time of the serial version
using HIFF.

Rust performance with parallel evaluation and HIFF is about 900 µs,
which is about _one hundred times faster_. Yowza! 

I'm was a _little_
concerned that maybe there's some kind of lazy evaluation thing
happening on the Rust side that I'm not taking into account? I went
bad and made some changes to ensure that everything was actually being
evaluated, and that had no effect on the Rust timings. So it's clear
that Rust is just a heck of a lot faster on this.

## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar clojure-ga-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2022 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
