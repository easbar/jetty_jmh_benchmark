# Jetty JMH Benchmark

```shell
mvn clean package
# run full benchmark
java -jar target/jetty-jmh-benchmark.jar
# run faster benchmark (probably less precise but quicker results)
java -jar target/jetty-jmh-benchmark.jar -f 2 -i 3 -wi 3 -r 3 -w 2
# use different array size(s)
java -jar target/jetty-jmh-benchmark.jar -p size=1000000,2000000
```

# What this benchmark does

Basically we start a jetty server instance with two servlets. One servlet returns a constant number and the other does
some work (it sums up all integers of an array). We then run queries against the two servlets and measure the response
time. We also run a benchmark that measures the time it takes to run the work method (taking the array sum)
in isolation.

measureBaseline - baseline that does nothing at all measureSumArray - sums random integers that were written to an int[]
array, the array length is given by the size parameter measureBaselineHttp - runs queries against a servlet that returns
a constant number measureSumArrayHttp - runs queries against a servlet that does the same work as measureSumArray

# Results

On some machines everything seems to make sense:

```
Benchmark                             (size)  Mode  Cnt   Score     Error  Units
JettyBenchmark.measureBaseline      10000000  avgt   25  ≈ 10⁻⁶            ms/op
JettyBenchmark.measureBaseline      20000000  avgt   25  ≈ 10⁻⁶            ms/op
JettyBenchmark.measureBaseline      40000000  avgt   25  ≈ 10⁻⁶            ms/op
JettyBenchmark.measureBaselineHttp  10000000  avgt   25   0.052 ±   0.001  ms/op
JettyBenchmark.measureBaselineHttp  20000000  avgt   25   0.052 ±   0.001  ms/op
JettyBenchmark.measureBaselineHttp  40000000  avgt   25   0.051 ±   0.001  ms/op
JettyBenchmark.measureSumArray      10000000  avgt   25   9.015 ±   0.012  ms/op
JettyBenchmark.measureSumArray      20000000  avgt   25  18.099 ±   0.027  ms/op
JettyBenchmark.measureSumArray      40000000  avgt   25  36.262 ±   0.059  ms/op
JettyBenchmark.measureSumArrayHttp  10000000  avgt   25  10.401 ±   0.120  ms/op
JettyBenchmark.measureSumArrayHttp  20000000  avgt   25  18.701 ±   0.108  ms/op
JettyBenchmark.measureSumArrayHttp  40000000  avgt   25  36.843 ±   0.047  ms/op
```

measureBaseline takes almost no time at all measureBaselineHttp is quite fast (less than 0.1ms server response time)
measureSumArray is proportional to the array size measureSumArrayHttp does not take much longer than measureSumArray,
which can be expected because the server overhead is small according to measureBaselineHttp

... However, on some other machines things aren't as clear, like this one:

```
Benchmark                             (size)  Mode  Cnt   Score    Error  Units
JettyBenchmark.measureBaseline      10000000  avgt   25  ≈ 10⁻⁶           ms/op
JettyBenchmark.measureBaseline      20000000  avgt   25  ≈ 10⁻⁶           ms/op
JettyBenchmark.measureBaseline      40000000  avgt   25  ≈ 10⁻⁶           ms/op
JettyBenchmark.measureBaselineHttp  10000000  avgt   25   0.064 ±  0.006  ms/op
JettyBenchmark.measureBaselineHttp  20000000  avgt   25   0.067 ±  0.006  ms/op
JettyBenchmark.measureBaselineHttp  40000000  avgt   25   0.069 ±  0.004  ms/op
JettyBenchmark.measureSumArray      10000000  avgt   25   6.460 ±  0.086  ms/op
JettyBenchmark.measureSumArray      20000000  avgt   25  12.975 ±  0.072  ms/op
JettyBenchmark.measureSumArray      40000000  avgt   25  25.975 ±  0.166  ms/op
JettyBenchmark.measureSumArrayHttp  10000000  avgt   25  11.287 ±  0.319  ms/op
JettyBenchmark.measureSumArrayHttp  20000000  avgt   25  18.906 ±  0.191  ms/op
JettyBenchmark.measureSumArrayHttp  40000000  avgt   25  31.976 ±  0.046  ms/op
```

measureBaseline, measureBaselineHttp and measureSumArray all seem fine, but measureSumArrayHttp seems to add around 6ms
additional query time! Where is this coming from?

On some other machines it is even a lot worse, like this one:

```
Benchmark                             (size)  Mode  Cnt   Score    Error  Units
JettyBenchmark.measureBaseline      10000000  avgt   25  ? 10??           ms/op
JettyBenchmark.measureBaseline      20000000  avgt   25  ? 10??           ms/op
JettyBenchmark.measureBaseline      40000000  avgt   25  ? 10??           ms/op
JettyBenchmark.measureBaselineHttp  10000000  avgt   25   0.044 ?  0.003  ms/op
JettyBenchmark.measureBaselineHttp  20000000  avgt   25   0.043 ?  0.002  ms/op
JettyBenchmark.measureBaselineHttp  40000000  avgt   25   0.045 ?  0.003  ms/op
JettyBenchmark.measureSumArray      10000000  avgt   25   6.442 ?  0.114  ms/op
JettyBenchmark.measureSumArray      20000000  avgt   25  12.948 ?  0.165  ms/op
JettyBenchmark.measureSumArray      40000000  avgt   25  25.829 ?  0.419  ms/op
JettyBenchmark.measureSumArrayHttp  10000000  avgt   25  13.675 ?  0.521  ms/op
JettyBenchmark.measureSumArrayHttp  20000000  avgt   25  27.514 ?  0.176  ms/op
JettyBenchmark.measureSumArrayHttp  40000000  avgt   25  54.949 ?  0.247  ms/op
```

measureSumArrayHttp is not just slower by a few ms, but the difference even depends on the array length:
Depending on the array size it takes around 7ms/15ms/29ms longer than measureSumArray, so the additional response time
seems to increase proportionally to the array size! What is going on here?
