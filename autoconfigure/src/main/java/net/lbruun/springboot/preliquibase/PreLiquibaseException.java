/*
 * Copyright 2021 lbruun.net.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lbruun.springboot.preliquibase;

/**
 * Base class for exceptions for Pre-Liquibase module.
 * 
 * @author lbruun
 */
public abstract class PreLiquibaseException extends RuntimeException {

    public PreLiquibaseException(String message) {
        super(message);
    }

    public PreLiquibaseException(String message, Throwable cause) {
        super(message, cause);

    }

    
    /**
     * Thrown on certain {@link PreLiquibase} methods if the method
     * is invoked <i>prior</i> to {@link PreLiquibase#execute()}.
     */
    public static class UninitializedError extends PreLiquibaseException {

        public static final UninitializedError DEFAULT = new UninitializedError("Method must not be invoked prior to execute()");
        public UninitializedError(String message) {
            super(message);
        }
    }

    
    /**
     * Thrown if the current db platform cannot be auto-detected 
     * from a DataSource.
     */
    public static class ResolveDbPlatformError extends PreLiquibaseException {

        public ResolveDbPlatformError(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Thrown on I/O errors during the stage where placeholder values in
     * SQL script are being replaced with their resolved values.
     */
    public static class SqlScriptReadError extends PreLiquibaseException {

        public SqlScriptReadError(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Thrown when placeholder variables in SQL scripts cannot be resolved
     * or if they are circular.
     */
    public static class SqlScriptVarError extends PreLiquibaseException {

        public SqlScriptVarError(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Thrown when location of individual SQL scripts are
     * {@link PreLiquibaseProperties#setSqlScriptReferences(java.util.List) explicitly specified}
     * and one or more of the references is invalid (file not found, incorrect
     * specification of Spring Resource reference, etc).
     */
    public static class SqlScriptRefError extends PreLiquibaseException {

        public SqlScriptRefError(String message) {
            super(message);
        }
    }

}
