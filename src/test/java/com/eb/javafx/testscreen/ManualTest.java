package com.eb.javafx.testscreen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks tests that should appear in the test screen with auto execution disabled by default. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ManualTest {
}
