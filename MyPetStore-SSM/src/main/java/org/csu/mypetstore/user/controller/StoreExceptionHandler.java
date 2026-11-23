package org.csu.mypetstore.user.controller;

import org.csu.mypetstore.user.service.StoreException;import org.springframework.http.HttpStatus;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class StoreExceptionHandler {
    @ExceptionHandler(StoreException.class)ModelAndView business(StoreException e){ModelAndView v=new ModelAndView("error/400");v.setStatus(HttpStatus.BAD_REQUEST);v.addObject("message",e.getMessage());return v;}
}
