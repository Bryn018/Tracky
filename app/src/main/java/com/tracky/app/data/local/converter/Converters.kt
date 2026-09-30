package com.tracky.app.data.local.converter

/**
 * Intentionally empty.
 *
 * Every money column is an integer count of shilling cents declared directly
 * on the entity (`amountCents`, `balanceCents`, `monthlyLimitCents`), and
 * transaction type is a plain String holding the enum name — so Room needs no
 * converters. An earlier revision stored a `Money` value class on the entity
 * and converted it here, which Room 2.6.1's KSP processor cannot resolve for
 * entity fields; the unit now lives in the column name instead, and
 * [com.tracky.app.data.model.Money] is used above the data layer.
 */
class Converters
