package io.quarkiverse.dapr.demo;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * TestData
 *
 * @author naah69
 * @date 2022/4/25 11:22 AM
 */
@RegisterForReflection //add it on model class if in native mode
public class TestData {

    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
