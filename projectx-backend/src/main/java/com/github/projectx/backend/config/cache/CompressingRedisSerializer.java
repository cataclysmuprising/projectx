package com.github.projectx.backend.config.cache;

import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Redis serializer decorator that applies GZIP compression for large payloads.
 * <p>
 * A one-byte prefix is written to payloads so deserialization can distinguish:
 * - raw payload
 * - compressed payload
 * - legacy payloads without flag byte
 */
public class CompressingRedisSerializer implements RedisSerializer<Object> {
	private static final byte FLAG_RAW = 0;
	private static final byte FLAG_COMPRESSED = 1;

	private final RedisSerializer<Object> delegate;
	private final int minSize;

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public CompressingRedisSerializer(RedisSerializer<Object> delegate, int minSize) {
		this.delegate = delegate;
		this.minSize = minSize;
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Override
	public byte[] serialize(@Nullable Object value) throws SerializationException {
		byte[] raw = delegate.serialize(value);
		if (raw.length == 0) {
			return raw;
		}

		if (raw.length < minSize) {
			return withFlag(FLAG_RAW, raw);
		}

		byte[] compressed = compress(raw);
		if (compressed.length >= raw.length) {
			return withFlag(FLAG_RAW, raw);
		}
		return withFlag(FLAG_COMPRESSED, compressed);
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Override
	public @Nullable Object deserialize(byte @Nullable [] bytes) throws SerializationException {
		if (bytes == null || bytes.length == 0) {
			return null;
		}

		byte flag = bytes[0];
		byte[] payload = slice(bytes, 1);

		if (flag == FLAG_COMPRESSED) {
			return delegate.deserialize(decompress(payload));
		}
		if (flag == FLAG_RAW) {
			return delegate.deserialize(payload);
		}

		return delegate.deserialize(bytes);
	}

	private byte[] withFlag(byte flag, byte[] payload) {
		byte[] out = new byte[payload.length + 1];
		out[0] = flag;
		System.arraycopy(payload, 0, out, 1, payload.length);
		return out;
	}

	private byte[] slice(byte[] src, int start) {
		int len = src.length - start;
		byte[] out = new byte[len];
		System.arraycopy(src, start, out, 0, len);
		return out;
	}

	private byte[] compress(byte[] input) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
		     GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
			gzip.write(input);
			gzip.finish();
			return baos.toByteArray();
		}
		catch (IOException e) {
			throw new SerializationException("Redis cache compression failed", e);
		}
	}

	private byte[] decompress(byte[] input) {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(input);
		     GZIPInputStream gzip = new GZIPInputStream(bais);
		     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[4096];
			int read;
			while ((read = gzip.read(buffer)) > 0) {
				baos.write(buffer, 0, read);
			}
			return baos.toByteArray();
		}
		catch (IOException e) {
			throw new SerializationException("Redis cache decompression failed", e);
		}
	}
}
