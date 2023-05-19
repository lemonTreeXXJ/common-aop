package com.xxlgroup.aop;

import com.xxlgroup.annotation.Retry;
import com.xxlgroup.template.RetryTemplate;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * 重试机制
 */
@Aspect
@Component
@Slf4j
public class RetryAspect {

    @Value("retry.corePoolSize:3")
    private int corePoolSize;
    @Value("retry.maximumPoolSize:5")
    private int maximumPoolSize;

    ExecutorService executorService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
            1, TimeUnit.MINUTES,
            new LinkedBlockingQueue<Runnable>());


    @Around(value = "@annotation(retry)")
    public Object execute(ProceedingJoinPoint joinPoint, Retry retry) throws Exception {
        RetryTemplate retryTemplate = new RetryTemplate() {
            @Override
            protected Object doBiz() throws Throwable {
                return joinPoint.proceed();
            }
        };

        retryTemplate.setRetryCount(retry.count())
                .setSleepTime(retry.sleep());


        if (retry.async()) {
            return retryTemplate.submit(executorService);
        } else {
            return retryTemplate.execute();
        }
    }


}
