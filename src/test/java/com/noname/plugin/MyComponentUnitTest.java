package com.noname.plugin;

import com.noname.plugin.api.MyPluginComponent;
import com.noname.plugin.impl.MyPluginComponentImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MyComponentUnitTest {

    @Test
    void testMyName() {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("myComponent", component.getName(), "names do not match!");
    }
}
