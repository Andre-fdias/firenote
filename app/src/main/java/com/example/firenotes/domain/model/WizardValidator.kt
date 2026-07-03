package com.example.firenotes.domain.model

data class ChecklistItem(
    val title: String,
    val isComplete: Boolean,
    val targetStep: WizardStep,
    val errorDescription: String? = null
)

class WizardValidator {
    
    fun getChecklist(state: WizardState): List<ChecklistItem> {
        return listOf(
            ChecklistItem(
                title = "Número do Talão",
                isComplete = state.protocolo.isNotBlank(),
                targetStep = WizardStep.INITIAL_DATA,
                errorDescription = "Número do talão é obrigatório."
            ),
            ChecklistItem(
                title = "Endereço ou GPS",
                isComplete = state.latitude != null || (state.rua.isNotBlank() && state.cidade.isNotBlank()),
                targetStep = WizardStep.INITIAL_DATA,
                errorDescription = "Capturar GPS ou informar rua e cidade."
            ),
            ChecklistItem(
                title = "Natureza da Ocorrência",
                isComplete = true, // Nature is selected in step 2
                targetStep = WizardStep.NATURE_SELECTION
            ),
            ChecklistItem(
                title = "Viaturas e Militares",
                isComplete = state.viaturas.isNotEmpty() && state.viaturas.any { it.equipe.isNotEmpty() },
                targetStep = WizardStep.VIATURAS_EQUIPE,
                errorDescription = "Cadastrar ao menos uma viatura com militares."
            ),
            ChecklistItem(
                title = "Evidências e Fotos",
                isComplete = true,
                targetStep = WizardStep.EVIDENCIAS
            ),
            ChecklistItem(
                title = "Histórico Narrativo",
                isComplete = state.historico.isNotBlank(),
                targetStep = WizardStep.HISTORICO,
                errorDescription = "O histórico narrativo é obrigatório."
            )
        )
    }

    fun isWizardComplete(state: WizardState): Boolean {
        return getChecklist(state).all { it.isComplete }
    }
}
