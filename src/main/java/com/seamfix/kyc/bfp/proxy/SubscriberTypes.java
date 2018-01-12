package com.seamfix.kyc.bfp.proxy;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class SubscriberTypes implements Serializable, Comparable {

	private static final long serialVersionUID = -6495987021277454458L;

	public static final SubscriberTypes PREPAID = new SubscriberTypes("PREPAID");

	public static final SubscriberTypes POSTPAID = new SubscriberTypes("POSTPAID");
	private String value;
	private static final Map<String, SubscriberTypes> values = new LinkedHashMap<String, SubscriberTypes>(2, 1.0F);

	private static List<String> literals = new ArrayList<String>();
	private static List<String> names = new ArrayList<String>();

	private static List<SubscriberTypes> valueList = new ArrayList<SubscriberTypes>(2);

	private SubscriberTypes(String value) {
		this.value = value;
	}

	protected SubscriberTypes() {

	}

	@Override
	public String toString() {
		return String.valueOf(this.value);
	}

	public static SubscriberTypes fromString(String value) {
		SubscriberTypes typeValue = values.get(value);
		if (typeValue == null) {
			throw new IllegalArgumentException("invalid value '" + value + "', possible values are: " + literals);
		}
		return typeValue;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public int compareTo(Object that) {
		return this == that ? 0 : getValue().compareTo(((SubscriberTypes) that).getValue());
	}

	public static List<String> literals() {
		return literals;
	}

	public static List<String> names() {
		return names;
	}

	public static List<SubscriberTypes> values() {
		return valueList;
	}

	@Override
	public boolean equals(Object object) {
		return (this == object) || (((object instanceof SubscriberTypes))
				&& (((SubscriberTypes) object).getValue().equals(getValue())));
	}

	@Override
	public int hashCode() {
		return getValue().hashCode();
	}

	private Object readResolve() throws ObjectStreamException {
		return fromString(this.value);
	}

	static {
		values.put(PREPAID.value, PREPAID);
		valueList.add(PREPAID);
		literals.add(PREPAID.value);
		names.add("PREPAID");
		values.put(POSTPAID.value, POSTPAID);
		valueList.add(POSTPAID);
		literals.add(POSTPAID.value);
		names.add("POSTPAID");
		valueList = Collections.unmodifiableList(valueList);
	}

}
