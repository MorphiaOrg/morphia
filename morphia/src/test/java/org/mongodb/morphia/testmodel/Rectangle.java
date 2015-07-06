/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */


package org.mongodb.morphia.testmodel;


import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.testutil.TestEntity;


/**
 * @author Olafur Gauti Gudmundsson
 */
public class Rectangle extends TestEntity implements Shape {
    @Property("h")
    private double height;
    @Property("w")
    private double width;

    public Rectangle() {
    }

    public Rectangle(final double height, final double width) {
        this.height = height;
        this.width = width;
    }

    @Override
    public double getArea() {
        return height * width;
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }
}
