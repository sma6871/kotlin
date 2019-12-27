/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettings;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.TestJdkKind;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;

public class SyntheticMethodForAnnotatedPropertyGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        CompilerConfiguration configuration = createConfiguration(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );

        CommonConfigurationKeysKt.setLanguageVersionSettings(configuration, new CompilerTestLanguageVersionSettings(
                Collections.singletonMap(
                        LanguageFeature.UseGetterNameForPropertyAnnotationsMethodOnJvm, LanguageFeature.State.ENABLED
                ),
                ApiVersion.LATEST_STABLE, LanguageVersion.LATEST_STABLE
        ));

        myEnvironment =
                KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
    }

    @NotNull
    @Override
    protected String getPrefix() {
        return "properties/syntheticMethod";
    }

    private static final String TEST_SYNTHETIC_METHOD_NAME = JvmAbi.getSyntheticMethodNameForAnnotatedProperty("getProperty");

    public void testInClass() {
        loadFile();
        assertAnnotatedSyntheticMethodExistence(true, generateClass("A"));
    }

    public void testTopLevel() {
        loadFile();
        Class<?> a = generateClass("TopLevelKt");
        assertAnnotatedSyntheticMethodExistence(true, a);
    }

    public void testInTrait() throws ClassNotFoundException {
        loadFile();
        GeneratedClassLoader loader = generateAndCreateClassLoader();
        assertAnnotatedSyntheticMethodExistence(false, loader.loadClass("T"));
        assertAnnotatedSyntheticMethodExistence(true, loader.loadClass("T" + JvmAbi.DEFAULT_IMPLS_SUFFIX));
    }

    private static void assertAnnotatedSyntheticMethodExistence(boolean expected, @NotNull Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (TEST_SYNTHETIC_METHOD_NAME.equals(method.getName())) {
                if (!expected) {
                    fail("Synthetic method for annotated property found, but not expected: " + method);
                }
                assertTrue(method.isSynthetic());
                int modifiers = method.getModifiers();
                assertTrue(Modifier.isStatic(modifiers));
                assertTrue(Modifier.isPublic(modifiers));

                Annotation[] annotations = method.getDeclaredAnnotations();
                assertSize(1, annotations);
                assertEquals("@SomeAnnotation(value=OK)", annotations[0].toString());
                return;
            }
        }
        if (expected) {
            fail("Synthetic method for annotated property expected, but not found: " + TEST_SYNTHETIC_METHOD_NAME);
        }
    }
}
