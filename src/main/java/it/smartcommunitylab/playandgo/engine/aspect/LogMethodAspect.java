package it.smartcommunitylab.playandgo.engine.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class LogMethodAspect {
    private final Logger logger = LoggerFactory.getLogger(LogMethodAspect.class);
    
    
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void withinService() {}
}
