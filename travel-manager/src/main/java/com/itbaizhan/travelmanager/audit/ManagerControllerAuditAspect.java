package com.itbaizhan.travelmanager.audit;

import com.itbaizhan.travelcommon.pojo.ManagerOperationLog;
import com.itbaizhan.travelmanager.mapper.ManagerOperationLogMapper;
import com.itbaizhan.travelmanager.security.ManagerSecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 记录 travel-manager 所有 Controller 调用（不写 body，避免密码与上传文件入库）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ManagerControllerAuditAspect {

    private static final int MAX_QUERY_LEN = 1000;
    private static final int MAX_ERR_LEN = 1000;

    private final ManagerOperationLogMapper managerOperationLogMapper;

    @Around("execution(public * com.itbaizhan.travelmanager.controller..*.*(..))")
    public Object aroundController(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest req = attrs != null ? attrs.getRequest() : null;

        String module = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();
        String httpMethod = req != null ? req.getMethod() : null;
        String uri = req != null ? req.getRequestURI() : "";
        String query = req != null ? req.getQueryString() : null;
        if (query != null && query.length() > MAX_QUERY_LEN) {
            query = query.substring(0, MAX_QUERY_LEN);
        }
        String clientIp = req != null ? resolveClientIp(req) : null;
        String action = (httpMethod != null ? httpMethod + " " : "") + uri + " #" + methodName;

        Long managerId = ManagerSecurityContext.currentManagerId();
        String managerName = ManagerSecurityContext.currentManagerName();

        try {
            Object result = pjp.proceed();
            saveLog(module, action, httpMethod, uri, query, clientIp, managerId, managerName,
                    true, null, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            String err = ex.getMessage();
            if (err != null && err.length() > MAX_ERR_LEN) {
                err = err.substring(0, MAX_ERR_LEN);
            }
            saveLog(module, action, httpMethod, uri, query, clientIp, managerId, managerName,
                    false, err, System.currentTimeMillis() - start);
            throw ex;
        }
    }

    private void saveLog(String module, String action, String httpMethod, String requestUri, String queryString,
                         String clientIp, Long managerId, String managerName,
                         boolean success, String errorMessage, long durationMs) {
        try {
            ManagerOperationLog row = new ManagerOperationLog();
            row.setManagerId(managerId);
            row.setManagerName(managerName);
            row.setModule(module);
            row.setAction(truncate(action, 128));
            row.setHttpMethod(truncate(httpMethod, 16));
            row.setRequestUri(truncate(requestUri, 512));
            row.setQueryString(truncate(queryString, 1024));
            row.setSuccess(success);
            row.setDurationMs(durationMs);
            row.setErrorMessage(truncate(errorMessage, 1024));
            row.setClientIp(truncate(clientIp, 64));
            row.setCreatedAt(LocalDateTime.now());
            managerOperationLogMapper.insert(row);
        } catch (Exception e) {
            log.warn("manager_operation_log insert failed: {}", e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String resolveClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
