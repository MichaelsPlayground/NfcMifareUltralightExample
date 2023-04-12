package de.androidcrypto.nfcmifareclassicexample;

import static de.androidcrypto.nfcmifareclassicexample.Utils.doVibrate;
import static de.androidcrypto.nfcmifareclassicexample.Utils.playSinglePing;

import android.content.Intent;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;

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

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    com.google.android.material.textfield.TextInputLayout inputField1Decoration, inputField2Decoration, inputField3Decoration;
    com.google.android.material.textfield.TextInputEditText typeDescription, inputField1, inputField2, inputField3, resultNfcWriting;
    SwitchMaterial addTimestampToData;
    AutoCompleteTextView autoCompleteTextView;
    com.google.android.material.textfield.TextInputLayout dataToSendLayout;
    com.google.android.material.textfield.TextInputEditText dataToSend;
    //private final String DEFAULT_URL = "https://www.google.de/maps/@34.7967917,-111.765671,3a,66.6y,15.7h,102.19t/data=!3m6!1e1!3m4!1sFV61wUEyLNwFi6zHHaKMcg!2e0!7i16384!8i8192";
    private final String DEFAULT_URL = "https://github.com/AndroidCrypto?tab=repositories";
    private NfcAdapter mNfcAdapter;

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

        typeDescription = getView().findViewById(R.id.etMainTypeDescription);
        inputField1 = getView().findViewById(R.id.etMainInputline1);
        inputField1Decoration = getView().findViewById(R.id.etMainInputline1Decoration);
        inputField2 = getView().findViewById(R.id.etMainInputline2);
        inputField2Decoration = getView().findViewById(R.id.etMainInputline2Decoration);
        inputField3 = getView().findViewById(R.id.etMainInputline3);
        inputField3Decoration = getView().findViewById(R.id.etMainInputline3Decoration);
        resultNfcWriting = getView().findViewById(R.id.etMainResult);
        addTimestampToData = getView().findViewById(R.id.swMainAddTimestampSwitch);

        String[] type = new String[]{
                "Text", "URI", "Telephone number", "Coordinate", "Coordinate userinfo", "StreetView",
                "Address", "Google navigation", "Email", "Application"};
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                getView().getContext(),
                R.layout.drop_down_item,
                type);

        autoCompleteTextView = getView().findViewById(R.id.ndef_type);
        autoCompleteTextView.setAdapter(arrayAdapter);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());

        hideAllInputFields();

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String choiceString = autoCompleteTextView.getText().toString();
                //Toast.makeText(MainActivity.this, autoCompleteTextView.getText().toString(), Toast.LENGTH_SHORT).show();
                switch (choiceString) {
                    case "Text": {
                        inputSchemeText();
                        break;
                    }
                    case "URI": {
                        inputSchemeUri();
                        break;
                    }
                    case "StreetView": {
                        inputSchemeStreetview();
                        break;
                    }
                    case "Email": {
                        inputSchemeEmail();
                        break;
                    }
                    case "Telephone number": {
                        inputSchemeTelephoneNumber();
                        break;
                    }
                    case "Coordinate": {
                        inputSchemeCoordinate();
                        break;
                    }
                    case "Coordinate userinfo": {
                        inputSchemeCoordinateUserinfo();
                        break;
                    }
                    case "Address": {
                        inputSchemeAddress();
                        break;
                    }
                    case "Google navigation": {
                        inputSchemeGoogleNavigation();
                        break;
                    }
                    case "Application": {
                        inputSchemeApplication();
                        break;
                    }
                    default: {
                        hideAllInputFields();
                        break;
                    }
                }
            }
        });

    }

    private void hideAllInputFields() {
        typeDescription.setVisibility(View.GONE);
        inputField1Decoration.setVisibility(View.GONE);
        inputField2Decoration.setVisibility(View.GONE);
        inputField3Decoration.setVisibility(View.GONE);
        addTimestampToData.setVisibility(View.GONE);
        resultNfcWriting.setVisibility(View.GONE);
    }

    private void inputSchemeText() {
        hideAllInputFields();
        String description = "writes an NDEF record with a line of text";
        typeDescription.setText(description);
        inputField1Decoration.setHint("Enter a text line");
        typeDescription.setVisibility(View.VISIBLE);
        inputField1Decoration.setVisibility(View.VISIBLE);
        addTimestampToData.setVisibility(View.VISIBLE);
        resultNfcWriting.setVisibility(View.VISIBLE);
        inputField1.setText("sample text");
    }

    private void inputSchemeUri() {
        hideAllInputFields();
        String description = "writes an NDEF record with an URI";
        typeDescription.setText(description);
        inputField1Decoration.setHint("Enter an URI including https://");
        typeDescription.setVisibility(View.VISIBLE);
        inputField1Decoration.setVisibility(View.VISIBLE);
        resultNfcWriting.setVisibility(View.VISIBLE);
        inputField1.setText("https://");
    }

    private void inputSchemeTelephoneNumber() {
        hideAllInputFields();
        String description = "writes an NDEF record with a telephone number";
        typeDescription.setText(description);
        inputField1Decoration.setHint("Enter a telephone number");
        typeDescription.setVisibility(View.VISIBLE);
        inputField1Decoration.setVisibility(View.VISIBLE);
        resultNfcWriting.setVisibility(View.VISIBLE);
        inputField1.setText("0049201234567890");
    }

    private void inputSchemeEmail() {
        hideAllInputFields();
        String description = "writes an NDEF record with a complete Email";
        typeDescription.setText(description);
        inputField1Decoration.setHint("Enter an email address for the recipient");
        inputField2Decoration.setHint("Enter the email subject");
        inputField3Decoration.setHint("Enter the email body");
        typeDescription.setVisibility(View.VISIBLE);
        inputField1Decoration.setVisibility(View.VISIBLE);
        inputField2Decoration.setVisibility(View.VISIBLE);
        inputField3Decoration.setVisibility(View.VISIBLE);
        addTimestampToData.setVisibility(View.VISIBLE);
        resultNfcWriting.setVisibility(View.VISIBLE);
        inputField1.setText("androidcrypto@gmx.de");
        inputField2.setText("sample email subject");
        inputField3.setText("Hello AndroidCrypto,\nThis is a sample mail.");
    }

    private void inputSchemeStreetview() {
        hideAllInputFields();
        String description = "writes an NDEF record with a Google streetview link";
        typeDescription.setText(description);
        inputField1Decoration.setHint("Enter coordinates (comma separated)");
        typeDescription.setVisibility(View.VISIBLE);
        inputField1Decoration.setVisibility(View.VISIBLE);
        resultNfcWriting.setVisibility(View.VISIBLE);
        inputField1.setText("34.792345,-111.762531");
    }

    private void inputSchemeCoordinate() {
        hideAllInputFields();
        String description = "writes an NDEF record with a coordinate";
        typeDescription.setText(description);
        inputField1Decoration.setHint("Enter coordinates (comma separated)");
        typeDescription.setVisibility(View.VISIBLE);
        inputField1Decoration.setVisibility(View.VISIBLE);
        resultNfcWriting.setVisibility(View.VISIBLE);
        inputField1.setText("34.792345,-111.762531");
    }

    private void inputSchemeCoordinateUserinfo() {
        hideAllInputFields();
        String description = "writes an NDEF record with a coordinate and user information";
        typeDescription.setText(description);
        inputField1Decoration.setHint("Enter coordinates (comma separated)");
        inputField2Decoration.setHint("Enter the user information");
        typeDescription.setVisibility(View.VISIBLE);
        inputField1Decoration.setVisibility(View.VISIBLE);
        inputField2Decoration.setVisibility(View.VISIBLE);
        resultNfcWriting.setVisibility(View.VISIBLE);
        inputField1.setText("34.792345,-111.762531");
        inputField2.setText("Bell Rock Sedona view point");
    }

    private void inputSchemeAddress() {
        hideAllInputFields();
        String description = "writes an NDEF record with an address for Google maps";
        typeDescription.setText(description);
        inputField1Decoration.setHint("Enter an street with (optional) house number");
        inputField2Decoration.setHint("Enter the zip code");
        inputField3Decoration.setHint("Enter the city");
        typeDescription.setVisibility(View.VISIBLE);
        inputField1Decoration.setVisibility(View.VISIBLE);
        inputField2Decoration.setVisibility(View.VISIBLE);
        inputField3Decoration.setVisibility(View.VISIBLE);
        resultNfcWriting.setVisibility(View.VISIBLE);
        inputField1.setText("Selmastr 5");
        inputField2.setText("45127");
        inputField3.setText("Essen");
    }

    private void inputSchemeGoogleNavigation() {
        hideAllInputFields();
        String description = "writes an NDEF record with a target address for Google navigation";
        typeDescription.setText(description);
        inputField1Decoration.setHint("Enter an street with (optional) house number");
        inputField2Decoration.setHint("Enter the zip code");
        inputField3Decoration.setHint("Enter the city");
        typeDescription.setVisibility(View.VISIBLE);
        inputField1Decoration.setVisibility(View.VISIBLE);
        inputField2Decoration.setVisibility(View.VISIBLE);
        inputField3Decoration.setVisibility(View.VISIBLE);
        resultNfcWriting.setVisibility(View.VISIBLE);
        inputField1.setText("Selmastr 5");
        inputField2.setText("45127");
        inputField3.setText("Essen");
    }


    private void inputSchemeApplication() {
        hideAllInputFields();
        String description = "writes an NDEF record with an application to start";
        typeDescription.setText(description);
        inputField1Decoration.setHint("Enter a packet name");
        typeDescription.setVisibility(View.VISIBLE);
        inputField1Decoration.setVisibility(View.VISIBLE);
        resultNfcWriting.setVisibility(View.VISIBLE);
        inputField1.setText("com.inkwired.droidinfo");
    }

    /**
     * section for NFC
     */

    @Override
    public void onTagDiscovered(Tag tag) {
// Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        Ndef mNdef = Ndef.get(tag);

        // Check that it is an Ndef capable card
        if (mNdef != null) {

            NdefMessage ndefMessage;
            NdefRecord ndefRecord1;
            // nfc ndef writing depends on the type
            String choiceString = autoCompleteTextView.getText().toString();
            String inputData1 = inputField1.getText().toString();
            boolean addTimestamp = addTimestampToData.isChecked();
            switch (choiceString) {
                case "Text": {
                    String data = inputData1;
                    if (addTimestamp) data = data + " " + Utils.getTimestamp();
                    ndefRecord1 = NdefRecord.createTextRecord("en", data);
                    ndefMessage = new NdefMessage(ndefRecord1);
                    break;
                }
                case "URI": {
                    String data = inputData1;
                    ndefRecord1 = NdefRecord.createUri(data);
                    ndefMessage = new NdefMessage(ndefRecord1);
                    break;
                }
                case "Telephone number": {
                    String data = inputData1;
                    String completeData = "tel:" + data;
                    ndefRecord1 = NdefRecord.createUri(completeData);
                    ndefMessage = new NdefMessage(ndefRecord1);
                    break;
                }
                case "Email": {
                    String data1 = inputData1;
                    String data2 = inputField2.getText().toString();
                    String data3 = inputField3.getText().toString();
                    String completeData = "mailto:" + Uri.encode(data1) + "?subject=" +
                            Uri.encode(data2);
                    if (addTimestamp) completeData = completeData + Uri.encode(" " + Utils.getTimestamp());
                    completeData = completeData + "&body=" + Uri.encode(data3);
                    ndefRecord1 = NdefRecord.createUri(completeData);
                    ndefMessage = new NdefMessage(ndefRecord1);
                    break;
                }
                case "StreetView": {
                    String data = inputData1;
                    String completeData = "google.streetview:cbll=" + data;
                    ndefRecord1 = NdefRecord.createUri(completeData);
                    ndefMessage = new NdefMessage(ndefRecord1);
                    break;
                }
                case "Coordinate": {
                    String data = inputData1;
                    String completeData = "geo:" + data;
                    ndefRecord1 = NdefRecord.createUri(completeData);
                    ndefMessage = new NdefMessage(ndefRecord1);
                    break;
                }
                case "Coordinate userinfo": {
                    String data1 = inputData1;
                    String data2 = Uri.encode(inputField2.getText().toString());
                    String completeData = "geo:0,0?q=" + data1 + "(" + data2 + ")";
                    ndefRecord1 = NdefRecord.createUri(completeData);
                    ndefMessage = new NdefMessage(ndefRecord1);
                    break;
                }
                case "Address": {
                    String data1 = Uri.encode(inputData1);
                    String data2 = Uri.encode(inputField2.getText().toString());
                    String data3 = Uri.encode(inputField3.getText().toString());
                    String completeData = "geo:0,0?q=" + data1 + "+" + data2 + "+" + data3;
                    ndefRecord1 = NdefRecord.createUri(completeData);
                    ndefMessage = new NdefMessage(ndefRecord1);
                    break;
                }
                case "Google navigation": {
                    String data1 = Uri.encode(inputData1);
                    String data2 = Uri.encode(inputField2.getText().toString());
                    String data3 = Uri.encode(inputField3.getText().toString());
                    String completeData = "google.navigation:q=" + data1 + "+" + data2 + "+" + data3;
                    ndefRecord1 = NdefRecord.createUri(completeData);
                    ndefMessage = new NdefMessage(ndefRecord1);
                    break;
                }
                case "Application": {
                    String data = inputData1;
                    ndefRecord1 = NdefRecord.createApplicationRecord(data);
                    ndefMessage = new NdefMessage(ndefRecord1);
                    break;
                }

                default:
                    throw new IllegalStateException("Unexpected value: " + choiceString);
            }

            // the tag is written here
            try {
                mNdef.connect();

                // check that the tag is writable
                if (!mNdef.isWritable()) {
                    showMessage("NFC tag is not writable");
                    return;
                }

                // check that the tag has sufficiant memory to write the ndef message
                int ndefMaxSize = mNdef.getMaxSize();
                int messageSize = ndefMessage.toByteArray().length;
                if (messageSize > ndefMaxSize) {
                    showMessage("Message is too large to write on NFC tag");
                    return;
                }

                mNdef.writeNdefMessage(ndefMessage);
                // Success if got to here
                showMessage("write to NFC success, total message size is " + messageSize);
            } catch (FormatException e) {
                showMessage("FormatException: " + e.getMessage());
                // if the NDEF Message to write is malformed
            } catch (TagLostException e) {
                showMessage("TagLostException: " + e.getMessage());
                // Tag went out of range before operations were complete
            } catch (IOException e) {
                // if there is an I/O failure, or the operation is cancelled
                showMessage("IOException: " + e.getMessage() + " I'm trying to format the tag... please try again");
                // try to format the tag
                formatNdef(tag);
            } finally {
                // Be nice and try and close the tag to
                // Disable I/O operations to the tag from this TagTechnology object, and release resources.
                try {
                    mNdef.close();
                } catch (IOException e) {
                    // if there is an I/O failure, or the operation is cancelled
                    showMessage("IOException on close: " + e.getMessage());
                }
            }
            doVibrate(getActivity());
            playSinglePing(getContext());
        } else {
            showMessage("mNdef is null, not an NDEF formatted tag, try to format the tag");
            // trying to format the tag
            formatNdef(tag);
        }
    }

    private void formatNdef(Tag tag) {
        // trying to format the tag
        NdefFormatable format = NdefFormatable.get(tag);
        if(format != null){
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
        }
        else {
            showMessage("Tag not formattable or already formatted to Ndef");
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