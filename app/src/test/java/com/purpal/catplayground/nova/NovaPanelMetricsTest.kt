package com.purpal.catplayground.nova

import org.junit.Assert.*
import org.junit.Test

class NovaPanelMetricsTest {
    @Test fun hudReservesMetalBorderAcrossPhoneWidthsAndDensities() {
        val cuts=NovaPanelMetrics.cuts(640,156,true)
        for(widthDp in listOf(240f,320f,360f,480f,600f))for(density in listOf(1f,2f,3f)) {
            val width=widthDp*density
            val inset=NovaPanelMetrics.insets(width,640,cuts)
            val padding=4f*density
            assertTrue(width-2f*(inset.side+padding)>width*.7f)
            assertTrue(inset.top>inset.bottom)
            assertTrue(inset.side<=cuts.side*1.6f)
            // A text box starts beyond every painted corner, at any font-driven panel height.
            for(contentHeight in listOf(60f,90f,140f)) {
                val panelHeight=contentHeight*density+inset.top+inset.bottom+2f*padding
                assertTrue(panelHeight-inset.top-inset.bottom>=contentHeight*density)
            }
        }
    }
}
