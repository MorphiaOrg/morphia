package org.mongodb.morphia.release

import spock.lang.Specification

class ReleasePluginExtensionSpecification extends Specification {
    def 'should convert project version with snapshot suffix into correct release and snapshot versions'() {
        given:
        def version = '0.110-SNAPSHOT'
        def releasePluginExtension = new ReleasePluginExtension()
        
        when:
        releasePluginExtension.releaseVersion = version
        
        then:
        releasePluginExtension.releaseVersion == '0.110'
        releasePluginExtension.snapshotVersion == '0.110-SNAPSHOT'
    }

    def 'should convert project version without snapshot suffix into correct release and snapshot versions'() {
        given:
        def version = '0.110'
        def releasePluginExtension = new ReleasePluginExtension()
        
        when:
        releasePluginExtension.releaseVersion = version
        
        then:
        releasePluginExtension.releaseVersion == '0.110'
        releasePluginExtension.snapshotVersion == '0.110-SNAPSHOT'
    }

}
