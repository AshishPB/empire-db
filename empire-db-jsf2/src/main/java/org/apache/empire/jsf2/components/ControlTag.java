/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.jsf2.components;

import java.io.IOException;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIInput;
import javax.faces.component.UINamingContainer;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.ConverterException;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.db.exceptions.FieldIllegalValueException;
import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlTag extends UIInput implements NamingContainer
{
    public static String DEFAULT_CONTROL_SEPARATOR_TAG = "td";
    public static String DEFAULT_LABEL_SEPARATOR_CLASS = "eCtlLabel";
    public static String DEFAULT_INPUT_SEPARATOR_CLASS = "eCtlInput";

    public static abstract class ControlSeparatorComponent extends javax.faces.component.UIComponentBase
    {
        private ControlTag control = null;

        /*
        protected ControlSeparatorComponent()
        {
            if (log.isTraceEnabled())
                log.trace("ControlSeparatorComponent "+getClass().getName()+" created.");
        }
        */

        @Override
        public String getFamily()
        {
            return UINamingContainer.COMPONENT_FAMILY;
        }
        
        /*
        @Override
        public String getClientId(FacesContext context)
        {
            String clientId = super.getClientId(context);
            log.info("ControlSeparatorComponent-ID is {}", clientId);
            // default behavior
            return clientId;
        }
        */

        @Override
        public void encodeBegin(FacesContext context)
            throws IOException
        {
            super.encodeBegin(context);

            UIComponent parent = getParent();
            if (!(parent instanceof ControlTag))
                parent = parent.getParent();
            if (!(parent instanceof ControlTag))
            {   log.error("Invalid parent component for " + getClass().getName());
                return;
            }

            this.control = (ControlTag) parent;
        }

        protected abstract void writeAttributes(ResponseWriter writer, TagEncodingHelper helper, String tagName)
            throws IOException;

        @Override
        public boolean getRendersChildren()
        {
            return true;
        }

        @Override
        public void encodeChildren(FacesContext context)
            throws IOException
        {
            if (this.control != null)
            { // write end tag
                TagEncodingHelper helper = this.control.helper;
                String tagName = helper.getTagAttributeString("tag", "td");

                // render components
                ResponseWriter writer = context.getResponseWriter();
                writer.startElement(tagName, this);
                writeAttributes(writer, helper, tagName);
                // write children
                if (control.helper.isVisible())
                    super.encodeChildren(context);
                else
                    log.debug("Field {} is not visible.", helper.getColumn().getName());
                // end
                writer.endElement(tagName);
            }
        }
    }

    public static class LabelSeparatorComponent extends ControlSeparatorComponent
    {
        @Override
        protected void writeAttributes(ResponseWriter writer, TagEncodingHelper helper, String tagName)
            throws IOException
        {
            String styleClass = helper.getTagAttributeString("labelClass", ControlTag.DEFAULT_LABEL_SEPARATOR_CLASS);
            if (StringUtils.isNotEmpty(styleClass))
                writer.writeAttribute("class", styleClass, null);
        }
    }

    public static class InputSeparatorComponent extends ControlSeparatorComponent
    {
        @Override
        protected void writeAttributes(ResponseWriter writer, TagEncodingHelper helper, String tagName)
            throws IOException
        {
            String styleClass = helper.getTagAttributeString("inputClass", ControlTag.DEFAULT_INPUT_SEPARATOR_CLASS);
            // styleClass
            if (StringUtils.isNotEmpty(styleClass))
                writer.writeAttribute("class", styleClass, null);
            // colspan
            String colSpan = helper.getTagAttributeString("colspan");
            if (StringUtils.isNotEmpty(colSpan) && tagName.equalsIgnoreCase("td"))
                writer.writeAttribute("colspan", colSpan, null);
        }
        
    }

    public static class ValueOutputComponent extends javax.faces.component.UIComponentBase
    {
        private final String tagName = "span";

        /*
        public ValueOutputComponent()
        {
            if (log.isTraceEnabled())
                log.trace("ValueOutputComponent created.");
        } 
        */

        @Override
        public String getFamily()
        {
            return UINamingContainer.COMPONENT_FAMILY;
        }

        @Override
        public void encodeBegin(FacesContext context)
            throws IOException
        {
            super.encodeBegin(context);

            UIComponent parent = getParent();
            if (!(parent instanceof ControlTag))
                parent = parent.getParent();
            if (!(parent instanceof ControlTag))
                parent = parent.getParent();
            if (!(parent instanceof ControlTag))
            {   log.error("Invalid parent component for " + getClass().getName());
                return;
            }

            ControlTag controlTag = (ControlTag) parent;
            InputControl control = controlTag.control;
            InputControl.ValueInfo valInfo = controlTag.inpInfo;

            TagEncodingHelper helper = controlTag.helper;
            if (control == null)
                control = helper.getInputControl(); // Oops, should not come here 
            if (valInfo == null)
                valInfo = helper.getValueInfo(context); // Oops, should not come here 

            String styleClass = helper.getTagStyleClass("eInpDis");
            String tooltip = helper.getValueTooltip(helper.getTagAttributeString("title"));

            // render components
            ResponseWriter writer = context.getResponseWriter();
            writer.startElement(this.tagName, this);
            if (StringUtils.isNotEmpty(styleClass))
                writer.writeAttribute("class", styleClass, null);
            if (StringUtils.isNotEmpty(tooltip))
                writer.writeAttribute("title", tooltip, null);
            // render Value
            control.renderValue(valInfo, writer);
            writer.endElement(this.tagName);
        }
    }

    // Logger
    private static final Logger       log                = LoggerFactory.getLogger(ControlTag.class);

    private static final String       readOnlyState      = "readOnlyState";

    private static final boolean      encodeLabel        = true;

    protected final TagEncodingHelper helper             = new TagEncodingHelper(this, "eInput");

    protected InputControl            control            = null;
    protected InputControl.InputInfo  inpInfo            = null;
    protected boolean                 hasRequiredFlagSet = false;
    private   boolean                 creatingComponents = false;

    public ControlTag()
    {
        super();
    }

    @Override
    public String getFamily()
    {
        return "javax.faces.NamingContainer";
    }

    private void saveState()
    {
        // getStateHelper().put(inpControlPropName, control);
        // getStateHelper().put(inputInfoPropName, inpInfo);
        getStateHelper().put(ControlTag.readOnlyState, (this.inpInfo == null));
    }

    private boolean initState(FacesContext context)
    {
        // Check read-Only
        Boolean ros = (Boolean) getStateHelper().get(ControlTag.readOnlyState);
        if (ros != null && ros.booleanValue())
            return false;
        // Must have children        
        if (getChildCount() == 0)
        {   log.warn("InputTag has no children! Unable to init Input state for id={}", getClientId());
            log.warn("Problem might be related to Mojarra's state context saving for dynamic components (affects all versions > 2.1.6). See com.sun.faces.context.StateContext.java:AddRemoveListener");
            return false;
        }
        // control = ;
        this.control = helper.getInputControl();
        this.inpInfo = helper.getInputInfo(context);
        return (this.control != null && this.inpInfo != null);
    }

    /**
     * remember original clientId
     * necessary only inside UIData
     */
    private String treeClientId = null;
    
    @Override
    public boolean visitTree(VisitContext visitContext, VisitCallback callback) 
    {
        FacesContext context = visitContext.getFacesContext();
        treeClientId = this.getClientId(context);
        return super.visitTree(visitContext, callback);
    }

    @Override
    public String getClientId(FacesContext context)
    {
        // Check if dynamic components are being created
        if (this.treeClientId!=null && (this.creatingComponents || this.control!=null && this.control.isCreatingComponents()))
        {   // return the original tree client id
            return treeClientId; 
        }
        // default behavior
        return super.getClientId(context);
    }

    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // add label and input components when the view is loaded for the first time
        super.encodeBegin(context);

        // init
        helper.encodeBegin();
        this.control = helper.getInputControl();

        boolean isCustomInput = isCustomInput();

        // create children
        if (ControlTag.encodeLabel)
        {   // Create Label Separator Tag
            ControlSeparatorComponent labelSepTag = null;
            if (getChildCount() > 0)
                labelSepTag = (ControlSeparatorComponent) getChildren().get(0);
            if (labelSepTag == null)
            {   try {
                    creatingComponents = true;
                    labelSepTag = new LabelSeparatorComponent();
                    getChildren().add(labelSepTag);
                    helper.resetComponentId(labelSepTag);
                } finally {
                    creatingComponents = false;
                }
            }
            labelSepTag.setRendered(true);
            encodeLabel(context, labelSepTag);
            if (isCustomInput)
            { // don't render twice!
                labelSepTag.setRendered(false);
            }
        }

        if (!isCustomInput)
        {   // Create Input Separator Tag
            ControlSeparatorComponent inputSepTag = null;
            if (getChildCount() > 1)
                inputSepTag = (ControlSeparatorComponent) getChildren().get(1);
            if (inputSepTag == null)
            {   try {
                    creatingComponents = true;
                    inputSepTag = new InputSeparatorComponent();
                    getChildren().add(inputSepTag);
                    helper.resetComponentId(inputSepTag);
                } finally {
                    creatingComponents = false;
                }
            }
            encodeInput(context, inputSepTag);
        }
        // done
        saveState();
    }

    @Override
    public boolean getRendersChildren()
    {
        return true;
    }

    @Override
    public void encodeChildren(FacesContext context)
        throws IOException
    {
        super.encodeChildren(context);
    }

    @Override
    public void encodeEnd(FacesContext context)
        throws IOException
    {
        if (isRendered() && isCustomInput()) // MyFaces Patch!
        {
            String tagName  = helper.getTagAttributeString("tag", ControlTag.DEFAULT_CONTROL_SEPARATOR_TAG);
            String inpClass = helper.getTagAttributeString("inputClass", ControlTag.DEFAULT_INPUT_SEPARATOR_CLASS);
            String colSpan  = helper.getTagAttributeString("colspan");

            ResponseWriter writer = context.getResponseWriter();
            writer.startElement(tagName, this);
            if (StringUtils.isNotEmpty(inpClass))
                writer.writeAttribute("class", inpClass, null);
            if (StringUtils.isNotEmpty(colSpan) && tagName.equalsIgnoreCase("td"))
                writer.writeAttribute("colspan", colSpan, null);
            // encode children
            super.encodeEnd(context);
            // end of element
            writer.endElement(tagName);
        }
        else
        { // default
            super.encodeEnd(context);
        }
    }
    
    @Override
    public void setId(String id) 
    {
        super.setId(id);
        // reset record
        helper.setRecord(null);
    }

    @Override
    public void processDecodes(FacesContext context) 
    {
        if (helper.isInsideUIData())
        {   // Check input controls
            if (getChildCount()>1 && (getChildren().get(1) instanceof InputSeparatorComponent))
            {   // Make sure all inputs are rendered
                boolean hasChanged = false;
                boolean readOnly = helper.isRecordReadOnly();
                InputSeparatorComponent parent = (InputSeparatorComponent)getChildren().get(1);
                // set rendered of children
                for (UIComponent child : parent.getChildren())
                {   // set rendered 
                    boolean rendered = (child instanceof ValueOutputComponent) ? readOnly : !readOnly;
                    if (child.isRendered()!=rendered)
                    {
                        child.setRendered(rendered);
                        hasChanged = true;
                    }    
                }
                // give control chance to update
                if (hasChanged && log.isDebugEnabled())
                    log.debug("Changing UIInput readOnly state for {} to {}", helper.getColumnName(), readOnly);
                if (this.control==null)
                    this.control = helper.getInputControl();
                if (this.inpInfo==null)
                    this.inpInfo = helper.getInputInfo(context);
                this.control.updateInputState(parent, inpInfo, context);
            }
        }
        // default
        super.processDecodes(context);
    }

    @Override
    public void setRequired(boolean required)
    {
        super.setRequired(required);
        // flag has been set
        this.hasRequiredFlagSet = true;
    }

    public boolean isCustomInput()
    {
        Object custom = getAttributes().get("custom");
        if (custom != null)
            return ObjectUtils.getBoolean(custom);
        return false;
    }

    private void encodeLabel(FacesContext context, UIComponentBase parent)
        throws IOException
    {
        // render components
        try {
            creatingComponents = true;
            HtmlOutputLabel labelComponent = null;
            if (parent.getChildCount() > 0)
            {
                labelComponent = (HtmlOutputLabel) parent.getChildren().get(0);
                // update
                helper.updateLabelComponent(context, labelComponent, null);
            }
            if (labelComponent == null)
            {
                String forInput = isCustomInput() ? helper.getTagAttributeString("for") : "*";
                // createLabelComponent 
                labelComponent = helper.createLabelComponent(context, forInput, "eLabel", null, true);
                parent.getChildren().add(0, labelComponent);
                helper.resetComponentId(labelComponent);
            }
        } finally {
            creatingComponents = false;
        }
        // render components
        parent.encodeAll(context);
    }

    private void encodeInput(FacesContext context, UIComponentBase parent)
        throws IOException
    {
        // render components
        try {
            creatingComponents = true;
            // check children
            int count = parent.getChildCount();
            boolean resetChildId = (count==0);
            // continue
            this.inpInfo = helper.getInputInfo(context);
            // set required
            if (this.hasRequiredFlagSet==false)
                super.setRequired(helper.isValueRequired());
	        // create Input Controls
            // boolean recordReadOnly = helper.isRecordReadOnly();
            control.renderInput(parent, inpInfo, context, false);
            // create Value Component
            UIComponent valueComp = (count>0 ? parent.getChildren().get(count-1) : null);
            if (valueComp == null)
            {   // create ValueOutputComponent
                valueComp = new ValueOutputComponent();
                parent.getChildren().add(valueComp);
            }
            // Walk through the list of controls
            boolean readOnly = helper.isRecordReadOnly();
            for (UIComponent child : parent.getChildren())
            {   // reset child-id
                if (resetChildId)
                    helper.resetComponentId(child);
                // set rendered 
                boolean rendered = (child instanceof ValueOutputComponent) ? readOnly : !readOnly;
                child.setRendered(rendered);
            }
        } finally {
            creatingComponents = false;
        }
        // render components
        parent.encodeAll(context);
    }

    @Override
    public Object getValue()
    {
        // check for record
        if (helper.getRecord() != null)
            return helper.getDataValue(true);
        // default
        Object value = super.getValue();
        return value;
    }
    
    @Override
    public Object getSubmittedValue()
    { // Check state
        if (this.control == null || this.inpInfo == null || helper.isReadOnly())
            return null;
        // Get Input Tag
        if (getChildCount() <= 1)
            return null;
        // get Input Value
        ControlSeparatorComponent inputSepTag = (ControlSeparatorComponent) getChildren().get(1);
        return this.control.getInputValue(inputSepTag, this.inpInfo, true);
    }
    
    @Override
    protected Object getConvertedValue(FacesContext context, Object newSubmittedValue)
        throws ConverterException
    { // Check state
        if (this.control == null || this.inpInfo == null || helper.isReadOnly())
            return null;
        // Get Input Tag
        if (getChildCount() <= 1)
            return null;
        // get Input Value
        ControlSeparatorComponent inputSepTag = (ControlSeparatorComponent) getChildren().get(1);
        return this.control.getConvertedValue(inputSepTag, this.inpInfo, newSubmittedValue);
    }

    @Override
    public void validateValue(FacesContext context, Object value)
    { // Check state
        if (this.inpInfo == null || !isValid())
            return;
        // Skip Null values on partial submit
        if (UIInput.isEmpty(value) && isPartialSubmit(context)) //  && helper.isValueRequired()
        { // Value is null
            log.debug("Skipping validation for {} due to Null value.", this.inpInfo.getColumn().getName());
            return;
        }
        // Validate value
        this.inpInfo.validate(value);
        setValid(true);
        // don't call base class!
        // super.validateValue(context, value);
    }

    @Override
    public void validate(FacesContext context)
    {
        if (initState(context) == false)
            return;
        // get submitted value and validate
        if (log.isDebugEnabled())
            log.debug("Validating input for {}.", this.inpInfo.getColumn().getName());
        // Validate value
        try
        {   // Will internally call getSubmittedValue() and validateValue() 
            super.validate(context);

        } catch (Exception e) {
            // Value is not valid
            if (!(e instanceof EmpireException))
                e = new FieldIllegalValueException(helper.getColumn(), "", e);
            // Add error message
            helper.addErrorMessage(context, e);
            setValid(false);
        }
    }

    @Override
    public void updateModel(FacesContext context)
    {
        if (initState(context) == false)
            return;
        // No Action
        if (!isValid() || !isLocalValueSet())
            return;
        // check required?
        Object value = getLocalValue();
        // check required
        if (UIInput.isEmpty(value) && isPartialSubmit(context) && !helper.isTempoaryNullable())
        { // Value is null, but required
            log.debug("Skipping model update for {} due to Null value.", this.inpInfo.getColumn().getName());
            return;
        }
        // super.updateModel(context);
        log.debug("Updating model input for {}.", this.inpInfo.getColumn().getName());
        this.inpInfo.setValue(value);
        setValue(null);
        setLocalValueSet(false);
        // Post update
        ControlSeparatorComponent inputSepTag = (ControlSeparatorComponent) getChildren().get(1);
        this.control.postUpdateModel(inputSepTag, this.inpInfo, context);
    }

    public InputControl getInputControl()
    {
        return this.control;
    }

    public Column getInputColumn()
    {
        return helper.getColumn();
    }

    public boolean isInputReadOnly()
    {
        return helper.isRecordReadOnly();
    }

    public boolean isInputRequired()
    {
        return helper.isValueRequired();
    }

    private boolean isPartialSubmit(FacesContext context)
    {
        // Check Required Flag
        if (this.hasRequiredFlagSet && !isRequired())
            return true;
        // partial  
        return helper.isPartialSubmit(context);
    }
    
}
