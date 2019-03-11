/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.morphia.annotations;

import com.mongodb.client.model.CollationAlternate;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;

/**
 * Defines the collation options for an index
 * @since 1.3
 */
public @interface Collation {
    /**
     * Causes secondary differences to be considered in reverse order, as it is done in the French language
     *
     * @return  the backwards value
     */
    boolean backwards() default false;

    /**
     * Turns on case sensitivity
     *
     * @return the case level value
     */
    boolean caseLevel() default false;

    /**
     * @return the locale
     *
     * @see <a href="http://userguide.icu-project.org/locale">ICU User Guide - Locale</a>
     */
    String locale();

    /**
     * @return the normalization value.  If true, normalizes text into Unicode NFD.
     */
    boolean normalization() default false;

    /**
     * @return  the numeric ordering.  if true will order numbers based on numerical order and not collation order
     */
    boolean numericOrdering() default false;

    /**
     * Controls whether spaces and punctuation are considered base characters
     *
     * @return the alternate
     */
    CollationAlternate alternate() default CollationAlternate.NON_IGNORABLE;

    /**
     * Determines if Uppercase or lowercase values should come first
     *
     * @return the collation case first value
     */
    CollationCaseFirst caseFirst() default CollationCaseFirst.OFF;

    /**
     * @return the maxVariable
     */
    CollationMaxVariable maxVariable() default CollationMaxVariable.PUNCT;

    /**
     * @return the collation strength
     */
    CollationStrength strength() default CollationStrength.TERTIARY;
}
