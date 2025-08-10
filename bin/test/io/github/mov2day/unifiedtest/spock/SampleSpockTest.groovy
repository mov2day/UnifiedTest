package io.github.mov2day.unifiedtest.spock

import spock.lang.Specification

class SampleSpockTest extends Specification {
    def "a simple passing test"() {
        expect:
        1 == 1
    }

    def "a simple passing test 2"() {
        expect:
        1 == 1
    }
}
