+++
title = "JRebel"
[menu.main]
  parent = "Reference Guides"
  pre = "<i class='fa fa-file-text-o'></i>"
+++

This is a simple extension to Morphia to allow classes to be re-mapped once they are reloaded by JRebel.

## Prerequisites

Of course you need to run JRebel (http://zeroturnaround.com) for this extension to be useful.
On top of that, you need to tell JRebel about the Plugin. In order to do that, add the following two new Elements to the VM startup command:

```
-Drebel.morphia=true -Drebel.plugins=/path/to/morphia-jrebel-plug-1.0-SNAPSHOT.jar
```

If you´re using Eclipse with the JRebel Plugin this can be skipped and instead configured in the Agent Settings GUI.
- Choose Window->Prefs->JRebel
- Click Agent Settings
- Choose Plugins Tab
- Click custom plugins
- add the plugin jar, OK
- Choose custom, add key "rebel.morphia" with value of "true"

## Using
Register the Extension to your Morphia instance

```java
new JRebelExtension(morphia);
```

## Dependencies

### Maven
If you use Maven to manage your project, you can reference the Extension as a dependency:
  
```xml
<dependency>
    <groupId>xyz.morphia</groupId>
    <artifactId>morphia-jrebel-plug</artifactId>
    <version>1.5.0-SNAPSHOT</version>
</dependency>
```
