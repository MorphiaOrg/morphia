/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.morphia.dao;

import java.util.List;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
public interface MongoDAO<T> {

    public T save( T entity );

    public void removeById( String id );

    public boolean exists( String key, String value );
    public boolean exists( String key, int value );
    public boolean exists( String key, long value );
    public boolean exists( String key, double value );
    public boolean exists( String key, boolean value );
    public boolean exists( String key, Enum value );

    public T get( String id );

    public T getByValue( String key, String value );
    public T getByValue( String key, int value );
    public T getByValue( String key, long value );
    public T getByValue( String key, double value );
    public T getByValue( String key, boolean value );
    public T getByValue( String key, Enum value );

    public long getCount();

    public void dropCollection();

    public List<T> findAll( int startIndex, int resultSize );
}
