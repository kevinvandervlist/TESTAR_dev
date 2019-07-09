/*
 * Copyright (c) 2013, 2014, 2015, 2016, 2017 Universitat Politecnica de Valencia - www.upv.es
 * Copyright (c) 2019 Open Universiteit - www.ou.nl
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.fruit.alayer.webdriver;

import org.fruit.alayer.Rect;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WdElement implements Serializable {
  private static final long serialVersionUID = 2695983969893321255L;

  private static final List<String> scrollOn = Arrays.asList("auto", "scroll");
  private static final List<String> scrollableChildren = Arrays.asList(
      "block", "run-in", "flow", "flow-root", "table", "flex", "grid",
      "list-item", "table-row", "table-cell", "table-caption", "inline-block",
      "inline-table", "inline-flex", "inline-grid");
  private static final List<String> focusableTags = Arrays.asList(
      "input", "select", "textarea", "a", "button", "area");

  List<WdElement> children = new ArrayList<>();
  WdElement parent;
  WdRootElement root;
  WdWidget backRef;

  public boolean blocked;
  long culture = 0L;
  boolean isModal = false; // i.c.w. access key

  public String id, name, tagName, textContent, helpText;
  public List<String> cssClasses = new ArrayList<>();
  public String display, type;

  boolean enabled, ignore;
  public boolean isClickable;
  boolean isContentElement, isControlElement;
  boolean hasKeyboardFocus, isKeyboardFocusable;
  String acceleratorKey, accessKey;
  String valuePattern;

  double zindex;
  Rect rect;
  boolean scrollPattern, hScroll, vScroll;
  public double hScrollViewSize, vScrollViewSize, hScrollPercent, vScrollPercent;

  // Keep these here for fillScrollValues
  protected String overflowX, overflowY;
  protected long clientWidth, clientHeight;
  private long offsetWidth, offsetHeight;
  public long scrollWidth, scrollHeight;
  public long scrollLeft, scrollTop;
  private long borderWidth, borderHeight;

  public Map<String, String> attributeMap;

  @SuppressWarnings("unchecked")
  public WdElement(Map<String, Object> packedElement,
                   WdRootElement root, WdElement parent) {
    this.root = root;
    this.parent = parent;

    attributeMap = (Map<String, String>) packedElement.get("attributeMap");

    id = attributeMap.getOrDefault("id", "");
    name = (String) packedElement.get("name");
    tagName = (String) packedElement.get("tagName");
    textContent = ((String) packedElement.get("textContent"))
        .replaceAll("\\s+", " ").trim();
    helpText = attributeMap.get("title");
    valuePattern = attributeMap.getOrDefault("href", "");
    if (valuePattern == null || valuePattern.equals("")) {
      valuePattern = String.valueOf(packedElement.getOrDefault("value", ""));
    }

    String classesString = attributeMap.getOrDefault("class", "");
    if (classesString != null) {
      cssClasses = Arrays.asList(classesString.split(" "));
    }
    display = (String) packedElement.get("display");
    type = attributeMap.get("type");

    zindex = (double) (long) packedElement.get("zIndex");
    fillRect(packedElement);
    fillDimensions(packedElement);

    blocked = (Boolean) packedElement.get("isBlocked");
    isClickable = (Boolean) packedElement.get("isClickable");
    isKeyboardFocusable = getIsFocusable();
    hasKeyboardFocus = (Boolean) packedElement.get("hasKeyboardFocus");

    enabled = !Constants.hiddenTags.contains(tagName);
    if (display != null && display.toLowerCase().equals("none")) {
      enabled = false;
    }

    List<Map<String, Object>> wrappedChildren =
        (List<Map<String, Object>>) packedElement.get("wrappedChildren");
    for (Map<String, Object> wrappedChild : wrappedChildren) {
      WdElement child = new WdElement(wrappedChild, root, this);
      if (!Constants.hiddenTags.contains(child.tagName) &&
          !Constants.ignoredTags.contains(child.tagName)) {
        children.add(child);
      }
    }

    setName();
    fillScrollValues();
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.defaultWriteObject();
  }

  private void readObject(ObjectInputStream ois)
      throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
  }

  private void setName() {
    if (name == null || name.equals("null") || name.isEmpty()) {
      name = textContent;
    }
  }

  public boolean getIsFocusable() {
    // It's much more complex than this
    // https://allyjs.io/data-tables/focusable.html#document-elements
    // https://allyjs.io/tests/focusable/test.html
    // https://gist.github.com/jamiewilson/c3043f8c818b6b0ccffd
    // https://www.w3.org/TR/html5/editing.html#focus

    return focusableTags.contains(tagName);
  }

  protected void fillScrollValues() {
    // https://stackoverflow.com/a/29956778

    boolean hasHorizontalScrollbar = offsetHeight > (clientHeight + borderHeight);
    hScroll = scrollWidth > clientWidth &&
              (scrollOn.contains(overflowX) || hasHorizontalScrollbar);
    if (scrollWidth != clientWidth) {
      hScrollPercent = 100.0 * scrollLeft / (scrollWidth - clientWidth);
      hScrollViewSize = clientWidth;
    }

    boolean hasVerticalScrollbar = offsetWidth > (clientWidth + borderWidth);
    vScroll = scrollHeight > clientHeight &&
              (scrollOn.contains(overflowY) || hasVerticalScrollbar);
    if (scrollHeight != clientHeight) {
      vScrollPercent = 100.0 * scrollTop / (scrollHeight - clientHeight);
      vScrollViewSize = clientHeight;
    }

    scrollPattern = hScroll || vScroll;
  }

  public boolean visibleAt(double x, double y) {
    int scrollLeft = (root == null) ? 0 : (int) root.scrollLeft;
    int scrollHeight = (root == null) ? 0 : (int) root.scrollHeight;
    return rect != null && rect.contains(x - scrollLeft, y - scrollHeight);
  }

  public boolean visibleAt(double x, double y, boolean obscuredByChildFeature) {
    return visibleAt(x, y);
  }

  @SuppressWarnings("unchecked")
  /*
   * This gets the position relative to the viewport
   */
  private void fillRect(Map<String, Object> packedElement) {
    List<Long> rect = (List<Long>) packedElement.get("rect");
    this.rect = Rect.from(rect.get(0), rect.get(1), rect.get(2), rect.get(3));
  }

  @SuppressWarnings("unchecked")
  private void fillDimensions(Map<String, Object> packedElement) {
    Map<String, Object> dims = (Map<String, Object>) packedElement.get("dimensions");
    overflowX = String.valueOf(dims.get("overflowX"));
    overflowY = String.valueOf(dims.get("overflowY"));
    clientWidth = castDimensionsToLong(dims.get("clientWidth"));
    clientHeight = castDimensionsToLong(dims.get("clientHeight"));
    offsetWidth = castDimensionsToLong(dims.get("offsetWidth"));
    offsetHeight = castDimensionsToLong(dims.get("offsetHeight"));
    scrollWidth = castDimensionsToLong(dims.get("scrollWidth"));
    scrollHeight = castDimensionsToLong(dims.get("scrollHeight"));
    scrollLeft = castDimensionsToLong(dims.get("scrollLeft"));
    scrollTop = castDimensionsToLong(dims.get("scrollTop"));
    borderWidth = castDimensionsToLong(dims.get("borderWidth"));
    borderHeight = castDimensionsToLong(dims.get("borderHeight"));
  }

  private long castDimensionsToLong(Object o) {
	  if(o instanceof Double)
		  return ((Double) o).longValue();
	  else if(o instanceof Long)
		  return ((Long) o).longValue();

	  return (long)o;
  }
}
