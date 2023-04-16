package de.androidcrypto.nfcmifareultralightexample;

import static de.androidcrypto.nfcmifareultralightexample.Utils.bytesToHexNpe;
import static de.androidcrypto.nfcmifareultralightexample.Utils.doVibrate;
import static de.androidcrypto.nfcmifareultralightexample.Utils.hexStringToByteArray;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

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
    Button checkAccessBytes, dumpColored;
    private View loadingLayout;
    private String outputString = ""; // used for the UI output
    private NfcAdapter mNfcAdapter;
    String dumpExportString = "";
    String tagIdString = "";
    String tagTypeString = "";
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 100;
    Context contextSave;
    List<SectorMc1kModel> sectorMc1kModels;
    private byte[][] pagesComplete;
    private int pagesToRead;
    private boolean isUltralight = false;
    private boolean isUltralightC = false;
    private boolean isUltralightEv1 = false;
    private int counter0 = 0;
    private int counter1 = 0;
    private int counter2 = 0;

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
        dumpColored = getView().findViewById(R.id.btnDumpColored);
        loadingLayout = getView().findViewById(R.id.loading_layout);

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
                String desc21 = GetAccessConditionsDescription(context, accBytes2Matrix, 0, false);
                String desc22 = GetAccessConditionsDescription(context, accBytes2Matrix, 1, false);
                String desc23 = GetAccessConditionsDescription(context, accBytes2Matrix, 2, false);
                String desc24 = GetAccessConditionsDescription(context, accBytes2Matrix, 3, true);
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

                // a human readable form


                writeToUiAppend(sb.toString());
                writeToUiFinal(readResult);
            }
        });

        dumpColored.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pagesComplete != null) {
                    int numberOfPages = pagesToRead;
                    readResult.setBackgroundColor(getResources().getColor(R.color.dark_gray));
                    SpannableStringBuilder ssb = new SpannableStringBuilder();
                    for (int i = 0; i < numberOfPages; i++) {
                        SpannableString sector = colorString("Sector: " + String.valueOf(i),
                                ContextCompat.getColor(getContext(), R.color.blue));
                        ssb.append(sector).append("\n");
                        byte[] page = pagesComplete[i];
                        if (page != null) {
                            SpannableString spStr0;
                            if (i == 0) {
                                spStr0 = colorDataBlock(bytesToHexNpe(page), true);
                            } else if (i == 1) {
                                spStr0 = colorDataBlock(bytesToHexNpe(page), true);
                            } else if (i == 2) {
                                spStr0 = colorString(bytesToHexNpe(page),
                                        ContextCompat.getColor(getContext(), R.color.yellow));
                            } else if (i == 3) {
                                spStr0 = colorString(bytesToHexNpe(page),
                                        ContextCompat.getColor(getContext(), R.color.light_green));
                            }


                            else {
                                spStr0 = colorDataBlock(bytesToHexNpe(page), false);
                            }
                            ssb.append(spStr0).append("\n");
                            //ssb.append(spStr1).append("\n");
                            //ssb.append(spStr2).append("\n");
                            //ssb.append(spStr3).append("\n"); // trailer
                        } else {
                            // page was not readable
                            SpannableString error = colorString("page was not readable: " + String.valueOf(i),
                                    ContextCompat.getColor(getContext(), R.color.red));
                            ssb.append(error).append("\n");
                            ssb.append("sector was not readable").append("\n");
                        }
                        ssb.append("\n");
                    }
                    // caption
                    // Color caption.
                    SpannableString keyA = colorString("KeyA",
                            ContextCompat.getColor(getContext(), R.color.light_green));
                    SpannableString keyB = colorString("KeyB",
                            ContextCompat.getColor(getContext(), R.color.dark_green));
                    SpannableString ac = colorString("ACs",
                            ContextCompat.getColor(getContext(), R.color.orange));
                    SpannableString uidAndManuf = colorString("UID & ManuInfo",
                            ContextCompat.getColor(getContext(), R.color.purple));
                    SpannableString vb = colorString("ValueBlock",
                            ContextCompat.getColor(getContext(), R.color.yellow));
                    SpannableString sep = colorString(" | ",
                            ContextCompat.getColor(getContext(), R.color.white));
                    SpannableString sep2 = colorString(" | ",
                            ContextCompat.getColor(getContext(), R.color.white));
                    SpannableString sep3 = colorString(" | ",
                            ContextCompat.getColor(getContext(), R.color.white));
                    SpannableString sep4 = colorString(" | ",
                            ContextCompat.getColor(getContext(), R.color.white));
                    ssb.append(uidAndManuf).append(sep).append(vb).append(sep2).append(keyA).append(sep3).append(keyB).append(sep4).append(ac).append("\n");
                    /*
                    ssb.append(TextUtils.concat(uidAndManuf, sep,
                            vb, sep, keyA, sep, keyB, sep, ac)).append("\n");

                     */
                    /*
                    ssb.append(TextUtils.concat(uidAndManuf, " | ",
                            vb, " | ", keyA, " | ", keyB, " | ", ac)).append("\n");
                    */
                    readResult.setText(ssb);


                }
            }
        });
    }

    /**
     * Create a full colored string (representing one block).
     *
     * @param data   Block data as hex string (16 Byte, 32 Chars.).
     * @param hasUID True if the block is the first block of the entire tag
     *               (Sector 0, Block 0).
     * @return A full colored string.
     */
    private SpannableString colorDataBlock(String data, boolean hasUID) {
        SpannableString ret;
        if (hasUID) {
            // First block (UID, manuf. data).
            ret = new SpannableString(TextUtils.concat(
                    colorString(data,
                            ContextCompat.getColor(getContext(), R.color.purple))));
        } else {
            if (isValueBlock(data)) {
                // Value block.
                ret = colorString(data,
                        ContextCompat.getColor(getContext(), R.color.yellow));
            } else {
                // Just data.
                //ret = new SpannableString(data);
                ret = colorString(data,
                        ContextCompat.getColor(getContext(), R.color.white));
            }
        }
        return ret;
    }

    /**
     * Create a full colored sector trailer (representing the last block of
     * every sector).
     *
     * @param data Block data as hex string (16 Byte, 32 Chars.).
     * @return A full colored string.
     */
    private SpannableString colorSectorTrailer(String data) {
        // Get sector trailer colors.
        int colorKeyA = ContextCompat.getColor(getContext(), R.color.light_green);
        int colorKeyB = ContextCompat.getColor(getContext(), R.color.dark_green);
        int colorAC = ContextCompat.getColor(getContext(), R.color.orange);
        int colorDatByte = ContextCompat.getColor(getContext(), R.color.white);
        try {
            SpannableString keyA = colorString(
                    data.substring(0, 12), colorKeyA);
            SpannableString keyB = colorString(
                    data.substring(20), colorKeyB);
            SpannableString ac = colorString(
                    data.substring(12, 18), colorAC);
            SpannableString datByte = colorString(
                    data.substring(18, 20), colorDatByte);
            return new SpannableString(
                    TextUtils.concat(keyA, ac, datByte, keyB));
        } catch (IndexOutOfBoundsException e) {
            Log.d(TAG, "Error while coloring " +
                    "sector trailer");
        }
        return new SpannableString(data);
    }

    /**
     * Create a colored string.
     *
     * @param data  The text to be colored.
     * @param color The color for the text.
     * @return A colored string.
     */
    public static SpannableString colorString(String data, int color) {
        SpannableString ret = new SpannableString(data);
        ret.setSpan(new ForegroundColorSpan(color),
                0, data.length(), 0);
        return ret;
    }

    /**
     * Check if the given block (hex string) is a value block.
     * NXP has PDFs describing what value blocks are. Google something
     * like "nxp MIFARE classic value block" if you want to have a
     * closer look.
     *
     * @param hexString Block data as hex string.
     * @return True if it is a value block. False otherwise.
     */
    public static boolean isValueBlock(String hexString) {
        byte[] b = hexStringToByteArray(hexString);
        if (b != null && b.length == 16) {
            // Google some NXP info PDFs about MIFARE Classic to see how
            // Value Blocks are formatted.
            // For better reading (~ = invert operator):
            // if (b0=b8 and b0=~b4) and (b1=b9 and b9=~b5) ...
            // ... and (b12=b14 and b13=b15 and b12=~b13) then
            return (b[0] == b[8] && (byte) (b[0] ^ 0xFF) == b[4]) &&
                    (b[1] == b[9] && (byte) (b[1] ^ 0xFF) == b[5]) &&
                    (b[2] == b[10] && (byte) (b[2] ^ 0xFF) == b[6]) &&
                    (b[3] == b[11] && (byte) (b[3] ^ 0xFF) == b[7]) &&
                    (b[12] == b[14] && b[13] == b[15] &&
                            (byte) (b[12] ^ 0xFF) == b[13]);
        }
        return false;
    }

    private String getAccessConditionHR(Context context, byte[] accessBytes, int blockIndex, boolean isSectorTrailer) {
        byte[][] accBytesMatrix = ACBytesToACMatrix(accessBytes);
        String desc = GetAccessConditionsDescription(context, accBytesMatrix, blockIndex, isSectorTrailer);
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_read, container, false);
    }

    //public static String GetAccessConditionsDescription(byte[][] sectorAccessBits, int blockIndex, boolean isSectorTrailer) {
    public static String GetAccessConditionsDescription(Context context, byte[][] sectorAccessBits, int blockIndex, boolean isSectorTrailer) {
        if (sectorAccessBits == null || blockIndex < 0 || blockIndex >= sectorAccessBits[0].length) {
            return "";
        }
        int accessBitsNumber = (sectorAccessBits[0][blockIndex] << 2) | (sectorAccessBits[1][blockIndex] << 1) | sectorAccessBits[0][blockIndex];
        String resAccessCondsPrefix = isSectorTrailer ? "ac_sector_trailer_" : "ac_data_block_";
        String accessCondsResIdStr = resAccessCondsPrefix + accessBitsNumber;
        //Context appContext = localMFCDataIface.GetApplicationContext();
        Log.d(TAG, "resAccessCondsPrefix: " + resAccessCondsPrefix);
        Log.d(TAG, "accessCondsResIdStr: " + accessCondsResIdStr);
        try {
            int accessCondsResId = R.string.class.getField(accessCondsResIdStr).getInt(null);
            Log.d(TAG, "appContext.getResources().getString(accessCondsResId): " + context.getResources().getString(accessCondsResId));
            return context.getResources().getString(accessCondsResId);
        } catch (Exception nsfe) {
            Log.e(TAG, "Exception in GetAccessConditionsDescription: " + nsfe.getMessage());
            return "";
        }
    }

    /**
     * Convert the Access Condition bytes to a matrix containing the
     * resolved C1, C2 and C3 for each block.
     *
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
                (byte) ((acBytes[1] >>> 4) & 0x0F) ==
                        (byte) ((acBytes[0] ^ 0xFF) & 0x0F) &&
                (byte) (acBytes[2] & 0x0F) ==
                        (byte) (((acBytes[0] ^ 0xFF) >>> 4) & 0x0F) &&
                (byte) ((acBytes[2] >>> 4) & 0x0F) ==
                        (byte) ((acBytes[1] ^ 0xFF) & 0x0F)) {
            // C1, Block 0-3
            for (int i = 0; i < 4; i++) {
                acMatrix[0][i] = (byte) ((acBytes[1] >>> 4 + i) & 0x01);
            }
            // C2, Block 0-3
            for (int i = 0; i < 4; i++) {
                acMatrix[1][i] = (byte) ((acBytes[2] >>> i) & 0x01);
            }
            // C3, Block 0-3
            for (int i = 0; i < 4; i++) {
                acMatrix[2][i] = (byte) ((acBytes[2] >>> 4 + i) & 0x01);
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
        setLoadingLayoutVisibility(true);
        outputString = "";
        sectorMc1kModels = new ArrayList<>();

        requireActivity().runOnUiThread(() -> {
            readResult.setBackgroundColor(getResources().getColor(R.color.white));
            readResult.setText("");
        });

        // you should have checked that this device is capable of working with Mifare Ultralight tags, otherwise you receive an exception


        MifareUltralight mfu = MifareUltralight.get(tag);

        if (mfu == null) {
            writeToUiAppend("The tag is not readable with Mifare Ultralight classes, sorry");
            writeToUiFinal(readResult);
            setLoadingLayoutVisibility(false);
            return;
        }

        // get card details
        int tagType = mfu.getType();
        StringBuilder sb = new StringBuilder();
        sb.append("MifareUltralight type: ").append(tagType).append("\n");
        byte[] id = mfu.getTag().getId();
        sb.append("Tag ID: ").append(bytesToHexNpe(id)).append("\n");
        String[] techlist = mfu.getTag().getTechList();
        sb.append("Tag Techlist: ").append(Arrays.toString(techlist));
        writeToUiAppend(sb.toString());

        // go through all sectors
        try {
            mfu.connect();

            if (mfu.isConnected()) {

                // we are trying to read 48 pages (maximum for Ultralight-C) although Ultralight and Ultralight EV-1 are smaller
                pagesToRead = 48;
                pagesComplete = new byte[pagesToRead][];
                for (int i = 0; i < pagesToRead; i++) {
                    pagesComplete[i] = readPage(mfu, i);
                    writeToUiAppend(printData("page " + i, pagesComplete[i]));
                }

                byte[] counter0B = getCounter(mfu, 0);
                byte[] counter1B = getCounter(mfu, 1);
                byte[] counter2B = getCounter(mfu, 2);
                writeToUiAppend(printData("counter0", counter0B));
                writeToUiAppend(printData("counter10=-", counter1B));
                writeToUiAppend(printData("counter2", counter2B));

                // 425245414b4d454946594f5543414e21
                byte[] defaultKey = hexStringToByteArray("425245414b4d454946594f5543414e21"); // "BREAKMEIFYOUCAN!", 16 bytes long
                byte[] defaultKeyZero = hexStringToByteArray("00000000000000000000000000000000"); // "..zeroes...", 16 bytes long
                // NXP default key: BREAKMEIFYOUCAN! (16 bytes)
                byte[] mifareULCDefaultKey = { (byte) 0x49, (byte) 0x45,
                        (byte) 0x4D, (byte) 0x4B, (byte) 0x41, (byte) 0x45,
                        (byte) 0x52, (byte) 0x42, (byte) 0x21, (byte) 0x4E,
                        (byte) 0x41, (byte) 0x43, (byte) 0x55, (byte) 0x4F,
                        (byte) 0x59, (byte) 0x46 };
                try {
                    authenticate(mfu, defaultKey);
                } catch (Exception e) {
                    //throw new RuntimeException(e);
                    // this is just an advice - if an error occurs - close the connection and reconnect the tag
                    // https://stackoverflow.com/a/37047375/8166854
                    try {
                        mfu.close();
                    } catch (Exception e1) {
                    }
                    try {
                        mfu.connect();
                    } catch (Exception e2) {
                    }
                }
                byte[] page04 = readPage(mfu, 04);
                writeToUiAppend(printData("page04", page04));


                // https://stackoverflow.com/questions/45545493/distinguish-different-types-of-mifare-ultralight
                // https://stackoverflow.com/questions/37002498/distinguish-ntag213-from-mf0icu2
                /*
In order to distinguish MIFARE Ultralight, Ultralight C, Ultralight EV1, and NTAG tags, you would first send a GET_VERSION command:

> 60
If this command succeeds, you know that the tag is an EV1 (or later) tag (e.g. MIFARE Ultralight EV1
or NTAG21x). You can, thus, narrow down the specific tag type by analyzing the resonse to the GET_VERSION command.
This will reveal the product type (NTAG or Ultralight EV1) as well as product subtype, product version and storage
size (which allows you to determine the exact chip type). See Distinguish NTAG213 from MF0ICU2 for a list of example
product identification values.

If the GET_VERSION command fails, you can assume that it is a first generation tag (MIFARE Ultralight, Ultralight C,
NTAG203). You can, thus, narrow down the specific tag type by sending an AUTHENTICATE (part 1) command:

> 1A 00
If this command succeeds, you know that the tag is MIFARE Ultralight C.

If this command fails, you can assume that the tag is either Ultralight or NTAG203. In order to distinguish between
MIFARE Ultralight and NTAG203, you can try to read pages that do not exist on Ultralight (e.g. read page 41):

> 30 29
Share
Edit
Follow
Flag
answered Aug 11, 2017 at 11:08
Michael Roland's user avatar
Michael Roland
                 */
                int storageSize = 0;
                isUltralight = false;
                isUltralightC = false;
                isUltralightEv1 = false;



                // checks for distinguishing the correct type of card
                byte[] getVersionResp = getVersion(mfu);
                byte[] doAuthResp = doAuthenticate(mfu);
                writeToUiAppend(printData("getVersionResp", getVersionResp));
                writeToUiAppend(printData("doAuthResp", doAuthResp));

                // if getVersionResponse is not null it is an Ultralight EV1 or later
                if (getVersionResp != null) {
                    isUltralightEv1 = true;
                    // storage size is in byte 6, 0b or 0e
                    if (getVersionResp[6] == (byte) 0x0b) {
                        storageSize = 64; // 48 bytes user memory
                        pagesToRead = storageSize / 4;
                    }
                    if (getVersionResp[6] == (byte) 0x0e) {
                        storageSize = 144; // 128 bytes user memory
                        pagesToRead = storageSize / 4;
                    }
                    Log.d(TAG, "Tag is an Mifare Ultralight EV1 with a storage size of " + storageSize + " bytes");
                } else {
                    // now we are checking if getVersionResponse is not null meaning an Ultralight-C tag
                    if (doAuthResp != null) {
                        isUltralightC = true;
                        storageSize = 192;
                        pagesToRead = storageSize / 4;
                        Log.d(TAG, "Tag is an Mifare Ultralight-C with a storage size of " + storageSize + " bytes");
                    } else {
                        // the tag is an Ultralight tag
                        isUltralight = true;
                        storageSize = 64;
                        pagesToRead = storageSize / 4;
                        Log.d(TAG, "Tag is an Mifare Ultralight with a storage size of " + storageSize + " bytes");
                    }
                }

                // tag identification
                if (isUltralight) {
                    writeToUiAppend("The tag is an Mifare Ultralight tag with a storage size of " + storageSize + " bytes");
                }
                if (isUltralightC) {
                    writeToUiAppend("The tag is an Mifare Ultralight-C tag with a storage size of " + storageSize + " bytes");
                }
                if (isUltralightEv1) {
                    writeToUiAppend("The tag is an Mifare Ultralight EV1 tag with a storage size of " + storageSize + " bytes");
                }

                if (storageSize == 0) {
                    writeToUiAppend("As the storage size is 0 the tag seems to be unknown to the app. I'm aborting, sorry.");
                }
/*
                // 425245414b4d454946594f5543414e21
                byte[] defaultKey = hexStringToByteArray("425245414b4d454946594f5543414e21"); // "BREAKMEIFYOUCAN!", 16 bytes long
                try {
                    authenticate(mfu, defaultKey);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
*/



                /*

                // see https://blog.linuxgemini.space/derive-pk-of-nxp-mifare-classic-ev1-ecdsa-signature
                // r can be read on PM3 with the command hf mf rdbl 69 B 4b791bea7bcc
                // s can be read on PM3 with the command hf mf rdbl 70 B 4b791bea7bcc
                byte[] r = readBlock(mfc, 69, hexStringToByteArray("4b791bea7bcc"));
                byte[] s = readBlock(mfc, 70, hexStringToByteArray("4b791bea7bcc"));
                if ((r != null) && (s != null)) {
                    System.out.println("r length:" + r.length + " data: " + bytesToHexNpe(r));
                    System.out.println("s length:" + s.length + " data: " + bytesToHexNpe(s));
                } else {
                    System.out.println("r and/or s are null");
                }

                for (int secCnt = 0; secCnt < sectorCount; secCnt++) {
                    writeToUiAppend("");
                    // this is the loop for all sectors of a Mifare Classic card
                    int sectorNumber;
                    boolean isSector0 = false;
                    boolean isReadableSector;
                    byte[] sectorData = new byte[64]; // sector 0 first 16 bytes a re UID & manufacture info, last 16 bytes is access block
                    byte[] uidData = null; // contains the UID & manufacture info, only if isSector0 = true
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

                        // length depends on sector0 or not
                        if (isSector0) {
                            uidData = Arrays.copyOfRange(sectorData, 0, 16);
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

                 */
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dumpColored.setEnabled(true);
                }
            });
            mfu.close();

        } catch (IOException e) {
            writeToUiAppend("IOException on connection: " + e.getMessage());
            e.printStackTrace();
        }

        writeToUiFinal(readResult);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());
    }


    /**
     * auth code taken from  https://stackoverflow.com/a/44640515/8166854
     * Note: this code uses Apache Commons Lang.
     * // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
     * implementation group: 'org.apache.commons', name: 'commons-lang3', version: '3.12.0'
     */
    public void authenticate(MifareUltralight mfu, byte[] key) throws Exception {
        System.out.println("AUTHENTICATE");
        //byte[] encRndB = transmitRaw(new byte[] { 0x1A, 0x00 });
        byte[] encRndB = mfu.transceive((new byte[] { 0x1A, 0x00 }));
        if((encRndB.length!=9)||(encRndB[0]!= (byte) 0xAF)) {
            throw new RuntimeException("Invalid response!");
        }
        encRndB=Arrays.copyOfRange(encRndB, 1, 9);
        System.out.println(" - EncRndB: " + bytesToHexNpe(encRndB));
        byte[] rndB = desDecrypt(key, encRndB);
        System.out.println(" - RndB: " + bytesToHexNpe(rndB));
        byte[] rndBrot = rotateLeft(rndB);
        System.out.println(" - RndBrot: " + bytesToHexNpe(rndBrot));
        byte[] rndA = new byte[8];
        generateRandom(rndA);
        System.out.println(" - RndA: " + bytesToHexNpe(rndA));
        //byte[] encRndArotPrime = transmitRaw(ArrayUtils.addAll(new byte[] {(byte) 0xAF}, desEncrypt(key, ArrayUtils.addAll(rndA, rndBrot))));
        byte[] encRndArotPrime;
        try {
            encRndArotPrime = mfu.transceive((ArrayUtils.addAll(new byte[]{(byte) 0xAF}, desEncrypt(key, ArrayUtils.addAll(rndA, rndBrot)))));
        } catch (IOException e) {
            Log.e(TAG, "IOEx on second auth round");
            return;
        }

        if((encRndArotPrime.length!=9)||(encRndArotPrime[0]!=0x00)) {
            throw new RuntimeException("Invalid response!");
        }
        encRndArotPrime=Arrays.copyOfRange(encRndArotPrime, 1, 9);
        System.out.println(" - EncRndArot': " + bytesToHexNpe(encRndArotPrime));
        byte[] rndArotPrime = desDecrypt(key, encRndArotPrime);
        System.out.println(" - RndArot': " + bytesToHexNpe(rndArotPrime));
        if(!Arrays.equals(rotateLeft(rndA), rndArotPrime)) {
            throw new RuntimeException("Card authentication failed");
        } else {
            Log.d(TAG, "Card authentication success");
            System.out.println("Card authentication success");
        }
    }

    protected static SecureRandom rnd = new SecureRandom();
    protected static void generateRandom(byte[] rndA) {
        rnd.nextBytes(rndA);
    }

    protected byte[] desEncrypt(byte[] key, byte[] data) {
        return performDes(Cipher.ENCRYPT_MODE, key, data);
    }
    protected byte[] desDecrypt(byte[] key, byte[] data) {
        return performDes(Cipher.DECRYPT_MODE, key, data);
    }
    private byte[] iv = new byte[8];
    protected byte[] performDes(int opMode, byte[] key, byte[] data) {
        try {
            Cipher des = Cipher.getInstance("DESede/CBC/NoPadding");
            SecretKeyFactory desKeyFactory = SecretKeyFactory.getInstance("DESede");
            Key desKey = desKeyFactory.generateSecret(new DESedeKeySpec(ArrayUtils.addAll(key, Arrays.copyOf(key, 8))));
            des.init(opMode, desKey, new IvParameterSpec(iv));
            byte[] ret = des.doFinal(data);
            if(opMode== Cipher.ENCRYPT_MODE) {
                iv=Arrays.copyOfRange(ret, ret.length-8, ret.length);
            } else {
                iv=Arrays.copyOfRange(data, data.length-8, data.length);
            }
            return ret;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidKeySpecException |
                 IllegalBlockSizeException |
                 BadPaddingException |
                 InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    protected static byte[] rotateLeft(byte[] in) {
        return ArrayUtils.add(Arrays.copyOfRange(in, 1, 8), in[0]);
    }

    /**
     * auth code ends
     */

    private byte[] getVersion(MifareUltralight mfu) {
        byte[] getVersionResponse = null;
        try {
            byte[] getVersionCommand = new byte[]{(byte) 0x60};
            getVersionResponse = mfu.transceive(getVersionCommand);
            return getVersionResponse;
        } catch (IOException e) {
            Log.d(TAG, "getVersion unsupported, IOException: " + e.getMessage());
            //writeToUiAppend("getVersion unsupported, IOException: " + e.getMessage());
        }
        // this is just an advice - if an error occurs - close the connection and reconnect the tag
        // https://stackoverflow.com/a/37047375/8166854
        try {
            mfu.close();
        } catch (Exception e) {
        }
        try {
            mfu.connect();
        } catch (Exception e) {
        }
        return null;
    }

    // just for distinguish between Ultralight (auth fails) and Ultralight C (auth succeeds)
    private byte[] doAuthenticate(MifareUltralight mfu) {
        byte[] getAuthresponse = null;
        try {
            byte[] getAuthCommand = new byte[]{(byte) 0x1a, (byte) 0x00};
            getAuthresponse = mfu.transceive(getAuthCommand);
            return getAuthresponse;
        } catch (IOException e) {
            writeToUiAppend("doAuthentication unsupported, IOException: " + e.getMessage());
        }
        // this is just an advice - if an error occurs - close the connenction and reconnect the tag
        // https://stackoverflow.com/a/37047375/8166854
        try {
            mfu.close();
        } catch (Exception e) {
        }
        try {
            mfu.connect();
        } catch (Exception e) {
        }
        return null;
    }

    private byte[] getCounter(MifareUltralight mfu, int counterNumber) {
        if ((counterNumber < 0) | (counterNumber > 2)) {
            return null;
        }
        byte[] response = null;
        try {
            byte[] getCounterCommand = new byte[]{(byte) 0x39, (byte) (counterNumber & 0x0ff)};
            response = mfu.transceive(getCounterCommand);
            return response;
        } catch (IOException e) {
            //writeToUiAppend("doAuthentication unsupported, IOException: " + e.getMessage());
        }
        // this is just an advice - if an error occurs - close the connenction and reconnect the tag
        // https://stackoverflow.com/a/37047375/8166854
        try {
            mfu.close();
        } catch (Exception e) {
        }
        try {
            mfu.connect();
        } catch (Exception e) {
        }
        return null;
    }


    private String printData(String dataName, byte[] data) {
        int dataLength;
        String dataString = "";
        if (data == null) {
            dataLength = 0;
            dataString = "IS NULL";
        } else {
            dataLength = data.length;
            dataString = Utils.bytesToHexNpe(data);
        }
        StringBuilder sb = new StringBuilder();
        sb
                .append(dataName)
                .append(" length: ")
                .append(dataLength)
                .append(" data: ")
                .append(dataString);
        return sb.toString();
    }

    private byte[] readPage(MifareUltralight mfu, int page) {
        byte[] response = null;
        try {
            response = mfu.transceive(new byte[]{
                    (byte) 0x30,           // READ a page is 4 bytes long
                    (byte) (page & 0x0ff)  // page address
            });
            return response;
        } catch (IOException e) {
            Log.d(TAG, "on page " + page + " readPage failed with IOException: " + e.getMessage());
            //writeToUiAppend("on page " + page + " readPage failed with IOException: " + e.getMessage());
        }
        // this is just an advice - if an error occurs - close the connection and reconnect the tag
        // https://stackoverflow.com/a/37047375/8166854
        try {
            mfu.close();
        } catch (Exception e) {
        }
        try {
            mfu.connect();
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * read a single block from mifare classic tag by block
     *
     * @param mif
     * @param blockCnt
     * @param key      usually keyB for blocks outside the scope of user accessible memory
     * @return the content of block (16 bytes) or null if any error occurs
     */
    private byte[] readBlock(MifareClassic mif, int blockCnt, byte[] key) {
        byte[] block;
        int secCnt = mif.blockToSector(blockCnt);
        System.out.println("readBlock for block " + blockCnt + " is in sector " + secCnt);
        try {
            mif.authenticateSectorWithKeyB(secCnt, key);
            block = mif.readBlock(blockCnt);
        } catch (IOException e) {
            //throw new RuntimeException(e);
            System.out.println("RuntimeException: " + e.getMessage());
            return null;
        }
        return block;
    }

    /**
     * read mifare classic card sector by sector
     *
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
        Log.d(TAG, "");
        Log.d(TAG, "readMifareSector " + secCnt);
        try {
            if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                keyABytes = MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY.clone();
                Log.d(TAG, "Auth success with A KEY_MIFARE_APPLICATION_DIRECTORY");
                isAuthenticated = true;
                // there are 3 default keys available
                // MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY: a0a1a2a3a4a5
                // MifareClassic.KEY_DEFAULT:                      ffffffffffff
                // MifareClassic.KEY_NFC_FORUM:                    d3f7d3f7d3f7
            } else if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_DEFAULT)) {
                keyABytes = MifareClassic.KEY_DEFAULT.clone();
                Log.d(TAG, "Auth success with A KEY_DEFAULT");
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_NFC_FORUM)) {
                keyABytes = MifareClassic.KEY_NFC_FORUM.clone();
                Log.d(TAG, "Auth success with A KEY_NFC_FORUM");
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_DEFAULT)) {
                keyBBytes = MifareClassic.KEY_DEFAULT.clone();
                Log.d(TAG, "Auth success with B KEY_DEFAULT");
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                keyBBytes = MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY.clone();
                isAuthenticated = true;
                Log.d(TAG, "Auth success with B KEY_MIFARE_APPLICATION_DIRECTORY");
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_NFC_FORUM)) {
                keyBBytes = MifareClassic.KEY_NFC_FORUM;
                Log.d(TAG, "Auth success with B KEY_NFC_FORUM");
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyA(secCnt, hexStringToByteArray("4D57414C5648"))) {
                keyABytes = hexStringToByteArray("4D57414C5648");
                Log.d(TAG, "Auth success with A Crowne Plaza key");
                isAuthenticated = true;
                //4D57414C5648
            } else {
                //return null;
                Log.d(TAG, "NO Auth success");
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
            Log.e(TAG, "Sector " + secCnt + " IOException: " + e.getMessage());
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

    /**
     * shows a progress bar as long as the reading lasts
     *
     * @param isVisible
     */

    private void setLoadingLayoutVisibility(boolean isVisible) {
        getActivity().runOnUiThread(() -> {
            if (isVisible) {
                loadingLayout.setVisibility(View.VISIBLE);
            } else {
                loadingLayout.setVisibility(View.GONE);
            }
        });
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