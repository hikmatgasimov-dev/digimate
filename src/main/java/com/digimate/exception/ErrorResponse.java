package com.digimate.exception;

import java.time.Instant;

/**
 * The one JSON shape that every error returns, so clients and tests always know what to expect.
 */
public record ErrorResponse(Instant timestamp, int status, String message, String path) {
}