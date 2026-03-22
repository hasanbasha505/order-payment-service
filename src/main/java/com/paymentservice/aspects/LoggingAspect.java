package com.paymentservice.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;

/**
 * Logging Aspect for cross-cutting logging concerns.
 *
 * Automatically logs:
 * - Method entry with arguments
 * - Method exit with return value
 * - Execution time
 * - Exceptions thrown
 *
 * Applied to:
 * - Service layer methods
 * - Controller methods (optional)
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Pointcut for all service layer methods.
     */
    @Pointcut("execution(* com.paymentservice.services.*.*(..))")
    public void serviceLayer() {
    }

    /**
     * Pointcut for all controller methods.
     */
    @Pointcut("execution(* com.paymentservice.controllers.*.*(..))")
    public void controllerLayer() {
    }

    /**
     * Pointcut for all repository methods.
     */
    @Pointcut("execution(* com.paymentservice.repositories.*.*(..))")
    public void repositoryLayer() {
    }

    /**
     * Logs service method execution with timing.
     */
    @Around("serviceLayer()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "SERVICE");
    }

    /**
     * Core logging logic for method execution.
     *
     * @param joinPoint The join point representing the method call
     * @param layer The layer name for logging context
     * @return The result of the method execution
     * @throws Throwable if the method throws an exception
     */
    private Object logMethodExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String fullMethodName = className + "." + methodName;

        // Log method entry (DEBUG level to avoid log spam)
        if (log.isDebugEnabled()) {
            log.debug("[{}] Entering: {} with args: {}",
                    layer,
                    fullMethodName,
                    sanitizeArgs(joinPoint.getArgs()));
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();

            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();

            // Log successful execution
            if (log.isDebugEnabled()) {
                log.debug("[{}] Completed: {} in {}ms",
                        layer,
                        fullMethodName,
                        executionTime);
            }

            // Log slow methods at WARN level (> 1000ms)
            if (executionTime > 1000) {
                log.warn("[{}] Slow method detected: {} took {}ms",
                        layer,
                        fullMethodName,
                        executionTime);
            }

            return result;

        } catch (Exception ex) {
            stopWatch.stop();

            // Log exception at ERROR level
            log.error("[{}] Exception in: {} after {}ms - {}: {}",
                    layer,
                    fullMethodName,
                    stopWatch.getTotalTimeMillis(),
                    ex.getClass().getSimpleName(),
                    ex.getMessage());

            throw ex;
        }
    }

    /**
     * Sanitizes method arguments for logging.
     * Masks sensitive data and truncates large objects.
     *
     * @param args The method arguments
     * @return A sanitized string representation
     */
    private String sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        return Arrays.stream(args)
                .map(this::sanitizeArg)
                .toList()
                .toString();
    }

    /**
     * Sanitizes a single argument for logging.
     */
    private String sanitizeArg(Object arg) {
        if (arg == null) {
            return "null";
        }

        String argStr = arg.toString();

        // Mask potential sensitive data
        if (argStr.toLowerCase().contains("password") ||
            argStr.toLowerCase().contains("token") ||
            argStr.toLowerCase().contains("secret")) {
            return "[REDACTED]";
        }

        // Truncate long strings
        if (argStr.length() > 200) {
            return argStr.substring(0, 200) + "...[truncated]";
        }

        return argStr;
    }
}
