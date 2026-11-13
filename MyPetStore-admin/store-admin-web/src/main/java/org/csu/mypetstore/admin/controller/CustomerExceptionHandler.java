package org.csu.mypetstore.admin.controller;

import org.csu.mypetstore.service.*;
import org.springframework.http.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(assignableTypes = CustomerController.class)
public class CustomerExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalid(IllegalArgumentException exception, Model model) {
        model.addAttribute("message", exception.getMessage()); return "error/400";
    }
    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String missing(CustomerNotFoundException exception, Model model) {
        model.addAttribute("message", exception.getMessage()); return "error/404";
    }
    @ExceptionHandler(CustomerForbiddenException.class)
    public ResponseEntity<String> forbidden(CustomerForbiddenException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }
    @ExceptionHandler(CustomerStateException.class)
    public ResponseEntity<String> conflict(CustomerStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }
}
