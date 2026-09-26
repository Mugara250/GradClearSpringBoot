package auca.ac.rw.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApiResponse <T>{
    private final boolean success = true;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp = LocalDateTime.now();

    private ApiResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(message, data);
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>("Success", data);
    }
}
