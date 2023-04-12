package de.androidcrypto.nfcmifareclassicexample;

import static android.content.Context.VIBRATOR_SERVICE;
import static de.androidcrypto.nfcmifareclassicexample.Utils.bytesToHexNpe;
import static de.androidcrypto.nfcmifareclassicexample.Utils.doVibrate;
import static de.androidcrypto.nfcmifareclassicexample.Utils.hexStringToByteArray;
import static de.androidcrypto.nfcmifareclassicexample.Utils.playSinglePing;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReadFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReadFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "ReadFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ReadFragment() {
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
    public static ReadFragment newInstance(String param1, String param2) {
        ReadFragment fragment = new ReadFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    TextView readResult;
    Button checkAccessBytes;
    private String outputString = ""; // used for the UI output
    private NfcAdapter mNfcAdapter;
    String dumpExportString = "";
    String tagIdString = "";
    String tagTypeString = "";
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 100;
    Context contextSave;
    List<SectorMc1kModel> sectorMc1kModels;

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
        checkAccessBytes = getView().findViewById(R.id.btnCheckAccessBytes);
        //doVibrate();

        checkAccessBytes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // we do have these access bytes found: 787788 + 7f0788
                byte[] accBytes1 = hexStringToByteArray("787788");
                byte[] accBytes2 = hexStringToByteArray("7f0788");

                byte[][] accBytes1Matrix = ACBytesToACMatrix(accBytes1);
                byte[][] accBytes2Matrix = ACBytesToACMatrix(accBytes2);
                Context context = getContext();
                String desc11 = GetAccessConditionsDescription(context, accBytes1Matrix, 0, false);
                String desc12 = GetAccessConditionsDescription(context, accBytes1Matrix, 1, false);
                String desc13 = GetAccessConditionsDescription(context, accBytes1Matrix, 2, false);
                String desc14 = GetAccessConditionsDescription(context, accBytes1Matrix, 3, true);
                String desc21 = GetAccessConditionsDescription(context, accBytes2Matrix, 5, false);
                String desc22 = GetAccessConditionsDescription(context, accBytes2Matrix, 6, false);
                String desc23 = GetAccessConditionsDescription(context, accBytes2Matrix, 7, false);
                String desc24 = GetAccessConditionsDescription(context, accBytes2Matrix, 8, true);
                StringBuilder sb = new StringBuilder();
                sb.append("accBytesMatrix description").append("\n");
                sb.append("desc11: ").append(desc11).append("\n");
                sb.append("desc12: ").append(desc12).append("\n");
                sb.append("desc13: ").append(desc13).append("\n");
                sb.append("desc14: ").append(desc14).append("\n");
                sb.append("desc21: ").append(desc21).append("\n");
                sb.append("desc22: ").append(desc22).append("\n");
                sb.append("desc23: ").append(desc23).append("\n");
                sb.append("desc24: ").append(desc24).append("\n");
                writeToUiAppend(sb.toString());
                writeToUiFinal(readResult);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_read, container, false);
    }

    //public static String GetAccessConditionsDescription(byte[][] sectorAccessBits, int blockIndex, boolean isSectorTrailer) {
    public static String GetAccessConditionsDescription(Context context, byte[][] sectorAccessBits, int blockIndex, boolean isSectorTrailer) {
        if(sectorAccessBits == null || blockIndex < 0 || blockIndex >= sectorAccessBits[0].length) {
            return "";
        }
        int accessBitsNumber = (sectorAccessBits[0][blockIndex] << 2) | (sectorAccessBits[1][blockIndex] << 1) | sectorAccessBits[0][blockIndex];
        String resAccessCondsPrefix = isSectorTrailer ? "ac_sector_trailer_" : "ac_data_block_";
        String accessCondsResIdStr = resAccessCondsPrefix + accessBitsNumber;
        //Context appContext = localMFCDataIface.GetApplicationContext();
        Log.d(TAG, "resAccessCondsPrefix: " + resAccessCondsPrefix);
        Log.d(TAG, "accessCondsResIdStr: " + accessCondsResIdStr);
        Context appContext = context;
        try {
            int accessCondsResId = R.string.class.getField(accessCondsResIdStr).getInt(null);
            Log.d(TAG, "appContext.getResources().getString(accessCondsResId): " + appContext.getResources().getString(accessCondsResId));
            return appContext.getResources().getString(accessCondsResId);
        } catch(Exception nsfe) {
            Log.e(TAG, "Exception in GetAccessConditionsDescription: " + nsfe.getMessage());
            return "";
        }
    }

    /**
     * Convert the Access Condition bytes to a matrix containing the
     * resolved C1, C2 and C3 for each block.
     * @param acBytes The Access Condition bytes (3 byte).
     * @return Matrix of access conditions bits (C1-C3) where the first
     * dimension is the "C" parameter (C1-C3, Index 0-2) and the second
     * dimension is the block number (Index 0-3). If the ACs are incorrect
     * null will be returned.
     */
    private static byte[][] ACBytesToACMatrix(byte acBytes[]) {
        // ACs correct?
        // C1 (Byte 7, 4-7) == ~C1 (Byte 6, 0-3) and
        // C2 (Byte 8, 0-3) == ~C2 (Byte 6, 4-7) and
        // C3 (Byte 8, 4-7) == ~C3 (Byte 7, 0-3)
        byte[][] acMatrix = new byte[3][4];
        if (acBytes.length > 2 &&
                (byte)((acBytes[1]>>>4)&0x0F)  ==
                        (byte)((acBytes[0]^0xFF)&0x0F) &&
                (byte)(acBytes[2]&0x0F) ==
                        (byte)(((acBytes[0]^0xFF)>>>4)&0x0F) &&
                (byte)((acBytes[2]>>>4)&0x0F)  ==
                        (byte)((acBytes[1]^0xFF)&0x0F)) {
            // C1, Block 0-3
            for (int i = 0; i < 4; i++) {
                acMatrix[0][i] = (byte)((acBytes[1]>>>4+i)&0x01);
            }
            // C2, Block 0-3
            for (int i = 0; i < 4; i++) {
                acMatrix[1][i] = (byte)((acBytes[2]>>>i)&0x01);
            }
            // C3, Block 0-3
            for (int i = 0; i < 4; i++) {
                acMatrix[2][i] = (byte)((acBytes[2]>>>4+i)&0x01);
            }
            return acMatrix;
        }
        return null;
    }


    // This method is running in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        System.out.println("NFC tag discovered");
        playSinglePing();
        outputString = "";
        sectorMc1kModels = new ArrayList<>();
        requireActivity().runOnUiThread(() -> {
            readResult.setText("");
        });

        // you should have checked that this device is capable of working with Mifare Classic tags, therwise you receive an exception

        MifareClassic mfc = MifareClassic.get(tag);
        if (mfc == null) {
            writeToUiAppend("The tag is not readable with Mifare Classic classes, sorry");
            writeToUiFinal(readResult);
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
/*
keys for red crowne card
4D57414C5648
FFFFFFFFFFFF
A0A1A2A3A4A5
4D48414C5648
 */
/*
promark keys
[ 0] ffffffffffff
[ 1] 000000000000
[ 2] a0a1a2a3a4a5
[ 3] b0b1b2b3b4b5
[ 4] c0c1c2c3c4c5
[ 5] d0d1d2d3d4d5
[ 6] aabbccddeeff
[ 7] 1a2b3c4d5e6f
[ 8] 123456789abc
[ 9] 010203040506
[10] 123456abcdef
[11] abcdef123456
[12] 4d3a99c351dd
[13] 1a982c7e459a
[14] d3f7d3f7d3f7
[15] 714c5c886e97
[16] 587ee5f9350f
[17] a0478cc39091
[18] 533cb6c723f6
[19] 8fd0a4f256e9

 */

        // go through all sectors
        try {
            mfc.connect();

            if (mfc.isConnected()) {
                for (int secCnt = 0; secCnt < sectorCount; secCnt++) {
                    writeToUiAppend("");
                    // this is the loop for all sectors of a Mifare Classic card
                    int sectorNumber;
                    boolean isSector0 = false;
                    boolean isReadableSector;
                    byte[] sectorData = new byte[64]; // sector 0 first 16 bytes a re UID & manufacture info, last 16 bytes is access block
                    byte[] uidData; // contains the UID & manufacture info, only if isSector0 = true
                    byte[] blockData; // contains 2 (if sector 0) or 3 blocks of 16 bytes of data
                    byte[] accessBlock = new byte[16]; // complete block, sections see below. key A and B are nulled as they can't read out
                    byte[] keyA = new byte[6]; // access key A
                    byte[] accessBits = new byte[3]; // 3 access bytes for access to the data elements
                    byte[] unused = new byte[1]; // unused byte, can be used for data
                    byte[] keyB = new byte[6]; // access key B
                    byte[][] readResult = readMifareSector(mfc, secCnt);
                    SectorMc1kModel sectorMc1kModel = null;
                    if (readResult == null) {
                        writeToUiAppend("ERROR - sector " + secCnt + " could not get read");
                        // write a zero data to SectorMc1kModel
                        sectorNumber = secCnt;
                        if (secCnt == 0) isSector0 = true;
                        isReadableSector = false;
                        sectorData = null;
                        uidData = null;
                        blockData = null;
                        accessBlock = null;
                        keyA = null;
                        accessBits = null;
                        unused = null;
                        keyB = null;
                        sectorMc1kModel = new SectorMc1kModel(
                                sectorNumber,
                                isSector0,
                                isReadableSector,
                                sectorData,
                                uidData,
                                blockData,
                                accessBlock,
                                keyA,
                                accessBits,
                                unused,
                                keyB
                        );
                    } else {
                        // analyze content data and write to SectorMc1kModel
                        sectorNumber = secCnt;
                        if (secCnt == 0) isSector0 = true;
                        isReadableSector = true;
                        sectorData = readResult[0];
                        uidData = Arrays.copyOfRange(sectorData, 0, 16);
                        // length depends on sector0 or not
                        if (isSector0) {
                            blockData = Arrays.copyOfRange(sectorData, 16, 48);
                        } else {
                            blockData = Arrays.copyOfRange(sectorData, 0, 48);
                        }
                        accessBlock = Arrays.copyOfRange(sectorData, 48, 64);
                        keyA = readResult[1];
                        accessBits = Arrays.copyOfRange(accessBlock, 6, 9);
                        unused = Arrays.copyOfRange(accessBlock, 9, 10);
                        keyB = readResult[2];
                        sectorMc1kModel = new SectorMc1kModel(
                          sectorNumber,
                          isSector0,
                          isReadableSector,
                          sectorData,
                          uidData,
                          blockData,
                          accessBlock,
                          keyA,
                          accessBits,
                          unused,
                          keyB
                        );
                    }
                    sectorMc1kModels.add(sectorMc1kModel);
                    if (sectorMc1kModel != null) {
                        writeToUiAppend(sectorMc1kModel.dump());
                    }
                } // for (int secCnt = 0; secCnt < sectorCount; secCnt++) {
                writeToUiAppend("collected all sectors in sectorMc1kModels: " + sectorMc1kModels.size());
            }
            mfc.close();

        } catch (IOException e) {
            writeToUiAppend("IOException on connection: " + e.getMessage());
            e.printStackTrace();
        }

        writeToUiFinal(readResult);
        playDoublePing();
        doVibrate(getActivity());
    }

    /**
     * read mifare classic card sector by sector
     * @param mif
     * @param secCnt 0 to 15 (for Mifare Classic 1K) or in general sectorCount
     * @return a double byte array
     * [0] returns the complete data of one sector
     * [1] returns key A if used for authentication
     * [2] returns key B if used for authentication
     * returns NULL if no key was found
     */
    private byte[][] readMifareSector(MifareClassic mif, int secCnt) {
        byte[][] returnBytes = new byte[3][64];
        byte[] keyABytes = null;
        byte[] keyBBytes = null;
        byte[] dataBytes = new byte[64];
        boolean isAuthenticated = false;
        // try to authenticate with known keys - no brute force
        try {
            if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                keyABytes = MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY.clone();
                isAuthenticated = true;
                // there are 3 default keys available
                // MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY: a0a1a2a3a4a5
                // MifareClassic.KEY_DEFAULT:                      ffffffffffff
                // MifareClassic.KEY_NFC_FORUM:                    d3f7d3f7d3f7
            } else if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_DEFAULT)) {
                keyABytes = MifareClassic.KEY_DEFAULT.clone();
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_NFC_FORUM)) {
                keyABytes = MifareClassic.KEY_NFC_FORUM;
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_DEFAULT)) {
                keyBBytes = MifareClassic.KEY_DEFAULT.clone();
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                keyBBytes = MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY.clone();
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_NFC_FORUM)) {
                keyBBytes = MifareClassic.KEY_NFC_FORUM;
                isAuthenticated = true;
            } else {
                //return null;
            }
            // get the blockindex
            int block_index = mif.sectorToBlock(secCnt);
            // get block in sector
            int blocksInSector = mif.getBlockCountInSector(secCnt);
            // get the data of each block
            dataBytes = new byte[(16 * blocksInSector)];
            for (int blockInSectorCount = 0; blockInSectorCount < blocksInSector; blockInSectorCount++) {
                // get following data
                byte[] block = mif.readBlock((block_index + blockInSectorCount));
                System.arraycopy(block, 0, dataBytes, (blockInSectorCount * 16), 16);
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        System.out.println("*** dataBytes for sector " + secCnt + " length: " + dataBytes.length + " data: " + bytesToHexNpe(dataBytes));
        returnBytes[0] = dataBytes;
        returnBytes[1] = keyABytes;
        returnBytes[2] = keyBBytes;
        return returnBytes;
    }

/*
    private byte[] readMifareSector(MifareClassic mif, int secCnt) {
        byte[] returnBytes = new byte[0];
        boolean isAuthenticated = false;
        // try to authenticate with known keys - no brute force
        try {
            if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_DEFAULT)) {
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_NFC_FORUM)) {
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_DEFAULT)) {
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_NFC_FORUM)) {
                isAuthenticated = true;
                writeToUiAppend(readResult, "auth success in sector: " + secCnt);
            } else {
                writeToUiAppend(readResult, "auth denied in sector: " + secCnt);
                // gives an error, no access to textview from background thread
                return null;
            }
            // get the blockindex
            int block_index = mif.sectorToBlock(secCnt);
            // get block in sector
            int blocksInSector = mif.getBlockCountInSector(secCnt);
            // get the data of each block
            returnBytes = new byte[(16 * blocksInSector)];
            for (int blockInSectorCount = 0; blockInSectorCount < blocksInSector; blockInSectorCount++) {
                // get following data
                byte[] block = mif.readBlock((block_index + blockInSectorCount));
                System.arraycopy(block, 0, returnBytes, (blockInSectorCount * 16), 16);
            }
        } catch (IOException e) {
            writeToUiAppend(readResult, "IOException: " + e);
            e.printStackTrace();
            return null;
        }
        return returnBytes;
    }

 */

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
        if (textView == (TextView) readResult) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(outputString);
                    System.out.println(outputString); // print the data to console
                }
            });
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

}