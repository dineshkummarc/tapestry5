// Copyright 2006 The Apache Software Foundation
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

package org.apache.tapestry.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.tapestry.TapestryConstants;

/**
 * Marks a method as a handler for a client side event. The handler method will be invoked when a
 * component triggers an event. Filters on the type of event and on the originating component ensure
 * that only the appropriate methods are invoked.
 * <p>
 * Client events include a <em>context</em> of one or more values. These context values are
 * included in the action URI. The values are optionally supplied to the handler method as
 * parameters. Automatic type coercions from string to the type of the actual parameter occur.
 * <p>
 * Handlers may return a value. Returning a non-null value will abort the handling of the event, and
 * will usually control the response sent to the client web browser. The details are somewhat
 * specific to the type of event and the component involved.
 * <p>
 * If a handler is not found within the originating component (or no handler aborts the event
 * handling), then handlers within the containing component will be searched. This continues up the
 * page hierarchy. In some cases, having no handlers (or no aborting handlers) is considered
 * acceptible; in others, it is an error. Again, this is defined by the type of originating
 * component, and the type of event.
 * <p>
 * <strong>If you fail to provide filters on either component or event type, then your method will
 * be invoked for all component events, possibly including events that bubble up from embedded
 * sub-components. </strong>
 * 
 * 
 */
@Target(ElementType.METHOD)
@Retention(RUNTIME)
@Documented
public @interface OnEvent {

    /**
     * A list of event types to match. The handler will only be invoked if the client event type
     * matches one of the values.
     * 
     * @return
     * @see TapestryConstants
     */
    String[] value() default {};

    /**
     * A list of component ids. The handler will only be invoked if the id of the event originating
     * component matches one of the values.
     * 
     * @return
     */
    String[] component() default {};

}