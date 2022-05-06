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