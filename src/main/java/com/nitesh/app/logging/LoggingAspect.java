package com.nitesh.app.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Aspect for logging execution of service and repository Spring components. */
@Aspect
@Component
public class LoggingAspect {

  private final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

  //TODO
  //*****--  Update the package name here based on your project's package
  private final String arg = "within(com.nitesh.app..*)";
  //*****--

  private final String correlationId = UUID.randomUUID().toString();

  private BufferedWriter bufferWriter;

  @Value("${spring.application.name}")
  private String applicationName;

  @Value("${logging.aspectLogging.logFileEnabled:false}")
  private boolean logFileEnabled;

  @PostConstruct
  public void setupLogFile() throws IOException {
    if (logFileEnabled) {
      String outputFileName =
          "AnalysisLogs_"
              + applicationName
              + "_"
              + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSS"))
              + ".log";
      Path output_path = Paths.get(outputFileName);
      bufferWriter = Files.newBufferedWriter(output_path);
    }
  }
  /** Pointcut that matches all repositories, services and Web REST endpoints. */
  @Pointcut(
      "within(@org.springframework.stereotype.Component *)"
          + " || within(@org.springframework.web.bind.annotation.RestController *) "
          + "|| within(@org.springframework.stereotype.Service *)")
  public void springBeanPointcut() {
    // Method is empty as this is just a Pointcut, the implementations are in the advices.
  }

  /** Pointcut that matches all Spring beans in the application's main packages. */
  @Pointcut(arg)
  public void applicationPackagePointcut() {
    // Method is empty as this is just a Pointcut, the implementations are in the advices.
  }

  /**
   * Advice that logs methods throwing exceptions.
   *
   * @param joinPoint join point for advice
   * @param e exception
   */
  @AfterThrowing(pointcut = "applicationPackagePointcut() && springBeanPointcut()", throwing = "e")
  public void logAfterThrowing(JoinPoint joinPoint, Throwable e) throws IOException {
    log.error(
        "Exception in {}.{}() with cause = {}",
        joinPoint.getSignature().getDeclaringTypeName(),
        joinPoint.getSignature().getName(),
        e.getCause() != null ? e.getCause() : "NULL");
  }

  /**
   * Advice that logs when a method is entered and exited.
   *
   * @param joinPoint join point for advice
   * @return result
   * @throws Throwable throws IllegalArgumentException
   */
  @Around("applicationPackagePointcut() && springBeanPointcut()")
  public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSS");
    log.info(
        "{},{}:{}.{}(),START,{},",
        correlationId,
        applicationName,
        joinPoint.getSignature().getDeclaringTypeName(),
        joinPoint.getSignature().getName(),
        dtf.format(LocalDateTime.now()));
    // ******--Write to Log file
    if (logFileEnabled) {
      writeLogsToFile(
          joinPoint.getSignature().getDeclaringTypeName(),
          joinPoint.getSignature().getName(),
          "START",
          dtf.format(LocalDateTime.now()));
      // ******--
    }
    try {
      Object result = joinPoint.proceed();

      log.info(
          "{},{}:{}.{}(),FINISH,{},",
          correlationId,
          applicationName,
          joinPoint.getSignature().getDeclaringTypeName(),
          joinPoint.getSignature().getName(),
          dtf.format(LocalDateTime.now()));
      // ******--Write to Log file
      if (logFileEnabled) {
        writeLogsToFile(
            joinPoint.getSignature().getDeclaringTypeName(),
            joinPoint.getSignature().getName(),
            "FINISH",
            dtf.format(LocalDateTime.now()));
        // ******--
      }
      return result;
    } catch (IllegalArgumentException e) {
      log.error(
          "{} | Illegal argument: {} in {}.{}()",
          correlationId,
          Arrays.toString(joinPoint.getArgs()),
          joinPoint.getSignature().getDeclaringTypeName(),
          joinPoint.getSignature().getName());
      throw e;
    }
  }

  private void writeLogsToFile(String declaringTypeNm, String name, String startFinish, String timeNow)
      throws IOException {
    bufferWriter.write(
        correlationId
            + ","
            + applicationName
            + ":"
            + declaringTypeNm
            + "."
            + name
            + "(),"
            + startFinish
            + timeNow
            + ",");
    bufferWriter.newLine();
    bufferWriter.flush();
  }
}
