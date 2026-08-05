package com.guzheng.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数：{}", e.getParameterName());
        return error(400, "缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);
        String message = fieldError == null ? "请求参数不合法" : fieldError.getDefaultMessage();
        log.warn("请求参数校验失败：{}", message);
        return error(400, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("请求参数类型错误：{}", e.getName());
        return error(400, "请求参数类型错误：" + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException e) {
        log.warn("请求体无法解析：{}", e.getMessage());
        return error(400, "请求体格式不正确");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误：{}", e.getMessage());
        return error(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return error(500, "系统繁忙，请稍后重试");
    }

    private ResponseEntity<ApiResponse<Void>> error(int code, String message) {
        return ResponseEntity.status(code).body(ApiResponse.error(code, message));
    }
}
