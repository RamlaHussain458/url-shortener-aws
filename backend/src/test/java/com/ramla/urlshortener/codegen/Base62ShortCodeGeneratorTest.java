package com.ramla.urlshortener.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Base62ShortCodeGeneratorTest {

    private final Base62ShortCodeGenerator generator = new Base62ShortCodeGenerator(new SecureRandom(), 7);

    @Test
    void generatesCodesOfConfiguredLengthUsingOnlyBase62Characters() {
        for (int i = 0; i < 500; i++) {
            assertThat(generator.generate()).matches("[0-9A-Za-z]{7}");
        }
    }

    @Test
    void generatesDistinctCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            codes.add(generator.generate());
        }
        assertThat(codes).hasSize(2000);
    }

    @Test
    void rejectsInvalidLength() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(new SecureRandom(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
