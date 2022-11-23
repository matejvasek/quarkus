package io.quarkus.funqy.test;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@ExtendWith(UseGreetBinaryExtension.class)
public class GreetBinaryTest extends GreetTestBase {
}
