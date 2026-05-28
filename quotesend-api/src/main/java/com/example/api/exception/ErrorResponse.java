package com.example.api.exception;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.io.Serializable;
@NoArgsConstructor @AllArgsConstructor
@Getter @Setter @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse implements Serializable {
    private int    code;
    private int    status;
    private String message;
    private String error;
    private String path;
}
