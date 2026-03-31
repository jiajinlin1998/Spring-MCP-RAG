package com.jjl.mcpclient;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
@Slf4j
public class ServiceLogAspect {

    /**
     * 记录方法执行时间
     * @param joinPoint
     * @return *任意返回参数格式
     *          com.jjl.mcpclient.service 指定的包名
     *          第一个 * 包下所有类
     *          第二个 * 所有方法
     *          (..) 任意参数
     * @throws Throwable
     */
    @Around("execution(* com.jjl.mcpclient.service.*.*(..))")
    public Object recordTimeLog(ProceedingJoinPoint joinPoint) throws Throwable {
        //long startTime = System.currentTimeMillis();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();


        Object result = joinPoint.proceed();

        String point = joinPoint.getTarget().getClass().getName()
                + "." + joinPoint.getSignature().getName();

       // long endTime = System.currentTimeMillis();
        stopWatch.stop();
        //long TokenTime = endTime - startTime;
        long TokenTime = stopWatch.getTotalTimeMillis();
        if(TokenTime > 3000){
            log.warn("方法名:{}, 耗时偏长:{}ms", point, TokenTime);
        }else if(TokenTime > 2000){
            log.warn("方法名:{}, 耗时中等:{}ms", point, TokenTime);
        }else {
            log.info("方法名:{}, 耗时:{}ms", point, TokenTime);
        }

        return result;
    }
}
