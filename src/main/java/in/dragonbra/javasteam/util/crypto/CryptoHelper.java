package in.dragonbra.javasteam.util.crypto;

import in.dragonbra.javasteam.util.Passable;
import in.dragonbra.javasteam.util.log.LogManager;
import in.dragonbra.javasteam.util.log.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * frenchpress shadow of JavaSteam's {@code CryptoHelper}.
 *
 * <p>Functionally identical to upstream 1.8.0, but uses only the stock JDK
 * security providers — no BouncyCastle. Two BC-flavored transformation strings
 * are replaced with their JDK spellings, verified to produce byte-identical
 * results for AES:
 * <ul>
 *   <li>{@code AES/CBC/PKCS7Padding} (BC) -&gt; {@code AES/CBC/PKCS5Padding} (JDK).
 *       For AES's 16-byte block these padding schemes are identical bytes.</li>
 *   <li>All {@code getInstance(algo, SEC_PROV)} calls drop the provider argument
 *       and use the default JDK provider search.</li>
 * </ul>
 *
 * <p>{@link #SEC_PROV} is retained only so other upstream classes that read it
 * ({@code DepotChunk}, {@code DepotManifest}) still link. frenchpress never
 * exercises those depot paths, and none of the methods here use it.
 */
public class CryptoHelper {
    private static final Logger logger = LogManager.getLogger(CryptoHelper.class);

    /**
     * Provider name used by JavaSteam's SteamAuthentication for
     * {@code Cipher.getInstance("RSA/None/PKCS1Padding", SEC_PROV)}. The stock JDK
     * has no provider that answers to the BC-spelled "None" mode, so we route it to
     * the frenchpress shim provider ({@code co.frenchpress.FrenchpressJceProvider},
     * registered via its ensureRegistered() before login), which delegates to the
     * JDK's "RSA/ECB/PKCS1Padding". Kept as a literal to avoid a crypto -> co.frenchpress
     * package dependency; must equal FrenchpressJceProvider.NAME.
     */
    public static final String SEC_PROV = "FrenchpressJCE";

    public static byte[] shaHash(MessageDigest digest, byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }
        if (digest == null) {
            throw new IllegalArgumentException("digest is null");
        }
        digest.reset();
        return digest.digest(input);
    }

    // NoSuchProviderException kept in the signature for binary compatibility with
    // upstream callers, even though the default-provider lookup never throws it.
    public static byte[] shaHash(byte[] input) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        return sha.digest(input);
    }

    public static byte[] generateRandomBlock(int size) {
        SecureRandom random = new SecureRandom();
        byte[] b = new byte[size];
        random.nextBytes(b);
        return b;
    }

    public static byte[] crcHash(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }
        CRC32 crc = new CRC32();
        crc.update(input, 0, input.length);
        int hash = (int) crc.getValue();
        // Upstream writes via BinaryWriter.writeInt, which is little-endian.
        return new byte[]{
                (byte) (hash & 0xFF),
                (byte) ((hash >>> 8) & 0xFF),
                (byte) ((hash >>> 16) & 0xFF),
                (byte) ((hash >>> 24) & 0xFF),
        };
    }

    public static byte[] symmetricDecrypt(byte[] input, byte[] key) throws CryptoException {
        return symmetricDecrypt(input, key, new Passable<>());
    }

    public static byte[] symmetricDecrypt(byte[] input, byte[] key, Passable<byte[]> iv) throws CryptoException {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (key.length != 32) {
            throw new IllegalArgumentException("SymmetricDecrypt used with non 32 byte key!");
        }
        try {
            // Step 1: the first 16 bytes are the IV, itself AES/ECB-encrypted with the key.
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            byte[] cryptedIv = Arrays.copyOfRange(input, 0, 16);
            byte[] cipherText = Arrays.copyOfRange(input, cryptedIv.length, input.length);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
            iv.setValue(cipher.doFinal(cryptedIv));
            // Step 2: decrypt the body in CBC mode using the recovered IV.
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, (Key) new SecretKeySpec(key, "AES"), new IvParameterSpec(iv.getValue()));
            return cipher.doFinal(cipherText);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException
                 | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            throw new CryptoException("failed to symmetric decrypt", e);
        }
    }

    public static byte[] symmetricEncryptWithIV(byte[] input, byte[] key, byte[] iv) throws CryptoException {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (iv == null) {
            throw new IllegalArgumentException("iv is null");
        }
        try {
            // ECB-encrypt Iv (for safety along the network)
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            byte[] cryptedIv = cipher.doFinal(iv);

            // CBC-encrypt (plain Iv drives the algo, since it will be decrypted first later)
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, (Key) new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] cipherText = cipher.doFinal(input);

            // Pack together in expected order [cryptedIv|cipherText]
            byte[] cryptedPkt = new byte[cryptedIv.length + cipherText.length];
            System.arraycopy(cryptedIv, 0, cryptedPkt, 0, cryptedIv.length);
            System.arraycopy(cipherText, 0, cryptedPkt, cryptedIv.length, cipherText.length);
            return cryptedPkt;
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException
                 | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            throw new CryptoException("failed to symmetric encrypt", e);
        }
    }

    public static byte[] symmetricEncrypt(byte[] input, byte[] key) throws CryptoException {
        return symmetricEncryptWithIV(input, key, generateRandomBlock(16));
    }

    public static byte[] symmetricDecryptHMACIV(byte[] input, byte[] key, byte[] hmacSecret) throws CryptoException {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (hmacSecret == null) {
            throw new IllegalArgumentException("hmacSecret is null");
        }
        if (key.length < 16) {
            logger.debug("symmetricDecryptHMACIV used with shorter than 16 byte key!");
        }
        Passable<byte[]> iv = new Passable<>(new byte[16]);
        byte[] plaintextData = symmetricDecrypt(input, key, iv);
        // Recompute HMAC over (last 3 IV bytes || plaintext) and compare the
        // truncated result against the leading 13 IV bytes.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(iv.getValue(), iv.getValue().length - 3, 3);
        baos.write(plaintextData, 0, plaintextData.length);
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(hmacSecret, "HmacSHA1"));
            byte[] hmacBytes = mac.doFinal(baos.toByteArray());
            for (int i = 0; i < iv.getValue().length - 3; i++) {
                if (hmacBytes[i] != iv.getValue()[i]) {
                    throw new CryptoException("NetFilterEncryption was unable to decrypt packet: "
                            + "HMAC from server did not match computed HMAC.");
                }
            }
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CryptoException("NetFilterEncryption was unable to decrypt packet", e);
        }
        return plaintextData;
    }

    public static byte[] symmetricEncryptWithHMACIV(byte[] input, byte[] key, byte[] hmacSecret) throws CryptoException {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (hmacSecret == null) {
            throw new IllegalArgumentException("hmacSecret is null");
        }
        // IV = HMAC-SHA1(random[3] || input)[0..13] || random[3]
        byte[] iv = new byte[16];
        byte[] random = generateRandomBlock(3);
        System.arraycopy(random, 0, iv, iv.length - random.length, random.length);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(random, 0, random.length);
        baos.write(input, 0, input.length);
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(hmacSecret, "HmacSHA1"));
            byte[] hash = mac.doFinal(baos.toByteArray());
            System.arraycopy(hash, 0, iv, 0, iv.length - random.length);
            return symmetricEncryptWithIV(input, key, iv);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CryptoException("NetFilterEncryption was unable to decrypt packet", e);
        }
    }
}
