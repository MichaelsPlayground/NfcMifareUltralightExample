package de.androidcrypto.nfcndefexample;

import static android.content.Context.VIBRATOR_SERVICE;

import static de.androidcrypto.nfcndefexample.Utils.bytesToHexNpe;
import static de.androidcrypto.nfcndefexample.Utils.doVibrate;
import static de.androidcrypto.nfcndefexample.Utils.playSinglePing;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReadCiphertextFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReadCiphertextFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ReadCiphertextFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReceiveFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReadCiphertextFragment newInstance(String param1, String param2) {
        ReadCiphertextFragment fragment = new ReadCiphertextFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    TextView ndefMessage;
    Button readNdefMessage, writeNdefMessage, writeCiphertext, decryptCiphertext;
    String ndefMessageString;

    TextView ciphertextFound;
    TextView salt, nonce, ciphertext; // data shown as hex string
    TextView plaintext; // data shown as string
    byte[] saltBytes = new byte[0], nonceBytes = new byte[0], ciphertextBytes = new byte[0]; // real data
    byte[] plaintextBytes = new byte[0];
    EditText passphraseDecryption;
    TextView readResult;
    private NfcAdapter mNfcAdapter;
    Context contextSave;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        contextSave = getActivity().getApplicationContext();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this.getContext());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        readResult = getView().findViewById(R.id.tvReadResult);

        ndefMessage = getView().findViewById(R.id.tvNdefMessage);
        ciphertextFound = getView().findViewById(R.id.tvNdefCiphertextFound);
        salt = getView().findViewById(R.id.tvNdefSalt);
        nonce = getView().findViewById(R.id.tvNdefNonce);
        ciphertext = getView().findViewById(R.id.tvNdefCiphertext);
        plaintext = getView().findViewById(R.id.tvPlaintextDecrypted);
        decryptCiphertext = getView().findViewById(R.id.btnDecryptCiphertext);
        passphraseDecryption = getView().findViewById(R.id.etPassphraseDecryption);

        //doVibrate();
        decryptCiphertext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkCiphertextIsPresent()) {
                    int passphraseLength = 0;
                    if (passphraseDecryption != null) {
                        passphraseLength = passphraseDecryption.length();
                    }
                    // get the passphrase as char[]
                    char[] passphraseChar = new char[passphraseLength];
                    passphraseDecryption.getText().getChars(0, passphraseLength, passphraseChar, 0);
                    if (passphraseLength < 1) {
                        Toast.makeText(getContext(),
                                "Enter a longer passphrase",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    plaintextBytes = CryptoManager.aes256GcmPbkdf2Sha256Decryption2(saltBytes, nonceBytes, ciphertextBytes, passphraseChar);
                    if (plaintextBytes.length > 0) {
                        plaintext.setText(new String(plaintextBytes, StandardCharsets.UTF_8));
                    } else {
                        plaintext.setText("Error on decryption (wrong passphrase ???), try again");
                    }
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_read_ciphertext, container, false);
    }

    // This method is running in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        System.out.println("NFC tag discovered");
        // clear the datafields
        clearEncryptionData();

        Ndef mNdef = Ndef.get(tag);

        if (mNdef == null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(),
                        "mNdef is null",
                        Toast.LENGTH_SHORT).show();
            });
        }

        // Check that it is an Ndef capable card
        if (mNdef != null) {

            // If we want to read
            // As we did not turn on the NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
            // We can get the cached Ndef message the system read for us.

            NdefMessage mNdefMessage = mNdef.getCachedNdefMessage();
            ndefMessageString = mNdefMessage.toString();

            // Make a Sound

            NdefRecord[] record = mNdefMessage.getRecords();
            String ndefContent = "";
            int ndefRecordsCount = record.length;
            ndefContent = "nr of records: " + ndefRecordsCount + "\n";
            // Success if got to here
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(),
                        "Read from NFC success, number of records: " + ndefRecordsCount,
                        Toast.LENGTH_SHORT).show();
            });

            if (ndefRecordsCount > 0) {
                for (int i = 0; i < ndefRecordsCount; i++) {
                    short ndefInf = record[i].getTnf();
                    byte[] ndefType = record[i].getType();
                    byte[] ndefPayload = record[i].getPayload();
                    // check for encrypted content in an External NDEF message
                    short ndefInf4 = (short) 4;
                    if (Short.compare(ndefInf, ndefInf4) == 0) {
                        // this is a record type 4
                        byte[] saltDefinition = "de.androidcrypto.aes256gcmpbkdf2:salt".getBytes(StandardCharsets.UTF_8);
                        byte[] nonceDefinition = "de.androidcrypto.aes256gcmpbkdf2:nonce".getBytes(StandardCharsets.UTF_8);
                        byte[] ciphertextDefinition = "de.androidcrypto.aes256gcmpbkdf2:ciphertext".getBytes(StandardCharsets.UTF_8);
                        // checking for salt
                        if (Arrays.equals(ndefType, saltDefinition)) {
                            // salt definition found
                            saltBytes = Arrays.copyOf(ndefPayload, ndefPayload.length);
                        }
                        if (Arrays.equals(ndefType, nonceDefinition)) {
                            // nonce definition found
                            nonceBytes = Arrays.copyOf(ndefPayload, ndefPayload.length);
                        }
                        if (Arrays.equals(ndefType, ciphertextDefinition)) {
                            // ciphertext definition found
                            ciphertextBytes = Arrays.copyOf(ndefPayload, ndefPayload.length);
                        }
                    }

                    ndefContent = ndefContent + "rec " + i + " inf: " + ndefInf +
                            " type: " + bytesToHexNpe(ndefType) +
                            " payload: " + bytesToHexNpe(ndefPayload) +
                            " \n" + new String(ndefPayload) + " \n";
                    String finalNdefContent = ndefContent;
                    getActivity().runOnUiThread(() -> {
                        ndefMessage.setText(finalNdefContent);
                    });
                    if (checkCiphertextIsPresent()) {
                        getActivity().runOnUiThread(() -> {
                            salt.setText(bytesToHexNpe(saltBytes));
                        });
                        getActivity().runOnUiThread(() -> {
                            nonce.setText(bytesToHexNpe(nonceBytes));
                        });
                        getActivity().runOnUiThread(() -> {
                            ciphertext.setText(bytesToHexNpe(ciphertextBytes));
                        });
                        getActivity().runOnUiThread(() -> {
                            ciphertextFound.setVisibility(View.VISIBLE);
                        });
                        getActivity().runOnUiThread(() -> {
                            passphraseDecryption.setVisibility(View.VISIBLE);
                        });
                        getActivity().runOnUiThread(() -> {
                            decryptCiphertext.setVisibility(View.VISIBLE);
                        });
                    }
                }
            }
            doVibrate(getActivity());
            playSinglePing(getContext());
        }
    }
    private void showWirelessSettings() {
        Toast.makeText(this.getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
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
            mNfcAdapter.enableReaderMode(this.getActivity(),
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
            mNfcAdapter.disableReaderMode(this.getActivity());
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

    private void clearEncryptionData() {
        saltBytes = new byte[0];
        nonceBytes = new byte[0];
        ciphertextBytes = new byte[0];
        plaintextBytes = new byte[0];
        getActivity().runOnUiThread(() -> {
            salt.setText("");
            nonce.setText("");
            ciphertext.setText("");
            plaintext.setText("");
            ciphertextFound.setVisibility(View.GONE);
            passphraseDecryption.setVisibility(View.GONE);
            decryptCiphertext.setVisibility(View.GONE);
        });

    }

}