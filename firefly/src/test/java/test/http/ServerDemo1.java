package test.http;

import com.firefly.server.http2.servlet.ServerBootstrap;

public class ServerDemo1 {

	/**
	 * the JVM arguments: -XX:+UseParallelGC -XX:+UseParallelOldGC -Xmx1024m -Xms1024m
	 * performance test: ab -k -n1000000 -c10  http://localhost:6656/index_1
	 * the result: 
	 * 
		Server Software:        Firefly
		Server Hostname:        localhost
		Server Port:            6656
		
		Document Path:          /index_1
		Document Length:        2480 bytes
		
		Concurrency Level:      10
		Time taken for tests:   23.440 seconds
		Complete requests:      1000000
		Failed requests:        0
		Keep-Alive requests:    1000000
		Total transferred:      2633000000 bytes
		HTML transferred:       2480000000 bytes
		Requests per second:    42661.84 [#/sec] (mean)
		Time per request:       0.234 [ms] (mean)
		Time per request:       0.023 [ms] (mean, across all concurrent requests)
		Transfer rate:          109695.93 [Kbytes/sec] received
		
		Connection Times (ms)
		              min  mean[+/-sd] median   max
		Connect:        0    0   0.0      0       1
		Processing:     0    0   0.1      0       7
		Waiting:        0    0   0.1      0       7
		Total:          0    0   0.1      0       8
		
		Percentage of the requests served within a certain time (ms)
		  50%      0
		  66%      0
		  75%      0
		  80%      0
		  90%      0
		  95%      0
		  98%      0
		  99%      0
		 100%      8 (longest request)
	 * 
	 * 
	 * the JVM arguments: -XX:+UseG1GC -Xmx1024m -Xms1024m
	 * performance test: ab -k -n1000000 -c10  http://localhost:6656/index_1
	 * the result:
	 *  
		Server Software:        Firefly
		Server Hostname:        localhost
		Server Port:            6656
		
		Document Path:          /index_1
		Document Length:        2480 bytes
		
		Concurrency Level:      10
		Time taken for tests:   23.683 seconds
		Complete requests:      1000000
		Failed requests:        0
		Keep-Alive requests:    1000000
		Total transferred:      2633000000 bytes
		HTML transferred:       2480000000 bytes
		Requests per second:    42225.22 [#/sec] (mean)
		Time per request:       0.237 [ms] (mean)
		Time per request:       0.024 [ms] (mean, across all concurrent requests)
		Transfer rate:          108573.25 [Kbytes/sec] received
		
		Connection Times (ms)
		              min  mean[+/-sd] median   max
		Connect:        0    0   0.0      0       0
		Processing:     0    0   0.1      0      10
		Waiting:        0    0   0.1      0      10
		Total:          0    0   0.1      0      10

		Percentage of the requests served within a certain time (ms)
		  50%      0
		  66%      0
		  75%      0
		  80%      0
		  90%      0
		  95%      0
		  98%      0
		  99%      0
		 100%     10 (longest request)
	 */
	public static void main(String[] args) throws Throwable {
		ServerBootstrap bootstrap = new ServerBootstrap("firefly-server1.xml", "localhost", 6656);
		bootstrap.start();
//		Thread.sleep(2000L);
//		bootstrap.stop();
	}

}
