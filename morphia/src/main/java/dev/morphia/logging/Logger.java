/*
 * Copyright (c) 2008 - 2014 MongoDB, Inc. <http://mongodb.com>
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

package dev.morphia.logging;

/**
 * A generic logger interface used internally by Morphia.  At runtime the actual implementation used is chosen to match which logging
 * framework (e.g., java.util.logging vs slf4j) is used in the application.
 */
@SuppressWarnings("unused")
public interface Logger {
    /**
     * Logs a message at this level.
     *
     * @param msg the message to log
     */
    void debug(String msg);

    /**
     * Logs a message at this level.
     *
     * @param msg the message to log
     * @param arg formatting arguments for the message
     */
    void debug(String msg, Object... arg);

    /**
     * Logs a message at this level.
     *
     * @param msg the message to log
     * @param t   the Throwable to log
     */
    void debug(String msg, Throwable t);

    /**
     * Logs a message at this level.
     *
     * @param msg the message to log
     */
    void error(String msg);

    /**
     * Logs a message at this level.
     *
     * @param msg the message to log
     * @param arg formatting arguments for the message formatting arguments for the message
     */
    void error(String msg, Object... arg);

    /**
     * Logs a message at this level.
     *
     * @param msg the message to log
     * @param t   the Throwable to log
     */
    void error(String msg, Throwable t);

    /**
     * Logs a message at this level.
     *
     * @param msg the message to log
     */
    void info(String msg);

    /**
     * Logs a message at this level.
     *
     * @param msg the message to log
     * @param arg formatting arguments for the message
     */
    void info(String msg, Object... arg);

    /**
     * Logs a message at this level.
     *
     * @param msg the message to log
     * @param t   the Throwable to log the Throwable to log
     */
    void info(String msg, Throwable t);

    /**
     * @return true if logging is enabled at this level
     */
    boolean isDebugEnabled();

    /**
     * @return true if logging is enabled at this level
     */
    boolean isErrorEnabled();

    /**
     * @return true if logging is enabled at this level
     */
    boolean isInfoEnabled();

    /**
     * @return true if logging is enabled at this level
     */
    boolean isTraceEnabled();

    /**
     * @return true if logging is enabled at this level
     */
    boolean isWarningEnabled();

    /**
     * @param msg the message to log
     */
    void trace(String msg);

    /**
     * @param msg the message to log
     * @param arg formatting arguments for the message
     */
    void trace(String msg, Object... arg);

    /**
     * @param msg the message to log
     * @param t   the Throwable to log
     */
    void trace(String msg, Throwable t);

    /**
     * @param msg the message to log
     */
    void warning(String msg);

    /**
     * @param msg the message to log
     * @param arg formatting arguments for the message
     */
    void warning(String msg, Object... arg);

    /**
     * @param msg the message to log
     * @param t   the Throwable to log
     */
    void warning(String msg, Throwable t);
}
