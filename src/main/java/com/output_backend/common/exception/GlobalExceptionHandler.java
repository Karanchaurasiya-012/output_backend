package com.output_backend.common.exception;

import com.output_backend.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
		if (ex.getStatus().is5xxServerError()) {
			log.warn("Business failure status={} message={}", ex.getStatus().value(), ex.getMessage());
		}
		return ResponseEntity.status(ex.getStatus()).body(ApiResponse.fail(ex.getMessage()));
	}
}
