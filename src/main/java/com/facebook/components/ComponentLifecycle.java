/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.yoga.YogaAlign;

import com.facebook.yoga.YogaFlexDirection;

import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pools;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.view.View;

import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaMeasureMode;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaNodeAPI;
import com.facebook.yoga.YogaMeasureOutput;

/**
 * {@link ComponentLifecycle} is a stateless singleton object that defines how {@link Component}
 * instances calculate their layout bounds and mount elements, among other things. This is the
 * base class from which all new component types inherit.
 *
 * In most cases, the {@link ComponentLifecycle} class will be automatically generated by the
 * annotation processor at build-time based on your spec class and you won't have to deal with
 * it directly when implementing new component types.
 */
public abstract class ComponentLifecycle implements EventDispatcher {
  private static final AtomicInteger sComponentId = new AtomicInteger();
  private static final int DEFAULT_MAX_PREALLOCATION = 15;

  private boolean mPreallocationDone;

  public enum MountType {
    NONE,
    DRAWABLE,
    VIEW,
  }

  public interface StateContainer {}

  private static final YogaBaselineFunction sBaselineFunction = new YogaBaselineFunction() {
    public float baseline(YogaNodeAPI cssNode, float width, float height) {
      final InternalNode node = (InternalNode) cssNode.getData();
      return node.getComponent()
          .getLifecycle()
          .onMeasureBaseline(node.getContext(), (int) width, (int) height);
    }
  };

  private static final YogaMeasureFunction sMeasureFunction = new YogaMeasureFunction() {

    private final Pools.SynchronizedPool<Size> mSizePool =
        new Pools.SynchronizedPool<>(2);

    private Size acquireSize(int initialValue) {
      Size size = mSizePool.acquire();
      if (size == null) {
        size = new Size();
      }

      size.width = initialValue;
      size.height = initialValue;
      return size;
    }

    private void releaseSize(Size size) {
      mSizePool.release(size);
    }

    @Override
    @SuppressLint("WrongCall")
    @SuppressWarnings("unchecked")
    public long measure(
        YogaNodeAPI cssNode,
        float width,
        YogaMeasureMode widthMode,
        float height,
        YogaMeasureMode heightMode) {
      final InternalNode node = (InternalNode) cssNode.getData();
      final DiffNode diffNode = node.areCachedMeasuresValid() ? node.getDiffNode() : null;
      final Component<?> component = node.getComponent();
      final int widthSpec;
      final int heightSpec;

      ComponentsSystrace.beginSection("measure:" + component.getSimpleName());
      widthSpec = SizeSpec.makeSizeSpecFromCssSpec(width, widthMode);
      heightSpec = SizeSpec.makeSizeSpecFromCssSpec(height, heightMode);

      node.setLastWidthSpec(widthSpec);
      node.setLastHeightSpec(heightSpec);

      int outputWidth = 0;
      int outputHeight = 0;

      if (Component.isNestedTree(component)) {
        final InternalNode nestedTree = LayoutState.resolveNestedTree(node, widthSpec, heightSpec);

        outputWidth = nestedTree.getWidth();
        outputHeight = nestedTree.getHeight();
      } else if (diffNode != null
          && diffNode.getLastWidthSpec() == widthSpec
          && diffNode.getLastHeightSpec() == heightSpec) {
        outputWidth = (int) diffNode.getLastMeasuredWidth();
        outputHeight = (int) diffNode.getLastMeasuredHeight();
      } else {
        final Size size = acquireSize(Integer.MIN_VALUE /* initialValue */);

        try {
          component.getLifecycle().onMeasure(
              node.getContext(),
              node,
              widthSpec,
              heightSpec,
              size,
              component);

          if (size.width < 0 || size.height < 0) {
            throw new IllegalStateException(
                "MeasureOutput not set, ComponentLifecycle is: " + component.getLifecycle());
          }

          outputWidth = size.width;
          outputHeight = size.height;

          if (node.getDiffNode() != null) {
            node.getDiffNode().setLastWidthSpec(widthSpec);
            node.getDiffNode().setLastHeightSpec(heightSpec);
            node.getDiffNode().setLastMeasuredWidth(outputWidth);
            node.getDiffNode().setLastMeasuredHeight(outputHeight);
          }
        } finally {
          releaseSize(size);
        }
      }

      node.setLastMeasuredWidth(outputWidth);
      node.setLastMeasuredHeight(outputHeight);

      ComponentsSystrace.endSection();

      return YogaMeasureOutput.make(outputWidth, outputHeight);
    }
  };

  private final int mId;

  protected ComponentLifecycle() {
    mId = sComponentId.incrementAndGet();
  }

  int getId() {
    return mId;
  }

  Object createMountContent(ComponentContext c) {
    return onCreateMountContent(c);
  }

  void mount(ComponentContext c, Object convertContent, Component<?> component) {
    onMount(c, convertContent, component);
  }

  void bind(ComponentContext c, Object mountedContent, Component<?> component) {
    onBind(c, mountedContent, component);
  }

  void unbind(ComponentContext c, Object mountedContent, Component<?> component) {
    onUnbind(c, mountedContent, component);
  }

  void unmount(ComponentContext c, Object mountedContent, Component<?> component) {
    onUnmount(c, mountedContent, component);
  }

  /**
   * Create a layout from the given component.
   *
   * @param context ComponentContext associated with the current ComponentTree.
   * @param component Component to process the layout for.
   * @param resolveNestedTree if the component's layout tree should be resolved as part of this
   *                          call.
   * @return New InternalNode associated with the given component.
   */
  ComponentLayout createLayout(
      ComponentContext context,
      Component<?> component,
      boolean resolveNestedTree) {
    final boolean deferNestedTreeResolution =
        Component.isNestedTree(component) && !resolveNestedTree;

    final TreeProps parentTreeProps = context.getTreeProps();
    populateTreeProps(component, parentTreeProps);
    context.setTreeProps(getTreePropsForChildren(context, component, parentTreeProps));

    ComponentsSystrace.beginSection("createLayout:" + component.getSimpleName());
    final InternalNode node;
    if (deferNestedTreeResolution) {
      node = ComponentsPools.acquireInternalNode(context, context.getResources());
      node.markIsNestedTreeHolder(context.getTreeProps());
    } else if (Component.isLayoutSpecWithSizeSpec(component)) {
      node = (InternalNode) onCreateLayoutWithSizeSpec(
          context,
          context.getWidthSpec(),
          context.getHeightSpec(),
          component);
    } else {
      node = (InternalNode) onCreateLayout(context, component);
    }

    ComponentsSystrace.endSection();

    if (node == null) {
      return ComponentContext.NULL_LAYOUT;
    }

    // Set component on the root node of the generated tree so that the mount calls use
    // those (see Controller.mountNodeTree()). Handle the case where the component simply
    // delegates its layout creation to another component i.e. the root node belongs to
    // another component.
    if (node.getComponent() == null) {
      node.setComponent(component);
      node.setBaselineFunction(sBaselineFunction);

      final boolean isMountSpecWithMeasure = canMeasure() && Component.isMountSpec(component);

      if (isMountSpecWithMeasure || deferNestedTreeResolution) {
        node.setMeasureFunction(sMeasureFunction);
      }
    }

    if (!deferNestedTreeResolution) {
      onPrepare(context, component);
    }

    if (context.getTreeProps() != parentTreeProps) {
      ComponentsPools.release(context.getTreeProps());
      context.setTreeProps(parentTreeProps);
    }

    return node;
  }

  void loadStyle(
      ComponentContext c,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      Component<?> component) {
    c.setDefStyle(defStyleAttr, defStyleRes);
    onLoadStyle(c, component);
    c.setDefStyle(0, 0);
  }

  void loadStyle(ComponentContext c, Component<?> component) {
    onLoadStyle(c, component);
  }

  protected Output acquireOutput() {
    return ComponentsPools.acquireOutput();
  }

  protected void releaseOutput(Output output) {
    ComponentsPools.release(output);
  }

  protected final <T> Diff<T> acquireDiff(T previousValue, T nextValue) {
    Diff<T> diff =  ComponentsPools.acquireDiff();
    diff.setPrevious(previousValue);
    diff.setNext(nextValue);

    return diff;
  }

  protected void releaseDiff(Diff diff) {
    ComponentsPools.release(diff);
  }

  /**
   * Retrieves all of the tree props used by this Component from the TreeProps map
   * and sets the tree props as fields on the ComponentImpl.
   */
  protected void populateTreeProps(Component<?> component, TreeProps parentTreeProps) {
  }

  /**
   * Updates the TreeProps map with outputs from all {@link OnCreateTreeProp} methods.
   */
  protected TreeProps getTreePropsForChildren(
      ComponentContext c,
      Component<?> component,
      TreeProps previousTreeProps) {
    return previousTreeProps;
  }

  /**
   * Generate a tree of {@link ComponentLayout} representing the layout structure of
   * the {@link Component} and its sub-components. You should use
   * {@link ComponentContext#newLayoutBuilder} to build the layout tree.
   *
   * @param c The {@link ComponentContext} to build a {@link ComponentLayout} tree.
   * @param component The component to create the {@link ComponentLayout} tree from.
   */
  protected ComponentLayout onCreateLayout(ComponentContext c, Component<?> component) {
    return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START).build();
  }

  protected ComponentLayout onCreateLayoutWithSizeSpec(
      ComponentContext c,
      int widthSpec,
      int heightSpec,
      Component<?> component) {
    return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START).build();
  }

  protected void onPrepare(ComponentContext c, Component<?> component) {
    // do nothing, by default
  }

  protected void onLoadStyle(ComponentContext c, Component<?> component) {
  }

  /**
   * Called after the layout calculation is finished and the given {@link ComponentLayout}
   * has its bounds defined. You can use {@link ComponentLayout#getX()},
   * {@link ComponentLayout#getY()}, {@link ComponentLayout#getWidth()}, and
   * {@link ComponentLayout#getHeight()} to get the size and position of the component
   * in the layout tree.
   *
   * @param c The {@link Context} used by this component.
   * @param layout The {@link ComponentLayout} with defined position and size.
   * @param component The {@link Component} for this component.
   */
  protected void onBoundsDefined(
      ComponentContext c,
      ComponentLayout layout,
      Component<?> component) {
  }

  /**
   * Called during layout calculation to determine the baseline of a component.
   *
   * @param c The {@link Context} used by this component.
   * @param width The width of this component.
   * @param height The height of this component.
   */
  protected int onMeasureBaseline(ComponentContext c, int width, int height) {
    return height;
  }

  /**
   * Whether this {@link ComponentLifecycle} is able to measure itself according
   * to specific size constraints.
   */
  protected boolean canMeasure() {
    return false;
  }

  protected void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      Component<?> component) {
    throw new IllegalStateException(
        "You must override onMeasure() if you return true in canMeasure(), " +
            "ComponentLifecycle is: " + component.getLifecycle());
  }

  /**
   * Whether this {@link ComponentLifecycle} mounts views that contain component-based
   * content that can be incrementally mounted e.g. if the mounted view has a
   * ComponentView with incremental mount enabled.
   */
  protected boolean canMountIncrementally() {
    return false;
  }

  /**
   * Whether this drawable mount spec should cache its drawing in a display list.
   */
  protected boolean shouldUseDisplayList() {
    return false;
  }

  /**
   * Create the object that will be mounted in the {@link ComponentView}.
   *
   * @param context The {@link ComponentContext} to be used to create the content.
   * @return an Object that can be mounted for this component.
   */
  protected Object onCreateMountContent(ComponentContext context) {
    throw new RuntimeException(
        "Trying to mount a MountSpec that doesn't implement @OnCreateMountContent");
