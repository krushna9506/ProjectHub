package com.projecthub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.projecthub.utils.SafeUrlUtils;

import org.junit.Test;

public class SafeUrlUtilsTest {

    @Test
    public void isSafeHttpUrl_allowsOnlyValidHttpAndHttpsWithHost() {
        assertTrue(SafeUrlUtils.isSafeHttpUrl("https://github.com/krushna9506/ProjectHub"));
        assertTrue(SafeUrlUtils.isSafeHttpUrl("http://meet.jit.si/ProjectHub_123"));
        assertTrue(SafeUrlUtils.isSafeHttpUrl("  https://example.edu/docs/spec.pdf  "));

        assertFalse(SafeUrlUtils.isSafeHttpUrl("javascript:alert(1)"));
        assertFalse(SafeUrlUtils.isSafeHttpUrl("JAVASCRIPT:alert(1)"));
        assertFalse(SafeUrlUtils.isSafeHttpUrl("file:///etc/passwd"));
        assertFalse(SafeUrlUtils.isSafeHttpUrl("content://com.android.contacts/data"));
        assertFalse(SafeUrlUtils.isSafeHttpUrl("intent://scan/#Intent;end"));
        assertFalse(SafeUrlUtils.isSafeHttpUrl("data:text/html,<script>alert(1)</script>"));
        assertFalse(SafeUrlUtils.isSafeHttpUrl("https:///nohost"));
        assertFalse(SafeUrlUtils.isSafeHttpUrl(""));
        assertFalse(SafeUrlUtils.isSafeHttpUrl(null));
    }

    @Test
    public void normalizeSafeHttpUrl_addsHttpsOnlyForSchemelessDomains() {
        assertEquals("https://figma.com/file/abc", SafeUrlUtils.normalizeSafeHttpUrl("figma.com/file/abc"));
        assertEquals("https://github.com/a/b", SafeUrlUtils.normalizeSafeHttpUrl("https://github.com/a/b"));
        assertEquals("", SafeUrlUtils.normalizeSafeHttpUrl("javascript:alert(document.cookie)"));
        assertEquals("", SafeUrlUtils.normalizeSafeHttpUrl("file:///sdcard/secret"));
        assertEquals("", SafeUrlUtils.normalizeSafeHttpUrl("//evil.com/phish"));
    }
}
