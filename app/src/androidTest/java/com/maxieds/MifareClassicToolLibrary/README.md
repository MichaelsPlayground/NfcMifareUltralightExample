# MCTLibraryDemo Application

## License and credits -- GPLv3

This original sources for the [MFCTool application](www.icaria.de/mct/) were
originally developed by
[**Gerhard Klostermeier**](https://github.com/ikarus23) in cooperation with SySS GmbH
([www.syss.de](https://www.syss.de/)) and Aalen
University ([www.htw-aalen.de](http://www.htw-aalen.de/)) in 2012/2013.
It is free software and licensed under the
[GNU General Public License v3.0 (GPLv3)](https://www.gnu.org/licenses/gpl-3.0.txt):

>This program (MifareClassicToolLibrary) is free software: you can redistribute it and/or modify
>it under the terms of the GNU General Public License as published by
>the Free Software Foundation, either version 3 of the License, or
>(at your option) any later version.
>
>This program is distributed in the hope that it will be useful,
>but WITHOUT ANY WARRANTY; without even the implied warranty of
>MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
>GNU General Public License for more details.

## Documentation for the MCTLibrary

Note that for the most part this demo application should serve as documentation
for how to use the library in client applications. Other main documentation is
provided in this README below. There is a user-friendly open source
client application that works on Android
[available here](https://github.com/maxieds/MCTLibraryDemo).

### Descriptions of files in the library

The following files form the core implementation of the library:
* **MCTUtils.java:** Utility functions from the MCT app. Includes operations for 
converting arrays of bytes to hex strings and vice versa. 
* **MifareClassicDataInterface.java:** The interface that must be implemented by 
client applications using the library. See below for description. 
* **MifareClassicLibraryException.java:** Custom exception class for exceptions 
thrown by the library. See the top of the file for the types of custom 
exceptions supported (and to add new ones). 
* **MifareClassicTag.java:** Implementation, storage and parsing routines for 
new MFC1K tags. Also includes many undocumented (except for in the source code) 
helper functions. Much of this code was derived from the MCT reader functions.  
* **MifareClassicToolLibrary.java:** The library class which includes helper 
functions for scanning tags and reading in the extended key sets.
* **MifareClassicUtils.java:** MFC1K tag helper functions. Includes routines for 
writing blank tags with Android and for loading a new tag from a dump image file. 

### Key library settings

The following settings are used to configure the library:
```
public class MifareClassicToolLibrary {
     public static int RETRIES_TO_AUTH_KEYAB = 1;
     public static boolean AUTORECONNECT = false;
     // ...
}
```

### Data interface implementations

All client applications (activities) using the library need to provide an 
implementation of the following interface:
```
public interface MifareClassicDataInterface {

     void RegisterNewIntent(Intent mfcIntent);
     Context GetApplicationContext();
     Activity GetApplicationActivity();

     void PostTagScanKeyMapProgress(int position, int total);
     void PostTagScanSectorReadProgress(int position, int total);

}
```
Before using the library, the client application implementing the above data 
interface should make a call to the next library function:
```
public class MifareClassicToolLibrary {
     public static boolean InitializeLibrary(MifareClassicDataInterface mfcDataIface);
}
```

### How to read a new MFC1K tag

#### Enabling tag scanning in the application

Once the library is inititalized as in the last section by calling 
``MifareClassicToolLibrary.InitializeLibrary``, you can enable automatic tag 
scanning by implementing the following in your client activity:
```
// call this to start live tag scanning:
MifareClassicToolLibrary.StartLiveTagScanning(this);

// implement these functions in the client activity:
@Override
public void onResume() {
     super.onResume();
     if(currentlyTagScanning) {
          MifareClassicToolLibrary.StartLiveTagScanning(this);
     }
}

@Override
public void onPause() {
     if(currentlyTagScanning) {
          MifareClassicToolLibrary.StopLiveTagScanning(this);
     }
     super.onPause();
}
```
You will also need to add the following to the **activity** tag in the client application's 
*Manifest* file:
```
           <intent-filter android:priority="1000">
                <action android:name="android.nfc.action.TECH_DISCOVERED" />
                <action android:name="android.nfc.action.TAG_DISCOVERED" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>

            <meta-data
                android:name="android.nfc.action.TECH_DISCOVERED"
                android:resource="@xml/nfc_tech_filter" />
            <meta-data
                android:name="android.nfc.action.TAG_DISCOVERED"
                android:resource="@xml/nfc_tech_filter" />
```
The *xml/nfc_tech_filter.xml* file is found in the *res/* directory and contains the 
following information:
```
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
    <tech-list>
        <tech>android.nfc.tech.MifareClassic</tech>
        <tech>android.nfc.tech.NfcA</tech>
    </tech-list>
</resources>
```

#### Processing a new Tag

Once you have enabled live tag scanning in the client activity (and added the intents to 
process new tags to the application *Manifest* file), newly found NFC tags will be registered 
via *Intents* to the client activity, which should implement a handler that looks something 
like the following code snippet:
```
     @Override
     protected void onNewIntent(Intent intent) {
          if(intent == null || intent.getAction() == null) {
               return;
          }
          if(intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED) ||
             intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
               final Tag nfcTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
               if(MifareClassicTag.CheckMifareClassicSupport(nfcTag) != 0) {
                    DisplayToastMessage("The discovered NFC device is not a Mifare Classic tag.");
               }
               else {
                    MifareClassicTag mfcTag = MifareClassicTag.Decode(nfcTag, keyStringArray, displayProgressBar);
               }
          }
          // process other intents
     }
``` 
The ``MifareClassicTag.Decode`` function has the next specification:
```
public static MifareClassicTag Decode(Tag nfcTag, String[] keyData, boolean displayGUIProgressBar) throws MifareClassicLibraryException
```
The following utility functions are available in the ``MifareClassicTag`` class after scanning 
a new tag:
```
     public String[] GetMFCDumpImageData();
     public String GetTagType();
     public int GetSectorCount();
     public int GetBlockCount();
     public int GetBytesPerBlock();
     public int GetBlockCountInSector();
     public int GetSize();
     public List<MFCSector> GetFailedSectors();
     public String GetManufacturer();
     public String GetTagUID();
     public int GetTagUIDSize();
     public String GetATQA();
     public String GetSAK();
     public String GetATS();
     public long GetTotalReadTime();
     public MFCSector GetSectorByIndex(int index);
     public boolean GetSectorReadStatus(int index);
```
The subclass ``MifareClassicTag.MFCSector`` contains the following public fields:
```
     public static class MFCSector {
          public int sectorAddress;
          public int sectorSize;
          public int sectorBlockCount;
          public int sectorFirstBlock;
          public int sectorBytesPerBlock;
          public String[] sectorBlockData;
          // ...
     }
```

### Other useful functions

```
public class MifareClassicToolLibrary {
     // checking for NFC / MFC support on the target phone:
     public static boolean CheckNFCEnabled(boolean promptUser);
     public static boolean CheckPhoneMFCSupport();
     
     // access conditions processing:
     public static byte[][] GetAccessBitsArray(byte[] accessBytes);
     public static String GetAccessConditionsDescription(byte[][] sectorAccessBits, int blockIndex, boolean isSectorTrailer);
     
     // loading standard and extended key sets (as in MCT app):
     public static boolean LoadStandardKeySets(boolean useExtendedKeys);
     public static int GetStandardKeyCount();
     public static String GetStandardKey(int kidx);
     public static String[] GetStandardAllKeys();
     
     // displaying a progress bar in the client activity:
     public static void DisplayProgressBar(String thingsName, int curPos, int totalPos);
}
```


