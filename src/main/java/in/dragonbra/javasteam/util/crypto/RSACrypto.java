package in.dragonbra.javasteam.util.crypto;

import in.dragonbra.javasteam.util.log.LogManager;
import in.dragonbra.javasteam.util.log.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.interfaces.RSAPublicKey;
import java.util.AbstractList;
import java.util.ArrayList;

/**
 * frenchpress shadow of JavaSteam's {@code RSACrypto}.
 *
 * <p>Identical to the upstream 1.8.0 class except it targets the stock JDK
 * providers instead of BouncyCastle. The only substantive change is the cipher
 * transformation string: upstream uses the BC-flavored
 * {@code "RSA/None/OAEPWithSHA1AndMGF1Padding"} with the {@code CryptoHelper.SEC_PROV}
 * provider; the JDK requires {@code "RSA/ECB/OAEPWithSHA1AndMGF1Padding"} and the
 * default provider search. OAEP with SHA-1 digest uses MGF1-SHA1 in both
 * providers, so the ciphertext Steam receives is decryptable identically.
 *
 * <p>Reuses upstream {@link AsnKeyParser} for parsing Steam's ASN.1 RSA public key.
 */
public class RSACrypto {
    private static final Logger logger = LogManager.getLogger(RSACrypto.class);
    private Cipher cipher;

    public RSACrypto(byte[] key) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        try {
            AsnKeyParser keyParser = new AsnKeyParser(new AbstractList<Byte>() {
                @Override
                public Byte get(int index) {
                    return key[index];
                }

                @Override
                public int size() {
                    return key.length;
                }
            });
            BigInteger[] keys = keyParser.parseRSAPublicKey();
            init(keys[0], keys[1]);
        } catch (BerDecodeException e) {
            logger.error(e);
        }
    }

    private void init(BigInteger mod, BigInteger exp) {
        try {
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(mod, exp);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPublicKey rsaKey = (RSAPublicKey) factory.generatePublic(publicKeySpec);
            // JDK spelling: "ECB" mode (BC's "None"), default provider (was CryptoHelper.SEC_PROV).
            this.cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
            this.cipher.init(Cipher.ENCRYPT_MODE, rsaKey);
        } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException
                 | NoSuchPaddingException e) {
            logger.debug(e);
        }
    }

    public byte[] encrypt(byte[] input) {
        try {
            return this.cipher.doFinal(input);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            logger.debug(e);
            return null;
        }
    }
}
