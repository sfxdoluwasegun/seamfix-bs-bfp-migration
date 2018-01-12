/*     */ package com.seamfix.kyc.bfp.proxy;
/*     */
/*     */ import java.io.ObjectStreamException;
/*     */ import java.io.Serializable;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.LinkedHashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */
/*     */ public class FingerReasonCodes
/*     */   implements Serializable, Comparable<Object>
/*     */ {
/*     */   private static final long serialVersionUID = -216512250114274272L;
/*  22 */   public static final FingerReasonCodes MISSING_RIGHT_THUMB = new FingerReasonCodes("MISSING RIGHT THUMB");
/*     */
/*  27 */   public static final FingerReasonCodes MISSING_LEFT_THUMB = new FingerReasonCodes("MISSING LEFT THUMB");
/*     */
/*  32 */   public static final FingerReasonCodes INJURED_RIGHT_THUMB = new FingerReasonCodes("INJURED RIGHT THUMB");
/*     */
/*  37 */   public static final FingerReasonCodes INJURED_LEFT_THUMB = new FingerReasonCodes("INJURED LEFT THUMB");
/*     */
/*  42 */   public static final FingerReasonCodes MISSING_RIGHT_INDEX = new FingerReasonCodes("MISSING RIGHT INDEX");
/*     */
/*  47 */   public static final FingerReasonCodes MISSING_LEFT_INDEX = new FingerReasonCodes("MISSING LEFT INDEX");
/*     */
/*  52 */   public static final FingerReasonCodes INJURED_RIGHT_INDEX = new FingerReasonCodes("INJURED RIGHT INDEX");
/*     */
/*  57 */   public static final FingerReasonCodes INJURED_LEFT_INDEX = new FingerReasonCodes("INJURED LEFT INDEX");
/*     */
/*  62 */   public static final FingerReasonCodes INSUFFICIENT_QUALITY_RIGHT_THUMB = new FingerReasonCodes("INSUFFICIENT QUALITY RIGHT THUMB");
/*     */
/*  67 */   public static final FingerReasonCodes INSUFFICIENT_QUALITY_LEFT_THUMB = new FingerReasonCodes("INSUFFICIENT QUALITY LEFT THUMB");
/*     */
/*  72 */   public static final FingerReasonCodes INSUFFICIENT_QUALITY_RIGHT_INDEX = new FingerReasonCodes("INSUFFICIENT QUALITY RIGHT INDEX");
/*     */
/*  77 */   public static final FingerReasonCodes INSUFFICIENT_QUALITY_LEFT_INDEX = new FingerReasonCodes("INSUFFICIENT QUALITY LEFT INDEX");
/*     */
/*  82 */   public static final FingerReasonCodes NOT_APPLICABLE = new FingerReasonCodes("NOT APPLICABLE");
/*     */
/*  87 */   public static final FingerReasonCodes NOT_NEEDED = new FingerReasonCodes("NOT NEEDED");
/*     */   private String value;
/* 214 */   private static final Map<String, FingerReasonCodes> values = new LinkedHashMap<String, FingerReasonCodes>(14, 1.0F);
/*     */
/* 281 */   private static List<String> literals = new ArrayList<String>();
/* 282 */   private static List<String> names = new ArrayList<String>();
/*     */
/* 217 */   private static List<FingerReasonCodes> valueList = new ArrayList<FingerReasonCodes>(14);
/*     */
/*     */   private FingerReasonCodes(String value)
/*     */   {
/*  93 */     this.value = value;
/*     */   }
/*     */
/*     */   protected FingerReasonCodes()
/*     */   {
/*     */   }
/*     */
/*     */   @Override
public String toString()
/*     */   {
/* 109 */     return String.valueOf(this.value);
/*     */   }
/*     */
/*     */   public static FingerReasonCodes fromString(String value)
/*     */   {
/* 119 */     FingerReasonCodes typeValue = values.get(value);
/* 120 */     if (typeValue == null)
/*     */     {
/* 122 */       throw new IllegalArgumentException("invalid value '" + value + "', possible values are: " + literals);
/*     */     }
/* 124 */     return typeValue;
/*     */   }
/*     */
/*     */   public String getValue()
/*     */   {
/* 134 */     return this.value;
/*     */   }
/*     */
/*     */   @Override
public int compareTo(Object that)
/*     */   {
/* 142 */     return this == that ? 0 : getValue().compareTo(((FingerReasonCodes)that).getValue());
/*     */   }
/*     */
/*     */   public static List<String> literals()
/*     */   {
/* 153 */     return literals;
/*     */   }
/*     */
/*     */   public static List<String> names()
/*     */   {
/* 165 */     return names;
/*     */   }
/*     */
/*     */   public static List<FingerReasonCodes> values()
/*     */   {
/* 175 */     return valueList;
/*     */   }
/*     */
/*     */   @Override
public boolean equals(Object object)
/*     */   {
/* 183 */     return (this == object) || (((object instanceof FingerReasonCodes)) && (((FingerReasonCodes)object).getValue().equals(getValue())));
/*     */   }
/*     */
/*     */   @Override
public int hashCode()
/*     */   {
/* 193 */     return getValue().hashCode();
/*     */   }
/*     */
/*     */   private Object readResolve()
/*     */     throws ObjectStreamException
/*     */   {
/* 211 */     return fromString(this.value);
/*     */   }
/*     */
/*     */   static
/*     */   {
/* 224 */     values.put(MISSING_RIGHT_THUMB.value, MISSING_RIGHT_THUMB);
/* 225 */     valueList.add(MISSING_RIGHT_THUMB);
/* 226 */     literals.add(MISSING_RIGHT_THUMB.value);
/* 227 */     names.add("MISSING_RIGHT_THUMB");
/* 228 */     values.put(MISSING_LEFT_THUMB.value, MISSING_LEFT_THUMB);
/* 229 */     valueList.add(MISSING_LEFT_THUMB);
/* 230 */     literals.add(MISSING_LEFT_THUMB.value);
/* 231 */     names.add("MISSING_LEFT_THUMB");
/* 232 */     values.put(INJURED_RIGHT_THUMB.value, INJURED_RIGHT_THUMB);
/* 233 */     valueList.add(INJURED_RIGHT_THUMB);
/* 234 */     literals.add(INJURED_RIGHT_THUMB.value);
/* 235 */     names.add("INJURED_RIGHT_THUMB");
/* 236 */     values.put(INJURED_LEFT_THUMB.value, INJURED_LEFT_THUMB);
/* 237 */     valueList.add(INJURED_LEFT_THUMB);
/* 238 */     literals.add(INJURED_LEFT_THUMB.value);
/* 239 */     names.add("INJURED_LEFT_THUMB");
/* 240 */     values.put(MISSING_RIGHT_INDEX.value, MISSING_RIGHT_INDEX);
/* 241 */     valueList.add(MISSING_RIGHT_INDEX);
/* 242 */     literals.add(MISSING_RIGHT_INDEX.value);
/* 243 */     names.add("MISSING_RIGHT_INDEX");
/* 244 */     values.put(MISSING_LEFT_INDEX.value, MISSING_LEFT_INDEX);
/* 245 */     valueList.add(MISSING_LEFT_INDEX);
/* 246 */     literals.add(MISSING_LEFT_INDEX.value);
/* 247 */     names.add("MISSING_LEFT_INDEX");
/* 248 */     values.put(INJURED_RIGHT_INDEX.value, INJURED_RIGHT_INDEX);
/* 249 */     valueList.add(INJURED_RIGHT_INDEX);
/* 250 */     literals.add(INJURED_RIGHT_INDEX.value);
/* 251 */     names.add("INJURED_RIGHT_INDEX");
/* 252 */     values.put(INJURED_LEFT_INDEX.value, INJURED_LEFT_INDEX);
/* 253 */     valueList.add(INJURED_LEFT_INDEX);
/* 254 */     literals.add(INJURED_LEFT_INDEX.value);
/* 255 */     names.add("INJURED_LEFT_INDEX");
/* 256 */     values.put(INSUFFICIENT_QUALITY_RIGHT_THUMB.value, INSUFFICIENT_QUALITY_RIGHT_THUMB);
/* 257 */     valueList.add(INSUFFICIENT_QUALITY_RIGHT_THUMB);
/* 258 */     literals.add(INSUFFICIENT_QUALITY_RIGHT_THUMB.value);
/* 259 */     names.add("INSUFFICIENT_QUALITY_RIGHT_THUMB");
/* 260 */     values.put(INSUFFICIENT_QUALITY_LEFT_THUMB.value, INSUFFICIENT_QUALITY_LEFT_THUMB);
/* 261 */     valueList.add(INSUFFICIENT_QUALITY_LEFT_THUMB);
/* 262 */     literals.add(INSUFFICIENT_QUALITY_LEFT_THUMB.value);
/* 263 */     names.add("INSUFFICIENT_QUALITY_LEFT_THUMB");
/* 264 */     values.put(INSUFFICIENT_QUALITY_RIGHT_INDEX.value, INSUFFICIENT_QUALITY_RIGHT_INDEX);
/* 265 */     valueList.add(INSUFFICIENT_QUALITY_RIGHT_INDEX);
/* 266 */     literals.add(INSUFFICIENT_QUALITY_RIGHT_INDEX.value);
/* 267 */     names.add("INSUFFICIENT_QUALITY_RIGHT_INDEX");
/* 268 */     values.put(INSUFFICIENT_QUALITY_LEFT_INDEX.value, INSUFFICIENT_QUALITY_LEFT_INDEX);
/* 269 */     valueList.add(INSUFFICIENT_QUALITY_LEFT_INDEX);
/* 270 */     literals.add(INSUFFICIENT_QUALITY_LEFT_INDEX.value);
/* 271 */     names.add("INSUFFICIENT_QUALITY_LEFT_INDEX");
/* 272 */     values.put(NOT_APPLICABLE.value, NOT_APPLICABLE);
/* 273 */     valueList.add(NOT_APPLICABLE);
/* 274 */     literals.add(NOT_APPLICABLE.value);
/* 275 */     names.add("NOT_APPLICABLE");
/* 276 */     values.put(NOT_NEEDED.value, NOT_NEEDED);
/* 277 */     valueList.add(NOT_NEEDED);
/* 278 */     literals.add(NOT_NEEDED.value);
/* 279 */     names.add("NOT_NEEDED");
/* 280 */     valueList = Collections.unmodifiableList(valueList);
/*     */   }
/*     */ }

/* Location:           C:\workspace\deploy\BiocaptureSmartClient\lib\biocapture-common-1.0-SNAPSHOT.jar
 * Qualified Name:     com.sf.biocapture.verified.entity.wsqimage.FingerReasonCodes
 * JD-Core Version:    0.6.2
 */