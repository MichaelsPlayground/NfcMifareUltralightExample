package de.androidcrypto.nfcmifareclassicexample;

import static de.androidcrypto.nfcmifareclassicexample.Utils.bytesToHexNpe;
import static de.androidcrypto.nfcmifareclassicexample.Utils.doVibrate;
import static de.androidcrypto.nfcmifareclassicexample.Utils.playSinglePing;

import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WriteCiphertextFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WriteCiphertextFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    Button encryptData;
    //com.google.android.material.textfield.TextInputLayout inputField1Decoration, inputField2Decoration, inputField3Decoration;
    com.google.android.material.textfield.TextInputEditText plaintext, passphrase, writeResult;
    TextView infoReadyToWrite, infoNotReadyToWrite;
    boolean readyToWrite = false;
    byte[] saltBytes = new byte[0], nonceBytes = new byte[0], ciphertextBytes = new byte[0]; // real data
    private NfcAdapter mNfcAdapter;

    public WriteCiphertextFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SendFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WriteCiphertextFragment newInstance(String param1, String param2) {
        WriteCiphertextFragment fragment = new WriteCiphertextFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    // AID is setup in apduservice.xml
    // original AID: F0394148148100

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_write_ciphertext, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        encryptData = getView().findViewById(R.id.btnEncryptData);
        plaintext = getView().findViewById(R.id.etPlaintext);
        passphrase = getView().findViewById(R.id.etPassphrase);
        writeResult = getView().findViewById(R.id.etWriteResult);
        infoReadyToWrite = getView().findViewById(R.id.tvEncryptedWriteEnabled);
        infoNotReadyToWrite = getView().findViewById(R.id.tvEncryptedWriteDisabled);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());

        //hideAllInputFields();

        encryptData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearEncryptionData();
                int passphraseLength = 0;
                if (passphrase != null) {
                    passphraseLength = passphrase.length();
                }
                // get the passphrase as char[]
                char[] passphraseChar = new char[passphraseLength];
                passphrase.getText().getChars(0, passphraseLength, passphraseChar, 0);
                if (passphraseLength < 1) {
                    Toast.makeText(getContext(),
                            "Enter a longer passphrase",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                int plaintextLength = plaintext.getText().length();
                if (plaintextLength < 1) {
                    Toast.makeText(getContext(),
                            "Enter a longer plaintext",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (plaintextLength > 50) {
                    Toast.makeText(getContext(),
                            "Enter a shorter plaintext, maximum is 50 characters",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                byte[][] result = CryptoManager.aes256GcmPbkdf2Sha256Encryption2(String.valueOf(plaintext.getText()).getBytes(StandardCharsets.UTF_8), passphraseChar);
                //salt.setText(bytesToHex(result[0]));
                //nonce.setText(bytesToHex(result[1]));
                //ciphertext.setText(bytesToHex(result[2]));
                // real values for usage with NTAG writing
                saltBytes = result[0];
                nonceBytes = result[1];
                ciphertextBytes = result[2];
                StringBuilder sb = new StringBuilder();
                sb.append("Encrypted data ready for writing").append("\n");
                sb.append("Plaintext: ").append(plaintext.getText()).append("\n");
                sb.append("Passphrase: ").append(passphrase.getText()).append("\n");
                sb.append("Salt bytes: ").append(bytesToHexNpe(saltBytes)).append("\n");
                sb.append("Nonce bytes: ").append(bytesToHexNpe(nonceBytes)).append("\n");
                sb.append("Ciphertext bytes: ").append(bytesToHexNpe(ciphertextBytes));
                writeResult.setText(sb.toString());
                System.out.println(sb.toString());
                System.out.println("*** decrypted: " + new String(CryptoManager.aes256GcmPbkdf2Sha256Decryption2(result[0], result[1], result[2], passphraseChar), StandardCharsets.UTF_8));
                readyToWrite = true;
                infoReadyToWrite.setVisibility(View.VISIBLE);
                infoNotReadyToWrite.setVisibility(View.GONE);
            }
        });

    }

    private void clearEncryptionData() {
        saltBytes = new byte[0];
        nonceBytes = new byte[0];
        ciphertextBytes = new byte[0];
        readyToWrite = false;
        infoReadyToWrite.setVisibility(View.GONE);
        infoNotReadyToWrite.setVisibility(View.VISIBLE);
    }

    // checks that a complete set of data of ciphertext
    // is available
    private boolean checkCiphertextIsPresent() {
        boolean saltAvailable = false;
        boolean nonceAvailable = false;
        boolean ciphertextAvailable = false;
        boolean ciphertextIsPresent = false;
        if (saltBytes.length > 31) saltAvailable = true;
        if (nonceBytes.length > 11) nonceAvailable = true;
        if (ciphertextBytes.length > 16) ciphertextAvailable = true;
        if (saltAvailable && nonceAvailable && ciphertextAvailable) ciphertextIsPresent = true;
        return ciphertextIsPresent;
    }

    /**
     * section for NFC
     */

    @Override
    public void onTagDiscovered(Tag tag) {// first check that ciphertext is present, if not give a message and return
        if (checkCiphertextIsPresent() == false) {
            showMessage("There is no ciphertext available. Enter the fields and press ENCRYPT");
        } else {
            // the thread only runs when ciphertext is present

            // Read and or write to Tag here to the appropriate Tag Technology type class
            // in this example the card should be an Ndef Technology Type
            Ndef mNdef = Ndef.get(tag);

            // Check that it is an Ndef capable card
            if (mNdef != null) {

                // If we want to read
                // As we did not turn on the NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
                // We can get the cached Ndef message the system read for us.
                // Or if we want to write a Ndef message
                // Create a Ndef text record
                String headerString = "Encryption was done with AES-256 GCM PBKDF2 on ";
                String timeNow = ZonedDateTime
                        .now(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("uuuu.MM.dd HH.mm.ss"));
                NdefRecord ndefRecord1Text = NdefRecord.createTextRecord("en", headerString +
                        timeNow);
                System.out.println(headerString +
                        timeNow);
                NdefRecord ndefRecord2ExternalSalt = NdefRecord.createExternal("de.androidcrypto.aes256gcmpbkdf2", "salt", saltBytes);
                NdefRecord ndefRecord3ExternalNonce = NdefRecord.createExternal("de.androidcrypto.aes256gcmpbkdf2", "nonce", nonceBytes);
                NdefRecord ndefRecord4ExternalCiphertext = NdefRecord.createExternal("de.androidcrypto.aes256gcmpbkdf2", "ciphertext", ciphertextBytes);
                // Create an Ndef URI record
                String uriString = "http://androidcrypto.bplaced.net";
                NdefRecord ndefRecord5Uri = NdefRecord.createUri(uriString);
                // Create an Ndef Android application record
                String packageName = "de.androidcrypto.nfcndefexample";
                NdefRecord ndefRecord6Aar = NdefRecord.createApplicationRecord(packageName);

                // Add to a NdefMessage
                //NdefMessage mMsg = new NdefMessage(ndefRecord1Text); // this gives exact 1 message with 1 record
                NdefMessage mMsg = new NdefMessage(ndefRecord1Text, ndefRecord2ExternalSalt, ndefRecord3ExternalNonce, ndefRecord4ExternalCiphertext, ndefRecord5Uri, ndefRecord6Aar); // gives 1 message with 6 records
                // Catch errors

                try {
                    mNdef.connect();
                    mNdef.writeNdefMessage(mMsg);
                    // Success if got to here
                    showMessage("Write to NFC Success");
                } catch (FormatException e) {
                    showMessage("FormatException: " + e);
                    // if the NDEF Message to write is malformed
                } catch (TagLostException e) {
                    showMessage("TagLostException: " + e);
                    // Tag went out of range before operations were complete
                } catch (IOException e) {
                    // if there is an I/O failure, or the operation is cancelled
                    showMessage("1IOException: " + e);
                } finally {
                    // Be nice and try and close the tag to
                    // Disable I/O operations to the tag from this TagTechnology object, and release resources.
                    try {
                        mNdef.close();
                    } catch (IOException e) {
                        // if there is an I/O failure, or the operation is cancelled
                        showMessage("2IOException: " + e);
                    }
                }
            }
            doVibrate(getActivity());
            playSinglePing(getContext());
        }
    }

    private void showMessage(String message) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
            writeResult.setText(message);
        });
    }

    private void showWirelessSettings() {
        Toast.makeText(getView().getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {

            if (!mNfcAdapter.isEnabled())
                showWirelessSettings();

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(getActivity(),
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}