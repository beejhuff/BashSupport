/*
 * Copyright (c) Joachim Ansorg, mail@ansorg-it.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ansorgit.plugins.bash.editor.inspections.inspections;

import com.ansorgit.plugins.bash.editor.inspections.quickfix.RegisterGlobalVariableQuickfix;
import com.ansorgit.plugins.bash.editor.inspections.quickfix.UnregisterGlobalVariableQuickfix;
import com.ansorgit.plugins.bash.lang.psi.BashVisitor;
import com.ansorgit.plugins.bash.lang.psi.api.BashReference;
import com.ansorgit.plugins.bash.lang.psi.api.vars.BashVar;
import com.ansorgit.plugins.bash.settings.BashProjectSettings;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * This inspection marks unresolved variables.
 * <br>
 * @author jansorg
 */
public class UnresolvedVariableInspection extends LocalInspectionTool {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new UnresolvedVariableVisitor(holder);
    }

    private static final class UnresolvedVariableVisitor extends BashVisitor {
        private final ProblemsHolder holder;
        private Set<String> globalVariables;

        public UnresolvedVariableVisitor(ProblemsHolder holder) {
            this.holder = holder;
            this.globalVariables = BashProjectSettings.storedSettings(holder.getProject()).getGlobalVariables();
        }

        @Override
        public void visitVarUse(BashVar bashVar) {
            if (!bashVar.isBuiltinVar()) {
                BashReference ref = bashVar.getReference();

                PsiElement resolved = ref.resolve();
                if (resolved == null) {
                    String varName = ref.getReferencedName();

                    boolean isRegisteredAsGlobal = globalVariables.contains(varName);
                    if (isRegisteredAsGlobal) {
                        holder.registerProblem(bashVar, "This variable is currently registered as a global variable",
                                ProblemHighlightType.INFORMATION,
                                ref.getRangeInElement(),
                                new UnregisterGlobalVariableQuickfix(bashVar));
                    } else {
                        holder.registerProblem(bashVar,
                                "Unresolved variable",
                                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                                ref.getRangeInElement(),
                                new RegisterGlobalVariableQuickfix(bashVar));
                    }
                }
            }
        }
    }
}
