package org.mongodb.morphia.release

import spock.lang.Specification

class UpdateToNextVersionTaskSpecification extends Specification {
    def 'should parse an RC string to correctly increment the version number'() {
        when:
        def newVersion = UpdateToNextVersionTask.incrementToNextVersion('1.0.0-rc0')
        
        then:
        newVersion == '1.0.0-rc1-SNAPSHOT'
    }

    def 'should parse a string to correctly increment the version number'() {
        when:
        def newVersion = UpdateToNextVersionTask.incrementToNextVersion('0.109')
        
        then:
        newVersion == '0.110-SNAPSHOT'
    }

    def 'should parse a string to correctly increment the version number over a boundary'() {
        when:
        def newVersion = UpdateToNextVersionTask.incrementToNextVersion('0.110')
        
        then:
        newVersion == '0.111-SNAPSHOT'
    }

}
