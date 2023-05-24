package com.xxlgroup.enums;

public enum RepeatSubmitEnum {

    PARAM("PARAM", "param"),
    TOKEN("TOKEN", "token");

    /**
     * 值
     */
    private String key;
    /**
     * 标题
     */
    private String value;

    RepeatSubmitEnum(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
