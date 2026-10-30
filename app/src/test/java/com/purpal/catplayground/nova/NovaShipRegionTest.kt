package com.purpal.catplayground.nova

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class NovaShipRegionTest {
    @Test fun approvedSheetSplitExcludesOrangeWingFromBlackAndRestoresOrangeWingtip() {
        val path="src/main/assets/nova/approved-top-view.png"
        val file=listOf(File(path),File("app/$path")).first{it.isFile}
        // Android's compile bootclasspath omits java.desktop; the JVM test runner supplies it.
        val sheet=Class.forName("javax.imageio.ImageIO").getMethod("read",File::class.java).invoke(null,file)
        val width=sheet.javaClass.getMethod("getWidth").invoke(sheet) as Int
        val height=sheet.javaClass.getMethod("getHeight").invoke(sheet) as Int
        val getRGB=sheet.javaClass.getMethod("getRGB",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType)
        fun alpha(x:Int,y:Int)=(getRGB.invoke(sheet,x,y) as Int).ushr(24)
        val orange=NovaShipRegion.window(true,width)
        val black=NovaShipRegion.window(false,width)
        // Regression fixture: the reported foreign object is real opaque orange wing art.
        var fragment=0
        for(y in 514..740) for(x in 768..798) if(alpha(x,y)>40) fragment++
        assertTrue(fragment>3000)
        assertTrue(black.x>798)
        assertTrue(orange.x+orange.width>798)
        assertTrue(black.x<=806) // retains the black aircraft's leftmost solid pixels
        for(y in 0 until height) assertTrue("split must lie in the transparent gutter",alpha(black.x,y)<=40)
        assertEquals(width,orange.width+black.width)
        assertEquals(orange.width,black.x)
    }
}
