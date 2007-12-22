// Copyright 2006, 2007 The Apache Software Foundation
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

package org.apache.tapestry.corelib.components;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

import org.apache.tapestry.ComponentAction;
import org.apache.tapestry.ComponentEventHandler;
import org.apache.tapestry.ComponentResources;
import org.apache.tapestry.Field;
import org.apache.tapestry.Link;
import org.apache.tapestry.MarkupWriter;
import org.apache.tapestry.TapestryConstants;
import org.apache.tapestry.ValidationTracker;
import org.apache.tapestry.ValidationTrackerImpl;
import org.apache.tapestry.annotations.AfterRender;
import org.apache.tapestry.annotations.BeginRender;
import org.apache.tapestry.annotations.CleanupRender;
import org.apache.tapestry.annotations.ComponentClass;
import org.apache.tapestry.annotations.Environmental;
import org.apache.tapestry.annotations.Inject;
import org.apache.tapestry.annotations.Mixin;
import org.apache.tapestry.annotations.OnEvent;
import org.apache.tapestry.annotations.Parameter;
import org.apache.tapestry.annotations.Persist;
import org.apache.tapestry.annotations.SetupRender;
import org.apache.tapestry.corelib.mixins.RenderInformals;
import org.apache.tapestry.dom.Element;
import org.apache.tapestry.internal.services.FormParameterLookup;
import org.apache.tapestry.internal.services.HeartbeatImpl;
import org.apache.tapestry.internal.util.Base64ObjectInputStream;
import org.apache.tapestry.internal.util.Base64ObjectOutputStream;
import org.apache.tapestry.internal.util.Holder;
import org.apache.tapestry.runtime.Component;
import org.apache.tapestry.services.ActionResponseGenerator;
import org.apache.tapestry.services.ComponentEventResultProcessor;
import org.apache.tapestry.services.ComponentSource;
import org.apache.tapestry.services.Environment;
import org.apache.tapestry.services.FormSupport;
import org.apache.tapestry.services.Heartbeat;
import org.apache.tapestry.services.PageRenderSupport;

/**
 * An HTML form, which will enclose other components to render out the various types of fields.
 * <p>
 * A Form emits several notification events; when it renders it sends a {@link #PREPARE prepare}
 * notification event, to allow any listeners to set up the state of the page prior to rendering out
 * the form's content.
 * <p>
 * When the form is submitted, the component emits four notifications: first another prepare event
 * to allow the page to update its state as necessary to prepare for the form submission, then
 * (after components enclosed by the form have operated), a "validate" event is emitted, to allow
 * for cross-form validation. After that, either a "success" or "failure" event (depending on
 * whether the {@link ValidationTracker} has recorded any errors). Lastly, a "submit" event, for any
 * listeners that care only about form submission, regardless of success or failure.
 * <p>
 * For all of these notifications, the event context is derived from the <strong>context</strong>
 * parameter. This context is encoded into the form's action URI (the parameter is not read when the
 * form is submitted, instead the values encoded into the form are used).
 */
@ComponentClass
public class Form
{
    /**
     * Invoked to let the containing component(s) prepare for the form rendering or the form
     * submission.
     */
    public static final String PREPARE = "prepare";

    /**
     * Event type for a notification after the form has submitted. This event notification occurs on
     * any form submit, without respect to "success" or "failure".
     */
    public static final String SUBMIT = "submit";

    /**
     * Event type for a notification to perform validation of submitted data. This allows a listener
     * to perform cross-field validation. This occurs before the {@link #SUCCESS} or
     * {@link #FAILURE} notification.
     */
    public static final String VALIDATE = "validate";

    /**
     * Event type for a notification after the form has submitted, when there are no errors in the
     * validation tracker. This occurs before the {@link #SUBMIT} event.
     */
    public static final String SUCCESS = "success";

    /**
     * Event type for a notification after the form has been submitted, when there are errors in the
     * validation tracker. This occurs before the {@link #SUBMIT} event.
     */
    public static final String FAILURE = "failure";

    /**
     * The context for the link (optional parameter). This list of values will be converted into
     * strings and included in the URI. The strings will be coerced back to whatever their values
     * are and made available to event handler methods.
     */
    @Parameter
    private List<?> _context;

    @Parameter("defaultTracker")
    private ValidationTracker _tracker;

    /**
     * Query parameter name storing form data (the serialized commands needed to process a form
     * submission).
     */
    public static final String FORM_DATA = "t:formdata";

    @Inject("infrastructure:Environment")
    private Environment _environment;

    @Inject
    private ComponentResources _resources;

    @Environmental
    private PageRenderSupport _pageRenderSupport;

    @Inject("service:tapestry.internal.FormParameterLookup")
    private FormParameterLookup _paramLookup;

    @Inject("infrastructure:ComponentSource")
    private ComponentSource _source;

    @Persist
    private ValidationTracker _defaultTracker;

    private FormSupportImpl _formSupport;

    // Collects a stream of component actions. Each action goes in as a UTF string (the component
    // component id), followed by a ComponentAction

    private Base64ObjectOutputStream _actions;

    @SuppressWarnings("unused")
    @Mixin
    private RenderInformals _renderInformals;

    public ValidationTracker getDefaultTracker()
    {
        if (_defaultTracker == null)
            _defaultTracker = new ValidationTrackerImpl();

        return _defaultTracker;
    }

    public void setDefaultTracker(ValidationTracker defaultTracker)
    {
        _defaultTracker = defaultTracker;
    }

    @SetupRender
    void setup()
    {
        try
        {
            _actions = new Base64ObjectOutputStream();
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }

        _formSupport = new FormSupportImpl(_actions);

        // TODO: Forms should not allow to nest. Perhaps a set() method instead of a push() method
        // for this kind of check?

        _environment.push(FormSupport.class, _formSupport);
        _environment.push(ValidationTracker.class, _tracker);

    }

    private Element _div;

    @BeginRender
    void begin(MarkupWriter writer)
    {
        // Now that the environment is setup, inform the component or other listeners that the form
        // is about to
        // render.

        Object[] contextArray = _context == null ? new Object[0] : _context.toArray();

        _resources.triggerEvent(PREPARE, contextArray, null);

        String name = _pageRenderSupport.allocateClientId(_resources.getId());

        Link link = _resources
                .createActionLink(TapestryConstants.DEFAULT_EVENT, true, contextArray);
        writer.element("form", "name", name, "id", name, "method", "post", "action", link);

        _resources.renderInformalParameters(writer);

        _div = writer.element("div", "class", "t-invisible");

        for (String parameterName : link.getParameterNames())
        {
            String value = link.getParameterValue(parameterName);

            writer.element("input", "type", "hidden", "name", parameterName, "value", value);
            writer.end();
        }

        writer.end(); // div

        _environment.peek(Heartbeat.class).begin();

    }

    @AfterRender
    void after(MarkupWriter writer)
    {
        _environment.peek(Heartbeat.class).end();

        _formSupport.executeDeferred();

        writer.end(); // form

        // Now, inject into the div the remaining hidden field (the list of actions).

        try
        {
            _actions.close();
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }

        _div.element("input", "type", "hidden", "name", FORM_DATA, "value", _actions.toBase64());
    }

    @CleanupRender
    void cleanup()
    {
        _environment.pop(FormSupport.class);

        _formSupport = null;

        // This forces a change to the tracker, which is nice because its internal state has
        // changed.
        _tracker = _environment.pop(ValidationTracker.class);
    }

    @Inject("infrastructure:ComponentEventResultProcessor")
    private ComponentEventResultProcessor _eventResultProcessor;

    @SuppressWarnings("unchecked")
    @OnEvent("action")
    Object onSubmit(Object[] context)
    {
        _tracker.clear();

        _formSupport = new FormSupportImpl();

        _environment.push(ValidationTracker.class, _tracker);
        _environment.push(FormSupport.class, _formSupport);

        Heartbeat heartbeat = new HeartbeatImpl();

        _environment.push(Heartbeat.class, heartbeat);

        heartbeat.begin();

        try
        {
            final Holder<ActionResponseGenerator> holder = Holder.create();

            ComponentEventHandler handler = new ComponentEventHandler()
            {
                public boolean handleResult(Object result, Component component,
                        String methodDescription)
                {
                    if (result instanceof Boolean)
                        return ((Boolean) result);

                    holder.put(_eventResultProcessor.processComponentEvent(
                            result,
                            component,
                            methodDescription));

                    return true; // Abort other event processing.
                }
            };

            _resources.triggerEvent(PREPARE, context, handler);

            if (holder.hasValue())
                return holder.get();

            // TODO: Ajax stuff will eventually mean there are multiple values for this parameter
            // name

            String actionsBase64 = _paramLookup.getParameter(FORM_DATA);

            try
            {
                ObjectInputStream ois = new Base64ObjectInputStream(actionsBase64);

                while (true)
                {
                    String componentId = ois.readUTF();
                    ComponentAction action = (ComponentAction) ois.readObject();

                    Component component = _source.getComponent(componentId);

                    action.execute(component);
                }
            }
            catch (EOFException ex)
            {
                // Expected.
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }

            heartbeat.end();

            ValidationTracker tracker = _environment.peek(ValidationTracker.class);

            // Let the listeners peform any final validations

            // Update through the parameter because the tracker has almost certainly changed
            // internal state.

            _tracker = tracker;

            _resources.triggerEvent(VALIDATE, context, handler);

            if (holder.hasValue())
                return holder.get();

            _formSupport.executeDeferred();

            // Let the listeners know about overall success or failure. Most listeners fall into
            // one of those two camps.

            // If the tracker has no errors, then clear it of any input values
            // as well, so that the next page render will be "clean" and show
            // true persistent data, not value from the previous form submission.

            if (!_tracker.getHasErrors())
                _tracker.clear();

            _resources.triggerEvent(tracker.getHasErrors() ? FAILURE : SUCCESS, context, handler);

            // Lastly, tell anyone whose interested that the form is completely submitted.

            if (holder.hasValue())
                return holder.get();

            _resources.triggerEvent(SUBMIT, context, handler);

            return holder.get();
        }
        finally
        {

            _environment.pop(Heartbeat.class);
            _environment.pop(FormSupport.class);
        }
    }

    /**
     * A convienience for invoking {@link ValidationTracker#recordError(String)}.
     */
    public void recordError(String errorMessage)
    {
        ValidationTracker tracker = _tracker;

        tracker.recordError(errorMessage);

        _tracker = tracker;
    }

    /**
     * A convienience for invoking {@link ValidationTracker#recordError(Field, String)}.
     */
    public void recordError(Field field, String errorMessage)
    {
        ValidationTracker tracker = _tracker;

        tracker.recordError(field, errorMessage);

        _tracker = tracker;
    }

    /**
     * Returns true if the form's {@link ValidationTracker} contains any
     * {@link ValidationTracker#getHasErrors() errors}.
     */
    public boolean getHasErrors()
    {
        return _tracker.getHasErrors();
    }

    /**
     * Returns true if the form's {@link ValidationTracker} does not contain any
     * {@link ValidationTracker#getHasErrors() errors}.
     */
    public boolean isValid()
    {
        return !_tracker.getHasErrors();
    }

    // For testing:

    void setTracker(ValidationTracker tracker)
    {
        _tracker = tracker;
    }

    /**
     * Invokes {@link ValidationTracker#clear()}.
     */
    public void clearErrors()
    {
        _tracker.clear();
    }
}