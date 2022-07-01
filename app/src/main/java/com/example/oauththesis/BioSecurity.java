package com.example.oauththesis;


import android.content.Context;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

interface CryptoObjectListener {
    void available(BiometricPrompt.CryptoObject cryptoObject);
}
public class BioSecurity {

    private CryptoObjectListener listener = null;
    private MainActivity context = null;

    private static final String SHARED_PREFERENCE_KEY_IV = "iv";

    public BioSecurity(MainActivity context){
        this.context = context;

        try {
            generateSecretKey(new KeyGenParameterSpec.Builder(
                    "KEY_NAME",
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
                    .build());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    public void encrypt(CryptoObjectListener listener){
        this.listener = listener;
        Cipher cipher = null;
        try {
            cipher = getCipher();
            SecretKey secretKey = getSecretKey();
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit().putString(SHARED_PREFERENCE_KEY_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .apply();

            BiometricPrompt.PromptInfo promptInfo;
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric login for my app")
                    .setSubtitle("")
                    .setNegativeButtonText("Cancel")
                    .build();
            createBiometricPrompt().authenticate(promptInfo,new BiometricPrompt.CryptoObject(cipher));
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | CertificateException | IOException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public void decrypt(CryptoObjectListener listener){
        this.listener = listener;


        Cipher cipher = null;
        try {
            cipher = getCipher();
            SecretKey secretKey = getSecretKey();

            String keyIV = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(SHARED_PREFERENCE_KEY_IV, "");


            byte[] iv = Base64.decode(keyIV, Base64.NO_WRAP);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, secretKey,ivParameterSpec);

            BiometricPrompt.PromptInfo promptInfo;
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric login for my app")
                    .setSubtitle("")
                    .setNegativeButtonText("Cancel")
                    .build();
            createBiometricPrompt().authenticate(promptInfo,new BiometricPrompt.CryptoObject(cipher));
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | CertificateException | IOException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private BiometricPrompt createBiometricPrompt() {

        Executor executor = ContextCompat.getMainExecutor(context);
        String TAG = "BIO TAG";

        BiometricPrompt.AuthenticationCallback callback =  new BiometricPrompt.AuthenticationCallback(){
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Log.d(TAG, "$errorCode :: $errString");
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.d(TAG, "Authentication failed for an unknown reason");
            }

            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Log.d(TAG, "Authentication was successful");
                listener.available(result.getCryptoObject());
            }

        };

        //The API requires the client/Activity context for displaying the prompt
        BiometricPrompt biometricPrompt = new BiometricPrompt(context, executor, callback);
        return biometricPrompt;
    }


    //// new code added
    private void generateSecretKey(KeyGenParameterSpec keyGenParameterSpec) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        keyGenerator.init(keyGenParameterSpec);
        keyGenerator.generateKey();
    }

    private SecretKey getSecretKey() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");

        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null);
        return ((SecretKey)keyStore.getKey("KEY_NAME", null));
    }

    private Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7);
    }
}
