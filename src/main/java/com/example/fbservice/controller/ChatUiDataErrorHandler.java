package com.example.fbservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Safe, scoped fallback for ChatWebController database failures. No exception
 * details, message bodies, contact names or connection strings leave the server.
 * Unrelated controllers retain their own failure handling.
 */
@ControllerAdvice(assignableTypes = ChatWebController.class)
public class ChatUiDataErrorHandler {
    @ExceptionHandler(DataAccessException.class)
    public ModelAndView onUnavailable(DataAccessException ignored,
                                      HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return new ModelAndView("chat_error");
    }
}
