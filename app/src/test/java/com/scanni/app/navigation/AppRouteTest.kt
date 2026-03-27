package com.scanni.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteTest {
    @Test
    fun documentDetailCreate_buildsConcreteRoute() {
        assertEquals("document/42", AppRoute.DocumentDetail.create(42L))
    }
}
