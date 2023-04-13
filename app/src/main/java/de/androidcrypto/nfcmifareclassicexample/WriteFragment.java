package de.androidcrypto.nfcmifareclassicexample;

import static de.androidcrypto.nfcmifareclassicexample.Utils.bytesToHexNpe;
import static de.androidcrypto.nfcmifareclassicexample.Utils.doVibrate;
import static de.androidcrypto.nfcmifareclassicexample.Utils.getTimestamp;
import static de.androidcrypto.nfcmifareclassicexample.Utils.hexStringToByteArray;
import static de.androidcrypto.nfcmifareclassicexample.Utils.playSinglePing;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WriteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WriteFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "WriteFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    com.google.android.material.textfield.TextInputEditText dataToSend, resultNfcWriting;
    SwitchMaterial addTimestampToData;
    AutoCompleteTextView autoCompleteTextView;
    com.google.android.material.textfield.TextInputLayout dataToSendLayout;

    private NfcAdapter mNfcAdapter;
    private int sectorToWrite;
    private String outputString = ""; // used for the UI output

    public WriteFragment() {
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
    public static WriteFragment newInstance(String param1, String param2) {
        WriteFragment fragment = new WriteFragment();
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
        return inflater.inflate(R.layout.fragment_write, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        dataToSend = getView().findViewById(R.id.etWriteData);
        dataToSendLayout = getView().findViewById(R.id.etWriteDataLayout);
        resultNfcWriting = getView().findViewById(R.id.etMainResult);
        addTimestampToData = getView().findViewById(R.id.swMainAddTimestampSwitch);

        String[] type = new String[]{
                "1", "2", "3", "4", "5",
                "6", "7", "8", "9"};
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                getView().getContext(),
                R.layout.drop_down_item,
                type);

        autoCompleteTextView = getView().findViewById(R.id.writeSector);
        autoCompleteTextView.setText(type[0]);
        autoCompleteTextView.setAdapter(arrayAdapter);


        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());

        // todo work with sector 0, has only 32 bytes of data to write (block 1 + 2)
        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "sectorToWrite: " + sectorToWrite);
            }
        });

        addTimestampToData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                // ts is 19 chars long + 1 separator = 20 characters
                if (b) {
                    dataToSendLayout.setCounterMaxLength(48 - 20);
                    String ds = dataToSend.getText().toString();
                    if (ds.length() > 28) dataToSend.setText(ds.substring(0, 28));
                    setEditTextMaxLength(dataToSend, 28);
                } else {
                    dataToSendLayout.setCounterMaxLength(48);
                    setEditTextMaxLength(dataToSend, 48);
                }
            }
        });

    }

    private void setEditTextMaxLength(EditText et, int maxLength) {
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
    }

    /**
     * section for NFC
     */

    @Override
    public void onTagDiscovered(Tag tag) {

        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        System.out.println("NFC tag discovered");
        playSinglePing();
        outputString = "";

        requireActivity().runOnUiThread(() -> {
            resultNfcWriting.setText("");
        });

        // you should have checked that this device is capable of working with Mifare Classic tags, otherwise you receive an exception

        String sendData = dataToSend.getText().toString();
        if (addTimestampToData.isChecked()) sendData = sendData + " " + getTimestamp();
        if (TextUtils.isEmpty(sendData)) {
            writeToUiAppend("Please enter some data to write on tag. Aborted");
            writeToUiFinal(resultNfcWriting);
            return;
        }

        MifareClassic mfc = MifareClassic.get(tag);
        if (mfc == null) {
            writeToUiAppend("The tag is not readable with Mifare Classic classes, sorry");
            writeToUiFinal(resultNfcWriting);
            return;
        }

        // get card details
        int ttype = mfc.getType();
        StringBuilder sb = new StringBuilder();
        sb.append("MifareClassic type: ").append(ttype).append("\n");
        int tagSize = mfc.getSize();
        sb.append("MifareClassic size: ").append(tagSize).append("\n");
        int sectorCount = mfc.getSectorCount();
        sb.append("MifareClassic sector count: ").append(sectorCount).append("\n");
        int blockCount = mfc.getBlockCount();
        sb.append("MifareClassic block count: ").append(blockCount).append("\n");

        sb.append("APP_DIRECTORY: ").append(bytesToHexNpe(MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)).append("\n");
        sb.append("KEY_DEFAULT  : ").append(bytesToHexNpe(MifareClassic.KEY_DEFAULT)).append("\n");
        sb.append("KEY_NFC_FORUM: ").append(bytesToHexNpe(MifareClassic.KEY_NFC_FORUM)).append("\n");

        writeToUiAppend(sb.toString());

        try {
            mfc.connect();

            if (mfc.isConnected()) {

                // get sector to write
                String choiceString = autoCompleteTextView.getText().toString();
                sectorToWrite = Integer.parseInt(choiceString);

                // get the block to write and split the data into block of maximal 16 bytes long
                byte[] dtw = new byte[48]; // todo for sector 0 just 32 bytes and 2 blocks
                System.arraycopy(sendData.getBytes(StandardCharsets.UTF_8), 0, dtw, 0, sendData.getBytes(StandardCharsets.UTF_8).length); // this is an array filled up with 0x00
                byte[] block1 = Arrays.copyOfRange(dtw, 0, 16);
                byte[] block2 = Arrays.copyOfRange(dtw, 16, 32);
                byte[] block3 = Arrays.copyOfRange(dtw, 32, 48);
                writeToUiAppend("block  length: " + dtw.length + " data: " + bytesToHexNpe(dtw));
                writeToUiAppend("block1 length: " + block1.length + " data: " + bytesToHexNpe(block1));
                writeToUiAppend("block2 length: " + block2.length + " data: " + bytesToHexNpe(block2));
                writeToUiAppend("block3 length: " + block3.length + " data: " + bytesToHexNpe(block3));

                // write to tag
                boolean writeSuccess = writeMifareSector(mfc, sectorToWrite, block1, block2, block3);
                writeToUiAppend("Tried to write data to tag, success ? : " + writeSuccess);

                mfc.close();
            }
        } catch (IOException e) {
            writeToUiAppend("IOException on connection: " + e.getMessage());
            e.printStackTrace();
        }

        doVibrate(getActivity());
        playDoublePing();
        writeToUiFinal(resultNfcWriting);

    }

    private boolean writeMifareSector(MifareClassic mif, int secCnt, byte[] bd1, byte[] bd2,
                                      byte[] bd3) {
        byte[][] returnBytes = new byte[3][64];
        byte[] keyABytes = null;
        byte[] keyBBytes = null;
        byte[] dataBytes = new byte[64];
        boolean isAuthenticated = false;
        // try to authenticate with known keys - no brute force
        Log.d(TAG, "");
        Log.d(TAG, "writeMifareSector " + secCnt);
        try {

            // this method is just using the KEY_DEFAULT_KEY for keyB
            if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_DEFAULT)) {
                keyBBytes = MifareClassic.KEY_DEFAULT.clone();
                Log.d(TAG, "Auth success with B KEY_DEFAULT");
                isAuthenticated = true;
            }


            /*
            // construct is there to run the break command
            boolean c = true;          // true by default
            while ( c == true ) {       // only loop while true
                c = false;                  // kill loop on first iteration

                if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                    keyBBytes = MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY.clone();
                    Log.d(TAG, "Auth success with B KEY_MIFARE_APPLICATION_DIRECTORY");
                    isAuthenticated = true;
                    break;
                    // there are 3 default keys available
                    // MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY: a0a1a2a3a4a5
                    // MifareClassic.KEY_DEFAULT:                      ffffffffffff
                    // MifareClassic.KEY_NFC_FORUM:                    d3f7d3f7d3f7
                } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_DEFAULT)) {
                    keyBBytes = MifareClassic.KEY_DEFAULT.clone();
                    Log.d(TAG, "Auth success with B KEY_DEFAULT");
                    isAuthenticated = true;
                    break;
                } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_NFC_FORUM)) {
                    keyBBytes = MifareClassic.KEY_NFC_FORUM.clone();
                    Log.d(TAG, "Auth success with B KEY_NFC_FORUM");
                    isAuthenticated = true;
                    break;
                } else if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_DEFAULT)) {
                    keyABytes = MifareClassic.KEY_DEFAULT.clone();
                    Log.d(TAG, "Auth success with A KEY_DEFAULT");
                    isAuthenticated = true;
                    break;
                } else if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                    keyABytes = MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY.clone();
                    isAuthenticated = true;
                    Log.d(TAG, "Auth success with A KEY_MIFARE_APPLICATION_DIRECTORY");
                    break;
                } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_NFC_FORUM)) {
                    keyBBytes = MifareClassic.KEY_NFC_FORUM;
                    Log.d(TAG, "Auth success with B KEY_NFC_FORUM");
                    isAuthenticated = true;
                    break;
                } else if (mif.authenticateSectorWithKeyA(secCnt, hexStringToByteArray("4D57414C5648"))) {
                    keyABytes = hexStringToByteArray("4D57414C5648");
                    Log.d(TAG, "Auth success with A Crowne Plaza key");
                    isAuthenticated = true;
                    break;
                    //4D57414C5648
                } else {
                    //return null;
                    Log.d(TAG, "NO Auth success");
                }
            }

             */
            // get the blockindex
            int block_index = mif.sectorToBlock(secCnt);
            // get block in sector
            int blocksInSector = mif.getBlockCountInSector(secCnt);
            int blockInSectorCount = 0;
            mif.writeBlock((block_index + blockInSectorCount), bd1);
            blockInSectorCount = 1;
            mif.writeBlock((block_index + blockInSectorCount), bd2);
            blockInSectorCount = 2;
            mif.writeBlock((block_index + blockInSectorCount), bd3);
        } catch (IOException e) {
            Log.e(TAG, "Sector " + secCnt + " IOException: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void formatNdef(Tag tag) {
        // trying to format the tag
        NdefFormatable format = NdefFormatable.get(tag);
        if (format != null) {
            try {
                format.connect();
                format.format(new NdefMessage(new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)));
                format.close();
                showMessage("Tag formatted, try again to write on tag");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                showMessage("Failed to connect");
                e.printStackTrace();
            } catch (FormatException e) {
                // TODO Auto-generated catch block
                showMessage("Failed Format");
                e.printStackTrace();
            }
        } else {
            showMessage("Tag not formattable or already formatted to Ndef");
        }
    }

    /**
     * Sound files downloaded from Material Design Sounds
     * https://m2.material.io/design/sound/sound-resources.html
     */
    private void playSinglePing() {
        MediaPlayer mp = MediaPlayer.create(getContext(), R.raw.notification_decorative_02);
        mp.start();
    }

    private void playDoublePing() {
        MediaPlayer mp = MediaPlayer.create(getContext(), R.raw.notification_decorative_01);
        mp.start();
    }

    private void writeToUiAppend(String message) {
        //System.out.println(message);
        outputString = outputString + message + "\n";
    }

    private void writeToUiFinal(final TextView textView) {
        if (textView == (TextView) resultNfcWriting) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(outputString);
                    System.out.println(outputString); // print the data to console
                }
            });
        }
    }


    private void showMessage(String message) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
            resultNfcWriting.setText(message);
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