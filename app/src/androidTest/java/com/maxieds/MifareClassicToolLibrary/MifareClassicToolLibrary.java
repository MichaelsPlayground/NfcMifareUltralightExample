/*
This program (MifareClassicToolLibrary) is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

The complete license provided with source distributions of this library is
available at the following link:
https://github.com/maxieds/MifareClassicToolLibrary

Copyright by Maxie Schmidt and Gerhard Klostermeier.
*/

package com.maxieds.MifareClassicToolLibrary;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import de.androidcrypto.mifareclassictoollibrarydemosdk33.R;

public class MifareClassicToolLibrary {

     private static final String TAG = MifareClassicToolLibrary.class.getSimpleName();
     private static final String PACKAGE = "com.maxieds.MifareClassicToolLibrary";

     public static int RETRIES_TO_AUTH_KEYAB = 1;
     public static boolean AUTORECONNECT = false;

     private static MifareClassicDataInterface localMFCDataIface = null;

     public static MifareClassicDataInterface GetDataInterface() { return localMFCDataIface; }

     // todo BuildConfig
     /*
     public static String GetLibraryVersion() {
          return String.format(Locale.US, "v%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
     }
      */

     public static boolean Initialized() {
          return localMFCDataIface != null;
     }

     public static Context GetApplicationContext() {
          if(!Initialized()) {
               return null;
          }
          return localMFCDataIface.GetApplicationContext();
     }

     public static boolean InitializeLibrary(MifareClassicDataInterface mfcDataIface) {
          localMFCDataIface = mfcDataIface;
          return Initialized();
     }

     private static NfcAdapter GetContextNFCAdapter() {
          if(!Initialized()) {
               return null;
          }
          Context appContext = localMFCDataIface.GetApplicationContext();
          NfcManager nfcManager = (NfcManager) appContext.getSystemService(Context.NFC_SERVICE);
          return nfcManager.getDefaultAdapter();
     }

     public static boolean CheckNFCEnabled(boolean promptUser) {
          if(!MifareClassicToolLibrary.Initialized()) {
               return false;
          }
          NfcAdapter nfcAdapter = GetContextNFCAdapter();
          if(nfcAdapter != null && nfcAdapter.isEnabled()) {
               return true;
          }
          else if(nfcAdapter != null && promptUser) {
               Intent startNFCIntent;
               if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    startNFCIntent = new Intent(Settings.ACTION_NFC_SETTINGS);
               }
               else {
                    startNFCIntent = new Intent(Settings.ACTION_NFC_SETTINGS);
               }
               startNFCIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
               GetApplicationContext().startActivity(startNFCIntent);
               return CheckNFCEnabled(false);
          }
          return false;
     }

     public static boolean CheckNFCEnabled() {
          return CheckNFCEnabled(true);
     }

     public static boolean CheckPhoneMFCSupport() {
          // Check if ther is any NFC hardware at all.
          if(!Initialized() || NfcAdapter.getDefaultAdapter(localMFCDataIface.GetApplicationContext()) == null) {
               return false;
          }
          // Check if there is the NFC device "bcm2079x-i2c".
          // Chips by Broadcom don't support MIFARE Classic.
          // This could fail because on a lot of devices apps don't have
          // the sufficient permissions.
          // Another exception:
          // The Lenovo P2 has a device at "/dev/bcm2079x-i2c" but is still
          // able of reading/writing MIFARE Classic tags. I don't know why...
          // https://github.com/ikarus23/MifareClassicTool/issues/152
          boolean isLenovoP2 = Build.MANUFACTURER.equals("LENOVO") && Build.MODEL.equals("Lenovo P2a42");
          File nfcDevice = new File("/dev/bcm2079x-i2c");
          if (!isLenovoP2 && nfcDevice.exists()) {
               return false;
          }
          // Check if there is the NFC device "pn544".
          // The PN544 NFC chip is manufactured by NXP.
          // Chips by NXP support MIFARE Classic.
          nfcDevice = new File("/dev/pn544");
          if (nfcDevice.exists()) {
               return true;
          }
          // Check if there are NFC libs with "brcm" in their names.
          // "brcm" libs are for devices with Broadcom chips. Broadcom chips
          // don't support MIFARE Classic.
          File libsFolder = new File("/system/lib");
          File[] libs = libsFolder.listFiles();
          for (File lib : libs) {
               if (lib.isFile() && lib.getName().startsWith("libnfc") && lib.getName().contains("brcm")) {
                    return false;
               }
          }
          return true;
     }

     public static boolean StartLiveTagScanning(Activity targetActivity) {
          NfcAdapter nfcAdapter = GetContextNFCAdapter();
          if(nfcAdapter == null || !CheckPhoneMFCSupport() ||
               !CheckNFCEnabled(true)) {
               return false;
          }
          Intent startDispatchIntent = new Intent(targetActivity, targetActivity.getClass());
          startDispatchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
          PendingIntent startPendingIntent = PendingIntent.getActivity(targetActivity, 0, startDispatchIntent, 0);
          String[][] enableFlags = new String[][] { new String[] { NfcA.class.getName(), MifareClassic.class.getName()} };
          nfcAdapter.enableForegroundDispatch(targetActivity, startPendingIntent, null, enableFlags);
          return true;
     }

     public static boolean StopLiveTagScanning(Activity targetActivity) {
          NfcAdapter nfcAdapter = GetContextNFCAdapter();
          if(nfcAdapter != null && nfcAdapter.isEnabled()) {
               nfcAdapter.disableForegroundDispatch(targetActivity);
               return true;
          }
          return false;
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

     public static byte[][] GetAccessBitsArray(byte[] accessBytes) {
          if(accessBytes.length != 4) {
               return null;
          }
          return ACBytesToACMatrix(accessBytes);
     }

     public static String GetAccessConditionsDescription(byte[][] sectorAccessBits, int blockIndex, boolean isSectorTrailer) {
          if(sectorAccessBits == null || blockIndex < 0 || blockIndex >= sectorAccessBits[0].length) {
               return "";
          }
          int accessBitsNumber = (sectorAccessBits[0][blockIndex] << 2) | (sectorAccessBits[1][blockIndex] << 1) | sectorAccessBits[0][blockIndex];
          String resAccessCondsPrefix = isSectorTrailer ? "ac_sector_trailer_" : "ac_data_block_";
          String accessCondsResIdStr = resAccessCondsPrefix + accessBitsNumber;
          Context appContext = localMFCDataIface.GetApplicationContext();
          try {
               int accessCondsResId = R.string.class.getField(accessCondsResIdStr).getInt(null);
               return appContext.getResources().getString(accessCondsResId);
          } catch(Exception nsfe) {
               return "";
          }
     }

     private static HashMap<String, MifareClassicTag> libraryTagHashMap = new HashMap<>();
     private static Stack<MifareClassicTag> libraryTagStack = new Stack<>();
     private static int libraryTagStackSize = 0;

     public static int GetLibraryTagStackSize() {
          return libraryTagStackSize;
     }

     public static boolean AddToLibraryTagStack(MifareClassicTag mfcTagData) {
          if(mfcTagData == null) {
               return false;
          }
          else if(libraryTagHashMap.get(mfcTagData.GetTagUID()) != null) {
               return false;
          }
          libraryTagHashMap.put(mfcTagData.GetTagUID(), mfcTagData);
          libraryTagStack.push(mfcTagData);
          libraryTagStackSize++;
          localMFCDataIface.RegisterNewIntent(new Intent(GetApplicationContext(), MifareClassicToolLibrary.class));
          return true;
     }

     public static MifareClassicTag PopFromLibraryTagStack() {
          if(libraryTagStackSize == 0) {
               return null;
          }
          MifareClassicTag mfcTagData = libraryTagStack.pop();
          libraryTagHashMap.remove(mfcTagData.GetTagUID());
          libraryTagStackSize--;
          return mfcTagData;
     }

     public static boolean ProcessNewTagFound(Tag nfcTag, boolean displayGUIProgressBar) throws MifareClassicLibraryException {
          if(nfcTag == null || !Initialized()) {
               return false;
          }
          MifareClassicTag mfcTagData = MifareClassicTag.Decode(nfcTag, displayGUIProgressBar);
          if(mfcTagData != null) {
               return AddToLibraryTagStack(mfcTagData);
          }
          return false;
     }

     private static String[] standardKeys = null;

     public static boolean LoadStandardKeySets(boolean useExtendedKeys) {
          if(!Initialized()) {
               return true;
          }
          else if(standardKeys != null) {
               standardKeys = null;
          }
          String[] keyFiles;
          if(useExtendedKeys) {
               keyFiles = new String[] { "mct_standard_keys", "mct_extended_keys" };
          }
          else {
               keyFiles = new String[] { "mct_standard_keys" };
          }
          List<String> keysList = new ArrayList<String>();
          Context appContext = localMFCDataIface.GetApplicationContext();
          for(int kidx = 0; kidx < keyFiles.length; kidx++) {
               String resFilePath = keyFiles[kidx];
               int fileRes = appContext.getResources().getIdentifier(resFilePath, "raw", appContext.getPackageName());
               InputStream rawFileStream = appContext.getResources().openRawResource(fileRes);
               BufferedReader textFileReader = new BufferedReader(new InputStreamReader(rawFileStream));
               try {
                    while (true) {
                         String textLine = textFileReader.readLine();
                         if (textLine == null) {
                              break;
                         }
                         else if(textLine.length() == 0) {
                              continue;
                         }
                         else if(textLine.charAt(0) != '#' && textLine.charAt(0) != '\n') {
                              keysList.add(textLine);
                         }
                    }
                    textFileReader.close();
                    rawFileStream.close();
               } catch(IOException ioe) {
                    ioe.printStackTrace();
                    return false;
               }
          }
          standardKeys = new String[keysList.size()];
          for(int s = 0; s < keysList.size(); s++) {
               standardKeys[s] = keysList.get(s);
          }
          return true;
     }

     public static int GetStandardKeyCount() {
          if(standardKeys != null) {
               return standardKeys.length;
          }
          return 0;
     }

     public static String GetStandardKey(int kidx) {
          if(kidx < 0 || kidx >= GetStandardKeyCount()) {
               return null;
          }
          return standardKeys[kidx];
     }

     public static String[] GetStandardAllKeys() {
          return standardKeys;
     }

     private static int[] PROGRESS_BAR_POSITIONS = new int[] {
          R.drawable.statusbar_0,
          R.drawable.statusbar_1,
          R.drawable.statusbar_2,
          R.drawable.statusbar_3,
          R.drawable.statusbar_4,
          R.drawable.statusbar_5,
          R.drawable.statusbar_6,
          R.drawable.statusbar_7,
          R.drawable.statusbar_8,
     };

     private static final int STATUS_TOAST_DISPLAY_TIME = Toast.LENGTH_LONG;
     private static boolean toastsDismissed = true;
     private static int progressBarPos, progressBarTotal;
     private static String progressBarSliderName;
     private static Toast progressBarToast = null;
     private static Handler progressBarDisplayHandler = new Handler();
     private static Runnable progressBarDisplayRunnable = new Runnable() {
          public void run() {
               if (!toastsDismissed && progressBarToast != null) {
                    DisplayProgressBar(progressBarSliderName, progressBarPos, progressBarTotal);
               }
          }
     };

     public static void DisplayProgressBar(String thingsName, int curPos, int totalPos) {
          if(!thingsName.equals(progressBarSliderName) || curPos != progressBarPos || totalPos != progressBarTotal) {
               if(!toastsDismissed) {
                    progressBarDisplayHandler.removeCallbacks(progressBarDisplayRunnable);
               }
          }
          progressBarSliderName = thingsName;
          progressBarPos = curPos;
          progressBarTotal = totalPos;
          final int statusBarMarkerIdx = Math.min((int) ((curPos - 1) * PROGRESS_BAR_POSITIONS.length / totalPos),
               PROGRESS_BAR_POSITIONS.length - 1);
          final String statusBarMsg = String.format(Locale.US, "%s % 3d / % 3d (% .2g %%)",
               thingsName, curPos, totalPos, (float) curPos / totalPos * 100.0);
          final Activity mainAppActivity = localMFCDataIface.GetApplicationActivity();
          mainAppActivity.runOnUiThread(new Runnable() {
               @Override
               public void run() {
                    progressBarToast = Toast.makeText(localMFCDataIface.GetApplicationActivity(), statusBarMsg, STATUS_TOAST_DISPLAY_TIME);
                    progressBarToast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                    LayoutInflater layoutInflater = mainAppActivity.getLayoutInflater();
                    View toastProgressView = layoutInflater.inflate(R.layout.status_bar_layout, null);
                    Drawable statusBarMarkerImage = mainAppActivity.getResources().getDrawable(PROGRESS_BAR_POSITIONS[statusBarMarkerIdx]);
                    ((ImageView) toastProgressView.findViewById(R.id.progressBarImageMarker)).setImageDrawable(statusBarMarkerImage);
                    ((TextView) toastProgressView.findViewById(R.id.progressBarText)).setText(statusBarMsg);
                    progressBarToast.setView(toastProgressView);
                    if(!toastsDismissed) {
                         progressBarDisplayHandler.postDelayed(progressBarDisplayRunnable, STATUS_TOAST_DISPLAY_TIME + 1000);
                    }
                    progressBarToast.show();
               }
          });
     }

     public static void EnableProgressBarDisplay(boolean enableRedisplay) {
          toastsDismissed = !enableRedisplay;
          if(toastsDismissed) {
               progressBarDisplayHandler.removeCallbacks(progressBarDisplayRunnable);
          }
     }

}