package com.ecarxgroup.aop;

import com.alibaba.fastjson.JSON;
import com.ecarxgroup.annotation.RepeatSubmit;
import com.ecarxgroup.common.larkgroupmessage.exception.ResultResponseException;
import com.ecarxgroup.gateway.common.configuration.UserInfoThreadHolder;
import com.ecarxgroup.gateway.common.model.AccountInfo;
import com.ecarxgroup.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class RepeatSubmitAspect {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 定义 @Pointcut注解表达式, 通过特定的规则来筛选连接点, 就是Pointcut，选中那几个你想要的方法
     * 在程序中主要体现为书写切入点表达式（通过通配、正则表达式）过滤出特定的一组 JointPoint连接点
     * 方式一：@annotation：当执行的方法上拥有指定的注解时生效
     * 方式二：execution：一般用于指定方法的执行
     */
    @Pointcut("@annotation(repeatSubmit)")
    public void pointCutNoRepeatSubmit(RepeatSubmit repeatSubmit) {

    }

    /**
     * 环绕通知, 围绕着方法执行
     * @param joinPoint
     * @param repeatSubmit
     * @return
     * @throws Throwable
     * @Around 可以用来在调用一个具体方法前和调用后来完成一些具体的任务。
     * <p>
     * 方式一：单用 @Around("execution(* net.wnn.controller.*.*(..))")可以
     * 方式二：用@Pointcut和@Around联合注解也可以
     * <p>
     * <p>
     * 两种方式
     * 方式一：加锁 固定时间内不能重复提交
     * <p>
     * 方式二：先请求获取token，这边再删除token,删除成功则是第一次提交
     */
    @Around("pointCutNoRepeatSubmit(repeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        AccountInfo accountInfo =UserInfoThreadHolder.getCurrentUser();
        if(null==accountInfo){
            throw new ResultResponseException("1100003","用户未登录");
        }
        String loginName = accountInfo.getLoginName();
        if(StringUtils.isEmpty(loginName)){
            throw new ResultResponseException("1100003","用户未登录");
        }
        //用于记录成功或者失败
        boolean res;
        //防重提交类型
        String type = repeatSubmit.limitType().name();
        long lockTime = repeatSubmit.lockTime();
        RLock lock =null;
        if (type.equalsIgnoreCase(RepeatSubmit.Type.PARAM.name())) {
            //方式一，参数形式防重提交
            String ipAddr = CommonUtil.getIp(request);
            MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            String className = method.getDeclaringClass().getName();
            String requestBody = getRequestBody(joinPoint.getArgs());
            String key = "order-server:repeat_submit:"+ DigestUtils.md5Hex((String.format("%s-%s-%s-%s-%s",ipAddr,loginName,className,method,requestBody)));
            //加锁
            // 这种也可以 redisson的使用
             //res  = redisTemplate.opsForValue().setIfAbsent(key, "1", lockTime, TimeUnit.SECONDS);
            lock = redissonClient.getLock(key);
            // 尝试加锁，最多等待0秒，上锁以后5秒自动解锁 [lockTime默认为5s, 可以自定义]
            res = lock.tryLock(0,lockTime,TimeUnit.SECONDS);
        } else {
            //方式二，令牌形式防重提交
            String requestToken = request.getHeader("repeat_submit_request_token");
            if (StringUtils.isBlank(requestToken)) {
                throw new ResultResponseException("1100002","请求令牌为空");
            }
            /**
             * 提交表单的token key
             * 方式一：不用lua脚本获取再判断，之前是因为 key组成是 order:submit:accountNo, value是对应的token，所以需要先获取值，再判断
             * 方式二：可以直接key是 order:submit:accountNo:token,然后直接删除成功则完成
             */
            lock = redissonClient.getLock(requestToken);
            // 尝试加锁，最多等待0秒，上锁以后5秒自动解锁 [lockTime默认为5s, 可以自定义]
            res = lock.tryLock(0,lockTime,TimeUnit.SECONDS);
        }
        if (!res) {
            throw new ResultResponseException("1100001","请勿重复提交");
            //return null;
        }
        try {
            return joinPoint.proceed();
        }catch(Exception e){
            //业务处理失败，则允许重复提交
            lock.unlock();
            throw new ResultResponseException("1100005","服务异常");
        }

    }

    /**
     * 获取请求body参数
     * @param args
     * @return
     */
    private String getRequestBody(Object[] args){
        if (Objects.isNull(args)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest
                    || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile
                    || arg instanceof BindResult
                    || arg instanceof MultipartFile[]
                    || arg instanceof ModelMap
                    || arg instanceof Model
                    || arg instanceof ExtendedServletRequestDataBinder
                    || arg instanceof byte[]) {
                continue;
            }
            sb.append(JSON.toJSONString(arg));
        }
        return sb.toString();
    }


}
