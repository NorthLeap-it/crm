package it.northleap.backend.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeUrlValidatorTest {

    @Test
    void allowsPublicHttpsUrl() {
        assertTrue(SafeUrlValidator.isSafe("https://example.com/webhook"));
    }

    @Test
    void allowsPublicHttpUrl() {
        assertTrue(SafeUrlValidator.isSafe("http://example.com/webhook"));
    }

    @Test
    void blocksNonHttpScheme() {
        assertFalse(SafeUrlValidator.isSafe("file:///etc/passwd"));
        assertFalse(SafeUrlValidator.isSafe("ftp://example.com/file"));
    }

    @Test
    void blocksMalformedUrl() {
        assertFalse(SafeUrlValidator.isSafe("not a url"));
        assertFalse(SafeUrlValidator.isSafe(""));
    }

    @Test
    void blocksLocalhostAndLoopback() {
        assertFalse(SafeUrlValidator.isSafe("http://localhost/secrets"));
        assertFalse(SafeUrlValidator.isSafe("http://0.0.0.0/secrets"));
        assertFalse(SafeUrlValidator.isSafe("http://127.0.0.1/secrets"));
    }

    @Test
    void blocksLocalAndInternalSuffixes() {
        assertFalse(SafeUrlValidator.isSafe("http://service.local/api"));
        assertFalse(SafeUrlValidator.isSafe("http://service.internal/api"));
    }

    @Test
    void blocksPrivateIpv4Ranges() {
        assertFalse(SafeUrlValidator.isSafe("http://10.0.0.5/api"));
        assertFalse(SafeUrlValidator.isSafe("http://192.168.1.1/api"));
        assertFalse(SafeUrlValidator.isSafe("http://172.16.0.1/api"));
        assertFalse(SafeUrlValidator.isSafe("http://172.31.255.255/api"));
        assertTrue(SafeUrlValidator.isSafe("http://172.32.0.1/api"));
        assertTrue(SafeUrlValidator.isSafe("http://172.15.255.255/api"));
    }

    @Test
    void blocksLinkLocalAndCloudMetadataEndpoint() {
        assertFalse(SafeUrlValidator.isSafe("http://169.254.169.254/latest/meta-data"));
        assertFalse(SafeUrlValidator.isSafe("http://169.254.0.1/api"));
    }
}
