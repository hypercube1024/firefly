package com.fireflysource.common.sys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestProjectVersion {

    @Test
    @DisplayName("should generate project logo")
    void testLogo() {
        String logo = ProjectVersion.logo();
        System.out.println(logo);
        assertNotNull(logo);
    }
}
