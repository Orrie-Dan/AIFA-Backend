package com.aifa.modules.importing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SmsMessageSplitterTest {

    @Test
    void splitsOnBlankLines() {
        String input = "Message one\n\nMessage two";
        assertThat(SmsMessageSplitter.split(input)).containsExactly("Message one", "Message two");
    }

    @Test
    void splitsOnKnownMtnLineStartsWithoutBlankLines() {
        String input =
                "You have received 1000 RWF from Alice\nYou have received 2000 RWF from Bob\n*165*S*500 RWF transferred to Pat";
        assertThat(SmsMessageSplitter.split(input)).hasSize(3);
    }

    @Test
    void keepsContinuationLinesTogether() {
        String input = "You have received RWF 5000 from JOHN\n(0781234567). Balance: 5000";
        assertThat(SmsMessageSplitter.split(input)).containsExactly(
                "You have received RWF 5000 from JOHN (0781234567). Balance: 5000");
    }
}
