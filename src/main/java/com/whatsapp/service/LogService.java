package com.whatsapp.service;

import com.whatsapp.model.Log;
import com.whatsapp.repository.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);
    private final LogRepository logRepository;
    private static LogService instance;

    private LogService() {
        this.logRepository = ServerRuntime.isServerProcess() ? new LogRepository() : null;
    }

    public static synchronized LogService getInstance() {
        if (instance == null) {
            instance = new LogService();
        }
        return instance;
    }

    public String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    public void log(Log.NivelLog nivel, String mensaje, String modulo, String traceId, Long userId) {
        Log log = new Log(nivel, mensaje, modulo, traceId, userId);
        if (logRepository != null) {
        logRepository.save(log);
        }
        
        // También loggear a slf4j
        switch (nivel) {
            case DEBUG -> logger.debug("[{}] {}: {}", modulo, traceId, mensaje);
            case INFO -> logger.info("[{}] {}: {}", modulo, traceId, mensaje);
            case WARN -> logger.warn("[{}] {}: {}", modulo, traceId, mensaje);
            case ERROR -> logger.error("[{}] {}: {}", modulo, traceId, mensaje);
        }
    }

    public void logInfo(String mensaje, String modulo, String traceId, Long userId) {
        log(Log.NivelLog.INFO, mensaje, modulo, traceId, userId);
    }

    public void logError(String mensaje, String modulo, String traceId, Long userId) {
        log(Log.NivelLog.ERROR, mensaje, modulo, traceId, userId);
    }

    public void logWarning(String mensaje, String modulo, String traceId, Long userId) {
        log(Log.NivelLog.WARN, mensaje, modulo, traceId, userId);
    }

    public List<Log> getLogsByTraceId(String traceId) {
        return logRepository == null ? java.util.Collections.emptyList() : logRepository.findByTraceId(traceId);
    }

    public List<Log> getAllLogs() {
        return logRepository == null ? java.util.Collections.emptyList() : logRepository.findAll();
    }
}

