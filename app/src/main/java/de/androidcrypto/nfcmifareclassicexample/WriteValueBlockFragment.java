package de.androidcrypto.nfcmifareclassicexample;

import static de.androidcrypto.nfcmifareclassicexample.Utils.bytesToHexNpe;
import static de.androidcrypto.nfcmifareclassicexample.Utils.doVibrate;
import static de.androidcrypto.nfcmifareclassicexample.Utils.getTimestamp;
import static de.androidcrypto.nfcmifareclassicexample.Utils.hexStringToByteArray;

import android.content.Intent;
import android.media.MediaPlayer;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
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
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WriteValueBlockFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WriteValueBlockFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "WriteValueBlockFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    com.google.android.material.textfield.TextInputEditText incrementValueBlock, dataToSend, resultNfcWriting;
    SwitchMaterial swIncrementValueBlock;
    AutoCompleteTextView sectorSelect, blockSelect;
    com.google.android.material.textfield.TextInputLayout dataToSendLayout, incrementValueBlockLayout;


    private NfcAdapter mNfcAdapter;
    private int sectorToWrite, blockToWrite;
    private String outputString = ""; // used for the UI output

    public WriteValueBlockFragment() {
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
    public static WriteValueBlockFragment newInstance(String param1, String param2) {
        WriteValueBlockFragment fragment = new WriteValueBlockFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

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
        return inflater.inflate(R.layout.fragment_write_value_block, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        dataToSend = getView().findViewById(R.id.etWriteData);
        dataToSendLayout = getView().findViewById(R.id.etWriteDataLayout);
        resultNfcWriting = getView().findViewById(R.id.etMainResult);
        swIncrementValueBlock = getView().findViewById(R.id.swIncrementValueBlock);
        incrementValueBlock = getView().findViewById(R.id.etWriteIncrement);
        incrementValueBlockLayout = getView().findViewById(R.id.etWriteIncrementLayout);



        String[] sectorChoices = new String[]{
                "1", "2", "3", "4", "5", "6", "7", "8", "9",
                "10", "11", "12", "13", "14", "15"};
        ArrayAdapter<String> sectorArrayAdapter = new ArrayAdapter<>(
                getView().getContext(),
                R.layout.drop_down_item,
                sectorChoices);

        sectorSelect = getView().findViewById(R.id.writeSector);
        sectorSelect.setText(sectorChoices[13]);
        sectorSelect.setAdapter(sectorArrayAdapter);

        String[] blockChoices = new String[]{
                "0", "1", "2"};
        ArrayAdapter<String> blockArrayAdapter = new ArrayAdapter<>(
                getView().getContext(),
                R.layout.drop_down_item,
                blockChoices);

        blockSelect = getView().findViewById(R.id.writeBlock);
        blockSelect.setText(blockChoices[0]);
        blockSelect.setAdapter(blockArrayAdapter);


        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());

        // todo work with sector 0, has only 32 bytes of data to write (block 1 + 2)
        sectorSelect.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                sectorToWrite = i;
                Log.d(TAG, "sectorToWrite: " + sectorToWrite);
            }
        });

        blockSelect.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                blockToWrite = i;
                Log.d(TAG, "blockToWrite: " + blockToWrite);
            }
        });

        swIncrementValueBlock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                // if checked increment, if false just write a blank value block
                if (b) {
                    incrementValueBlockLayout.setVisibility(View.VISIBLE);
                } else {
                    incrementValueBlockLayout.setVisibility(View.GONE);
                    incrementValueBlock.setText("1");
                }
            }
        });

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
            resultNfcWriting.setBackgroundColor(getResources().getColor(R.color.white));
        });

        // you should have checked that this device is capable of working with Mifare Classic tags, otherwise you receive an exception
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
        boolean writeSuccess = false;
        try {
            mfc.connect();

            if (mfc.isConnected()) {

                // factory setting for AccessByte: FF 07 80
                // see http://calc.gmss.ru/Mifare1k/
                // Saddle West card sector 14 is available

                // https://stackoverflow.com/questions/26444995/mifare-1k-write-block-but-cannot-read-value-block
                // https://stackoverflow.com/questions/16480205/android-nfc-mifare-classic-1k-increment-operation-tranceive-failed
                // https://stackoverflow.com/questions/12208568/how-to-construct-a-value-block-on-mifareclassic-via-android-nfc-api


                // get sector and block to write
                String sectorChoiceString = sectorSelect.getText().toString();
                sectorToWrite = Integer.parseInt(sectorChoiceString);
                String blockChoiceString = blockSelect.getText().toString();
                blockToWrite = Integer.parseInt(blockChoiceString);

                // check if we should write a blank value or do an increment
                if (swIncrementValueBlock.isChecked()) {
                    // increment the value block
                    // write to tag
                    //int blockToWrite = 0;
                    int incrementValue = Integer.parseInt(incrementValueBlock.getText().toString());

                    if (incrementValue == 0) {
                        writeToUiAppend("enter a positive or negative number but not 0");
                        return;
                    }
                    if (incrementValue > 0) {
                        writeSuccess = incrementMifareSector(mfc, sectorToWrite, blockToWrite, incrementValue);
                        writeToUiAppend("Tried to increment data to tag, success ? : " + writeSuccess);
                    } else {
                        writeSuccess = decrementMifareSector(mfc, sectorToWrite, blockToWrite, incrementValue);
                        writeToUiAppend("Tried to decrement data to tag, success ? : " + writeSuccess);
                    }
                } else {
                    // write fixed value block data
                    // construct value block of value zero; "address" byte is set to 0 in this example
                    byte[] zeroValue = {0, 0, 0, 0, (byte) 255, (byte) 255, (byte) 255, (byte) 255, 0, 0, 0, 0, 0, (byte) 255, 0, (byte) 255};
                /*
                However, the format of the value-block is simple:
                byte 0..3:   32 bit value in little endian
                byte 4..7:   copy of byte 0..3, with inverted bits (aka. XOR 255)
                byte 8..11:  copy of byte 0..3
                byte 12:     index of backup block (can be any value)
                byte 13:     copy of byte 12 with inverted bits (aka. XOR 255)
                byte 14:     copy of byte 12
                byte 15:     copy of byte 13
                 */
                    //mifare.writeBlock(blockIndex, zeroValue);
                    // increase the value block by some amount
                    //mifare.increment(blockIndex, value);
                    // result is stored in scratch register inside tag; now write result to block
                    //mifare.transfer(blockIndex);
                    writeToUiAppend("This is the sample data block written to block " + blockToWrite+ " of sector " + sectorToWrite + " :\n" + bytesToHexNpe(zeroValue));

                /*

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

                 */

                    // todo check for AccessBytes before writing

                    // write to tag - 3 counterBlocks
                    //boolean writeSuccess = writeMifareSector3Blocks(mfc, sectorToWrite, zeroValue, zeroValue, zeroValue);

                    // write to one block only
                    writeSuccess = writeMifareSector1Block(mfc, sectorToWrite, blockToWrite, zeroValue);
                    writeToUiAppend("Tried to write data to tag, success ? : " + writeSuccess);
                }
                mfc.close();
            }
        } catch (IOException e) {
            writeToUiAppend("IOException on connection: " + e.getMessage());
            e.printStackTrace();
        }

        doVibrate(getActivity());
        playDoublePing();
        writeToUiFinal(resultNfcWriting);
        if (writeSuccess) {
            resultNfcWriting.setBackgroundColor(getResources().getColor(R.color.light_background_green));
        } else {
            resultNfcWriting.setBackgroundColor(getResources().getColor(R.color.light_background_red));
        }

    }

    private boolean incrementMifareSector(MifareClassic mif, int secCnt, int blockCnt, int incrValue) {
        if (!isAcFactorySetting(mif, secCnt)) {
            Log.e(TAG, "sector has not factory settings 'Access Bytes' (FF 07 80)");
            return false;
        }
        // todo sanity checks
        boolean isAuthenticated = false;
        // try to authenticate with known keys - no brute force
        Log.d(TAG, "");
        Log.d(TAG, "writeMifareSector " + secCnt);
        try {

            // this method is just using the KEY_DEFAULT_KEY for keyA
            if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_DEFAULT)) {

                Log.d(TAG, "Auth success with A KEY_DEFAULT");
                isAuthenticated = true;
            } else {
                Log.d(TAG, "NO Auth success");
                return false;
            }
            // get the blockindex
            int block_index = mif.sectorToBlock(secCnt);

            // increase the value block by some amount
            mif.increment(block_index + blockCnt, incrValue);
            // result is stored in scratch register inside tag; now write result to block
            mif.transfer(block_index + blockCnt);
        } catch (IOException e) {
            Log.e(TAG, "Sector " + secCnt + " Block " + blockCnt + " IOException: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean decrementMifareSector(MifareClassic mif, int secCnt, int blockCnt, int incrValue) {
        if (!isAcFactorySetting(mif, secCnt)) {
            Log.e(TAG, "sector has not factory settings 'Access Bytes' (FF 07 80)");
            return false;
        }
        // todo sanity checks

        // try to authenticate with known keys - no brute force
        Log.d(TAG, "");
        Log.d(TAG, "writeMifareSector " + secCnt);
        try {

            // this method is just using the KEY_DEFAULT_KEY for keyA
            if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_DEFAULT)) {
                Log.d(TAG, "Auth success with A KEY_DEFAULT");
            } else {
                Log.d(TAG, "NO Auth success");
                return false;
            }
            // get the block index
            int block_index = mif.sectorToBlock(secCnt);
            // decrease the value block by some amount, if the value is negative make it positive
            if (incrValue < 0) incrValue = incrValue * -1;
            mif.decrement(block_index + blockCnt, incrValue);
            // result is stored in scratch register inside tag; now write result to block
            mif.transfer(block_index + blockCnt);
        } catch (IOException e) {
            Log.e(TAG, "Sector " + secCnt + " Block " + blockCnt + " IOException: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean writeMifareSector1Block(MifareClassic mif, int secCnt, int blockCnt, byte[] blockData) {
        if (!isAcFactorySetting(mif, secCnt)) {
            Log.e(TAG, "sector has not factory settings 'Access Bytes' (FF 07 80), aborted");
            return false;
        }
        // try to authenticate with known key - no brute force
        Log.d(TAG, "");
        Log.d(TAG, "writeMifareSector for ValueBlock " + secCnt);
        try {
            // this method is just using the KEY_DEFAULT_KEY for keyA
            if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_DEFAULT)) {
                Log.d(TAG, "Auth success with A KEY_DEFAULT");
            }

            // get the block index
            int block_index = mif.sectorToBlock(secCnt);
            // get block in sector
            mif.writeBlock((block_index + blockCnt), blockData);
        } catch (IOException e) {
            Log.e(TAG, "Sector " + secCnt + " IOException: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private boolean writeMifareSector3Blocks(MifareClassic mif, int secCnt, byte[] bd1, byte[] bd2,
                                      byte[] bd3) {
        if (!isAcFactorySetting(mif, secCnt)) {
            Log.e(TAG, "sector has not factory settings 'Access Bytes' (FF 07 80)");
            return false;
        }
        // try to authenticate with known key - no brute force
        Log.d(TAG, "");
        Log.d(TAG, "writeMifareSector for ValueBlock " + secCnt);
        try {
            // this method is just using the KEY_DEFAULT_KEY for keyA
            if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_DEFAULT)) {
                Log.d(TAG, "Auth success with A KEY_DEFAULT");
            }

            // get the block index
            int block_index = mif.sectorToBlock(secCnt);
            // get block in sector
            //int blocksInSector = mif.getBlockCountInSector(secCnt);
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

    /**
     * reads the sector and checks if the access bytes are factory settings = FF 07 80
     * @param mif
     * @param secCnt
     * @return true is factory setting or false if not
     */
    private boolean isAcFactorySetting(MifareClassic mif, int secCnt) {
        try {
            // this method is just using the KEY_DEFAULT_KEY for keyA
            if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_DEFAULT)) {
                Log.d(TAG, "Auth success with A KEY_DEFAULT");
            } else {
                Log.d(TAG, "NO Auth success");
                return false;
            }
            // get the blockindex
            int block_index = mif.sectorToBlock(secCnt);
            // get block in sector
            int blocksInSector = mif.getBlockCountInSector(secCnt);
            // get the data of block 3 = keys & access bytes
            byte[] block = mif.readBlock((block_index + 3));
            // get the access bytes
            byte[] accessBytes = Arrays.copyOfRange(block, 6, 9);
            // factory setting is FF 07 80
            if (Arrays.equals(accessBytes, hexStringToByteArray("FF0780"))) return true;
        } catch (IOException e) {
            //throw new RuntimeException(e);
            return false;
        }
        return false;
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