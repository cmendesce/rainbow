/*
 * The MIT License
 *
 * Copyright 2014 CMU MSIT-SE Rainbow Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package edu.cmu.rainbow_ui.display.config;

import edu.cmu.rainbow_ui.display.widgets.IWidget;

import java.util.HashMap;
import java.util.Map;

/**
 * Widget configuration.
 *
 * @author Denis Anisimov <dbanisimov@cmu.edu>
 */
public class WidgetConfiguration {

    /**
     * Id of the widget
     */
    public String id = "";
    /**
     * Mapping of the widget to a property
     */
    public String mapping = "";
    /**
     * Properties map
     */
    public Map<String, Object> properties = new HashMap<>();

    /**
     * Default constructor
     */
    public WidgetConfiguration() {
    }

    /**
     * Construct a configuration for an existing widget
     *
     * @param widget widget to make configuration for
     */
    public WidgetConfiguration(IWidget widget) {
        this.id = widget.getWidgetDescription().getName();
        this.mapping = widget.getMapping();
        properties.putAll(widget.getProperties());
    }
}
