package com.universalreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteAdapterRegistryTest {

    @Test
    fun registryIsLargeAndConsistent() {
        assertTrue(SiteAdapterRegistry.adapters.size >= 40)
        assertEquals(
            SiteAdapterRegistry.adapters.size,
            SiteAdapterRegistry.adapters.map { it.id }.distinct().size
        )
        SiteAdapterRegistry.adapters.forEach { adapter ->
            assertTrue(adapter.id, adapter.hosts.isNotEmpty())
            assertTrue(adapter.id, adapter.contentSelectors.isNotEmpty())
            adapter.hosts.forEach { host ->
                assertEquals(adapter.id, host.lowercase(), host)
            }
        }
    }

    @Test
    fun matchesExactHostAndSubdomains() {
        assertNotNull(SiteAdapterRegistry.forUrl("https://vnexpress.net/a"))
        assertEquals(
            "vnexpress",
            SiteAdapterRegistry.forUrl("https://e.vnexpress.net/a")?.id
        )
        assertEquals(
            "wikipedia",
            SiteAdapterRegistry.forUrl("https://vi.wikipedia.org/wiki/X")?.id
        )
        assertEquals(
            "ao3",
            SiteAdapterRegistry.forUrl("https://archiveofourown.org/works/1")?.id
        )
    }

    @Test
    fun unknownAndMalformedUrlsReturnNull() {
        assertNull(SiteAdapterRegistry.forUrl("https://example.org/a"))
        assertNull(SiteAdapterRegistry.forUrl("not a url"))
        assertNull(SiteAdapterRegistry.forUrl(""))
    }

    @Test
    fun adaptersCanBeLookedUpById() {
        assertEquals("royalroad", SiteAdapterRegistry.byId("royalroad")?.id)
        assertNull(SiteAdapterRegistry.byId("missing"))
    }
}
