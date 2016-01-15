/*
 * Copyright (C) 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.common.client.ui;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Widget;

/**
 * A widget that wraps an {@link Element} to support the registration of event listeners and data
 * binding.
 *
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public abstract class ElementWrapperWidget<T> extends Widget {
  private static Map<Element, ElementWrapperWidget<?>> widgetMap = new HashMap<>();

  public static ElementWrapperWidget<?> getWidget(final Element element) {
    ElementWrapperWidget<?> widget = widgetMap.get(element);
    if (widget == null) {
      widget = createElementWrapperWidget(element);
      widgetMap.put(element, widget);
    }
    return widget;
  }

  private static ElementWrapperWidget<?> createElementWrapperWidget(final Element element) {
    if (InputElement.is(element) || TextAreaElement.is(element)) {
      return new InputElementWrapperWidget<>(element);
    }
    else {
      return new DefaultElementWrapperWidget<>(element);
    }
  }

  private static Class<?> getValueClassForInputType(final String inputType) {
    if ("checkbox".equalsIgnoreCase(inputType) || "radio".equalsIgnoreCase(inputType)) {
      return Boolean.class;
    }
    else {
      return String.class;
    }
  }

  private static boolean different(final Object oldValue, final Object newValue) {
    return (oldValue == null ^ newValue == null) || (oldValue != null && !oldValue.equals(newValue));
  }

  public static ElementWrapperWidget<?> removeWidget(final Element element) {
    return widgetMap.remove(element);
  }

  public static ElementWrapperWidget<?> removeWidget(final ElementWrapperWidget<?> widget) {
    return widgetMap.remove(widget.getElement());
  }

  private static class InputElementWrapperWidget<T> extends ElementWrapperWidget<T> implements HasValue<T> {

    private final ValueChangeManager<T, InputElementWrapperWidget<T>> valueChangeManager = new ValueChangeManager<>(this);

    private InputElementWrapperWidget(final Element element) {
      super(element);
    }

    @Override
    public void setValue(final T value, final boolean fireEvents) {
      final T oldValue = getValue();
      setValue(value);
      if (fireEvents && different(oldValue, value)) {
        ValueChangeEvent.fire(this, value);
      }
    }

    @Override
    public void setValue(final T value) {
      final String inputType = getElement().getPropertyString("type");
      final Class<?> valueType = getValueClassForInputType(inputType);

      if (Boolean.class.equals(valueType)) {
        getElement().setPropertyBoolean("checked", (Boolean) value);
      } else if (String.class.equals(valueType)) {
        getElement().setPropertyObject("value", value != null ? value : "");
      } else {
        throw new IllegalArgumentException("Cannot set value " + value + " to element input[type=\"" + inputType + "\"].");
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getValue() {
      final String inputType = getElement().getPropertyString("type");
      final Class<?> valueType = getValueClassForInputType(inputType);
      if (Boolean.class.equals(valueType)) {
        return (T) (Boolean) getElement().getPropertyBoolean("checked");
      }
      else if (String.class.equals(valueType)) {
        final Object rawValue = getElement().getPropertyObject("value");
        return (T) (rawValue != null ? rawValue : "");
      }
      else {
        throw new RuntimeException("Unrecognized input element type [" + inputType + "]");
      }
    }

    @Override
    public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<T> handler) {
      return valueChangeManager.addValueChangeHandler(handler);
    }

    @Override
    public Class<?> getValueType() {
      return getValueClassForInputType(getElement().getPropertyString("type"));
    }

  }

  private static class DefaultElementWrapperWidget<T> extends ElementWrapperWidget<T> implements HasHTML {

    private DefaultElementWrapperWidget(final Element element) {
      super(element);
    }

    @Override
    public String getText() {
      return getElement().getInnerText();
    }

    @Override
    public void setText(final String text) {
      getElement().setInnerText(text);
    }

    @Override
    public String getHTML() {
      return getElement().getInnerHTML();
    }

    @Override
    public void setHTML(final String html) {
      getElement().setInnerHTML(html);
    }

    @Override
    public Class<?> getValueType() {
      return String.class;
    }

  }

  private EventListener listener = new EventListener() {
    @Override
    public void onBrowserEvent(final Event event) {
      ElementWrapperWidget.super.onBrowserEvent(event);
    }
  };

  private ElementWrapperWidget(Element wrapped) {
    if (wrapped == null) {
      throw new IllegalArgumentException(
              "Element to be wrapped must not be null - Did you forget to initialize or @Inject a UI field?");
    }
    this.setElement(wrapped);
    DOM.setEventListener(this.getElement(), this);
  }

  public void setEventListener(final int eventsToSink, final EventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("EventListener cannot be null.");
    }

    sinkEvents(eventsToSink);
    this.listener = listener;
  }

  @Override
  public void onBrowserEvent(Event event) {
    listener.onBrowserEvent(event);
  }

  public abstract Class<?> getValueType();

}
