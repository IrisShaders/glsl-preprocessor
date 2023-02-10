package org.anarres.cpp.test_util;

import java.security.MessageDigest;
import java.util.Base64;

public class AssertUtil {
	public static String getBase64Hash(String content) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		digest.update(content.getBytes());
		return Base64.getEncoder().encodeToString(digest.digest());
	}
}
