/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.litho.specmodels.model;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Model that is an abstract representation of a {@link com.facebook.litho.annotations.LayoutSpec}.
 */
public class LayoutSpecModel implements SpecModel, HasPureRender {
  private final SpecModelImpl mSpecModel;
  private final boolean mIsPureRender;
  private final SpecGenerator<LayoutSpecModel> mLayoutSpecGenerator;

  public LayoutSpecModel(
      String qualifiedSpecClassName,
      String componentClassName,
      ImmutableList<SpecMethodModel<DelegateMethod, Void>> delegateMethods,
      ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> eventMethods,
      ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> triggerMethods,
      SpecMethodModel<EventMethod, Void> workingRangeRegisterMethod,
      ImmutableList<WorkingRangeMethodModel> workingRangeMethods,
      ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> updateStateMethods,
      ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> updateStateWithTransitionMethods,
      ImmutableList<String> cachedPropNames,
      ImmutableList<PropDefaultModel> propDefaults,
      ImmutableList<CommonPropDefaultModel> commonPropDefaults,
      ImmutableList<EventDeclarationModel> eventDeclarations,
      ImmutableList<AnnotationSpec> classAnnotations,
      ImmutableList<TagModel> tags,
      String classJavadoc,
      ImmutableList<PropJavadocModel> propJavadocs,
      boolean isPublic,
      DependencyInjectionHelper dependencyInjectionHelper,
      boolean isPureRender,
      SpecElementType specElementType,
      Object representedObject,
      SpecGenerator<LayoutSpecModel> layoutSpecGenerator,
      ImmutableList<TypeVariableName> typeVariables) {
    mSpecModel =
        SpecModelImpl.newBuilder()
            .qualifiedSpecClassName(qualifiedSpecClassName)
            .componentClassName(componentClassName)
            .componentClass(ClassNames.COMPONENT)
            .delegateMethods(delegateMethods)
            .eventMethods(eventMethods)
            .triggerMethods(triggerMethods)
            .workingRangeRegisterMethod(workingRangeRegisterMethod)
            .workingRangeMethods(workingRangeMethods)
            .updateStateMethods(updateStateMethods)
            .updateStateWithTransitionMethods(updateStateWithTransitionMethods)
            .cachedPropNames(cachedPropNames)
            .typeVariables(typeVariables)
            .propDefaults(propDefaults)
            .commonPropDefaults(commonPropDefaults)
            .eventDeclarations(eventDeclarations)
            .classAnnotations(classAnnotations)
            .tags(tags)
            .classJavadoc(classJavadoc)
            .propJavadocs(propJavadocs)
            .isPublic(isPublic)
            .dependencyInjectionHelper(dependencyInjectionHelper)
            .specElementType(specElementType)
            .representedObject(representedObject)
            .build();
    mIsPureRender = isPureRender;
    mLayoutSpecGenerator = layoutSpecGenerator;
  }

  @Override
  public String getSpecName() {
    return mSpecModel.getSpecName();
  }

  @Override
  public TypeName getSpecTypeName() {
    return mSpecModel.getSpecTypeName();
  }

  @Override
  public String getComponentName() {
    return mSpecModel.getComponentName();
  }

  @Override
  public TypeName getComponentTypeName() {
    return mSpecModel.getComponentTypeName();
  }

  @Override
  public ImmutableList<SpecMethodModel<DelegateMethod, Void>> getDelegateMethods() {
    return mSpecModel.getDelegateMethods();
  }

  @Override
  public ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> getEventMethods() {
    return mSpecModel.getEventMethods();
  }

  @Override
  public ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> getTriggerMethods() {
    return mSpecModel.getTriggerMethods();
  }

  @Override
  @Nullable
  public SpecMethodModel<EventMethod, Void> getWorkingRangeRegisterMethod() {
    return mSpecModel.getWorkingRangeRegisterMethod();
  }

  @Override
  public ImmutableList<WorkingRangeMethodModel> getWorkingRangeMethods() {
    return mSpecModel.getWorkingRangeMethods();
  }

  @Override
  public ImmutableList<SpecMethodModel<UpdateStateMethod, Void>> getUpdateStateMethods() {
    return mSpecModel.getUpdateStateMethods();
  }

  @Override
  public ImmutableList<SpecMethodModel<UpdateStateMethod, Void>>
      getUpdateStateWithTransitionMethods() {
    return mSpecModel.getUpdateStateWithTransitionMethods();
  }

  @Override
  public ImmutableList<PropModel> getRawProps() {
    return mSpecModel.getRawProps();
  }

  @Override
  public ImmutableList<PropModel> getProps() {
    return mSpecModel.getProps();
  }

  @Override
  public ImmutableList<InjectPropModel> getRawInjectProps() {
    return mSpecModel.getRawInjectProps();
  }

  @Override
  public ImmutableList<InjectPropModel> getInjectProps() {
    return mSpecModel.getInjectProps();
  }

  @Override
  public ImmutableList<PropDefaultModel> getPropDefaults() {
    return mSpecModel.getPropDefaults();
  }

  @Override
  public ImmutableList<CommonPropDefaultModel> getCommonPropDefaults() {
    return mSpecModel.getCommonPropDefaults();
  }

  @Override
  public ImmutableList<TypeVariableName> getTypeVariables() {
    return mSpecModel.getTypeVariables();
  }

  @Override
  public ImmutableList<StateParamModel> getStateValues() {
    return mSpecModel.getStateValues();
  }

  @Override
  public ImmutableList<InterStageInputParamModel> getInterStageInputs() {
    return mSpecModel.getInterStageInputs();
  }

  @Override
  public ImmutableList<TreePropModel> getTreeProps() {
    return mSpecModel.getTreeProps();
  }

  @Override
  public ImmutableList<EventDeclarationModel> getEventDeclarations() {
    return mSpecModel.getEventDeclarations();
  }

  @Override
  public ImmutableList<BuilderMethodModel> getExtraBuilderMethods() {
    return mSpecModel.getExtraBuilderMethods();
  }

  @Override
  public ImmutableList<RenderDataDiffModel> getRenderDataDiffs() {
    return mSpecModel.getRenderDataDiffs();
  }

  @Override
  public ImmutableList<AnnotationSpec> getClassAnnotations() {
    return mSpecModel.getClassAnnotations();
  }

  @Override
  public ImmutableList<TagModel> getTags() {
    return mSpecModel.getTags();
  }

  @Override
  public String getClassJavadoc() {
    return mSpecModel.getClassJavadoc();
  }

  @Override
  public ImmutableList<PropJavadocModel> getPropJavadocs() {
    return mSpecModel.getPropJavadocs();
  }

  @Override
  public boolean isPublic() {
    return mSpecModel.isPublic();
  }

  @Override
  public ClassName getContextClass() {
    return ClassNames.COMPONENT_CONTEXT;
  }

  @Override
  public ClassName getComponentClass() {
    return mSpecModel.getComponentClass();
  }

  @Override
  public ClassName getStateContainerClass() {
    return ClassNames.STATE_CONTAINER_COMPONENT;
  }

  @Override
  public ClassName getTransitionClass() {
    return ClassNames.TRANSITION;
  }

  @Override
  public ClassName getTransitionContainerClass() {
    return ClassNames.TRANSITION_CONTAINER;
  }

  @Override
  public TypeName getUpdateStateInterface() {
    return ClassNames.COMPONENT_STATE_UPDATE;
  }

  @Override
  public String getScopeMethodName() {
    return "getComponentScope";
  }

  @Override
  public boolean isStylingSupported() {
    return true;
  }

  @Override
  public boolean hasInjectedDependencies() {
    return mSpecModel.hasInjectedDependencies();
  }

  @Override
  public boolean shouldCheckIdInIsEquivalentToMethod() {
    return true;
  }

  @Override
  public boolean hasDeepCopy() {
    return false;
  }

  @Override
  public boolean shouldGenerateHasState() {
    return true;
  }

  @Override
  public boolean shouldGenerateCopyMethod() {
    return true;
  }

  @Override
  public DependencyInjectionHelper getDependencyInjectionHelper() {
    return mSpecModel.getDependencyInjectionHelper();
  }

  @Override
  public SpecElementType getSpecElementType() {
    return mSpecModel.getSpecElementType();
  }

  @Override
  public Object getRepresentedObject() {
    return mSpecModel.getRepresentedObject();
  }

  @Override
  public List<SpecModelValidationError> validate(RunMode runMode) {
    return SpecModelValidation.validateLayoutSpecModel(this, runMode);
  }

  @Override
  public TypeSpec generate() {
    return mLayoutSpecGenerator.generate(this);
  }

  @Override
  public boolean isPureRender() {
    return mIsPureRender;
  }

  @Override
  public String toString() {
    return "LayoutSpecModel{"
        + "mSpecModel="
        + mSpecModel
        + ", mIsPureRender="
        + mIsPureRender
        + ", mLayoutSpecGenerator="
        + mLayoutSpecGenerator
        + '}';
  }
}
