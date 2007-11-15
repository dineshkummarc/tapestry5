// Copyright 2007 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry.runtime;

/**
 * An exception thrown while invoking a component event handler method.
 */
public class ComponentEventException extends RuntimeException
{
    private final String _methodDescription;

    /**
     * @param message           message describing the problem, usually obtained from the underly cause exception
     * @param methodDescription describes the method being invoke that threw the cause exception
     * @param cause             the underlying exception actually thrown by the event handler method
     */
    public ComponentEventException(String message, String methodDescription, Throwable cause)
    {
        super(message, cause);

        _methodDescription = methodDescription;
    }

    public String getMethodDescription()
    {
        return _methodDescription;
    }
}
