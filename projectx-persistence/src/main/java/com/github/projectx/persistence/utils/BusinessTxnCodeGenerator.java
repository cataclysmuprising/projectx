package com.github.projectx.persistence.utils;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;

import java.io.Serial;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumSet;

public class BusinessTxnCodeGenerator implements BeforeExecutionGenerator {

	@Serial
	private static final long serialVersionUID = 8486544409953944909L;

	private static final SecureRandom RANDOM = new SecureRandom();

	private static String randomSuffix(int length, BusinessTxnCode.RandomType type) {
		if (length <= 0) {
			return "";
		}

		String chars;
		switch (type) {
			case NUMERIC -> chars = "0123456789";
			case ALPHA_UPPER -> chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
			default -> chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		}

		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
		}
		return sb.toString();
	}

	/**
	 * Exposes the resolved value through one documented accessor so callers avoid duplicating derivation logic.
	 */
	@Override
	public EnumSet<EventType> getEventTypes() {
		return EnumSet.of(EventType.INSERT);
	}

	/**
	 * Generates business transaction identifiers at INSERT time so code assignment is atomic with
	 * persistence and resilient to concurrent writers.
	 */
	@Override
	public Object generate(
			SharedSessionContractImplementor session,
			Object owner,
			Object currentValue,
			EventType eventType) {

		if (currentValue != null) {
			return currentValue;
		}

		Field field = Arrays.stream(owner.getClass().getDeclaredFields())
				.filter(f -> f.isAnnotationPresent(BusinessTxnCode.class))
				.findFirst()
				.orElseThrow(() ->
						new IllegalStateException("Exactly one @BusinessTxnCode required"));

		BusinessTxnCode cfg = field.getAnnotation(BusinessTxnCode.class);

		long seq;
		try (
				PreparedStatement ps =
						session.getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement(
										"select nextval('" + cfg.sequence() + "')"
								);
				ResultSet rs = ps.executeQuery()
		) {
			if (!rs.next()) {
				throw new IllegalStateException(
						"Failed to fetch nextval for sequence: " + cfg.sequence()
				);
			}
			seq = rs.getLong(1);
		}
		catch (SQLException e) {
			throw new IllegalStateException(
					"Error generating business transaction code using sequence: " + cfg.sequence(),
					e
			);
		}

		StringBuilder code = new StringBuilder();
		code.append(cfg.prefix()).append("-");

		if (cfg.includeDate()) {
			String date = DateTimeFormatter
					.ofPattern(cfg.datePattern())
					.format(LocalDate.now());
			code.append(date).append("-");
		}

		String padded = String.format("%0" + cfg.pad() + "d", seq);
		code.append(padded);

		String random = randomSuffix(cfg.randomLength(), cfg.randomType());
		if (!random.isEmpty()) {
			code.append("-").append(random);
		}

		return code.toString();
	}
}

