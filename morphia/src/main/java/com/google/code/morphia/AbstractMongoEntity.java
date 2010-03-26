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


package com.google.code.morphia;

import java.io.Serializable;

import com.google.code.morphia.annotations.CollectionName;
import com.google.code.morphia.annotations.Id;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */

public abstract class AbstractMongoEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/** The id for this instance */
	@Id protected String id;
	/** The collection this instance is stored in. You can use this for reference purposes.*/
    @CollectionName protected String collectionName;

    public AbstractMongoEntity() {
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
