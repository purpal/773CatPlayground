package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import org.junit.Assert.*
import org.junit.Test

class NovaDialogueTest {
    @Test fun defeatPreviewUsesRealOutroAndClearTransitions() {
        val world=World(Stages.dog,CatType.BLACK,1800f)
        NovaPreview.outro(world)
        assertEquals(Phase.OUTRO_DIALOG,world.phase)
        assertFalse(world.running)
        assertTrue(world.score>0)
        world.dialogFinished()
        assertEquals(Phase.CLEAR,world.phase)
    }

    @Test fun novaScriptsCoverEveryStageWithoutReplacingOriginalDialogue() {
        val original=Stages.dog.outro.map { it.text }
        for(stage in 1..13) for(cat in CatType.entries) {
            val script=novaOutro(Stages.get(stage)!!,cat)
            assertTrue(script.any { it.speaker==Speaker.BOSS })
            assertTrue(script.any { it.speaker==Speaker.CAT })
            assertTrue(script.all { it.text.isNotBlank() && '▼' !in it.text })
        }
        assertEquals(original,Stages.dog.outro.map { it.text })
        assertNotEquals(novaOutro(Stages.dog,CatType.ORANGE)[1].text,novaOutro(Stages.dog,CatType.BLACK)[1].text)
    }
}
