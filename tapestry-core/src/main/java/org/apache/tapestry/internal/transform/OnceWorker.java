// Copyright 2006, 2007, 2008 The Apache Software Foundation
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

package org.apache.tapestry.internal.transform;

import static java.lang.reflect.Modifier.PRIVATE;

import java.util.List;

import org.apache.tapestry.Binding;
import org.apache.tapestry.TapestryConstants;
import org.apache.tapestry.annotations.Once;
import org.apache.tapestry.ioc.util.BodyBuilder;
import org.apache.tapestry.model.MutableComponentModel;
import org.apache.tapestry.services.BindingSource;
import org.apache.tapestry.services.ClassTransformation;
import org.apache.tapestry.services.ComponentClassTransformWorker;
import org.apache.tapestry.services.TransformConstants;
import org.apache.tapestry.services.TransformMethodSignature;
import org.apache.tapestry.services.TransformUtils;

/** Caches method return values for methods annotated with {@link Once}. */
public class OnceWorker implements ComponentClassTransformWorker {
	private final BindingSource _bindingSource;
	
	public OnceWorker(BindingSource bindingSource) {
		super();
		this._bindingSource = bindingSource;
	}

	public void transform(ClassTransformation transformation, MutableComponentModel model) {
		List<TransformMethodSignature> methods = transformation.findMethodsWithAnnotation(Once.class);
		if (methods.isEmpty())
			return;
		
		for(TransformMethodSignature method : methods) {
			if (method.getReturnType().equals("void"))
				throw new IllegalArgumentException(TransformMessages.onceMethodMustHaveReturnValue(method));
	
			if (method.getParameterTypes().length != 0)
				throw new IllegalArgumentException(TransformMessages.onceMethodsHaveNoParameters(method));
			
			String propertyName = method.getMethodName();
						
			// add a property to store whether or not the method has been called
			String fieldName = transformation.addField(PRIVATE, method.getReturnType(), propertyName);
			String calledField = transformation.addField(PRIVATE, "boolean", fieldName + "$called");
	
			Once once = transformation.getMethodAnnotation(method, Once.class);
			String bindingField = null;
			String bindingValueField = null;
			boolean watching = once.watch().length() > 0;
			if (watching) {
				// add fields to store the binding and the value
				bindingField = transformation.addField(PRIVATE, Binding.class.getCanonicalName(), fieldName + "$binding");
				bindingValueField = transformation.addField(PRIVATE, "java.lang.Object", fieldName + "$bindingValue");

				String bindingSourceField = transformation.addInjectedField(BindingSource.class, fieldName + "$bindingsource", _bindingSource);
		
				String body = String.format("%s = %s.newBinding(\"Watch expression\", %s, \"%s\", \"%s\");", 
						bindingField,
						bindingSourceField,
						transformation.getResourcesFieldName(),
						TapestryConstants.PROP_BINDING_PREFIX,
						once.watch());
				transformation.extendMethod(TransformConstants.CONTAINING_PAGE_DID_LOAD_SIGNATURE, body);
			}
			
			BodyBuilder b = new BodyBuilder();

			// on cleanup, reset the field values
			b.begin();
						
			if (!TransformUtils.isPrimitive(method.getReturnType()))
				b.addln("%s = null;", fieldName);
			b.addln("%s = false;", calledField);
			
			if (watching)
				b.addln("%s = null;", bindingValueField);
			
			b.end();
			transformation.extendMethod(TransformConstants.POST_RENDER_CLEANUP_SIGNATURE, b.toString());
						
			// prefix the existing method to cache the result
			b.clear();
			b.begin();
			
			// if it has been called and watch is set and the old value is the same as the new value then return
			// get the old value and cache it
			/* NOTE: evaluates the binding twice when checking the new value.
			 * this is probably not a problem because in most cases properties
			 * that are being watched are not expensive operations. plus, we
			 * never guaranteed that it would be called exactly once when
			 * watching.
			 */
			if (watching) {
				b.addln("if (%s && %s == %s.get()) return %s;",
						calledField, bindingValueField, bindingField, fieldName);
				b.addln("%s = %s.get();", bindingValueField, bindingField);
			}
			else {
				b.addln("if (%s) return %s;", calledField, fieldName);
			}
			
			b.addln("%s = true;", calledField);			
			b.end();			
			transformation.prefixMethod(method, b.toString());
			
			// cache the return value
			b.clear();
			b.begin();
			b.addln("%s = $_;", fieldName);
			b.end();
			transformation.extendExistingMethod(method, b.toString());
		}
	}
}