package net.ivoireautoservice.ias_manager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MaxMediaExceededException extends RuntimeException {

    public MaxMediaExceededException(String message) {
        super(message);
    }
}
