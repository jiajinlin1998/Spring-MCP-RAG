package com.jjl.mcpclient;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

@SpringBootTest
class McpClientApplicationTests {

    @Test
    void testTime() throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("任务1");
        Thread.sleep(1000);
        stopWatch.stop();
        stopWatch.start("任务2");
        Thread.sleep(2000);
        stopWatch.stop();
        stopWatch.start("任务3");
        Thread.sleep(3000);
        stopWatch.stop();

        System.out.println(stopWatch.prettyPrint());
        System.out.println(stopWatch.shortSummary());
        System.out.println(stopWatch.getTotalTimeMillis());
        System.out.println(stopWatch.getTaskCount());
    }

}
