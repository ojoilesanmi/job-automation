package com.jobagent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {
    private String responseCode;
    private String responseStatus;
    private String responseMessage;
    private T data;

    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.<T>builder()
                .responseCode("00")
                .responseStatus("success")
                .responseMessage("Operation successful")
                .data(data)
                .build();
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return BaseResponse.<T>builder()
                .responseCode("00")
                .responseStatus("success")
                .responseMessage(message)
                .data(data)
                .build();
    }

    public static <T> BaseResponse<T> error(String code, String message) {
        return BaseResponse.<T>builder()
                .responseCode(code)
                .responseStatus("error")
                .responseMessage(message)
                .build();
    }

    public static <T> BaseResponse<T> error(String code, String message, T data) {
        return BaseResponse.<T>builder()
                .responseCode(code)
                .responseStatus("error")
                .responseMessage(message)
                .data(data)
                .build();
    }
}
