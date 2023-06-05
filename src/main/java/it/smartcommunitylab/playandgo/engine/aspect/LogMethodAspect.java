package it.smartcommunitylab.playandgo.engine.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LogMethodAspect {
    private final Logger logger = LoggerFactory.getLogger(LogMethodAspect.class);
    
    @Pointcut("execution(public * *(..))")
    public void publicMethod() {}
    
    @Pointcut("within(@org.springframework.stereotype.Component *)")
    public void withinService() {}
    
    @Pointcut("within(it.smartcommunitylab.playandgo.engine..*)")
    public void applicationPackagePointcut() {}
    
    @Around("publicMethod() && withinService() && applicationPackagePointcut()")
    public Object profileServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        // do some logging before method execution
        logger.trace(String.format("call %s.%s()", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName()));
        Object retVal = joinPoint.proceed();
        // and some logging after method execution
        return retVal;
    }
}
