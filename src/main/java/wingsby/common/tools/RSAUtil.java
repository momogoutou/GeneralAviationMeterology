package wingsby.common.tools;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAUtil {

    public static JSONObject generateKeyPairs() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance("RSA", "BC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        if (generator == null) return null;
        generator.initialize(128, new SecureRandom());
        KeyPair pair = generator.generateKeyPair();
        PublicKey pubKey = pair.getPublic();
        PrivateKey privKey = pair.getPrivate();
        byte[] pk = pubKey.getEncoded();
        byte[] privk = privKey.getEncoded();
        String strpk = new String(Base64.encodeBase64(pk));
        String strprivk = new String(Base64.encodeBase64(privk));
        System.out.println("公钥Base64编码:" + strpk);
        System.out.println("私钥Base64编码:" + strprivk);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("public", strpk);
        jsonObject.put("private", strprivk);
        return jsonObject;
    }

    //加密
    public static String encryptData(String data, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] dataToEncrypt = data.getBytes("utf-8");
            byte[] encryptedData = cipher.doFinal(dataToEncrypt);
            String encryptString = Base64.encodeBase64String(encryptedData);
            return encryptString;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String decryptData(String data, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] descryptData = Base64.decodeBase64(data);
            byte[] descryptedData = cipher.doFinal(descryptData);
            String srcData = new String(descryptedData, "utf-8");
            return srcData;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PublicKey encodePubKey(String key) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        X509EncodedKeySpec pubX509 = new X509EncodedKeySpec(Base64.decodeBase64(key.getBytes()));
        KeyFactory keyf = null;
        PublicKey pubKey = null;
        try {
            keyf = KeyFactory.getInstance("RSA", "BC");
            pubKey = keyf.generatePublic(pubX509);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return pubKey;
    }


    public static PrivateKey encodePriKey(String key) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.decodeBase64(key.getBytes()));
        PrivateKey privKey = null;
        KeyFactory keyf = null;
        try {
            keyf = KeyFactory.getInstance("RSA", "BC");
            privKey = keyf.generatePrivate(priPKCS8);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return privKey;
    }


}
